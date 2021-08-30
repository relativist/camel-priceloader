package sample.camel.directorder;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.annotations.Component;

import java.util.ArrayList;
import java.util.List;

@Component(value = "arrayListAggregationStrategy")
public class ListIntegersAggregationStrategy implements AggregationStrategy {

    public ListIntegersAggregationStrategy() {
        super();
    }

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        List<Integer> drinks;
        if (oldExchange == null) {
//            newExchange.setProperty(Exchange.AGGREGATION_COMPLETE_ALL_GROUPS_INCLUSIVE, true);
//            newExchange.setProperty(Exchange.AGGREGATION_COMPLETE_ALL_GROUPS, true);
//            newExchange.setProperty(Exchange.ASYNC_WAIT, false);
//            newExchange.setProperty(Exchange.AGGREGATION_COMPLETE_CURRENT_GROUP, true);
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
