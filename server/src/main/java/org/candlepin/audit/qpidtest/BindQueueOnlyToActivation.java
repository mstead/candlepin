package org.candlepin.audit.qpidtest;

import javax.jms.MessageConsumer;
import javax.jms.Queue;

import org.apache.qpid.client.AMQQueue;
import org.candlepin.audit.AMQPBusPublisher;
import org.candlepin.audit.Event;
import org.candlepin.audit.Event.Target;
import org.candlepin.audit.Event.Type;
import org.candlepin.auth.PrincipalData;
import org.candlepin.guice.AMQPBusPubProvider;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * This shows if our code can send the message with such
 * routing key, so that ActivationListener can receive only
 * the specific message
 * @author fnguyen
 *
 */
public class BindQueueOnlyToActivation {
    public static void main(String[] args) throws Exception {
        EventReceiver ereceiver = new EventReceiver(new AMQPDummyConfig(), new EventMessageListener("l1"));
        
        Thread.sleep(2000);
        
/**
 *      To receive the events, we can use the following snippet:
 *      
 *      Queue dest = new AMQQueue("qpoolcreated");
        MessageConsumer consumer = sess.createConsumer(dest);
        consumer.setMessageListener(eventMessageListener);
 */
        AMQPBusPubProvider provider = new  AMQPBusPubProvider(new AMQPDummyConfig(), new ObjectMapper());
        
        /**
         * The master implementation of AMQPBusPublisher already publishes events under
         * correct binding keys.
         */
        AMQPBusPublisher publisher = provider.get();
        publisher.setName("Pub1");
        
        Event e = new Event();
        Event e2 = new Event();
        e.setPrincipal(new PrincipalData("tprinc", "tprinc"));
        e2.setPrincipal(new PrincipalData("tprinc", "tprinc"));
        /**
         * Activation listener must receive only 
         * POOL CREATED
         */
        e.setTarget(Target.POOL);
        e.setType(Type.CREATED);
        e2.setTarget(Target.POOL);
        e2.setType(Type.EXPIRED);
        
        publisher.onEvent(e);
        publisher.onEvent(e2);
        
    }
}
