#NetProphet:
NetProphet is a library supporting networking delay diagnosis and optimization. We will provide customized packages for different HTTP libraries, minimizing the deployment burden of developers. This package is for okhttp3, so it’s backward compatible to okhttp functionalities.

In this version, NetProphet will breakdown the delays into 1. DNS delay, 2. TCP handshake delay, 3. TLS handshake delay, 4. request upload delay, 5. time-to-first-byte (TTFB) delay, 6. server delay (a part of TTFB delay) and 7. response transmission delay. The delay information will be cached onto app and uploaded to server: http://netprophet.xcdu.me/ when the number of requests have achieved a threshold and the user is switched on WIFI.

##Usage:
* Add the jar file okhttp/target/okhttp-3.1.0-SNAPSHOT.jar into your project:
```
    cp libs/okhttp-3.1.0-SNAPSHOT.jar <app-directory>/app/libs/
    cp libs/dnsjava-2.1.7.jar <app-directory>/app/libs/
```
	Also, guarantee the app has the following permissions:
    INTERNET, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE, ACCESS_NETWORK_STATE, READ_PHONE_STATE

* Initialization: In the beginning of the application (onCreate(…) method), add the following line of code:
```
    //The second argument indicates no optimization. 
    //Optimization progress is still ongoing.
    NetProphet.initializeNetProphet(getApplicationContext(), false); 
```
* Build and send request: NetProphet keeps all the usage of okhttp3.
```
    For example, send a synchronous GET request:
      Request request = new Request.Builder()
        .url("http://publicobject.com/helloworld.txt”)
        .build();
      Response response = client.newCall(request).execute();

    Send a POST request:
      RequestBody body = RequestBody.create(JSON, json);
      Request request = new Request.Builder()
        .url(url)
        .post(body)
        .build();
      Response response = client.newCall(request).execute();
```
    
    More usage can be found via this link: https://github.com/square/okhttp/wiki/Recipes

* Read response in non-streaming approach: this approach is convenient, but all contents will be read into memory, so it’s ideal for data less than 1MB.

```
    Plaintext:
      String contents = response.body().string();
    Image:
      Bitmap map = response.body().bitmap();
    Other binary:
      not supported for non-streaming approach. 
```

* Read response in streaming approach:  this approach will read contents as a stream, but it requires developer to explicitly inform NetProphet the end of the stream.

```
    Plaintext:
      InputStream is = response.body().charStream();
    Binary:
        InputStream is = response.body().byteStream();

    /* Note: in order to tell NetProphet the ending time of reading streaming contents, 
     * you need to inform the end of the stream by calling the following function when
     * streaming contents are all read: 
     * Arguments:
     *  respSize is the size of the contents;
     *  errorMsg: developer can specify customized error msg.
     *    By default, errorMsg is set as null;
     *  respEndTime: the ending timestamp. 
     *    By default, this value is set as null and NetProphet will use current 
     *    timestamp as the ending time.
     */
    
    response.body().informFinishedReadingResponse(int respSize, String errorMsg, int respEndTime );


```

##Development:
When importing NetPropohet, dnsjava is required

Steps to import into Eclipse
  1. mvn eclipse:eclipse
  2. mvn clean package
  3. Modify ./okhttp/pom.xml to add the following snippet of code within <dependencies></dependencies>: 
    <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.2.4</version>
    </dependency> 
    <dependency>
      <groupId>dnsjava</groupId>
      <artifactId>dnsjava</artifactId>
      <version>2.1.7</version>
    </dependency>
  4. mkdir tmp

  In eclipse
  5. Install Maven plugin in Eclipse.
  6. Import the project as Maven project (Import -> Maven).
  7. Window -> Preferences -> Maven -> Errors/Warnings -> Plugin execution not covered by lifecycle configuration = Warning.


