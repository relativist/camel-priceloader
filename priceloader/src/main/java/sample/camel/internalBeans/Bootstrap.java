package sample.camel.internalBeans;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sample.camel.directorder.DirectOnlyRouteBuilder;
import sample.camel.directorder.SemaphoreRouteBuilder;

@Slf4j
@Component
public class Bootstrap implements InitializingBean {

    @Autowired
    private CamelContext context;

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @SneakyThrows
    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
//        SandBoxRouteBuilder route = new SandBoxRouteBuilder( 1);
//        DirectOrderRouteBuilder route = new DirectOrderRouteBuilder(1);
//        SemaphoreRouteBuilder route = new SemaphoreRouteBuilder(1);
        DirectOnlyRouteBuilder route = new DirectOnlyRouteBuilder(1);
        context.addRoutes(route);

//        final ProducerTemplate template = context.createProducerTemplate();
//        final List<Integer> list = IntStream.range(1, 10).boxed().collect(Collectors.toList());
//        template.sendBodyAndHeader("direct:direct-order-file-process-route:" + 1, list, "Counter", 1);
//        template.sendBodyAndHeader("direct:direct-order-file-process-route:" + 1, list, "Counter", 2);
    }

}
