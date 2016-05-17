/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import netprophet.NetProphetDns;
import netprophet.NetProphetOptimizationTool;
import netprophet.NetProphetOptimizationTool.OptimiztionInterceptor;
import netprophet.NetProphetPropertyManager;
import netprophet.NetProphetTestingDns;
import netprophet.NetUtility;
import android.content.Context;
import okhttp3.internal.Internal;
import okhttp3.internal.Internal.NetProphetLogger;
import okhttp3.internal.InternalCache;
import okhttp3.internal.RouteDatabase;
import okhttp3.internal.Util;
import okhttp3.internal.http.StreamAllocation;
import okhttp3.internal.io.RealConnection;
import okhttp3.internal.tls.OkHostnameVerifier;
import static okhttp3.internal.Internal.logger;
/**
 * Factory for {@linkplain Call calls}, which can be used to send HTTP requests and read their
 * responses. Most applications can use a single OkHttpClient for all of their HTTP requests,
 * benefiting from a shared response cache, thread pool, connection re-use, etc.
 *
 * <p>To create an {@code OkHttpClient} with the default settings, use the {@linkplain
 * #OkHttpClient() default constructor}. Or create a configured instance with {@link
 * OkHttpClient.Builder}. To adjust an existing client before making a request, use {@link
 * #newBuilder()}. This example shows a call with a 30 second timeout:
 * <pre>   {@code
 *
 *   OkHttpClient client = ...
 *   OkHttpClient clientWith30sTimeout = client.newBuilder()
 *       .readTimeout(30, TimeUnit.SECONDS)
 *       .build();
 *   Response response = clientWith30sTimeout.newCall(request).execute();
 * }</pre>
 */
public final class OkHttpClient implements Cloneable, Call.Factory {
  private static final List<Protocol> DEFAULT_PROTOCOLS = Util.immutableList(
      Protocol.HTTP_2, Protocol.SPDY_3, Protocol.HTTP_1_1);

  private static final List<ConnectionSpec> DEFAULT_CONNECTION_SPECS = Util.immutableList(
      ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT);

  static {
    Internal.instance = new Internal() {
      @Override public void addLenient(Headers.Builder builder, String line) {
        builder.addLenient(line);
      }

      @Override public void addLenient(Headers.Builder builder, String name, String value) {
        builder.addLenient(name, value);
      }

      @Override public void setCache(OkHttpClient.Builder builder, InternalCache internalCache) {
        builder.setInternalCache(internalCache);
      }

      @Override public InternalCache internalCache(OkHttpClient client) {
        return client.internalCache();
      }

      @Override public boolean connectionBecameIdle(
          ConnectionPool pool, RealConnection connection) {
        return pool.connectionBecameIdle(connection);
      }

      @Override public RealConnection get(
          ConnectionPool pool, Address address, StreamAllocation streamAllocation) {
        return pool.get(address, streamAllocation);
      }

      @Override public void put(ConnectionPool pool, RealConnection connection) {
        pool.put(connection);
      }

      @Override public RouteDatabase routeDatabase(ConnectionPool connectionPool) {
        return connectionPool.routeDatabase;
      }

      @Override
      public void callEnqueue(Call call, Callback responseCallback, boolean forWebSocket) {
        ((RealCall) call).enqueue(responseCallback, forWebSocket);
      }

      @Override public StreamAllocation callEngineGetStreamAllocation(Call call) {
        return ((RealCall) call).engine.streamAllocation;
      }

      @Override
      public void apply(ConnectionSpec tlsConfiguration, SSLSocket sslSocket, boolean isFallback) {
        tlsConfiguration.apply(sslSocket, isFallback);
      }

      @Override public HttpUrl getHttpUrlChecked(String url)
          throws MalformedURLException, UnknownHostException {
        return HttpUrl.getChecked(url);
      }
    };
  }

  final Dispatcher dispatcher;
  final Proxy proxy;
  final List<Protocol> protocols;
  final List<ConnectionSpec> connectionSpecs;
  final List<Interceptor> interceptors;
  final List<Interceptor> networkInterceptors;
  final ProxySelector proxySelector;
  final CookieJar cookieJar;
  final Cache cache;
  final InternalCache internalCache;
  final SocketFactory socketFactory;
  final SSLSocketFactory sslSocketFactory;
  final HostnameVerifier hostnameVerifier;
  final CertificatePinner certificatePinner;
  final Authenticator proxyAuthenticator;
  final Authenticator authenticator;
  final ConnectionPool connectionPool;
  final Dns dns;
  final boolean followSslRedirects;
  final boolean followRedirects;
  final boolean retryOnConnectionFailure;
   int connectTimeout;
   int readTimeout;
  final int writeTimeout;

  /* NetProphet */
  Context context;
  NetProphetPropertyManager propertyManager;
  NetProphetOptimizationTool optimizationTool;
  static Context staticContext;
  static NetProphetDns netProphetDns = null;
  static NetProphetTestingDns netProphetTestingDns = null;
  static NetUtility netUtility;
 
  public static void initializeNetProphet(Context context, 
		  boolean enableOptimization,
		  boolean enableTestingMode){
	  NetUtility.getInstance(context, null);
	  OkHttpClient.staticContext = context;
	  if(enableTestingMode){
		  netProphetTestingDns = new NetProphetTestingDns();
		  //inTestingMode = true;
	  }
	  else if(enableOptimization){
		  netProphetDns = new NetProphetDns();
		  //inTestingMode = false;
	  }
	  else{
		  netProphetDns = null; //enable default DNS.
		  //inTestingMode = false;
	  }
  }
  public static void disableLogger(){
	  //logger.
  }
  public static void initializeNetProphetDesktop(boolean enableOptimization, boolean enableTestingMode){
	  OkHttpClient.staticContext = null;
	  if(enableTestingMode){
		  netProphetTestingDns = new NetProphetTestingDns();
		  //inTestingMode = true;
	  }
	  else if(enableOptimization){
		  netProphetDns = new NetProphetDns();
		  //inTestingMode = false;
	  }
	  else{
		  netProphetDns = null; //enable default DNS.
	  }
  }
  /*public static void initializeNetProphet(Context context, NetProphetDns  dns){
	  NetUtility.getInstance(context, null);
	  OkHttpClient.staticContext = context;
	  netProphetDns = dns;
  }*/
  public static NetProphetDns getNetProphetDns(){
	  return netProphetDns;
  }
 
  
  /* End NetProphet*/
  public OkHttpClient() {
    this(new Builder());
  }

  private OkHttpClient(Builder builder) {
    this.dispatcher = builder.dispatcher;
    this.proxy = builder.proxy;
    this.protocols = builder.protocols;
    this.connectionSpecs = builder.connectionSpecs;
    //this.interceptors = Util.immutableList(builder.interceptors);
    this.interceptors = builder.interceptors;
    this.networkInterceptors = Util.immutableList(builder.networkInterceptors);
    this.proxySelector = builder.proxySelector;
    this.cookieJar = builder.cookieJar;
    this.cache = builder.cache;
    this.internalCache = builder.internalCache;
    this.socketFactory = builder.socketFactory;
    
    /* NetProphet */
    this.context = staticContext;
    this.propertyManager = NetProphetPropertyManager.getInstance();
    this.optimizationTool = NetProphetOptimizationTool.getInstance();
    /* End NetProphet */

    boolean isTLS = true;
    for (ConnectionSpec spec : connectionSpecs) {
      isTLS = isTLS || spec.isTls();
    }

    if (builder.sslSocketFactory != null || !isTLS) {
      this.sslSocketFactory = builder.sslSocketFactory;
    } else {
      try {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        this.sslSocketFactory = sslContext.getSocketFactory();
      } catch (GeneralSecurityException e) {
        throw new AssertionError(); // The system has no TLS. Just give up.
      }
    }
    this.hostnameVerifier = builder.hostnameVerifier;
    this.certificatePinner = builder.certificatePinner;
    this.proxyAuthenticator = builder.proxyAuthenticator;
    this.authenticator = builder.authenticator;
    this.connectionPool = builder.connectionPool;
    this.dns = builder.dns;
    this.followSslRedirects = builder.followSslRedirects;
    this.followRedirects = builder.followRedirects;
    this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
    this.connectTimeout = builder.connectTimeout;
    this.readTimeout = builder.readTimeout;
    this.writeTimeout = builder.writeTimeout;
  }

  /* NetProphet */
  public Context getContext() {
	return context;
}

public void setContext(Context context) {
	this.context = context;
}

  /* NetProphet */

/** Default connect timeout (in milliseconds). */
  public int connectTimeoutMillis() {
    return connectTimeout;
  }

  /** Default read timeout (in milliseconds). */
  public int readTimeoutMillis() {
    return readTimeout;
  }

  /** Default write timeout (in milliseconds). */
  public int writeTimeoutMillis() {
    return writeTimeout;
  }

  public Proxy proxy() {
    return proxy;
  }

  public ProxySelector proxySelector() {
    return proxySelector;
  }

  public CookieJar cookieJar() {
    return cookieJar;
  }

  public Cache cache() {
    return cache;
  }

  InternalCache internalCache() {
    return cache != null ? cache.internalCache : internalCache;
  }

  public Dns dns() {
    return dns;
  }

  public SocketFactory socketFactory() {
    return socketFactory;
  }

  public SSLSocketFactory sslSocketFactory() {
    return sslSocketFactory;
  }

  public HostnameVerifier hostnameVerifier() {
    return hostnameVerifier;
  }

  public CertificatePinner certificatePinner() {
    return certificatePinner;
  }

  public Authenticator authenticator() {
    return authenticator;
  }

  public Authenticator proxyAuthenticator() {
    return proxyAuthenticator;
  }

  public ConnectionPool connectionPool() {
    return connectionPool;
  }

  public boolean followSslRedirects() {
    return followSslRedirects;
  }

  public boolean followRedirects() {
    return followRedirects;
  }

  public boolean retryOnConnectionFailure() {
    return retryOnConnectionFailure;
  }

  public Dispatcher dispatcher() {
    return dispatcher;
  }

  public List<Protocol> protocols() {
    return protocols;
  }

  public List<ConnectionSpec> connectionSpecs() {
    return connectionSpecs;
  }

  /**
   * Returns an immutable list of interceptors that observe the full span of each call: from before
   * the connection is established (if any) until after the response source is selected (either the
   * origin server, cache, or both).
   */
  public List<Interceptor> interceptors() {
    return interceptors;
  }

  /**
   * Returns an immutable list of interceptors that observe a single network request and response.
   * These interceptors must call {@link Interceptor.Chain#proceed} exactly once: it is an error for
   * a network interceptor to short-circuit or repeat a network request.
   */
  public List<Interceptor> networkInterceptors() {
    return networkInterceptors;
  }

  /**
   * Prepares the {@code request} to be executed at some point in the future.
   */
  @Override public Call newCall(Request request) {
	  /*NetProphet*/
	  RealCall rc = new RealCall(this, request);
	  if(!propertyManager.isEnableOptimization())
		  return rc;
	  //deals with header, e.g., gzip
	  boolean optInterceptorIncluded = false;
	  for(Interceptor interceptor : interceptors){
		  if (interceptor.getClass().equals(OptimiztionInterceptor.class)){
			  ((OptimiztionInterceptor)interceptor).setRealCall(rc);
			  optInterceptorIncluded = true;
		  }
	  }
	  if(!optInterceptorIncluded)
		  interceptors.add(optimizationTool.newOptimiztionInterceptor(rc));
	  int connTimeout = optimizationTool.getOptimizedConnectionTimeout();
	  if( connTimeout != 0){
		  this.connectTimeout = connTimeout;
	  }
	  
	  int dnsTimeout = optimizationTool.getOptimizedDNSTimeout();
	  if (dnsTimeout != 0){
		  //TODO: set NetProphetDns's timeout.
	  }
	  
	  int recvTimeout = optimizationTool.getOptimizedRecvTimeout();
	  if (recvTimeout != 0){
		  this.readTimeout = recvTimeout;
	  }
	  /*End NetProphet*/
    return rc;
  }

  public Builder newBuilder() {
    return new Builder(this);
  }

  public static final class Builder {
    Dispatcher dispatcher;
    Proxy proxy;
    List<Protocol> protocols;
    List<ConnectionSpec> connectionSpecs;
    final List<Interceptor> interceptors = new ArrayList<>();
    final List<Interceptor> networkInterceptors = new ArrayList<>();
    ProxySelector proxySelector;
    CookieJar cookieJar;
    Cache cache;
    InternalCache internalCache;
    SocketFactory socketFactory;
    SSLSocketFactory sslSocketFactory;
    HostnameVerifier hostnameVerifier;
    CertificatePinner certificatePinner;
    Authenticator proxyAuthenticator;
    Authenticator authenticator;
    ConnectionPool connectionPool;
    Dns dns;
    boolean followSslRedirects;
    boolean followRedirects;
    boolean retryOnConnectionFailure;
    int connectTimeout;
    int readTimeout;
    int writeTimeout;

    public Builder() {
      dispatcher = new Dispatcher();
      protocols = DEFAULT_PROTOCOLS;
      connectionSpecs = DEFAULT_CONNECTION_SPECS;
      proxySelector = ProxySelector.getDefault();
      cookieJar = CookieJar.NO_COOKIES;
      socketFactory = SocketFactory.getDefault();
      hostnameVerifier = OkHostnameVerifier.INSTANCE;
      certificatePinner = CertificatePinner.DEFAULT;
      proxyAuthenticator = Authenticator.NONE;
      authenticator = Authenticator.NONE;
      connectionPool = new ConnectionPool();
      /* NetProphet */
      if (netProphetTestingDns != null){
    	  dns = netProphetTestingDns;
    	  //dns = new NetProphetTestingDns();
    	  NetProphetLogger.logDebugging("OkHttpClient.build", 
    			  "using NetProphet Testing DNS");
      }
      else if(netProphetDns != null)
    	  dns = netProphetDns;
      else
    	  dns = Dns.SYSTEM;
      /*
      dns = Dns.SYSTEM;
      */
      
      followSslRedirects = true;
      followRedirects = true;
      retryOnConnectionFailure = true;
      connectTimeout = 10_000;
      readTimeout = 10_000;
      writeTimeout = 10_000;
    }

    Builder(OkHttpClient okHttpClient) {
      this.dispatcher = okHttpClient.dispatcher;
      this.proxy = okHttpClient.proxy;
      this.protocols = okHttpClient.protocols;
      this.connectionSpecs = okHttpClient.connectionSpecs;
      this.interceptors.addAll(okHttpClient.interceptors);
      this.networkInterceptors.addAll(okHttpClient.networkInterceptors);
      this.proxySelector = okHttpClient.proxySelector;
      this.cookieJar = okHttpClient.cookieJar;
      this.internalCache = okHttpClient.internalCache;
      this.cache = okHttpClient.cache;
      this.socketFactory = okHttpClient.socketFactory;
      this.sslSocketFactory = okHttpClient.sslSocketFactory;
      this.hostnameVerifier = okHttpClient.hostnameVerifier;
      this.certificatePinner = okHttpClient.certificatePinner;
      this.proxyAuthenticator = okHttpClient.proxyAuthenticator;
      this.authenticator = okHttpClient.authenticator;
      this.connectionPool = okHttpClient.connectionPool;
      this.dns = okHttpClient.dns;
      this.followSslRedirects = okHttpClient.followSslRedirects;
      this.followRedirects = okHttpClient.followRedirects;
      this.retryOnConnectionFailure = okHttpClient.retryOnConnectionFailure;
      this.connectTimeout = okHttpClient.connectTimeout;
      this.readTimeout = okHttpClient.readTimeout;
      this.writeTimeout = okHttpClient.writeTimeout;
    }

    /**
     * Sets the default connect timeout for new connections. A value of 0 means no timeout,
     * otherwise values must be between 1 and {@link Integer#MAX_VALUE} when converted to
     * milliseconds.
     */
    public Builder connectTimeout(long timeout, TimeUnit unit) {
      if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
      if (unit == null) throw new IllegalArgumentException("unit == null");
      long millis = unit.toMillis(timeout);
      if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
      if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
      connectTimeout = (int) millis;
      return this;
    }

    /**
     * Sets the default read timeout for new connections. A value of 0 means no timeout, otherwise
     * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
     */
    public Builder readTimeout(long timeout, TimeUnit unit) {
      if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
      if (unit == null) throw new IllegalArgumentException("unit == null");
      long millis = unit.toMillis(timeout);
      if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
      if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
      readTimeout = (int) millis;
      return this;
    }

    /**
     * Sets the default write timeout for new connections. A value of 0 means no timeout, otherwise
     * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
     */
    public Builder writeTimeout(long timeout, TimeUnit unit) {
      if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
      if (unit == null) throw new IllegalArgumentException("unit == null");
      long millis = unit.toMillis(timeout);
      if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
      if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
      writeTimeout = (int) millis;
      return this;
    }

    /**
     * Sets the HTTP proxy that will be used by connections created by this client. This takes
     * precedence over {@link #proxySelector}, which is only honored when this proxy is null (which
     * it is by default). To disable proxy use completely, call {@code setProxy(Proxy.NO_PROXY)}.
     */
    public Builder proxy(Proxy proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Sets the proxy selection policy to be used if no {@link #proxy proxy} is specified
     * explicitly. The proxy selector may return multiple proxies; in that case they will be tried
     * in sequence until a successful connection is established.
     *
     * <p>If unset, the {@link ProxySelector#getDefault() system-wide default} proxy selector will
     * be used.
     */
    public Builder proxySelector(ProxySelector proxySelector) {
      this.proxySelector = proxySelector;
      return this;
    }

    /**
     * Sets the handler that can accept cookies from incoming HTTP responses and provides cookies to
     * outgoing HTTP requests.
     *
     * <p>If unset, {@linkplain CookieJar#NO_COOKIES no cookies} will be accepted nor provided.
     */
    public Builder cookieJar(CookieJar cookieJar) {
      if (cookieJar == null) throw new NullPointerException("cookieJar == null");
      this.cookieJar = cookieJar;
      return this;
    }

    /** Sets the response cache to be used to read and write cached responses. */
    void setInternalCache(InternalCache internalCache) {
      this.internalCache = internalCache;
      this.cache = null;
    }

    public Builder cache(Cache cache) {
      this.cache = cache;
      this.internalCache = null;
      return this;
    }

    /**
     * Sets the DNS service used to lookup IP addresses for hostnames.
     *
     * <p>If unset, the {@link Dns#SYSTEM system-wide default} DNS will be used.
     */
    public Builder dns(Dns dns) {
      if (dns == null) throw new NullPointerException("dns == null");
      this.dns = dns;
      return this;
    }

    /**
     * Sets the socket factory used to create connections. OkHttp only uses the parameterless {@link
     * SocketFactory#createSocket() createSocket()} method to create unconnected sockets. Overriding
     * this method, e. g., allows the socket to be bound to a specific local address.
     *
     * <p>If unset, the {@link SocketFactory#getDefault() system-wide default} socket factory will
     * be used.
     */
    public Builder socketFactory(SocketFactory socketFactory) {
      if (socketFactory == null) throw new NullPointerException("socketFactory == null");
      this.socketFactory = socketFactory;
      return this;
    }

    /**
     * Sets the socket factory used to secure HTTPS connections.
     *
     * <p>If unset, a lazily created SSL socket factory will be used.
     */
    public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
      if (sslSocketFactory == null) throw new NullPointerException("sslSocketFactory == null");
      this.sslSocketFactory = sslSocketFactory;
      return this;
    }

    /**
     * Sets the verifier used to confirm that response certificates apply to requested hostnames for
     * HTTPS connections.
     *
     * <p>If unset, a default hostname verifier will be used.
     */
    public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
      if (hostnameVerifier == null) throw new NullPointerException("hostnameVerifier == null");
      this.hostnameVerifier = hostnameVerifier;
      return this;
    }

    /**
     * Sets the certificate pinner that constrains which certificates are trusted. By default HTTPS
     * connections rely on only the {@link #sslSocketFactory SSL socket factory} to establish trust.
     * Pinning certificates avoids the need to trust certificate authorities.
     */
    public Builder certificatePinner(CertificatePinner certificatePinner) {
      if (certificatePinner == null) throw new NullPointerException("certificatePinner == null");
      this.certificatePinner = certificatePinner;
      return this;
    }

    /**
     * Sets the authenticator used to respond to challenges from origin servers. Use {@link
     * #proxyAuthenticator} to set the authenticator for proxy servers.
     *
     * <p>If unset, the {@linkplain Authenticator#NONE no authentication will be attempted}.
     */
    public Builder authenticator(Authenticator authenticator) {
      if (authenticator == null) throw new NullPointerException("authenticator == null");
      this.authenticator = authenticator;
      return this;
    }

    /**
     * Sets the authenticator used to respond to challenges from proxy servers. Use {@link
     * #authenticator} to set the authenticator for origin servers.
     *
     * <p>If unset, the {@linkplain Authenticator#NONE no authentication will be attempted}.
     */
    public Builder proxyAuthenticator(Authenticator proxyAuthenticator) {
      if (proxyAuthenticator == null) throw new NullPointerException("proxyAuthenticator == null");
      this.proxyAuthenticator = proxyAuthenticator;
      return this;
    }

    /**
     * Sets the connection pool used to recycle HTTP and HTTPS connections.
     *
     * <p>If unset, a new connection pool will be used.
     */
    public Builder connectionPool(ConnectionPool connectionPool) {
      if (connectionPool == null) throw new NullPointerException("connectionPool == null");
      this.connectionPool = connectionPool;
      return this;
    }

    /**
     * Configure this client to follow redirects from HTTPS to HTTP and from HTTP to HTTPS.
     *
     * <p>If unset, protocol redirects will be followed. This is different than the built-in {@code
     * HttpURLConnection}'s default.
     */
    public Builder followSslRedirects(boolean followProtocolRedirects) {
      this.followSslRedirects = followProtocolRedirects;
      return this;
    }

    /** Configure this client to follow redirects. If unset, redirects be followed. */
    public Builder followRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    /**
     * Configure this client to retry or not when a connectivity problem is encountered. By default,
     * this client silently recovers from the following problems:
     *
     * <ul>
     *   <li><strong>Unreachable IP addresses.</strong> If the URL's host has multiple IP addresses,
     *       failure to reach any individual IP address doesn't fail the overall request. This can
     *       increase availability of multi-homed services.
     *   <li><strong>Stale pooled connections.</strong> The {@link ConnectionPool} reuses sockets
     *       to decrease request latency, but these connections will occasionally time out.
     *   <li><strong>Unreachable proxy servers.</strong> A {@link ProxySelector} can be used to
     *       attempt multiple proxy servers in sequence, eventually falling back to a direct
     *       connection.
     * </ul>
     *
     * Set this to false to avoid retrying requests when doing so is destructive. In this case the
     * calling application should do its own recovery of connectivity failures.
     */
    public Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
      this.retryOnConnectionFailure = retryOnConnectionFailure;
      return this;
    }

    /**
     * Sets the dispatcher used to set policy and execute asynchronous requests. Must not be null.
     */
    public Builder dispatcher(Dispatcher dispatcher) {
      if (dispatcher == null) throw new IllegalArgumentException("dispatcher == null");
      this.dispatcher = dispatcher;
      return this;
    }

    /**
     * Configure the protocols used by this client to communicate with remote servers. By default
     * this client will prefer the most efficient transport available, falling back to more
     * ubiquitous protocols. Applications should only call this method to avoid specific
     * compatibility problems, such as web servers that behave incorrectly when SPDY is enabled.
     *
     * <p>The following protocols are currently supported:
     *
     * <ul>
     *     <li><a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">http/1.1</a>
     *     <li><a
     *         href="http://www.chromium.org/spdy/spdy-protocol/spdy-protocol-draft3-1">spdy/3.1</a>
     *     <li><a href="http://tools.ietf.org/html/draft-ietf-httpbis-http2-17">h2</a>
     * </ul>
     *
     * <p><strong>This is an evolving set.</strong> Future releases include support for transitional
     * protocols. The http/1.1 transport will never be dropped.
     *
     * <p>If multiple protocols are specified, <a
     * href="http://tools.ietf.org/html/draft-ietf-tls-applayerprotoneg">ALPN</a> will be used to
     * negotiate a transport.
     *
     * <p>{@link Protocol#HTTP_1_0} is not supported in this set. Requests are initiated with {@code
     * HTTP/1.1} only. If the server responds with {@code HTTP/1.0}, that will be exposed by {@link
     * Response#protocol()}.
     *
     * @param protocols the protocols to use, in order of preference. The list must contain {@link
     * Protocol#HTTP_1_1}. It must not contain null or {@link Protocol#HTTP_1_0}.
     */
    public Builder protocols(List<Protocol> protocols) {
      protocols = Util.immutableList(protocols);
      if (!protocols.contains(Protocol.HTTP_1_1)) {
        throw new IllegalArgumentException("protocols doesn't contain http/1.1: " + protocols);
      }
      if (protocols.contains(Protocol.HTTP_1_0)) {
        throw new IllegalArgumentException("protocols must not contain http/1.0: " + protocols);
      }
      if (protocols.contains(null)) {
        throw new IllegalArgumentException("protocols must not contain null");
      }
      this.protocols = Util.immutableList(protocols);
      return this;
    }

    public Builder connectionSpecs(List<ConnectionSpec> connectionSpecs) {
      this.connectionSpecs = Util.immutableList(connectionSpecs);
      return this;
    }

    /**
     * Returns a modifiable list of interceptors that observe the full span of each call: from
     * before the connection is established (if any) until after the response source is selected
     * (either the origin server, cache, or both).
     */
    public List<Interceptor> interceptors() {
      return interceptors;
    }

    public Builder addInterceptor(Interceptor interceptor) {
      interceptors.add(interceptor);
      return this;
    }

    /**
     * Returns a modifiable list of interceptors that observe a single network request and response.
     * These interceptors must call {@link Interceptor.Chain#proceed} exactly once: it is an error
     * for a network interceptor to short-circuit or repeat a network request.
     */
    public List<Interceptor> networkInterceptors() {
      return networkInterceptors;
    }

    public Builder addNetworkInterceptor(Interceptor interceptor) {
      networkInterceptors.add(interceptor);
      return this;
    }

    public OkHttpClient build() {
      return new OkHttpClient(this);
    }
  }
}
