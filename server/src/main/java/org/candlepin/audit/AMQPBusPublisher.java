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
import org.candlepin.guice.QpidSessionPoolProvider;
import org.candlepin.util.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

/**
 * An EventListener that publishes events to an AMQP bus (qpid).
 */
public class AMQPBusPublisher implements EventListener {
    private static Logger log = LoggerFactory.getLogger(AMQPBusPublisher.class);
    private ObjectMapper mapper;
    private QpidSessionPool sessionPool;
    
    @Inject
    public AMQPBusPublisher(QpidSessionPool sessionPool, ObjectMapper omapper) {
        this.sessionPool = sessionPool;
        this.mapper = omapper;
    }

    @Override
    public void onEvent(Event e) {
        TextMessage tm;
        try {
            tm = sessionPool.getSession().createTextMessage(this.apply(e));

            TopicPublisher tp = sessionPool.getTopicPublisher(
                    QpidSessionPoolProvider.getTopicName(e.getType(),e.getTarget()));

            if (tp != null) {
                log.debug("Sending event to topic publisher: {}", e);
                tp.send(tm);
            } else {
                log.warn("TopicPublisher is NULL!");
            }
        } catch (JsonProcessingException e1) {
            throw new RuntimeException(e1);
        } catch (JMSException e1) {
            throw new RuntimeException(e1);
        }

    }

    public String apply(Event event) throws JsonProcessingException {
        return mapper.writeValueAsString(event);
    }
    
    @Override
    public void commit() {
        try {
            sessionPool.getSession().commit();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void rollback() {
        try {
            sessionPool.getSession().rollback();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
