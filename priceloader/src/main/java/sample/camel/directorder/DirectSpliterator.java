package sample.camel.directorder;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import sample.camel.internalBeans.ExcelConverterIterator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DirectSpliterator {
    public Iterator<Map<String, Object>> processFile(Exchange exchange) throws Exception {
        log.info("Excel process file: {}", exchange.getProperty("Counter"));
        //noinspection unchecked
        Map<String, Object> map = exchange.getIn().getBody(HashMap.class);
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        final AtomicLong totalRead = new AtomicLong(0);
        exchange.setProperty("excelConverterBeanIterator", iterator);
        exchange.setProperty("totalRead", totalRead);
        return new ExcelConverterIterator(totalRead, iterator);
    }

    public List<Integer> splitToList(Exchange exchange) throws Exception {
        Thread.sleep(200);
        log.info("Excel process file: {}", exchange.getIn().getHeader("Counter"));
        //noinspection unchecked
        List<Integer> list = (List<Integer>) exchange.getIn().getBody();
        exchange.setProperty("TOTAL_SIZE", list.size());
        return list;
    }
}
