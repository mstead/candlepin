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

import java.util.Set;

import javax.inject.Inject;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.candlepin.common.config.Configuration;
import org.candlepin.model.Consumer;
import org.candlepin.model.Entitlement;
import org.candlepin.model.Owner;
import org.candlepin.model.Pool;
import org.candlepin.model.Rules;
import org.candlepin.model.activationkeys.ActivationKey;
import org.candlepin.model.dto.Subscription;
import org.candlepin.policy.js.compliance.ComplianceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;

/**
 * EventSink - Queues events to be sent after request/job completes, and handles actual
 * sending of events on successful job or API request, as well as rollback if either fails.
 */
@Singleton
public class EventSinkImpl implements EventSink {

    private static Logger log = LoggerFactory.getLogger(EventSinkImpl.class);
    private EventFactory eventFactory;
    private TopicConnection topicConnection;
    private ObjectMapper mapper;
    private EventFilter eventFilter;
    private Topic listenerEventTopic;

    /*
     * Important use of ThreadLocal here, each Tomcat/Quartz thread gets it's own session
     * which is reused across invocations. Sessions must have commit or rollback called
     * on them per request/job. This is handled in EventFilter for the API, and KingpinJob
     * for quartz jobs.
     */
    private ThreadLocal<TopicSession> sessions = new ThreadLocal<TopicSession>();
    private ThreadLocal<TopicPublisher> producers = new ThreadLocal<TopicPublisher>();



    @Inject
    public EventSinkImpl(EventFilter eventFilter, EventFactory eventFactory,
            ObjectMapper mapper) {
        this.eventFactory = eventFactory;
        this.mapper = mapper;
        this.eventFilter = eventFilter;
        
    }

    /**
     * Initializes the Singleton from the ContextListener not from the ctor.
     * @throws Exception thrown if there's a problem creating the session factory.
     */
    @Override
    public void initialize() throws Exception {
        topicConnection = null;//tbd; 
        listenerEventTopic=null;//tbd
    }


    protected TopicSession getClientSession() {
        TopicSession session = sessions.get();
        if (session == null ) {
            try {
                /*
                 * Use a transacted HornetQ session, events will not be dispatched until
                 * commit() is called on it, and a call to rollback() will revert any queued
                 * messages safely and the session is then ready to start over the next time
                 * the thread is used.
                 */
                session = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.info("Created new HornetQ session.");
            sessions.set(session);
        }
        return session;
    }

    protected TopicPublisher getClientProducer() {
        TopicPublisher producer = producers.get();
        if (producer == null) {
            try {
                producer = getClientSession().createPublisher(listenerEventTopic);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.info("Created new HornetQ producer.");
            producers.set(producer);
        }
        return producer;
    }


    /**
     * Adds an event to the queue to be sent on successful completion of the request or job.
     * sendEvents() must be called for these events to actually go out. This happens
     * automatically after each successful REST API request, and KingpingJob. If either
     * is not successful, rollback() must be called.
     *
     * Events are filtered, meaning that some of them might not even get into HornetQ.
     * Details about the filtering are documented in EventFilter class
     *
     * HornetQ transaction actually manages the queue of events to be sent.
     */
    @Override
    public void queueEvent(Event event) {
        if (eventFilter.shouldFilter(event)) {
            log.debug("Filtering event {}", event);
            return;
        }

        log.debug("Queuing event: {}", event);

        try {
            TopicSession session = getClientSession();
            TextMessage message = session.createTextMessage();
            String eventString = mapper.writeValueAsString(event);
            message.setText(eventString);

            // NOTE: not actually send until we commit the session.
            getClientProducer().send(message);
        }
        catch (Exception e) {
            log.error("Error while trying to send event: " + event, e);
        }
    }

    /**
     * Dispatch queued events. (if there are any)
     *
     * Typically only called after a successful request or job execution.
     */
    @Override
    public void sendEvents() {
        try {
            log.debug("Committing hornetq transaction.");
            getClientSession().commit();
        }
        catch (Exception e) {
            // This would be pretty bad, but we always try not to let event errors
            // interfere with the operation of the overall application.
            log.error("Error committing hornetq transaction", e);
        }
    }

    @Override
    public void rollback() {
        log.warn("Rolling back hornetq transaction.");
        try {
            TopicSession session = getClientSession();
            session.rollback();
        }
        catch (Exception e) {
            log.error("Error rolling back hornetq transaction", e);
        }
    }

    public void emitConsumerCreated(Consumer newConsumer) {
        Event e = eventFactory.consumerCreated(newConsumer);
        queueEvent(e);
    }

    public void emitOwnerCreated(Owner newOwner) {
        Event e = eventFactory.ownerCreated(newOwner);
        queueEvent(e);
    }

    public void emitOwnerMigrated(Owner owner) {
        Event e = eventFactory.ownerMigrated(owner);
        queueEvent(e);
    }

    public void emitPoolCreated(Pool newPool) {
        Event e = eventFactory.poolCreated(newPool);
        queueEvent(e);
    }

    public void emitExportCreated(Consumer consumer) {
        Event e = eventFactory.exportCreated(consumer);
        queueEvent(e);
    }

    public void emitImportCreated(Owner owner) {
        Event e = eventFactory.importCreated(owner);
        queueEvent(e);
    }

    public void emitActivationKeyCreated(ActivationKey key) {
        Event e = eventFactory.activationKeyCreated(key);
        queueEvent(e);
    }

    public void emitSubscriptionExpired(Subscription subscription) {
        Event e = eventFactory.subscriptionExpired(subscription);
        queueEvent(e);
    }

    @Override
    public void emitRulesModified(Rules oldRules, Rules newRules) {
        queueEvent(eventFactory.rulesUpdated(oldRules, newRules));
    }

    @Override
    public void emitRulesDeleted(Rules rules) {
        queueEvent(eventFactory.rulesDeleted(rules));
    }

    @Override
    public void emitCompliance(Consumer consumer,
            Set<Entitlement> entitlements, ComplianceStatus compliance) {
        queueEvent(eventFactory.complianceCreated(consumer, entitlements, compliance));
    }
}
