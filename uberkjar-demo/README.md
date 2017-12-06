# Demo: Create an uber-kjar and deploy it

This project demonstrates how to bundle a kjar and all of its dependencies into one uber-jar and deploy it to the Kie Server.  For more information, please see my blog post: https://adotpalindrome.wordpress.com/2017/12/06/how-to-create-an-uber-jar-out-of-a-kjar-and-its-dependencies/

### Prerequisites

You should download/install the following:

WildFly 10.1.0:
http://wildfly.org/downloads/

Kie Server 6.4.x (you can build the war directly from our repo -- you will want the ee7 one): 
https://github.com/kiegroup/droolsjbpm-integration/tree/6.5.x/kie-server-parent/kie-server-wars/kie-server


### Setup

1.  Place the Kie Server war in the deployment directory of Wildfly
2.  Rename the war to kie-server.war
3.  Assuming you have already setup management users/passwords, start your wildfly instance with the following command from /bin:
```
./standalone.sh --server-config=standalone-full.xml -Dorg.kie.server.id=demo-kie-server -Dorg.kie.server.location=http://localhost:8080/kie-server/services/rest/server
``` 
4.  In the demo-tests module, modify TestDemo's username/password parameters to reflect your own

## Running the tests

Simply run the following from this directory:

```
mvn clean install
```
In addition to successful tests, you should see the following in the server output:

```
09:36:45,434 INFO  [stdout] (default task-6) Hello Anna Baker!
09:36:45,434 INFO  [stdout] (default task-6) You are rich!
```
