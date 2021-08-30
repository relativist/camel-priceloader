
package sample.camel.internalBeans;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.direct;

public class SandBoxRouteBuilder extends RouteBuilder {

    private static final boolean STOPPED = false;
    public static final String JDBC_STORE = "JDBCSTORE";
    private static final String EXCEL_CONVERTER_BEAN = "ExcelConverterBean";
    private Integer psId;
//    private final JdbcStore store = new JdbcStore();
    private final AtomicInteger timerMessageId = new AtomicInteger(1);

    public SandBoxRouteBuilder(Integer psId) {
        this.psId = psId;
    }

    @Override
    public void configure() throws Exception {
        checkErrors();
        timerRoute();

        ExcelConverterBean excelConverterBean = new ExcelConverterBean();
//        ValidatorBeanExpression validator = new ValidatorBeanExpression(excelConverterBean, store);
//        BeanExpression completedIterator = new BeanExpression(excelConverterBean, "isCompletedIterator");
//        BeanExpression completedStore = new BeanExpression(validator, "isCompletedStore");


//        SplitDefinition splitDefinition = from("direct:file-process-route:" + psId)
//                .routeId("routeId:" + psId)
//                .process(exchange -> {
//                    exchange.getIn().setHeader("pricelistSource", psId);
//                })
//                .bean(store, "prepare")
//                .split()
//                .method(excelConverterBean, "processFile")
//                .streaming();
//        processSplitedStream(completedIterator, completedStore, splitDefinition);


        from(direct("file-process-route:" + psId))
//        from("seda:file-process-route:" + psId )
                .routeId("routeId:" + psId)
                .process(exchange -> {
                    log.info("Started process message: {}", exchange.getProperty("Counter"));
                    exchange.getIn().setHeader("pricelistSource", psId);
                    exchange.setProperty(JDBC_STORE, new JdbcStore());
//                    exchange.setProperty(EXCEL_CONVERTER_BEAN, new ExcelConverterBean());
                })
                .process(e -> {
                    final JdbcStore store = (JdbcStore) e.getProperty(JDBC_STORE);
                    store.prepare(e);
                })
                .split()
                .method(excelConverterBean, "processFile")
                .streaming()
                .aggregate(constant(true), new ArrayListAggregationStrategy())
                .forceCompletionOnStop()
                .completionSize(5)
                .completionPredicate(exchange -> {
                    //noinspection unchecked
                    Iterator<Map.Entry<String, Object>> iterator = (Iterator<Map.Entry<String, Object>>) exchange.getProperty("excelConverterBeanIterator");
                    final boolean isFinished = iterator == null || !iterator.hasNext();
                    if (isFinished && iterator != null) {
                        log.info("isCompletedIterator. CLOSE ITERATOR. ({})", exchange.getProperty("Counter"));
                        iterator = null;
                    }
//                    log.info("Iterator finish check: {}", isFinished);
                    return isFinished;

//                    final JdbcStore store = (JdbcStore) exchange.getProperty(JDBC_STORE);
//                    ValidatorBeanExpression validator = new ValidatorBeanExpression(excelConverterBean, store);
//                    return validator.isCompletedIterator();
                })
                .process(e -> {
                    final JdbcStore store = (JdbcStore) e.getProperty(JDBC_STORE);
                    store.store(e);
                })
                .aggregate(constant(true), new UseLatestAggregationStrategy())
                .forceCompletionOnStop()
                .completionPredicate(exchange -> {
                    final AtomicLong totalRead = (AtomicLong) exchange.getProperty("totalRead");
                    final JdbcStore store = (JdbcStore) exchange.getProperty(JDBC_STORE);
                    final boolean allStored = store.getTotalStored() >= totalRead.get();
                    if (allStored) {
                        log.info("isCompletedStore ({}) (read:{}/stored:{}/result:{})",
                                exchange.getProperty("Counter"),
                                totalRead,
                                store.getTotalStored(),
                                allStored);
                    }
                    return allStored;

//                    ValidatorBeanExpression validator = new ValidatorBeanExpression(excelConverterBean, store);
//                    return validator.isCompletedStore();
                })
//                .bean(store, "finish")
                .process(e -> {
                    final JdbcStore store = (JdbcStore) e.getProperty(JDBC_STORE);
                    store.finish(e);
                })
                .process(exchange -> {
                    log.info("DONE ({})", exchange.getProperty("Counter"));
                })
                .end();
    }

//    protected void processSplitedStream(BeanExpression completedIterator, BeanExpression completedStore, ExpressionNode node) {
//        node
//                .choice()
//                .when(s -> !SandBoxRouteBuilder.STOPPED)
//                .to("direct:aggr-" + psId)
//                .otherwise()
//                .stop()
//                .end();
//
//        final ProcessorDefinition<?> def = from("direct:aggr-" + psId)
//                .routeId("aggr:" + psId)
//                .aggregate(constant(true), new ArrayListAggregationStrategy())
//                .setHeader("pricelistSource", constant(psId))
//                .completionSize(100)
//                .completionPredicate(completedIterator)
//                .bean(store, "store")
//                .aggregate(constant(true), new UseLatestAggregationStrategy())
//                .completionPredicate(completedStore)
//                .bean(store, "finish")
//                .log("DONE.")
//                .end();
//        def.setId("PricelistSource-aggr:" + psId);
//    }


    private void timerRoute() {

        from("timer:cTimer?period=1&repeatCount=2")
                .process(e -> {
                    final Integer oldBody = timerMessageId.getAndIncrement();
                    log.info("Send message: {}", oldBody);
                    e.setProperty("Counter", oldBody);
                    final Map<String, Object> collect = IntStream.range(0, 24).boxed().collect(toMap(Object::toString, o -> o));
//                    final Map<String, Object> collect = IntStream.range(0, 1115).boxed().collect(toMap(Object::toString, o -> o));
                    e.getIn().setBody(collect);
                })
//                .to("seda:file-process-route:" + psId);
                .to("direct:file-process-route:" + psId);
//                .to("direct:heavy-processor:" + psId);

//        from("direct:heavy-processor:" + psId)
//                .process(e -> {
//                    Thread.sleep(150);
//                    final Integer counter = (Integer) e.getProperty("Counter");
//                    log.info("Processed:{}", counter);
//                })
//                .end();
    }

    private void checkErrors() {
        errorHandler(deadLetterChannel("seda:errors")
                .onRedelivery(exchange -> {
                    log.info("onRedelivery errorHandler");
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    log.info("Exception: ", exception);
                })
                .redeliveryDelay(100)
                .log("redelivery by dead letter channel")
                .asyncDelayedRedelivery()
                .maximumRedeliveries(0)
                .logStackTrace(true)
                .retryAttemptedLogLevel(LoggingLevel.DEBUG)
                .log("========== FINISH ERROR HANDLER ==========")
        );
    }

}
