package org.candlepin.audit;

import javax.jms.MessageListener;

public interface QpidListener extends MessageListener {

    public String getQueueName();
}
