package netprophet;

import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.OkHttpClient.Builder;
import okhttp3.internal.InternalCache;
import android.content.Context;

public class NetProphet {
	/*
	 * This function has been called in the beginning of application.
	 */
	public static void initializeNetProphet(Context context, boolean enableOptimization){
		OkHttpClient.initializeNetProphet(context, enableOptimization);
	}
	/*
	 * This function is for testing/debugging on desktop.
	 */
	public static void initializeNetProphetDesktop(boolean enableOptimization){
		OkHttpClient.initializeNetProphetDesktop(enableOptimization);
	}
	
	/* Usage:
	 *  Request request = new Request.Builder().url(url).build();
	 *  NetProphetClient client = new NetProphetClient();
	 *  Builder b = client.newBuilder();
	 *  client.setBuilder(b);
	 *  Response response = client.newCall(request).execute();
	 * */
	public class NetProphetClient{
		
		private OkHttpClient client;
		
		public NetProphetClient(){
			client = new OkHttpClient();
		}
		public Call newCall(Request request) {
			return client.newCall(request);
		}
		public Builder newBuilder() {
			return client.newBuilder();
		}
		public NetProphetClient setBuilder(Builder b){
			client = b.build();
			return this;
		}
		
		/* Getter */
		public Context getContext(){
			return client.getContext();
		}
		
		public int connectTimeoutMillis() {
			/** Default connect timeout (in milliseconds). */
			return client.connectTimeoutMillis();
		}

		public int readTimeoutMillis() {
			/** Default read timeout (in milliseconds). */
			return client.readTimeoutMillis();
		}

		public int writeTimeoutMillis() {
			/** Default write timeout (in milliseconds). */
			return client.writeTimeoutMillis();
		}
		
		public Proxy proxy() {
			return client.proxy();
		}

		public ProxySelector proxySelector() {
			return client.proxySelector();
		}

		public CookieJar cookieJar() {
			return client.cookieJar();
		}

		public Cache cache() {
			return client.cache();
		}

		public Dns dns() {
			return client.dns();
		}

		public SocketFactory socketFactory() {
			return client.socketFactory();
		}

		public SSLSocketFactory sslSocketFactory() {
			return client.sslSocketFactory();
		}

		public HostnameVerifier hostnameVerifier() {
			return client.hostnameVerifier();
		}

		public CertificatePinner certificatePinner() {
			return client.certificatePinner();
		}

		public Authenticator authenticator() {
			return client.authenticator();
		}

		public Authenticator proxyAuthenticator() {
			return client.proxyAuthenticator();
		}

		public ConnectionPool connectionPool() {
			return client.connectionPool();
		}

		public boolean followSslRedirects() {
			return client.followSslRedirects();
		}

		public boolean followRedirects() {
			return client.followRedirects();
		}

		public boolean retryOnConnectionFailure() {
			return client.retryOnConnectionFailure();
		}

		public Dispatcher dispatcher() {
			return client.dispatcher();
		}

		public List<Protocol> protocols() {
			return client.protocols();
		}

		public List<ConnectionSpec> connectionSpecs() {
			return client.connectionSpecs();
		}
		
		public List<Interceptor> interceptors() {
		    return client.interceptors();
		}
		
		public List<Interceptor> networkInterceptors() {
		    return client.networkInterceptors();
		  }
		
		/* Setter */
		public void setContext(Context context) {
			client.setContext(context);
		}
		
	}
	
	
	
}
