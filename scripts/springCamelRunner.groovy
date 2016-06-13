
import org.apache.camel.CamelContext
import org.apache.camel.Endpoint
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.main.Main
import org.apache.camel.main.MainListener
import org.apache.camel.main.MainListenerSupport
import org.apache.camel.main.MainSupport
import org.apache.camel.spring.SpringCamelContext
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent
import org.springframework.context.annotation.Configuration;

@Grab('org.apache.camel:camel-core:2.16.3')
@Grab('org.apache.camel:camel-jms:2.16.3')
@Grab('org.apache.camel:camel-spring:2.16.3')
@Grab('org.springframework:spring-context:4.2.5.RELEASE')
@Grab(group='org.eclipse.jetty', module='jetty-spring', version='9.3.9.v20160517')
@Grab('org.apache.activemq:activemq-broker:5.11.1')
@Grab('org.apache.activemq:activemq-client:5.11.1')
@Grab('org.slf4j:slf4j-log4j12:1.6.1')
@Grab('org.slf4j:slf4j-api:1.6.1')
@Grab('log4j:log4j:1.2.16')
@GrabConfig(systemClassLoader=true)

public class AnotherTestBean{

    def callMeTo(){
        String dateTime = "Invoked timer at " + new Date();
        System.out.println(dateTime);
        dateTime
    }
}

@Configuration
public class MyBean {
    public void callMe() {
        System.out.println("MyBean.callMe method has been called");
    }

    @Bean(name="someString")
    String someString(){
        "ATestString"
    }
}

def routeBuilder(){
    return new RouteBuilder() {
        public void configure() {
            // you can configure the route rule with Java DSL here

            // populate the message queue with some messages
            from("timer:foo?delay=2000")
                    .process(new Processor() {
                public void process(Exchange exchange) throws Exception {
                    String dateTime = "Invoked timer at " + new Date();
                    System.out.println(dateTime);
                    exchange.getOut().setBody(dateTime);
                }
            }).to("jms:test.MyQueue");

            from("jms:test.MyQueue")
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            String dateString = String.valueOf(new Date()).replaceAll(" ","+")
                            String fileName = "email-" + new Date()+".html";
                            exchange.getIn().setHeader("CamelFileName",fileName)
                        }
                    })
                    .to("file://target/test?noop=true");

            // set up a listener on the file component
            from("file://target/test?noop=true").
                    bean(new MyBean(),"callMe");
        }
    }
}


def listenerSupport(){
    return new MainListenerSupport(){

        public void beforeStart(MainSupport main) {
            // noop
        }

        public void configure(CamelContext context) {
            // setup the ActiveMQ component
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
            connectionFactory.setBrokerURL("vm://localhost.spring.javaconfig?marshal=false&broker.persistent=false&broker.useJmx=false");

            // and register it into the CamelContext
            JmsComponent answer = new JmsComponent();
            answer.setConnectionFactory(connectionFactory);
            context.addComponent("jms", answer);
        }



        public void afterStop(MainSupport main) {
            // noop
        }
        @Override
        public void afterStart(MainSupport main) {
            System.out.println("MainExample with Camel is now started!");
        }

        @Override
        public void beforeStop(MainSupport main) {
            System.out.println("MainExample with Camel is now being stopped!");
        }
    };
}

def boot(){
    Main main = new Main();
    // bind MyBean into the registry
    main.bind("foo", new MyBean());
    // add routes
    main.addRouteBuilder(routeBuilder());
    // add event listener
    main.addMainListener(listenerSupport());

    // run until you terminate the JVM
    System.out.println("Starting Camel. Use ctrl + c to terminate the JVM.\n");
    main.run();
}

boot()
CamelContext camelContext = SpringCamelContext.springCamelContext(appContext, false);
