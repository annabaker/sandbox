package org.demo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.demo.model.Person;
import org.drools.core.command.impl.GenericCommand;
import org.drools.core.command.runtime.BatchExecutionCommandImpl;
import org.drools.core.command.runtime.rule.FireAllRulesCommand;
import org.drools.core.command.runtime.rule.InsertObjectCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.kie.remote.common.rest.KieRemoteHttpRequestException;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponse.ResponseType;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesException;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestDemo {
	
    private Logger log = LoggerFactory.getLogger( TestDemo.class );
	
    private static final String KIE_SERVER_URL = "http://localhost:8080/kie-server/services/rest/server";
    
    // set these parameters to the appropriate username/password
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    
    private static final String G = "org.demo";
    private static final String A = "demo-kjar";
    private static final String V = "1.0-SNAPSHOT";
    
    private static final String CONTAINER_ID = "demo";
    
    private KieServicesClient client;
    private RuleServicesClient ruleClient;
    private List<KieContainerResource> containers;
    private ServiceResponse<KieContainerResourceList> response;

	@Before
	public void setup() throws Exception {
		
		KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(KIE_SERVER_URL, USERNAME, PASSWORD);
		
		Set<Class<?>> allClasses = new HashSet<Class<?>>();
		allClasses.add(Person.class);
	
		config.addExtraClasses(allClasses);

		try {
			client  = KieServicesFactory.newKieServicesClient(config);
		}
		catch (KieRemoteHttpRequestException e) {
			fail("Please ensure that you have started a Wildfly instance and that your Kie Server has deployed properly.");
		}
		catch (KieServicesException e) {
			fail("Please ensure that your kie server username and password correct.");
		}
		
 		ruleClient = client.getServicesClient(RuleServicesClient.class);
		    
		KieContainerResource resource = new KieContainerResource(CONTAINER_ID, new ReleaseId(G, A, V));
		client.createContainer(CONTAINER_ID, resource);   
    }

	
	@After
	public void teardown() {

		if (containers != null && containers.size() > 0) {
			for (KieContainerResource container : containers) {
				log.info("Disposing container {}:" + container.getContainerId());
				client.disposeContainer(container.getContainerId());
			}
		}				
	}

	
	@Test
	public void testKieContainerShouldHaveDeployedSuccessfully() {
		
		  response = client.listContainers();
	      containers = response.getResult().getContainers();
		  
		  assertTrue(containers.size() > 0);
	}
				
	@Test
	public void testThatRulesSuccessfullyFire() {
	  
      Person p = new Person();
      p.setWage(12);
      p.setFirstName("Anna");
      p.setLastName("Baker");
      p.setHourlyRate(10);
      
      List<GenericCommand<?>> commands = new ArrayList<GenericCommand<?>>();
      
      InsertObjectCommand insertObjectCommand = new InsertObjectCommand();
      insertObjectCommand.setObject(p);
      
      FireAllRulesCommand fireAllRulesCommand = new FireAllRulesCommand();
      
      commands.add(insertObjectCommand);
      commands.add(fireAllRulesCommand);
      
      BatchExecutionCommandImpl executionCommand = new BatchExecutionCommandImpl(commands);
   
      ServiceResponse<ExecutionResults> results = ruleClient.executeCommandsWithResults(CONTAINER_ID, executionCommand);      
      assertTrue(results.getType() == ResponseType.SUCCESS);
      
	}
	
    @Test
    public void testUberKjarShouldContainTransitiveDependencies() throws IOException, URISyntaxException {
    	
    	String testJar = A + "-" + V + ".jar";
    	List<String> classNames = new ArrayList<String>();
    	
    	ClassLoader classLoader = getClass().getClassLoader();
    	File file = new File(classLoader.getResource(testJar).getFile());
    	System.out.println(file.getAbsolutePath());
    		
    	ZipFile zipFile = new ZipFile(file);
    	try
    	{
    	    final Enumeration<? extends ZipEntry> entries = zipFile.entries();
    	    
    	    while (entries.hasMoreElements()) {
    	    	ZipEntry entry = entries.nextElement();
    	    	if (!entry.isDirectory() && entry.getName().endsWith("class")) {
    	    		String className = entry.getName().replace('/', '.');
    	    		classNames.add(className.substring(0, className.length() - ".class".length()));		
    	    	}
    	    }
    	}
    	finally
    	{
    	    zipFile.close();
    	}
    	
    	// dependency tree for cucumber-junit 
    	// [INFO] +- info.cukes:cucumber-junit:jar:1.2.5:compile
    	// [INFO] |  +- info.cukes:cucumber-core:jar:1.2.5:compile
    	// [INFO] |  |  +- info.cukes:cucumber-html:jar:0.2.3:compile
    	// [INFO] |  |  +- info.cukes:cucumber-jvm-deps:jar:1.0.5:compile
    	// [INFO] |  |  \- info.cukes:gherkin:jar:2.12.2:compile

        // direct dependency of model
    	assertTrue(classNames.contains("cucumber.api.junit.Cucumber"));
    	// transitive dependency of model
    	assertTrue(classNames.contains("gherkin.util.Mapper"));
    	// provided dependency of model
    	assertFalse(classNames.contains("io.swagger.annotations.Api"));
    }
}
 
