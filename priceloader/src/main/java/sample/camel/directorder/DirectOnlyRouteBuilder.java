package sample.camel.directorder;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import sample.camel.internalBeans.JdbcStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.direct;


public class DirectOnlyRouteBuilder extends RouteBuilder {

    public static final String KEY = "Counter";
    public static final String CAMEL_SPLIT_COMPLETE = "CamelSplitComplete";
    private final Integer psId;

    public DirectOnlyRouteBuilder(Integer psId) {
        this.psId = psId;
    }

    @Override
    public void configure() throws Exception {
        final AtomicInteger timerMessageId = new AtomicInteger(1);
        final JdbcStore store = new JdbcStore();
        final SpliteratorBean spliterator = new SpliteratorBean();
        final BatchAggregator aggregator = new BatchAggregator(5);

        final Processor exceptionProcessor = exchange -> {
            final Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            log.info("onException: {}", exception.getMessage());
        };

        onException(Exception.class)
                .logStackTrace(false)
                .useOriginalMessage()
                .process(exceptionProcessor)
                .to("seda:my-errors")
                .end();

        errorHandler(deadLetterChannel("mock:errors")
                .logStackTrace(false)
                .log("========== FINISH ERROR HANDLER ==========")
        );

        from("seda:my-errors")
                .routeId("Errors_route_id")
                .process(exchange -> {
                    final Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.info("Process exception: {}", exception.getMessage());
                    exchange.setRouteStop(true);
                })
                .stop();

        from("timer:cTimer?period=1&repeatCount=1")
                .process(exchange -> {
                    final Integer oldBody = timerMessageId.getAndIncrement();
                    log.info("Send message: {}", oldBody);
                    exchange.getIn().setHeader(KEY, oldBody);
                    exchange.setProperty(KEY, oldBody);
                    final List<Integer> list = IntStream.range(1, 10).boxed().collect(Collectors.toList());
                    exchange.getIn().setBody(list);
                })
                .to("direct:direct-order-file-process-route:" + psId);



        from(direct("direct-order-file-process-route:" + psId).advanced().synchronous(true))
                .routeId("routeId:" + psId)
                .threads(1)
                .process(exchange -> {
                    exchange.setProperty("sourceId", constant(psId));
                    final Integer key = (Integer) exchange.getIn().getHeader(KEY);
                    log.info("Direct: process message: {}", key);
                    store.cleanResources();
                    spliterator.cleanResources();
                })
                .setHeader("pricelistSource", constant(psId))
                .bean(store, "prepare")
                .split()
                .method(spliterator, "splitList")
                .streaming()
                .filter(body().isNotNull())
                .bean(aggregator)
                .end()
                .filter(body().isNotNull())
                .doTry()
                .bean(store, "store")
                .doCatch(Exception.class)
                .to("seda:my-errors")
                .end()
                .filter(exchangeProperty(CAMEL_SPLIT_COMPLETE))
                .bean(store, "finish")
                .log("Done")
                .end();
    }

}
