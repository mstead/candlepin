package org.candlepin.audit;

import org.apache.qpid.client.AMQConnectionFactory;
import org.apache.qpid.jms.BrokerDetails;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * A test that uses real Qpid to query objects
 * @author fnguyen
 *
 */
public class QpidQmfTest {
    private Connection connection = null;
    private QpidQmf qpidQmf = null;
    
    @Before
    public void setupQpidConnection() throws NamingException, JMSException, URISyntaxException{
        Context ctx = new InitialContext(buildConfigurationProperties());
        AMQConnectionFactory connectionFactory = createConnectionFactory(ctx);
        connection = connectionFactory.createConnection();
        connection.start();
        
        qpidQmf = new QpidQmf(connection);
    }
    private AMQConnectionFactory createConnectionFactory(Context ctx)
        throws NamingException {

        AMQConnectionFactory connectionFactory = (AMQConnectionFactory) ctx.lookup("qpidConnectionfactory");
        for (BrokerDetails broker : connectionFactory.getConnectionURL().getAllBrokerDetails()) {
            broker.setProperty("trust_store", "/etc/candlepin/certs/amqp/candlepin.truststore");
            broker.setProperty("trust_store_password", "password");
            broker.setProperty("key_store", "/etc/candlepin/certs/amqp/candlepin.jks");
            broker.setProperty("key_store_password", "password");
            broker.setProperty("retries", "0");
            broker.setProperty("connectdelay", "1");
        }
        return connectionFactory;
    }
    
    private Properties buildConfigurationProperties() {
        Properties properties = new Properties();
        properties.put("java.naming.factory.initial",
            "org.apache.qpid.jndi.PropertiesFileInitialContextFactory");
        properties.put("connectionfactory.qpidConnectionfactory",
            "amqp://guest:guest@localhost/test?sync_publish='persistent'&brokerlist='" +
                "tcp://localhost:5671?ssl='true'&ssl_cert_alias='candlepin'" + "'");
        return properties;
    }

    @Test
    public void qmfBasics() throws JMSException{
        System.out.println(qpidQmf.getExchangeBoundQueueNames("event"));
    }

    @Test
    public void qmfQueueInfo() throws JMSException{
        qpidQmf.getQueueInfo("org.apache.qpid.broker:queue:qconsume");
    }
    
    
    @Test
    public void connectionWorks(){
        Assert.assertNotNull(connection);
    }

}
