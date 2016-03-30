/*
 * Copyright (C) 2014 Square, Inc.
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
package okhttp3.internal;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

import okhttp3.Address;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.internal.http.StreamAllocation;
import okhttp3.internal.io.RealConnection;

/**
 * Escalate internal APIs in {@code okhttp3} so they can be used from OkHttp's implementation
 * packages. The only implementation of this interface is in {@link OkHttpClient}.
 */
public abstract class Internal {
  public static final Logger logger = Logger.getLogger(OkHttpClient.class.getName());
  
  /* NetProphet */
  public static class NetProphetLogger {
	  final static String ERR_MSG = "[NETPROPHET] ERROR @ %s : %s";
	  final static String WARNING_MSG = "[NETPROPHET] WARNING @ %s : %s";
	  final static String DEBUG_MSG = "[NETPROPHET] DEBUG @ %s : %s";
	  final static String INFO_MSG = "[NETPROPHET] INFO @ %s : %s";
	  public static void logError(String funName, String errMsg){
		  logger.severe(String.format(ERR_MSG, funName, errMsg));
	  }
	  public static void logWarning(String funName, String errMsg){
		  logger.warning(String.format(WARNING_MSG, funName, errMsg));
	  }
	  public static void logDebugging(String funName, String errMsg){
		  logger.fine(String.format(DEBUG_MSG, funName, errMsg));
	  }
	  public static void logInfo(String funName, String errMsg){
		  logger.info(String.format(INFO_MSG, funName, errMsg));
	  }
  }
  
  public static class BriefFormatter extends Formatter 
  {   
      public BriefFormatter() { super(); }

      @Override 
      public String format(final LogRecord record) 
      {
          return record.getMessage()+" \n";
      }   
  }
  static {
	  Handler conHdlr = new ConsoleHandler();
	  conHdlr.setFormatter(new BriefFormatter());
	  conHdlr.setLevel(Level.INFO);
	  logger.setUseParentHandlers(false);
	  logger.addHandler(conHdlr);
  }	
  /* End NetProphet*/
  
  public static void initializeInstanceForTests() {
    // Needed in tests to ensure that the instance is actually pointing to something.
    new OkHttpClient();
  }

  public static Internal instance;

  public abstract void addLenient(Headers.Builder builder, String line);

  public abstract void addLenient(Headers.Builder builder, String name, String value);

  public abstract void setCache(OkHttpClient.Builder builder, InternalCache internalCache);

  public abstract InternalCache internalCache(OkHttpClient client);

  public abstract RealConnection get(
      ConnectionPool pool, Address address, StreamAllocation streamAllocation);

  public abstract void put(ConnectionPool pool, RealConnection connection);

  public abstract boolean connectionBecameIdle(ConnectionPool pool, RealConnection connection);

  public abstract RouteDatabase routeDatabase(ConnectionPool connectionPool);

  public abstract void apply(ConnectionSpec tlsConfiguration, SSLSocket sslSocket,
      boolean isFallback);

  public abstract HttpUrl getHttpUrlChecked(String url)
      throws MalformedURLException, UnknownHostException;

  // TODO delete the following when web sockets move into the main package.
  public abstract void callEnqueue(Call call, Callback responseCallback, boolean forWebSocket);

  public abstract StreamAllocation callEngineGetStreamAllocation(Call call);
}
