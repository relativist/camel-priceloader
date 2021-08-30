package sample.camel.internalBeans;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ListIterator implements Iterator<Integer> {
    private final Iterator<Integer> iterator;
    private final AtomicLong totalRead;

    public Long getTotalRead() {
        return totalRead.get();
    }

    public ListIterator(List<Integer> list) {
        this.totalRead = new AtomicLong(0);
        this.iterator = list.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public Integer next() {
        final Integer next = iterator.next();
        totalRead.incrementAndGet();
        return next;
    }


}
