package org.anbaker;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;
import org.kie.server.api.model.KieServerStateInfo;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponse.ResponseType;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

public class TestRemoteAPI {

	private static final String REMOTING_URL = "http://the.server.ip.address:8080/kie-server/services/rest/server";

	private static final String USER = "admin";
	private static final String PASSWORD = "admin";

	private static final String CONNECTION_FACTORY = new String("jms/RemoteConnectionFactory");
	private static final String REQUEST_QUEUE_JNDI = new String("jms/queue/KIE.SERVER.REQUEST");
	private static final String RESPONSE_QUEUE_JNDI = new String("jms/queue/KIE.SERVER.RESPONSE");

	private KieServicesConfiguration conf;
	private KieServicesClient kieServicesClient;

	private static InitialContext getRemoteInitialContext(URL url, String user, String password) {

		Properties initialProps = new Properties();

		initialProps.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY,
				"org.jboss.naming.remote.client.InitialContextFactory");
		initialProps.setProperty(InitialContext.PROVIDER_URL, "remote://" + url.getHost() + ":4447");
		initialProps.setProperty(InitialContext.SECURITY_PRINCIPAL, user);
		initialProps.setProperty(InitialContext.SECURITY_CREDENTIALS, password);

		for (Object keyObj : initialProps.keySet()) {
			String key = (String) keyObj;
			System.setProperty(key, (String) initialProps.get(key));
		}
		try {
			return new InitialContext(initialProps);
		} catch (NamingException e) {
			throw new IllegalStateException("Could not construct initial context for JMS", e);
		}
	}

	@Test
	public void testClientConnectionToRemoteKieServer() throws Exception {

		InitialContext context = getRemoteInitialContext(new URL(REMOTING_URL), USER, PASSWORD);

		Queue requestQueue = (Queue) context.lookup(REQUEST_QUEUE_JNDI);
		Queue responseQueue = (Queue) context.lookup(RESPONSE_QUEUE_JNDI);
		ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(CONNECTION_FACTORY);

		conf = KieServicesFactory.newJMSConfiguration(connectionFactory, requestQueue, responseQueue, USER, PASSWORD);

		// you will need to add any custom classes needed for your kjars
		// Set<Class<?>> extraClassList = new HashSet<Class<?>>();
		// extraClassList.add(YourCustomClass.class);
		// conf.addExtraClasses(extraClassList);

		kieServicesClient = KieServicesFactory.newKieServicesClient(conf);

		ServiceResponse<KieServerStateInfo> response = kieServicesClient.getServerState();

		assertEquals(ResponseType.SUCCESS, response.getType());
	}
}
