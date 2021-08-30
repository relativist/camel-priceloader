
package sample.camel;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import static org.junit.jupiter.api.Assertions.assertTrue;

//@CamelSpringBootTest
//@SpringBootTest(classes = SampleAmqApplication.class)
public class SampleAmqApplicationTests {
//    @Autowired
    private CamelContext camelContext;

    @Disabled("Requires a running activemq broker")
    @Test
    public void shouldProduceMessages() throws Exception {
//        MockEndpoint mock = camelContext.getEndpoint("mock:activemq", MockEndpoint.class);
//        NotifyBuilder notify = new NotifyBuilder(camelContext).whenDone(1).create();
//
//        assertTrue(notify.matches(10, TimeUnit.SECONDS));
//
//        mock.expectedMessageCount(1);
//        mock.assertIsSatisfied();
    }
}
