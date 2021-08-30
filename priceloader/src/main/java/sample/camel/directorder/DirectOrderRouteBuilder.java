package sample.camel.directorder;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategyBeanAdapter;
import org.apache.camel.processor.aggregate.DefaultAggregateController;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.direct;

public class DirectOrderRouteBuilder extends RouteBuilder {

    public static final String KEY = "Counter";
    private final AtomicInteger timerMessageId = new AtomicInteger(1);

    private final Integer psId;

    public DirectOrderRouteBuilder(Integer psId) {
        this.psId = psId;
    }

    @Override
    public void configure() throws Exception {
        from("timer:cTimer?period=1&repeatCount=2")
                .process(exchange -> {
                    final Integer oldBody = timerMessageId.getAndIncrement();
                    log.info("Send message: {}", oldBody);
                    exchange.getIn().setHeader(KEY, oldBody);
                    final List<Integer> list = IntStream.range(1, 10).boxed().collect(Collectors.toList());
                    exchange.getIn().setBody(list);
                })
                .to(direct("direct-order-file-process-route:" + psId).advanced().synchronous(true));

        final DefaultAggregateController aggregateController = new DefaultAggregateController();
        aggregateController.forceCompletionOfAllGroups();


        AggregationStrategyBeanAdapter myStrategy = new AggregationStrategyBeanAdapter(new ListIntegersAggregationStrategy(), "aggregate");
        myStrategy.setAllowNullOldExchange(true);
        myStrategy.setAllowNullNewExchange(true);

        ListIntegersAggregationStrategy strategy = new ListIntegersAggregationStrategy();

        from(direct("direct-order-file-process-route:" + psId).advanced().synchronous(true))
                .routeId("routeId:" + psId)
                .process(exchange -> {
                    log.info("Started process message: {}", exchange.getIn().getHeader(KEY));
                })
                .split(body())
                .streaming()
//                .to(direct("aggregate-messages").advanced().synchronous(true))
                .aggregate(header(KEY), this::aggregatorExchanges)
                .parallelProcessing(false)
                .completionSize(5)
                .aggregateController(aggregateController)
                .forceCompletionOnStop()
                .completionTimeout(500L)

                .process(exchange -> {
                    Thread.sleep(100);
                    log.info("DONE: {} / {}", exchange.getIn().getHeader(KEY), exchange.getIn().getBody());
                })

//                .to(direct("packs"))
//                .end()
//                .end()
                .aggregate(header(KEY), new UseLatestAggregationStrategy())
                .completionTimeout(1000L)
                .process(exchange -> {
                    Thread.sleep(100);
                    log.info("FINAL: {} / {}", exchange.getIn().getHeader(KEY), exchange.getIn().getBody());
                })
//                .end()

                .log("Last message")
                .end();

        from(direct("aggregate-messages").advanced().synchronous(true))
                .aggregate(header(KEY), this::aggregatorExchanges)
                .parallelProcessing(false)
                .completionSize(5)
                .aggregateController(aggregateController)
                .forceCompletionOnStop()
                .completionTimeout(500L)
                .to(direct("packs"));

        from(direct("packs"))
                .process(exchange -> {
//                    Thread.sleep(100);
                    log.info("PACKS: {} / {}", exchange.getIn().getHeader(KEY), exchange.getIn().getBody());
                });
    }

    private Exchange aggregatorExchanges(Exchange oldExchange, Exchange newExchange) {
        List<Integer> drinks;
        if (oldExchange == null) {
//            newExchange.setProperty(Exchange.AGGREGATION_COMPLETE_ALL_GROUPS_INCLUSIVE, true);
            newExchange.setProperty(Exchange.AGGREGATION_COMPLETE_ALL_GROUPS, true);
//            newExchange.setProperty(Exchange.ASYNC_WAIT, false);
//            newExchange.setProperty(Exchange.AGGREGATION_COMPLETE_CURRENT_GROUP, true);
            drinks = new ArrayList<>();
        } else {
            //noinspection unchecked
            drinks = (List<Integer>) oldExchange.getIn().getBody();
        }
        drinks.add((Integer) newExchange.getIn().getBody());
        newExchange.getIn().setBody(drinks);
        return newExchange;
    }

}
