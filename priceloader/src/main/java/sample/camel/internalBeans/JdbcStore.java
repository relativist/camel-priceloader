package sample.camel.internalBeans;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import sample.camel.exceptions.PriceloaderException;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JdbcStore {
    public static final String KEY = "Counter";
    private final AtomicBoolean connectionOpened = new AtomicBoolean(false);
    private final AtomicInteger totalStored = new AtomicInteger(0);

    public void cleanResources() {
        totalStored.set(0);
        if (connectionOpened.get()) {
            log.warn("CLOSE PREV CONNECTION!");
            connectionOpened.set(false);
        }
    }

    public void prepare(Exchange exchange) throws Exception {
        log.info("JDBC prepare.({})", exchange.getProperty(KEY));
        if (connectionOpened.get()) {
            throw new RuntimeException("Connection is opened already!");
        }
        connectionOpened.set(true);
    }

    public void store(Exchange exchange) throws Exception {
        Thread.sleep(200);
        //noinspection unchecked
        final ArrayList<Integer> body = (ArrayList<Integer>) exchange.getIn().getBody();
        totalStored.addAndGet(body.size());
        log.info(" JDBC store.({}) {}    {} / {}",exchange.getProperty(KEY), body ,body.size() , totalStored);
        if (!connectionOpened.get()) {
            throw new RuntimeException("Connection should be opened!");
        }
        final Integer key = (Integer) exchange.getIn().getHeader(KEY);
//        if (key.equals(1)) {
//            throw new PriceloaderException("====> PRICELOADER EXCEPTION");
//        }
    }

    public void finish(Exchange exchange) throws Exception {
        //noinspection unchecked
        final ArrayList<Integer> body = (ArrayList<Integer>) exchange.getIn().getBody();
        log.info("JDBC FINISH ({}). last pack: {}", exchange.getProperty(KEY), body.size());

        if (!connectionOpened.get()) {
            throw new RuntimeException("Connection is closed already!");
        }
        connectionOpened.set(false);
    }

    public Integer getTotalStored() {
        return totalStored.get();
    }
}
