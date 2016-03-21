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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

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
    private EventFilter eventFilter;
    private List<EventListener> listeners = new ArrayList<EventListener>();
    
    @Inject
    public EventSinkImpl(EventFilter eventFilter, EventFactory eventFactory) {
        this.eventFactory = eventFactory;
        this.eventFilter = eventFilter;
    }

    /**
     * Initializes the Singleton from the ContextListener not from the ctor.
     * @throws Exception thrown if there's a problem creating the session factory.
     */
    @Override
    public void initialize() throws Exception {
    }


    @Override
    public List<QueueStatus> getQueueInfo() {
        List<QueueStatus> results = new LinkedList<QueueStatus>();
        return results;
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
        for (EventListener l : listeners) {
            l.onEvent(event);
        }
            
    }


    @Override
    public void registerListener(EventListener instance) {
        listeners.add(instance);
    }
    
    /**
     * Dispatch queued events. (if there are any)
     *
     * Typically only called after a successful request or job execution.
     */
    @Override
    public void sendEvents() {
        log.debug("Committing transaction.");
        for (EventListener listener : listeners) {
            listener.commit();
        }
    }

    @Override
    public void rollback() {
        log.warn("Rolling back transaction.");
        for (EventListener listener : listeners) {
            listener.rollback();
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
