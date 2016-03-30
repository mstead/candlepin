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
package org.candlepin.guice;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.qpid.client.AMQConnectionFactory;
import org.apache.qpid.jms.BrokerDetails;
import org.candlepin.audit.ActiveMqSessionPool;
import org.candlepin.audit.Event;
import org.candlepin.audit.Event.Target;
import org.candlepin.audit.Event.Type;
import org.candlepin.audit.QpidSessionPool;
import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.util.Util;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * A provider that creates and configures ActiveMQ client.
 */
public class ActiveMQSessionPoolProvider implements Provider<ActiveMqSessionPool> {

    private TopicConnection connection;
    
    private static org.slf4j.Logger log = LoggerFactory.getLogger(ActiveMQSessionPoolProvider.class);


    // external events may not have the same name as the internal events
    private Map<String, String> targetToEvent = new HashMap<String, String>() {
        private static final long serialVersionUID = 2L;
        {
            this.put(Event.Target.SUBSCRIPTION.toString().toLowerCase(), "product");
            // add more mappings when necessary
        }
    };

    @Inject
    public ActiveMQSessionPoolProvider(Configuration config) {
        try {
            String user = "admin";
            String password = "password";
            String host = "localhost";
            int port = 61616;

            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port);
            connection = factory.createTopicConnection(user, password);
            //TODO clientid should be unique! It is needed so that its possible to create durable subscribers to
            //topics
            connection.setClientID("ClientId!");
            connection.start();
            
        }
        catch (Exception ex) {
            log.error("Unable to instantiate ActiveMQBusProvider: ", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ActiveMqSessionPool get() {
        try {
            return new ActiveMqSessionPool(connection);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (JMSException e) {
            throw new RuntimeException("Couldn't close ActiveMQ session connection", e);
        }
    }

    public static String getTopicName(Type type, Target target) {
        return target.toString().toLowerCase() +
            Util.capitalize(type.toString().toLowerCase());
    }

    private String getDestination(Type type, Target target) {
        String key = target.toString().toLowerCase();
        String object = targetToEvent.get(key);
        return (object == null ? key : object) + "." + type.toString().toLowerCase();
    }
}
