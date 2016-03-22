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

import java.net.URISyntaxException;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import org.apache.qpid.client.AMQQueue;
import org.candlepin.config.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Injector;


/**
 * HornetqContextListener - Invoked from our core CandlepinContextListener, thus
 * doesn't actually implement ServletContextListener.
 */
public class EventingContextListener {
    private static  Logger log = LoggerFactory.getLogger(EventingContextListener.class);

    private EventSink eventSink;

    public void contextDestroyed() {
    }

    public void contextInitialized(Injector injector) {

        org.candlepin.common.config.Configuration candlepinConfig =
            injector.getInstance(org.candlepin.common.config.Configuration.class);

        List<String> listeners = getEventListeners(candlepinConfig);

        eventSink = injector.getInstance(EventSink.class);
        for (int i = 0; i < listeners.size(); i++) {
            try {
                Class<?> clazz = this.getClass().getClassLoader().loadClass(
                    listeners.get(i));
                eventSink.registerListener((EventListener) injector.getInstance(clazz));
            }
            catch (Exception e) {
                log.warn("Unable to register listener " + listeners.get(i), e);
            }
        }
        
        //Setup QpidListener implementations
        //The QpidSessionPool should be a Singleton. In general its used to 
        //produce qpid sessions per thread. Our requirement is that on one
        //server, we will run only single instance of every QpidListener implementation.
        //So here we get a specific session for it from the QpidSessionPool. This session is
        //tied to the thread of this ContextListener
        try {
            QpidSessionPool sessionPool = injector.getInstance(QpidSessionPool.class);
            ActivationListener actListener = injector.getInstance(ActivationListener.class);
            MessageConsumer mc = sessionPool.getSession().createConsumer(new AMQQueue(actListener.getQueueName()));
            mc.setMessageListener(actListener);
        } catch (Exception e1) {
            throw new RuntimeException("Failed to setup a QpidListener", e1);
        }

        // Initialize the Event sink AFTER the internal server has been
        // created and started.
        EventSink sink = injector.getInstance(EventSink.class);
        try {
            sink.initialize();
        }
        catch (Exception e) {
            log.error("Failed to initialize EventSink:", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param candlepinConfig
     * @return List of class names that will be configured as HornetQ listeners.
     */
    public static List<String> getEventListeners(
            org.candlepin.common.config.Configuration candlepinConfig) {
        //AMQP integration here - If it is disabled, don't add it to listeners.
        List<String> listeners = Lists.newArrayList(
                candlepinConfig.getList(ConfigProperties.AUDIT_LISTENERS));

        if (candlepinConfig
            .getBoolean(ConfigProperties.AMQP_INTEGRATION_ENABLED)) {
            listeners.add(AMQPBusPublisher.class.getName());
        }
        return listeners;
    }

}

