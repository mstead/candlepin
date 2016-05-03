/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.audit;

import org.candlepin.audit.Event.Target;
import org.candlepin.audit.Event.Type;
import org.candlepin.util.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.client.AMQConnection;
import org.apache.qpid.client.AMQSession;
import org.apache.qpid.client.AMQSessionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

/**
 * An EventListener that publishes events to an AMQP bus (qpid).
 */
public class AMQPBusPublisher implements EventListener {
    private static Logger log = LoggerFactory.getLogger(AMQPBusPublisher.class);
    private TopicSession session;
    private Map<Target, Map<Type, TopicPublisher>> producerMap;
    private ObjectMapper mapper;
    private TopicConnection connection;
    
    public AMQPBusPublisher(TopicSession session,
        Map<Target, Map<Type, TopicPublisher>> producerMap, ObjectMapper omapper ) {
        this.session = session;
        this.producerMap = producerMap;
        
        this.mapper = omapper;
    }
    
    @Inject
    public AMQPBusPublisher(TopicSession session,
        Map<Target, Map<Type, TopicPublisher>> producerMap, ObjectMapper omapper, TopicConnection connection) {
        this.session = session;
        this.producerMap = producerMap;
        this.connection = connection;   
        this.mapper = omapper;
    }

    @Override
    public void onEvent(Event e) {
        try {
            Map<Type, TopicPublisher> m = this.producerMap.get(e.getTarget());
            if (m != null) {
                TopicPublisher tp = m.get(e.getType());
                if (tp != null) {
                    log.debug("Sending event to topic publisher: {}", e);
                    AMQSession<?, ?> ses = ((AMQSessionAdapter<AMQSession<?, ?>>)session).getSession();
                    AMQConnection con = (AMQConnection)connection;
                    log.info("Is flow blocked: "+ses.isFlowBlocked());
                    log.info("Is suspended: "+ses.isSuspended());
                    log.info("Is connection failing over: "+con.isFailingOver());
                    log.info("Is connection closed: "+con.isClosed());
                    
                    System.out.println(getExchangeCount());
                    tp.send(session.createTextMessage(this.apply(e)));
                }
                else {
                    log.warn("TopicPublisher is NULL!");
                }
            }
        }
        catch (JMSException ex) {
            log.error("Unable to send event: " + e, ex);
            throw new RuntimeException("Error sending event to message bus", ex);
        }
        catch (JsonProcessingException jpe) {
            log.error("Unable to send event: " + e, jpe);
            throw new RuntimeException("Error sending event to message bus", jpe);
        }
    }

    private int getExchangeCount() {
        try {
            Session session;

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            String exchange_name = "event";

            Destination snd_dest = new AMQAnyDestination("qmf.default.direct/broker"); // to
                                                                                       // send
                                                                                       // a
                                                                                       // message
                                                                                       // to
                                                                                       // QMF-related
                                                                                       // exchange
            Destination rec_dest = new AMQAnyDestination(
                "#reply-queue; {create:always, node:{x-declare:{auto-delete:true}}}"); // just
                                                                                       // for
                                                                                       // getting
                                                                                       // response
                                                                                       // of
                                                                                       // our
                                                                                       // request
            MessageProducer sender = session.createProducer(snd_dest);
            MessageConsumer receiver = session.createConsumer(rec_dest);

            MapMessage request = session.createMapMessage();
            request.setJMSReplyTo(rec_dest);
            request.setStringProperty("x-amqp-0-10.app-id", "qmf2"); // specifying
                                                                     // we are
                                                                     // using
                                                                     // QMF
                                                                     // version
                                                                     // 2
                                                                     // protocol
            // request.setStringProperty("qmf.opcode", "_method_request"); //
            // QMF operation code is "requesting a method"
            request.setStringProperty("qmf.opcode", "_query_request"); // QMF
                                                                       // operation
                                                                       // code
                                                                       // is
                                                                       // "requesting
                                                                       // a
                                                                       // method"

            Map<String, String> OID = new HashMap<String, String>();
            Map<String, Object> arguments = new HashMap<String, Object>();

            OID.put("_object_name", "org.apache.qpid.broker:exchange:" + exchange_name); // OID
                                                                                   // has
                                                                                   // to
                                                                                   // refer
                                                                                   // to
                                                                                   // the
                                                                                   // queue
                                                                                   // name
                                                                                   // in
                                                                                   // this
                                                                                   // format

            // arguments.put("request", messages); // to specify if all
            // (messages=0) or a given number (messages>0) of messages to be
            // purged

            request.setObject("_object_id", OID); // specifying the object ID on
                                                  // what we apply the method
            request.setObject("_what", "OBJECT"); // method name to be
            // applied
            // request.setObject("_arguments", arguments); // method arguments

            request.setJMSType("amqp/map");
            sender.send(request);
            Message response = receiver.receive(10 * 1000);
            if (response != null) {
                if (response instanceof MapMessage) {
                    MapMessage mm = (MapMessage)response;
                    System.out.println(mm);
                    
//                    if (((MapMessage) response).getStringProperty("qmf.opcode").equals("_method_response"))
//                        System.out.println("Queue purged.");
//                    else
//                        System.out.println("Queue not purged, negative response received:" + response);
                }
                else {
                    System.out.println("Received response in incorrect format: " + response);
                }
            }
            else {
                System.out.println("No response received");
            }

            return 0;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
    private int getMessageCount() {
        try {
            Session session;

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            String queue_name = "qconsume";

            Destination snd_dest = new AMQAnyDestination("qmf.default.direct/broker"); // to
                                                                                       // send
                                                                                       // a
                                                                                       // message
                                                                                       // to
                                                                                       // QMF-related
                                                                                       // exchange
            Destination rec_dest = new AMQAnyDestination(
                "#reply-queue; {create:always, node:{x-declare:{auto-delete:true}}}"); // just
                                                                                       // for
                                                                                       // getting
                                                                                       // response
                                                                                       // of
                                                                                       // our
                                                                                       // request
            MessageProducer sender = session.createProducer(snd_dest);
            MessageConsumer receiver = session.createConsumer(rec_dest);

            MapMessage request = session.createMapMessage();
            request.setJMSReplyTo(rec_dest);
            request.setStringProperty("x-amqp-0-10.app-id", "qmf2"); // specifying
                                                                     // we are
                                                                     // using
                                                                     // QMF
                                                                     // version
                                                                     // 2
                                                                     // protocol
            // request.setStringProperty("qmf.opcode", "_method_request"); //
            // QMF operation code is "requesting a method"
            request.setStringProperty("qmf.opcode", "_query_request"); // QMF
                                                                       // operation
                                                                       // code
                                                                       // is
                                                                       // "requesting
                                                                       // a
                                                                       // method"

            Map<String, String> OID = new HashMap<String, String>();
            Map<String, Object> arguments = new HashMap<String, Object>();

            OID.put("_object_name", "org.apache.qpid.broker:queue:" + queue_name); // OID
                                                                                   // has
                                                                                   // to
                                                                                   // refer
                                                                                   // to
                                                                                   // the
                                                                                   // queue
                                                                                   // name
                                                                                   // in
                                                                                   // this
                                                                                   // format

            // arguments.put("request", messages); // to specify if all
            // (messages=0) or a given number (messages>0) of messages to be
            // purged

            request.setObject("_object_id", OID); // specifying the object ID on
                                                  // what we apply the method
            request.setObject("_what", "OBJECT"); // method name to be
            // applied
            // request.setObject("_arguments", arguments); // method arguments

            request.setJMSType("amqp/map");
            sender.send(request);
            Message response = receiver.receive(10 * 1000);
            if (response != null) {
                if (response instanceof MapMessage) {
                    MapMessage mm = (MapMessage)response;
                    System.out.println(mm);
                    
//                    if (((MapMessage) response).getStringProperty("qmf.opcode").equals("_method_response"))
//                        System.out.println("Queue purged.");
//                    else
//                        System.out.println("Queue not purged, negative response received:" + response);
                }
                else {
                    System.out.println("Received response in incorrect format: " + response);
                }
            }
            else {
                System.out.println("No response received");
            }

            return 0;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        // Why this big loop? To log in case, we failed to close any publishers.
        for (Entry<Target, Map<Type, TopicPublisher>> entry : this.producerMap
            .entrySet()) {
            for (Entry<Type, TopicPublisher> tpMap : entry.getValue()
                .entrySet()) {
                Util.closeSafely(tpMap.getValue(), String.format(
                    "TopicPublisherOf[%s, %s]", entry.getKey(), tpMap.getKey()));
            }
        }
    }

    public String apply(Event event) throws JsonProcessingException {
        return mapper.writeValueAsString(event);
    }
}
