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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import netprophet.AsyncTaskManager;
import netprophet.DatabaseHandler;
import netprophet.NetProphetHTTPRequestInfoObject;
import netprophet.NetProphetIdentifierGenerator;
import netprophet.NetProphetNetworkData;
import netprophet.NetProphetPropertyManager;
import netprophet.NetUtility;
import netprophet.PostCallInfoTask;
import netprophet.NetUtility.State;
import okhttp3.Request.RequestTimingANP;
import okhttp3.Request.ResponseInfoANP;
import okhttp3.internal.NamedRunnable;
import okhttp3.internal.http.HttpEngine;
import okhttp3.internal.http.RequestException;
import okhttp3.internal.http.RouteException;
import okhttp3.internal.http.StreamAllocation;
import static okhttp3.internal.Internal.logger;
import static okhttp3.internal.http.HttpEngine.MAX_FOLLOW_UPS;

public class RealCall implements Call {
	private final OkHttpClient client;

	// Guarded by this.
	private boolean executed;
	volatile boolean canceled;

	/**
	 * The application's original request unadulterated by redirects or auth
	 * headers.
	 */
	Request originalRequest;
	HttpEngine engine;

	/* NetProphet Fields */
	private NetProphetPropertyManager propertyManager;
	private List<String> urlsANP;
	private List<RequestTimingANP> timingsANP;
	private List<ResponseInfoANP> infosANP;
	private long startTimeANP;
	private long endTimeANP;
	private AsyncTaskManager asyncTaskManager;

	private boolean isFailedCallANP;
	private String detailedErrorMsg;
	
	private boolean isCallStatInfoSavedLocally;
	private boolean isCallStatInfoSavedRemotely;
	private Context context;
	
	private NetUtility netUtilityANP;
	private State netStateANP;
    private String netTypeANP;
    private String netNameANP;
    private int WIFISigLevelANP;
    private int CellSigLevelANP;
    private int MCCANP;   // mobile network code
    private int MNCANP;   // mobile country code
    private int LACANP;   // local area code
    
    //optimization
    private boolean useGZIPOptimization;
    private int dnsTimeoutOptimization;
    private int connTimeoutOptimization;
    private int recvTimeoutOptimization;
    
    private boolean isRunningOnPhone;

	@NetProphet
	public CallStatInfo getCallStatInfo() {
		CallStatInfo info = new CallStatInfo(urlsANP, timingsANP, startTimeANP,
				endTimeANP, infosANP, isFailedCallANP, detailedErrorMsg);
		return info;
	}

	@NetProphet
	public void storeCallStatInfo(boolean storeToRemoteServer) {
		if(isCallStatInfoSavedRemotely && isCallStatInfoSavedLocally)
			return;
		else if(!storeToRemoteServer && isCallStatInfoSavedLocally)
			return ;
		else {
			if(urlsANP!=null && urlsANP.size()>0){
				try {
					URL url = new URL(urlsANP.get(0).split(" ")[1]);
					if(url.getHost().toLowerCase().equals(propertyManager.getServerHost().toLowerCase())){
						logger.info("ignore request to server: "+url);
						return ;
					}
				} catch (MalformedURLException e) {
					logger.severe("error parsing the url [storeCallStatInfo]: "+urlsANP.get(0)+" "+e);
					return ;
				}
			}
		}
		
		CallStatInfo callStatInfo = getCallStatInfo();	
		Iterator<String> urlIter = urlsANP.iterator();
		Iterator<RequestTimingANP> reqIter = timingsANP.iterator();
		Iterator<ResponseInfoANP> respIter = infosANP.iterator();
		List<NetProphetHTTPRequestInfoObject> objList = 
				new ArrayList<NetProphetHTTPRequestInfoObject>();
		NetProphetIdentifierGenerator idGen = NetProphetIdentifierGenerator
				.getInstance();
		Gson gson = new GsonBuilder().create();
		
		String userID = "non-android-user";
		if(context!=null && isRunningOnPhone){
			try{
				TelephonyManager mTelephony = 
						(TelephonyManager) (context.getSystemService(Context.TELEPHONY_SERVICE)); 
				userID = mTelephony.getDeviceId(); 
			}
			catch(Exception e){
				logger.log(Level.SEVERE,  "error in getting device id: "+e.toString());
				userID = "error-device-id";
			}		
		}
		
		//logger.log(Level.INFO, "DEBUG userID: "+userID+" "+platformType+" "+context);
		long prevReqID = 0;
		NetProphetHTTPRequestInfoObject prevObj = null;
		while (urlIter.hasNext()) {
			String tmp = urlIter.next();
			String[] methodAndURL = tmp.split(" ");
			if (methodAndURL.length != 2){
				logger.log(Level.SEVERE, "error in format of urlsANP: "+tmp);
				continue;
			}
			String url = methodAndURL[1];
			String method = methodAndURL[0];
			
			RequestTimingANP timingObj = reqIter.next();
			ResponseInfoANP respObj = respIter.next();
			long curID = idGen.getNextHTTPRequestID();
			long overallDelay = timingObj.getRespEndTimeANP()
					- timingObj.getReqStartTimeANP();
			long dnsDelay = timingObj.getDnsEndTimeANP()
					- timingObj.getDnsStartTimeANP();
			long connDelay = timingObj.getConnSetupEndTimeANP()
					- timingObj.getConnSetupStartTimeANP();
			long reqWriteDelay = timingObj.getReqWriteEndTimeANP()
					- timingObj.getReqWriteStartTimeANP();
			long TTFBDelay = timingObj.getRespStartTimeANP()
					- timingObj.getReqWriteEndTimeANP();
			long respTransDelay = timingObj.getRespEndTimeANP()
					- timingObj.getRespStartTimeANP();
			
			// TODO: cache needs more testing
			boolean useConnCache = timingObj.useConnCache();
			boolean useDNSCache =  timingObj.useDNSCache();
			boolean useRespCache = timingObj.useRespCache();
			int reqSize = timingObj.getReqSizeANP();
			
			if(prevObj != null)
				prevObj.setNextReqID(curID);

			NetProphetHTTPRequestInfoObject obj = new NetProphetHTTPRequestInfoObject(
					curID, url, method, userID, prevReqID, 0,
					timingObj.getReqStartTimeANP(),
					timingObj.getRespEndTimeANP(), overallDelay, dnsDelay,
					connDelay, timingObj.getHandshakeTimeANP(),
					timingObj.getTlsHandshakeTimeANP(), reqWriteDelay,
					timingObj.getEstimatedServerDelay(), TTFBDelay,
					respTransDelay, useConnCache, useDNSCache, useRespCache,
					respObj.getSizeANP(), respObj.getCodeANP(), reqSize, respObj.getRespType(),
					callStatInfo.isFailedCallANP(),
					callStatInfo.getCallErrorMsg().toString(),
					callStatInfo.getDetailedErrorMsgANP(), (long) 0, 0);
			objList.add(obj);
			prevObj = obj;
			prevReqID = curID;		
		}

		/* NetInfo: reqID, net type, net name, wifi signal, cell signal, 
		 * mcc, mnc, lac, firstMileLat, firstMilePLR */
		NetProphetNetworkData networkData = 
				new NetProphetNetworkData(objList.get(0).getReqID(), netTypeANP, netNameANP,
						WIFISigLevelANP, CellSigLevelANP, MCCANP, MNCANP, LACANP, 
						0, 0);

		if(!isCallStatInfoSavedLocally && context!=null){
			try{
				DatabaseHandler dbHandler = DatabaseHandler.getInstance(context);
				//if a call contains multiple requests (e.g., redirection, retries), 
				//all the request information will be stored.
				//However, only one instance of NetProphetNetworkData is stored, whose reqID
				//corresponds to the first request ID for this call.
				asyncTaskManager.postTask(dbHandler.getRequestBatchInsertTask(objList));
				asyncTaskManager.postTask(dbHandler.getNetSingleInsertTask(networkData));
				isCallStatInfoSavedLocally = true;
			}
			catch(Exception e){
				logger.severe("error in storing data to db: "+e.toString());
				e.printStackTrace();
			}
		}
	}

	/* End NetProphet Fields */

	protected RealCall(OkHttpClient client, Request originalRequest) {
		this.client = client;
		this.originalRequest = originalRequest;

		/* NetProphet Initialization */
		urlsANP = new ArrayList<String>();
		timingsANP = new ArrayList<RequestTimingANP>();
		infosANP = new ArrayList<ResponseInfoANP>();
		startTimeANP = 0;
		endTimeANP = 0;
		isFailedCallANP = false;
		detailedErrorMsg = "";
		asyncTaskManager = AsyncTaskManager.getInstance();
		isCallStatInfoSavedLocally = false;
		isCallStatInfoSavedRemotely = false;
		
		originalRequest.setCall(this);
		propertyManager = NetProphetPropertyManager.getInstance();
		context = client.getContext();
		
		netUtilityANP = null;
		netStateANP = State.CONNECTED;
	    netTypeANP = "";
	    netNameANP = "";
	    WIFISigLevelANP = 0;
	    CellSigLevelANP = 0;
	    MCCANP = 0;   // mobile network code
	    MNCANP = 0;   // mobile country code
	    LACANP = 0;
	    
	    String platformType = System.getProperty("java.vm.name");
	    isRunningOnPhone = platformType.equals("Dalvik");
		/* End NetProphet Initialization */
	}

	public boolean isCallStatInfoSavedLocally() {
		return isCallStatInfoSavedLocally;
	}

	public void setCallStatInfoSavedLocally(boolean isCallStatInfoSavedLocally) {
		this.isCallStatInfoSavedLocally = isCallStatInfoSavedLocally;
	}

	public boolean isCallStatInfoSavedRemotely() {
		return isCallStatInfoSavedRemotely;
	}

	public void setCallStatInfoSavedRemotely(boolean isCallStatInfoSavedRemotely) {
		this.isCallStatInfoSavedRemotely = isCallStatInfoSavedRemotely;
	}

	/* NetProphet Getter and Setter */
	public List<String> getUrlsANP() {
		return urlsANP;
	}

	public void setUrlsANP(List<String> urlsANP) {
		this.urlsANP = urlsANP;
	}

	public List<RequestTimingANP> getTimingsANP() {
		return timingsANP;
	}

	public void setTimingsANP(List<RequestTimingANP> timingsANP) {
		this.timingsANP = timingsANP;
	}

	public long getStartTimeANP() {
		return startTimeANP;
	}

	public void setStartTimeANP(long startTimeANP) {
		this.startTimeANP = startTimeANP;
	}

	public long getEndTimeANP() {
		long accurateEndTime = endTimeANP;
		for (RequestTimingANP timing : timingsANP) {
			if (timing.getRespEndTimeANP() > accurateEndTime)
				accurateEndTime = timing.getRespEndTimeANP();
		}
		return accurateEndTime;
	}

	public void setEndTimeANP(long endTimeANP) {
		this.endTimeANP = endTimeANP;
	}
	
	public boolean isUseGZIPOptimization() {
		return useGZIPOptimization;
	}

	public void setUseGZIPOptimization(boolean useGZIPOptimization) {
		this.useGZIPOptimization = useGZIPOptimization;
	}

	public int getDnsTimeoutOptimization() {
		return dnsTimeoutOptimization;
	}

	public void setDnsTimeoutOptimization(int dnsTimeoutOptimization) {
		this.dnsTimeoutOptimization = dnsTimeoutOptimization;
	}

	public int getConnTimeoutOptimization() {
		return connTimeoutOptimization;
	}

	public void setConnTimeoutOptimization(int connTimeoutOptimization) {
		this.connTimeoutOptimization = connTimeoutOptimization;
	}

	public int getRecvTimeoutOptimization() {
		return recvTimeoutOptimization;
	}

	public void setRecvTimeoutOptimization(int recvTimeoutOptimization) {
		this.recvTimeoutOptimization = recvTimeoutOptimization;
	}

	/* End NetProphet Getter and Setter */

	@Override
	public Request request() {
		return originalRequest;
	}

	@Override
	public Response execute() throws IOException {
		synchronized (this) {
			if (executed)
				throw new IllegalStateException("Already Executed");
			executed = true;
		}

		try {
			client.dispatcher().executed(this);
			Response result = getResponseWithInterceptorChain(false);
			if (result == null)
				throw new IOException("Canceled");
			return result;
		} finally {
			client.dispatcher().finished(this);

		}
	}

	Object tag() {
		return originalRequest.tag();
	}

	@Override
	public void enqueue(Callback responseCallback) {
		enqueue(responseCallback, false);
	}

	void enqueue(Callback responseCallback, boolean forWebSocket) {
		synchronized (this) {
			if (executed)
				throw new IllegalStateException("Already Executed");
			executed = true;
		}
		client.dispatcher().enqueue(
				new AsyncCall(responseCallback, forWebSocket));
	}

	@Override
	public void cancel() {
		canceled = true;
		if (engine != null)
			engine.cancel();
	}

	@Override
	public synchronized boolean isExecuted() {
		return executed;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	final class AsyncCall extends NamedRunnable {
		private final Callback responseCallback;
		private final boolean forWebSocket;

		private AsyncCall(Callback responseCallback, boolean forWebSocket) {
			super("OkHttp %s", originalRequest.url().toString());
			this.responseCallback = responseCallback;
			this.forWebSocket = forWebSocket;
		}

		String host() {
			return originalRequest.url().host();
		}

		Request request() {
			return originalRequest;
		}

		Object tag() {
			return originalRequest.tag();
		}

		void cancel() {
			RealCall.this.cancel();
		}

		RealCall get() {
			return RealCall.this;
		}

		@Override
		protected void execute() {
			boolean signalledCallback = false;
			try {
				Response response = getResponseWithInterceptorChain(forWebSocket);
				if (canceled) {
					signalledCallback = true;
					responseCallback.onFailure(RealCall.this, new IOException(
							"Canceled"));
				} else {
					signalledCallback = true;
					responseCallback.onResponse(RealCall.this, response);
				}
			} catch (IOException e) {
				if (signalledCallback) {
					// Do not signal the callback twice!
					logger.log(Level.INFO, "Callback failure for "
							+ toLoggableString(), e);
				} else {
					responseCallback.onFailure(RealCall.this, e);
				}
			} finally {
				client.dispatcher().finished(this);
			}
		}
	}

	/**
	 * Returns a string that describes this call. Doesn't include a full URL as
	 * that might contain sensitive information.
	 */
	private String toLoggableString() {
		String string = canceled ? "canceled call" : "call";
		HttpUrl redactedUrl = originalRequest.url().resolve("/...");
		return string + " to " + redactedUrl;
	}

	private Response getResponseWithInterceptorChain(boolean forWebSocket)
			throws IOException {
		Interceptor.Chain chain = new ApplicationInterceptorChain(0,
				originalRequest, forWebSocket);
		/* NetProphet */
		startTimeANP = System.currentTimeMillis();
		
		if(isRunningOnPhone){
			if(netUtilityANP == null){
				netUtilityANP = NetUtility.getInstance(context, null);
			}
			netStateANP = netUtilityANP.getNetworkingState();
			netTypeANP = netUtilityANP.getNetworkingType();
			netNameANP = netUtilityANP.getNetworkingName();
			WIFISigLevelANP = netUtilityANP.getWIFISignalStrength();
			CellSigLevelANP = netUtilityANP.getCellSignalStrength();
			MCCANP = netUtilityANP.getMCC();
			MNCANP = netUtilityANP.getMNC();
			LACANP = netUtilityANP.getLAC();
		}
		/* End NetProphet */
		Response rs = chain.proceed(originalRequest);
		/* NetProphet */
		endTimeANP = System.currentTimeMillis();
		/* End NetProphet */
		return rs;
	}

	class ApplicationInterceptorChain implements Interceptor.Chain {
		private final int index;
		private final Request request;
		private final boolean forWebSocket;

		ApplicationInterceptorChain(int index, Request request,
				boolean forWebSocket) {
			this.index = index;
			this.request = request;
			this.forWebSocket = forWebSocket;
		}

		@Override
		public Connection connection() {
			return null;
		}

		@Override
		public Request request() {
			return request;
		}

		@Override
		public Response proceed(Request request) throws IOException {
			// If there's another interceptor in the chain, call that.
			if (index < client.interceptors().size()) {
				Interceptor.Chain chain = new ApplicationInterceptorChain(
						index + 1, request, forWebSocket);
				Interceptor interceptor = client.interceptors().get(index);
				Response interceptedResponse = interceptor.intercept(chain);

				if (interceptedResponse == null) {
					throw new NullPointerException("application interceptor "
							+ interceptor + " returned null");
				}

				return interceptedResponse;
			}

			// No more interceptors. Do HTTP.
			return getResponse(request, forWebSocket);
		}
	}

	/**
	 * Performs the request and returns the response. May return null if this
	 * call was canceled.
	 */
	Response getResponse(Request request, boolean forWebSocket)
			throws IOException {
		// Copy body metadata to the appropriate request headers.
		RequestBody body = request.body();
		if (body != null) {
			Request.Builder requestBuilder = request.newBuilder();

			MediaType contentType = body.contentType();
			if (contentType != null) {
				requestBuilder.header("Content-Type", contentType.toString());
			}

			long contentLength = body.contentLength();
			if (contentLength != -1) {
				requestBuilder.header("Content-Length",
						Long.toString(contentLength));
				requestBuilder.removeHeader("Transfer-Encoding");
			} else {
				requestBuilder.header("Transfer-Encoding", "chunked");
				requestBuilder.removeHeader("Content-Length");
			}

			request = requestBuilder.build();
		}

		// Create the initial HTTP engine. Retries and redirects need new engine
		// for each attempt.
		engine = new HttpEngine(client, request, false, false, forWebSocket,
				null, null, null);

		int followUpCount = 0;
		while (true) {
			if (canceled) {
				engine.releaseStreamAllocation();
				throw new IOException("Canceled");
			}

			boolean releaseConnection = true;
			try {
				/* NetProphet */
				urlsANP.add(String.format("%s %s", 
						engine.getRequest().method(),
						engine.getRequest().url().toString()));
				engine.getRequest().getRequestTimingANP()
						.setReqStartTimeANP(System.currentTimeMillis());
				/* End NetProphet */

				engine.sendRequest();
				engine.readResponse();

				/* NetProphet */
				timingsANP.add(engine.getRequest().getRequestTimingANP());
				engine.getRequest().getResponseInfoANP()
						.setCodeANP(engine.getResponse().code());
				infosANP.add(engine.getRequest().getResponseInfoANP());
				/* End NetProphet */
				releaseConnection = false;
			} catch (RequestException e) {
				// The attempt to interpret the request failed. Give up.
				/* NetProphet */
				//request.getRequestTimingANP().setReqStartTimeANP(1); //REMOVE THIS LINE
				//logger.severe("RETRYDEBUG:"+request.url()+" "+e);
				
				timingsANP.add(request.getRequestTimingANP());
				infosANP.add(engine.getRequest().getResponseInfoANP());
				// error recording routine
				engine.getRequest().getRequestTimingANP()
						.setSuccessfulANP(false);
				engine.getRequest().getRequestTimingANP()
						.setErrorString(e.toString());
				engine.getRequest().getRequestTimingANP()
					.setRespEndTimeANP(System.currentTimeMillis());
				isFailedCallANP = true;
				detailedErrorMsg = e.toString();
				storeCallStatInfo(propertyManager.canStoreToRemoteServerEveryRequest());
				/* End NetProphet */
				throw e.getCause();
			} catch (RouteException e) {
				// The attempt to connect via a route failed. The request will
				// not have been sent.
				HttpEngine retryEngine = engine.recover(
						e.getLastConnectException(), null);

				/* NetProphet */	
				timingsANP.add(request.getRequestTimingANP());
				infosANP.add(engine.getRequest().getResponseInfoANP());
				/* End NetProphet */
				if (retryEngine != null) {
					releaseConnection = false;
					engine = retryEngine;
					continue;
				}

				/* NetProphet */
				// error recording routine
				engine.getRequest().getRequestTimingANP()
						.setSuccessfulANP(false);
				engine.getRequest().getRequestTimingANP()
						.setErrorString(e.toString());
				isFailedCallANP = true;
				detailedErrorMsg = e.toString();
				engine.getRequest().getRequestTimingANP()
					.setRespEndTimeANP(System.currentTimeMillis());
				storeCallStatInfo(propertyManager.canStoreToRemoteServerEveryRequest());
				/* End NetProphet */
				// Give up; recovery is not possible.
				throw e.getLastConnectException();
			} catch (IOException e) {
				// An attempt to communicate with a server failed. The request
				// may have been sent.
				HttpEngine retryEngine = engine.recover(e, null);

				/* NetProphet */
				//request.getRequestTimingANP().setReqStartTimeANP(1); //REMOVE THIS LINE
				//logger.severe("RETRYDEBUG:"+request.url()+" "+e);
				
				engine.getRequest().getRequestTimingANP()
						.setRespEndTimeANP(System.currentTimeMillis());
				timingsANP.add(request.getRequestTimingANP());
				infosANP.add(engine.getRequest().getResponseInfoANP());
				/* End NetProphet */
				if (retryEngine != null) {
					releaseConnection = false;
					engine = retryEngine;
					continue;
				}
				/* NetProphet */
				// error recording routine
				engine.getRequest().getRequestTimingANP()
						.setSuccessfulANP(false);
				engine.getRequest().getRequestTimingANP()
						.setErrorString(e.toString());
				isFailedCallANP = true;
				detailedErrorMsg = e.toString();
				storeCallStatInfo(propertyManager.canStoreToRemoteServerEveryRequest());
				engine.getRequest().getRequestTimingANP()
					.setRespEndTimeANP(System.currentTimeMillis());
				/* End NetProphet */
				// Give up; recovery is not possible.
				throw e;
			} finally {
				// We're throwing an unchecked exception. Release any resources.
				if (releaseConnection) {
					StreamAllocation streamAllocation = engine.close();
					streamAllocation.release();
				}
			}

			Response response = engine.getResponse();
			Request followUp = engine.followUpRequest();

			if (followUp == null) {
				if (!forWebSocket) {
					engine.releaseStreamAllocation();
				}
				return response;
			}

			StreamAllocation streamAllocation = engine.close();

			if (++followUpCount > MAX_FOLLOW_UPS) {
				streamAllocation.release();
				throw new ProtocolException("Too many follow-up requests: "
						+ followUpCount);
			}

			if (!engine.sameConnection(followUp.url())) {
				streamAllocation.release();
				streamAllocation = null;
			}

			request = followUp;
			/* NetProphet */
			request.setCall(this);
			/* End NetProphet*/
			engine = new HttpEngine(client, request, false, false,
					forWebSocket, streamAllocation, null, response);
		}
	}
}
