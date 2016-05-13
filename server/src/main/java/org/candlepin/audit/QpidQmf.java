package org.candlepin.audit;

import org.apache.qpid.client.AMQAnyDestination;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class QpidQmf {
    private Connection connection;
    private Destination qmfQueue;
    private Destination responseQueue;
    
    public QpidQmf(Connection connection) throws URISyntaxException {
        this.connection = connection;
        qmfQueue = new AMQAnyDestination("qmf.default.direct/broker"); 
        responseQueue = new AMQAnyDestination( "#reply-queue; {create:always, node:{x-declare:{auto-delete:true}}}"); 
    }
    
    private List<Map<String,Object>> runQuery(String targetType, Map<Object, Object> query) throws JMSException {
        Session session = null;
        List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer sender = session.createProducer(qmfQueue);
            MessageConsumer receiver = session.createConsumer(responseQueue);
            MapMessage request = session.createMapMessage();
            request.setJMSReplyTo(responseQueue);
            request.setStringProperty("x-amqp-0-10.app-id", "qmf2"); 
            request.setStringProperty("qmf.opcode", "_query_request");
            request.setObject(targetType, query);
            request.setObject("_what", "OBJECT"); // method name to be
            request.setJMSType("amqp/map");
            sender.send(request);
            Message response = receiver.receive(500);
            if (response != null) {
                if (response instanceof MapMessage) {
                    MapMessage mm = (MapMessage)response;
                    Enumeration en = mm.getMapNames();
                    while (en.hasMoreElements()){
                        Map<String,Object> next = (Map<String,Object>) mm.getObject(en.nextElement().toString());
                        result.add(next);
                    }
                    return result;
                }
                else {
                    System.out.println("Received response in incorrect format: " + response);
                }
            }
            else {
                System.out.println("No response received");
            } 
            
        } finally {
            session.close();
        }
        return null;
    }
    
    /**
     * Finds queues that are bound to an exchange
     * 
     * @param exchangeName
     * @return Fully qualified names of queues
     * @throws JMSException
     */
    public Set<String> getExchangeBoundQueueNames(String exchangeName) throws JMSException{
        List<Map<String,Object>> mm =  runQuery( "_schema_id", createQuery("_class_name","binding"));
      
        Set<String> result = new HashSet<String>();
        
        for (Map<String,Object> res : mm){
            if (extractString(res, "_values","exchangeRef", "_object_name")
                .equals("org.apache.qpid.broker:exchange:"+exchangeName))
                result.add(extractString(res, "_values","queueRef", "_object_name"));
        }
        return result;
    }
    
    /**
     * 
     * @param queueName fully qualified queueName
     * @throws JMSException 
     */
    public void getQueueInfo(String queueName) throws JMSException{
        List<Map<String,Object>> mm =  runQuery( "_object_id", createQuery("_object_name",queueName));
        if (mm.size()==0)
            throw new RuntimeException("Couldn't find a queue in Qpid: "+queueName);
    }
    
    private Map<Object, Object> createQuery(Object ... keys) {
        Map<Object, Object> query = new HashMap<Object, Object>();
        if (keys.length == 2) {
            query.put(keys[0], keys[1]);
            return query;
        }
        query.put(keys[0], createQuery(Arrays.copyOfRange(keys, 1, keys.length)));
        return query;
    }

    private String extractString(Object mm, String  ... hashKeys) throws JMSException {
        Object result = (Map<String, Object>)mm;
        for (String key : hashKeys){
            result = ((Map<String, Object>)result).get(key);
            if (result == null)
                throw new RuntimeException("Couldn't extract next hash value at: "+hashKeys);
        }
        if (result instanceof byte[])
            return new String((byte[])result);
        else
            throw new RuntimeException("Couldn't extract string at path: "+hashKeys);
    }


}
