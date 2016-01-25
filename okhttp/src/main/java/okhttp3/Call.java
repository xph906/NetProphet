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
import java.util.logging.Level;

import okhttp3.Request.RequestTimingANP;
import okhttp3.Request.ResponseInfoANP;
import static okhttp3.internal.Internal.logger;


/**
 * A call is a request that has been prepared for execution. A call can be
 * canceled. As this object represents a single request/response pair (stream),
 * it cannot be executed twice.
 */
public interface Call {
	/** Returns the original request that initiated this call. */
	Request request();

	/* NetProphet */
	public enum ErrorMsg {
		NOERROR,
		DNS_ERR,
		CONN_ERR,
		REQ_SENT_ERR,
		RESP_RECV_ERR,
		HTTP_ERR,
		OTHER_ERR,
		IN_COMPLETE_TIMING, /*hasn't executed informFinishedReadingResponse*/
		HTTP_SERVER_TIMEOUT_ERR, /*Server not responding HTTP response*/
		NO_REQ
	}
	
	public class CallStatInfo {
		private List<String> urlsANP;
		private List<RequestTimingANP> timingsANP;

		private long startTimeANP;
		private long endTimeANP;

		private List<ResponseInfoANP> infoANP;
		
		private int codeANP; // the last HTTP's code
		private long sizeANP; // the last HTTP's size
		
		//error related info
		private boolean isFailedCallANP;
		private ErrorMsg errorMsgANP;
		private String detailedErrorMsgANP;
		
		public CallStatInfo(List<String> urls, List<RequestTimingANP> timings,
				long startTime, long endTime,
				List<ResponseInfoANP> info,
				boolean isFailedCall, String detailedErrorMsg){
			this.urlsANP = urls;
			this.timingsANP = timings;
			this.startTimeANP = startTime;
			this.endTimeANP = endTime;
			this.infoANP = info;
			this.codeANP = 0;
			this.sizeANP = 0;
			this.isFailedCallANP = false;
			this.errorMsgANP = ErrorMsg.NOERROR;
			this.detailedErrorMsgANP = "";
			this.isFailedCallANP = isFailedCall;
			this.detailedErrorMsgANP = detailedErrorMsg;
		}
		
		public boolean isFailedCallANP() {
			return isFailedCallANP;
		}

		public void setFailedCallANP(boolean isFailedCallANP) {
			this.isFailedCallANP = isFailedCallANP;
		}

		public void setErrorMsgANP(ErrorMsg errorMsgANP) {
			this.errorMsgANP = errorMsgANP;
		}

		public String getDetailedErrorMsgANP() {
			return detailedErrorMsgANP;
		}

		public void setDetailedErrorMsgANP(String detailedErrorMsgANP) {
			this.detailedErrorMsgANP = detailedErrorMsgANP;
		}

		public void setSizeANP(long sizeANP) {
			this.sizeANP = sizeANP;
		}

		public ErrorMsg getCallErrorMsg(){
			Iterator<RequestTimingANP> iter = timingsANP.iterator(); 
			RequestTimingANP lastTiming = null;
			while(iter.hasNext()){
				lastTiming = iter.next();
			}
			if(urlsANP.size()==0 || lastTiming==null)
				return ErrorMsg.NO_REQ;
			else if(isFailedCallANP){
				if(lastTiming.getReqStartTimeANP()==0)
					return ErrorMsg.OTHER_ERR;
				else if(lastTiming.getDnsStartTimeANP()!=0 &&
						lastTiming.getDnsEndTimeANP()==0)
					return ErrorMsg.DNS_ERR;
				else if(lastTiming.getConnSetupStartTimeANP()!=0 &&
						lastTiming.getConnSetupEndTimeANP()==0)
					return ErrorMsg.CONN_ERR;
				else if(lastTiming.getReqWriteStartTimeANP()!=0 &&
						lastTiming.getReqWriteEndTimeANP()==0)
					return ErrorMsg.REQ_SENT_ERR;
				else if(lastTiming.getRespStartTimeANP()!=0 &&
						lastTiming.getRespEndTimeANP()==0)
					return ErrorMsg.RESP_RECV_ERR;
				else if(lastTiming.getRespStartTimeANP()==0)
					return ErrorMsg.HTTP_SERVER_TIMEOUT_ERR;
				
				logger.log(Level.INFO, String.format(
						"  unknown error: %s\n"+
						"  dns: %d, %d       \n"+
						"  conn:%d, %d      \n"+
						"  req :%d, %d      \n"+
						"  resp:%d, %d\n", detailedErrorMsgANP,
						lastTiming.getDnsStartTimeANP(), lastTiming.getDnsEndTimeANP(),
						lastTiming.getConnSetupStartTimeANP(), lastTiming.getConnSetupEndTimeANP(),
						lastTiming.getReqWriteStartTimeANP(), lastTiming.getReqWriteEndTimeANP(),
						lastTiming.getRespStartTimeANP(), lastTiming.getRespEndTimeANP()));
				return ErrorMsg.OTHER_ERR;
			}
			//non-zero error code means the connection succeed
			else if(getFinalCodeANP() >= 300 ) 
				return ErrorMsg.HTTP_ERR;
			
			if(!lastTiming.isAccurateEndTimeANP())
				return ErrorMsg.IN_COMPLETE_TIMING;
			
			return ErrorMsg.NOERROR; 
			
		}
		
		public List<ResponseInfoANP> getInfoANP() {
			return infoANP;
		}

		public void setInfoANP(List<ResponseInfoANP> infoANP) {
			this.infoANP = infoANP;
		}

		public int getFinalCodeANP() {
			if(infoANP == null)
				return 0;
			int result = 0;
			Iterator<ResponseInfoANP> iter = infoANP.iterator(); 
			while(iter.hasNext()){
				result = iter.next().getCodeANP();
			}
			return result;
		}

		public void setCodeANP(int codeANP) {
			this.codeANP = codeANP;
		}

		public long getSizeANP() {
			if(infoANP == null)
				return 0;
			long result = 0;
			Iterator<ResponseInfoANP> iter = infoANP.iterator(); 
			while(iter.hasNext()){
				result = iter.next().getSizeANP();
			}
			return result;
		}

		public void setSizeANP(int sizeANP) {
			this.sizeANP = sizeANP;
		}

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

		public void setEndTimeANP(long endTimeANP) {
			this.endTimeANP = endTimeANP;
		}

		public long getEndTimeANP() {
			long rs = endTimeANP;
			if (timingsANP == null) {
				return rs;
			}
			Iterator<RequestTimingANP> iter = timingsANP.iterator();
			while (iter.hasNext()) {
				RequestTimingANP obj = iter.next();
				if (obj.getRespEndTimeANP() > rs)
					rs = obj.getRespEndTimeANP();
			}
			return rs;
		}
	}

	public CallStatInfo getCallStatInfo();
	
	//this method will store call stat information to local storage
	// if storeToRemoteServer is set to be true, stat information 
	// will also be sent to remote server.
	public void storeCallStatInfo(boolean storeToRemoteServer); 

	/* End NetProphet */

	/**
	 * Invokes the request immediately, and blocks until the response can be
	 * processed or is in error.
	 *
	 * <p>
	 * The caller may read the response body with the response's
	 * {@link Response#body} method. To facilitate connection recycling, callers
	 * should always {@link ResponseBody#close() close the response body}.
	 *
	 * <p>
	 * Note that transport-layer success (receiving a HTTP response code,
	 * headers and body) does not necessarily indicate application-layer
	 * success: {@code response} may still indicate an unhappy HTTP response
	 * code like 404 or 500.
	 *
	 * @throws IOException
	 *             if the request could not be executed due to cancellation, a
	 *             connectivity problem or timeout. Because networks can fail
	 *             during an exchange, it is possible that the remote server
	 *             accepted the request before the failure.
	 * @throws IllegalStateException
	 *             when the call has already been executed.
	 */
	Response execute() throws IOException;

	/**
	 * Schedules the request to be executed at some point in the future.
	 *
	 * <p>
	 * The {@link OkHttpClient#dispatcher dispatcher} defines when the request
	 * will run: usually immediately unless there are several other requests
	 * currently being executed.
	 *
	 * <p>
	 * This client will later call back {@code responseCallback} with either an
	 * HTTP response or a failure exception. If you {@link #cancel} a request
	 * before it completes the callback will not be invoked.
	 *
	 * @throws IllegalStateException
	 *             when the call has already been executed.
	 */
	void enqueue(Callback responseCallback);

	/**
	 * Cancels the request, if possible. Requests that are already complete
	 * cannot be canceled.
	 */
	void cancel();

	/**
	 * Returns true if this call has been either {@linkplain #execute()
	 * executed} or {@linkplain #enqueue(Callback) enqueued}. It is an error to
	 * execute a call more than once.
	 */
	boolean isExecuted();

	boolean isCanceled();

	interface Factory {
		Call newCall(Request request);
	}
}
