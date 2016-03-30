package org.candlepin.audit;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.NamingException;

import org.apache.activemq.command.ActiveMQTopic;

/**
 * The following JMS objects are not thread safe: 
 *   - TopicSession
 *   - TopicPublisher
 * 
 * We are going to cache those objects for every thread.
 * The reason we can do this is that Qpid is offering 
 * automatic failover to us.
 * 
 * @author fnguyen
 *
 */
public class ActiveMqSessionPool {
    private TopicConnection connection;
    private ThreadLocal<TopicSession> sessions = new ThreadLocal<TopicSession>();
    private ThreadLocal<Map<String,TopicPublisher>> topicPublishers = new ThreadLocal<Map<String,TopicPublisher>>();
    
    public ActiveMqSessionPool(TopicConnection tc){
        this.connection = tc;
    }

    
    /**
     * Currently we create a session per request
     * @return
     */
    public TopicSession getSession() {
        TopicSession s = sessions.get();
        if (s == null) {
            try {
                s = connection.createTopicSession(true, Session.SESSION_TRANSACTED);
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
            sessions.set(s);
        }
        return s;
    }
    
    
    public TopicPublisher getTopicPublisher(String routingKey) {

        /*
         * In the POC for ActiveMQ we just send to 1 topic 'event' because internal routing of
         * messages works differently than in Qpid Java client
         */
        routingKey = "event";
        if (topicPublishers.get() == null)
            topicPublishers.set(new HashMap<String,TopicPublisher>());
        
        if (topicPublishers.get().containsKey(routingKey))
            return topicPublishers.get().get(routingKey);
        
        try {
            Destination dest = new ActiveMQTopic(routingKey);
            TopicPublisher publisher =  getSession().createPublisher((Topic)dest);
            topicPublishers.get().put(routingKey, publisher);
            return publisher;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
    

}
