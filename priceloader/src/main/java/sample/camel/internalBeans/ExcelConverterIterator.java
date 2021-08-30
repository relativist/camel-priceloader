package sample.camel.internalBeans;

import lombok.SneakyThrows;
import org.apache.camel.Exchange;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ExcelConverterIterator implements Iterator<Map<String, Object>> {
    private final Iterator<Map.Entry<String, Object>> internalIterator;
    private AtomicLong totalRead;

    public ExcelConverterIterator(AtomicLong totalRead, Iterator<Map.Entry<String, Object>> internalIterator) {
        this.totalRead = totalRead;
        this.internalIterator = internalIterator;
    }

    @Override
    public boolean hasNext() {
        final boolean hasNext = internalIterator != null && internalIterator.hasNext();
        return hasNext;
    }


    @Override
    @SneakyThrows
    public Map<String, Object> next() {
        final Map.Entry<String, Object> next = internalIterator.next();
        totalRead.incrementAndGet();
        final HashMap<String, Object> result = new LinkedHashMap<>();
        result.put("column:" + next.getKey(), next.getValue().toString());
        result.put("name:" + next.getKey(), next.getValue().toString());
        result.put("oem:" + next.getKey(), next.getValue().toString());
        result.put("brand:" + next.getKey(), next.getValue().toString());
        return result;
    }
}
