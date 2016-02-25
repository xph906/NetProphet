Usage:
  Permission: 
/*
 *  android.permission.INTERNET
 *  android.permission.ACCESS_COARSE_LOCATION
 *  android.permission.ACCESS_FINE_LOCATION
 *  android.permission.ACCESS_WIFI_STATE
 *  android.permission.ACCESS_NETWORK_STATE
 *  android.permission.READ_PHONE_STATE
 *  android.permission.GET_TASKS
 * */
  Initialization:
    OkHttpClient.initializeNetProphet(getApplicationContext());



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



BUG:
  1.W/System.err: Callback failure for call to http://www.bing.com/... 
    https://www.reddit.com/... 
    http://www.cnet.com/...
    http://www.adf.ly/...
    *http://www.gamersky.com/....
    *http://www.mydrivers.com/...
  2. Error inserting server_delay=-1 is_failed_req=1 tls_delay=0 use_resp_cache=0 ttfb_delay=0 trans_id=0 prev_req_id=0 req_size=0 http_code=0 dns_delay=7 next_req_id=0 req_id=1454640388031 user_id=357143045760714 detailed_error_msg=okhttp3.internal.http.RouteException: java.net.SocketTimeoutException: failed to connect to www.google.it/216.58.221.227 (port 443) after 10000ms conn_delay=-1454640550985 use_dns_cache=0 resp_trans_delay=0 resp_size=0 use_conn_cache=0 overall_delay=-1454640550978 error_msg=CONN_ERR url=https://www.google.it/?gws_rd=ssl req_write_delay=0 handshake_delay=0 end_time=0 start_time=1454640550978 method=GET trans_type=0
  E/SQLiteDatabase: android.database.sqlite.SQLiteConstraintException: PRIMARY KEY must be unique (code 19)



