package sample.camel.directorder;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import sample.camel.internalBeans.ListIterator;

import java.util.*;

import static sample.camel.directorder.SemaphoreRouteBuilder.KEY;

@Slf4j
public class SpliteratorBean {
    private ListIterator iterator;

    public boolean isFinishedIterator() {
        return iterator == null || !iterator.hasNext();
    }

    public void closeIterator() {
//        iterator = null;
    }

    public void cleanResources() {
        iterator = new ListIterator(Collections.emptyList());
    }

    public Long getTotalRead() {
        return iterator.getTotalRead();
    }

    public Iterator<Integer> splitList(Exchange exchange) throws Exception {
        log.info("Split list: {}", exchange.getProperty(KEY));
        //noinspection unchecked
        List<Integer> list = exchange.getIn().getBody(List.class);
        this.iterator = new ListIterator(list);
        return iterator;
    }

}
