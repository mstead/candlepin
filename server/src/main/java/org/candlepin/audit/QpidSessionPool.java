package org.candlepin.audit;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.NamingException;

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
public class QpidSessionPool {
    private TopicConnection connection;
    private ThreadLocal<TopicSession> sessions = new ThreadLocal<TopicSession>();
    private ThreadLocal<Map<String,TopicPublisher>> topicPublishers = new ThreadLocal<Map<String,TopicPublisher>>();
    private Context ctx;
    
    public QpidSessionPool(TopicConnection tc, Context ctx){
        this.connection = tc;
        this.ctx = ctx;
    }

    /**
     * Currently we create a session per request
     * @return
     */
    public TopicSession getSession() {
        TopicSession s = sessions.get();
        if (s == null) {
            try {
                s = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
            sessions.set(s);
        }
        return s;
    }
    
    
    public TopicPublisher getTopicPublisher(String routingKey) {
        if (topicPublishers.get() == null)
            topicPublishers.set(new HashMap<String,TopicPublisher>());
        
        if (topicPublishers.get().containsKey(routingKey))
            return topicPublishers.get().get(routingKey);
        
        try {
            TopicPublisher publisher =  getSession().createPublisher((Topic)ctx.lookup(routingKey));
            topicPublishers.get().put(routingKey, publisher);
            return publisher;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
    

}
