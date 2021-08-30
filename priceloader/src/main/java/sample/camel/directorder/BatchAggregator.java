package sample.camel.directorder;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;

import java.util.ArrayList;
import java.util.List;

import static sample.camel.directorder.DirectOnlyRouteBuilder.CAMEL_SPLIT_COMPLETE;

@Slf4j
public class BatchAggregator {
    private final int batchSize;
    private final List<Integer> buffer;

    public BatchAggregator(int batchSize) {
        this.batchSize = batchSize;
        buffer = new ArrayList<>();
    }

    public List<Integer> aggregate(Exchange exchange) {
        final Integer number = (Integer) exchange.getIn().getBody();
        if (number == null) {
            return null;
        }
        buffer.add(number);

        final Boolean isLastElement = (Boolean) exchange.getProperty(CAMEL_SPLIT_COMPLETE);
        if (isLastElement) {
            return copyAndGet();
        }

        if (buffer.size() == batchSize) {
            return copyAndGet();
        }

        return null;
    }

    private List<Integer> copyAndGet() {
        List<Integer> result =  new ArrayList<>(buffer);
        buffer.clear();
        return result;
    }
}
