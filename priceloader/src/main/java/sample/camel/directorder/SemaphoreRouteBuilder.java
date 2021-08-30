package sample.camel.directorder;

import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import sample.camel.internalBeans.JdbcStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SemaphoreRouteBuilder extends RouteBuilder {

//    @SneakyThrows
//    public static void main(String[] args) {
//        CamelContext context = new DefaultCamelContext();
//        context.addRoutes(new AggregateOrderRouteBuilder(1));
//        context.start();
//        Thread.sleep(10000);
//    }

    public static final String KEY = "Counter";
    private final Integer psId;

    public SemaphoreRouteBuilder(Integer psId) {
        this.psId = psId;
    }

    @Override
    public void configure() throws Exception {
        final AtomicInteger timerMessageId = new AtomicInteger(1);
        final Semaphore semaphore = new Semaphore(1);
        final JdbcStore store = new JdbcStore();
        final SpliteratorBean spliterator = new SpliteratorBean();

        final Processor exceptionProcessor = exchange -> {
            final Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            semaphore.release();
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

        from("timer:cTimer?period=1&repeatCount=2")
                .process(exchange -> {
                    final Integer oldBody = timerMessageId.getAndIncrement();
                    log.info("Send message: {}", oldBody);
                    exchange.getIn().setHeader(KEY, oldBody);
                    exchange.setProperty(KEY, oldBody);
                    final List<Integer> list = IntStream.range(1, 10).boxed().collect(Collectors.toList());
                    exchange.getIn().setBody(list);
                })
                .to("direct:direct-order-file-process-route:" + psId + "?synchronous=true");

        from("direct:direct-order-file-process-route:" + psId + "?synchronous=true")
                .routeId("routeId:" + psId)
                .process(exchange -> {
                    semaphore.tryAcquire(10, TimeUnit.SECONDS);
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
                .aggregate(header(KEY), this::aggregatorExchanges)
                .forceCompletionOnStop()
                .completionPredicate(exchange -> {
                    if (spliterator.isFinishedIterator()) {
                        log.info("isCompletedIterator. CLOSE ITERATOR. ({})", exchange.getProperty(KEY));
                        spliterator.closeIterator();
                    }
                    return spliterator.isFinishedIterator();
                })
                .completionSize(5)
                .doTry()
                .bean(store, "store")
                .doCatch(Exception.class)
                .to("seda:my-errors")
                .end()
                .aggregate(header(KEY), new UseLatestAggregationStrategy())
                .forceCompletionOnStop()
                .completionPredicate(exchange -> {
                    final Long totalRead = spliterator.getTotalRead();
                    final boolean allStored = store.getTotalStored() >= totalRead;
                    if (allStored) {
                        log.info("isCompletedStore ({}) (read:{}/stored:{}/result:{})",
                                exchange.getProperty(KEY),
                                totalRead,
                                store.getTotalStored(),
                                allStored);
                    }
                    return allStored;
                })
                .doTry()
                .bean(store, "finish")
                .doCatch(Exception.class)
                .to("seda:my-errors")
                .end()
                .process(exchange -> {
                    Thread.sleep(100);
                    log.info("FINISH ({}): / {}", exchange.getIn().getHeader(KEY), exchange.getIn().getBody());
                    semaphore.release();
                })
                .log("Done")
                .end();

    }

    private Exchange aggregatorExchanges(Exchange oldExchange, Exchange newExchange) {
        List<Integer> drinks;
        if (oldExchange == null) {
            newExchange.setProperty(Exchange.AGGREGATION_COMPLETE_ALL_GROUPS, true);
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
