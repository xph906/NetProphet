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
package okhttp3;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import okhttp3.Request.RequestTimingANP;

/**
 * A call is a request that has been prepared for execution. A call can be canceled. As this object
 * represents a single request/response pair (stream), it cannot be executed twice.
 */
public interface Call {
  /** Returns the original request that initiated this call. */
  Request request();
  
  /* NetProphet */
  public class CallTiming {
	  public List<String> urlsANP;
	  public List<RequestTimingANP> timingsANP;
	  public long startTimeANP;
	  public long endTimeANP;
	  
	  public long getEndTimeANP(){
		  long rs = endTimeANP;
		  if (timingsANP == null){
			  return rs;
		  }
		  Iterator<RequestTimingANP> iter = timingsANP.iterator();
		  while(iter.hasNext()){
			  RequestTimingANP obj = iter.next();
			  if (obj.getRespEndTimeANP() > rs)
				  rs = obj.getRespEndTimeANP();
		  }
		  return rs;
	  }
  }
  CallTiming getCallTiming();
  /* End NetProphet*/
  
  
  /**
   * Invokes the request immediately, and blocks until the response can be processed or is in
   * error.
   *
   * <p>The caller may read the response body with the response's {@link Response#body} method.  To
   * facilitate connection recycling, callers should always {@link ResponseBody#close() close the
   * response body}.
   *
   * <p>Note that transport-layer success (receiving a HTTP response code, headers and body) does
   * not necessarily indicate application-layer success: {@code response} may still indicate an
   * unhappy HTTP response code like 404 or 500.
   *
   * @throws IOException if the request could not be executed due to cancellation, a connectivity
   * problem or timeout. Because networks can fail during an exchange, it is possible that the
   * remote server accepted the request before the failure.
   * @throws IllegalStateException when the call has already been executed.
   */
  Response execute() throws IOException;

  /**
   * Schedules the request to be executed at some point in the future.
   *
   * <p>The {@link OkHttpClient#dispatcher dispatcher} defines when the request will run: usually
   * immediately unless there are several other requests currently being executed.
   *
   * <p>This client will later call back {@code responseCallback} with either an HTTP response or a
   * failure exception. If you {@link #cancel} a request before it completes the callback will not
   * be invoked.
   *
   * @throws IllegalStateException when the call has already been executed.
   */
  void enqueue(Callback responseCallback);

  /** Cancels the request, if possible. Requests that are already complete cannot be canceled. */
  void cancel();

  /**
   * Returns true if this call has been either {@linkplain #execute() executed} or {@linkplain
   * #enqueue(Callback) enqueued}. It is an error to execute a call more than once.
   */
  boolean isExecuted();

  boolean isCanceled();

  interface Factory {
    Call newCall(Request request);
  }
}
