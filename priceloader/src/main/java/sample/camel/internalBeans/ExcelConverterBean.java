package sample.camel.internalBeans;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ExcelConverterBean {
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

}
