Steps to import into Eclipse
  1. mvn eclipse:eclipse
  2. mvn clean package
  3. Modify ./okhttp/pom.xml to add the following snippet of code within <dependencies></dependencies>: 
    <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.2.4</version>
    </dependency> 
  4. mkdir tmp

  In eclipse
  5. Install Maven plugin in Eclipse.
  6. Import the project as Maven project (Import -> Maven).
  7. Window -> Preferences -> Maven -> Errors/Warnings -> Plugin execution not covered by lifecycle configuration = Warning.


Code:
  All added/modified codes in OKHTTP should be within /*NetProphet*/ /* End NetProphet */
  All added files should be in package netprophet. (okhttp/src/main/java/netprophet/)

Debugging:
  1. import static okhttp3.internal.Internal.logger;
  2. logger.log(Level.INFO, "some strings");

Testing and Packaging:
  1. samples/DegugMain.java has main function to drive OkHTTP in desktop
  2. Make sure it passes OkHTTP's testing (do: mvn clean package), everytime makes a change.  ***
     Note that it will fail the testing if you enable logger, so close or comment logging statements before packaging. 
  3. After finishing "mvn clean package", copy the jar file (okhttp/target/okhttp-3.1.0-SNAPSHOT.jar) to Android Studio.



