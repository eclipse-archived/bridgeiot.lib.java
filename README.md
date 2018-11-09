# Bridge.IoT Java Lib

## Step 1: Get the Bridge.IoT Lib


To start and get the API as a Java lib, you have two options: (1) you can get the Lib as source code, (2) you can import the JAR file as a dependency in your project.


### Get the Bridge.IoT Lib as Source Code:


1.  Do a Git checkout of the lib-java project:

        cd <workspace>
        git clone -b v0.9.5 --single-branch https://gitlab.com/BIG-IoT/lib-java.git

    **Note**: If you want to import lib-java in an IDE, \<workspace> should be accordingly your IDE workspace folder


2.  Import the lib-java project into your IDE (e.g., Eclipse):

    *Make sure have installed a gradle plugin (e.g. Buildship)*
    
    In Eclipse, choose: File -> Import -> Gradle Project -- Select <workspace>/lib-java directory. 
    
Checkout the examples in: <workspace>/lib-java/bridgeiot-lib-examples/src/main/java/org/eclipse/bridgeiot/lib/examples)


### Use the Bridge.IoT Lib as a JAR:

Alternatively to the source code, you can include the API lib as a dependency to your project. Just modify the 'build.gradle' file of your project and add the following Maven repository and dependencies:
 
	repositories {
	    maven {
			url "https://nexus.big-iot.org/content/repositories/releases/"
		}
	}
	dependencies {
    	compile group: 'org.eclipse.bridgeiot.lib', name: 'bridgeiot-lib-core', version: '0.9.5'
    	compile group: 'org.eclipse.bridgeiot.lib', name: 'bridgeiot-lib-advanced', version: '0.9.5'
    	compile group: 'org.eclipse.bridgeiot.lib', name: 'bridgeiot-lib-embeddedspark', version: '0.9.5'
    	compile group: 'org.eclipse.bridgeiot.lib', name: 'bridgeiot-lib-examples', version: '0.9.5'
	}
	
Checkout the examples [here](https://gitlab.com/BIG-IoT/lib-java/tree/master/bridgeiot-lib-examples/src/main/java/org/bridgeiot/lib/examples)

## Step 2: Run the Demo

**Note**: The marketplace requries authentication via Web Token which have to be set for consumer and provider accordingly in the code. For the moment the examples require the manual setup of the correct tokens in the code.

### Preparation of the examples

1. Go to the website of the [marketplace](https://market.big-iot.org/)
2. Login
3. Click on 'New Organization'
4. Enter a name for your organization
5. Click on 'My Providers' (maybe you have to reload the page via F5)
6. Click on 'Create a new Provider'
7. Enter the provider name
8. Click on 'Load Provider Secret'
9. Click on 'Copy Secret to Clipboard'
10. Open 'bridgeiot-lib-examples/src/main/java/org/bridgeiot/lib/examples/ExampleProvider.java'
11. Change constant PROVIDER_ID and PROVIDER_SECRET with the values from the Marketplace
12. Repeat step 5 to 11 by choosing 'Create a new Consumer' on the Marketplace Website and ExampleConsumer.java
13. If you are behind a proxy, support can be enabled via Java VM arguments (-Dhttps.proxyHost=localhost -Dhttps.proxyPort=3128);
alternatively you can add `provider.setProxy("127.0.0.1",3128);` inside the main method of the examples

Run the Example Provider and Consumer:

### In Eclipse, do the following in the **bridgeiot-lib-examples** project:

1.  Run `src/main/java/org/eclipse/bridgeiot/lib/examples/ExampleProvider.java` as a Java application
        
    *This creates a provider, who registers an offering at the marketplace and deploys it in the embedded web server Spark.*
    

2.  Run `src/main/java/org/eclipse/bridgeiot/lib/examples/ExampleConsumer.java` as a Java application

    *This creates a consumer, who discovers the offering and accesses it accordingly.*

## Step 3: Checkout some features

### Map the offering responses to your POJO

The Bridge.IoT Lib supports the mapping of the data fields of the offering response to your own Java object.

There are two ways using it

1. Annotate your custom class for offering responses
    1. Add @ResponseMappingType( <RDF Type>) before the respective fields in order to tell how to map.
    2. Add the line myResponsePojoList = yourAccessResponse.map()(MyAnnotatedPojo.class) to your consumer code in order to get a list with your custom POJOs
    3. Find an example in org.eclipse.bridgeiot.lib.examples.types.MyParkingResultPojoAnnotated

2. Define your mapping via API commands
    1. Create a mapping object via OutputMapping.create().addTypeMapping(<RDF typ>, <field name in your POJO>)
    2. Add the line myResponsePojoList = yourAccessResponse.map()(MyAnnotatedPojo.class, myOutputMapping) to your consumer code in order to get a list with your custom POJOs
        
Find an example in ExampleConsumer.java

## OPTIONAL: Use of Local Marketplace Instance

If you want to run a local Marketplace, you need to first deploy a local instance of the Bridge.IoT Marketplace according to the instructions presented at

[https://gitlab.com/BIG-IoT/LocalMarketplace](https://gitlab.com/BIG-IoT/LocalMarketplace).

**Note**: You have to set the URLs of the marketplace in the code accordingly


## Links

- [JavaDoc of lib-java API](https://big-iot.gitlab.io/lib-java/). Note: The JavaDoc is updated automatically after each successful build.


