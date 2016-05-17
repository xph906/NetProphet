package netprophet;

import static okhttp3.internal.Internal.logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import netprophet.PingTool.MeasureResult;
import android.R.array;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Internal.NetProphetLogger;

public class LocalBandwidthMeasureTool {
	private static LocalBandwidthMeasureTool localBandwidthMeasureTool = null;
	public static LocalBandwidthMeasureTool getInstance(){
		if (localBandwidthMeasureTool == null)
			localBandwidthMeasureTool = new LocalBandwidthMeasureTool();
		return localBandwidthMeasureTool;
	}

	private Map<String, Float> bandwidthCache;
	private boolean isRunning;
	private Handler handler;
	private NetUtility netUtility;
	private String phoneModel;
	
	private LocalBandwidthMeasureTool(){
		bandwidthCache = new HashMap<String, Float>();	
		isRunning = false;
		handler = null;
		try{
			phoneModel = android.os.Build.MODEL;
		}
		catch(Exception e){
			phoneModel = null;
		}
	}
	
	public void startMeasuringTask(NetUtility netUtility){
		String networkName = null;
		String networkType = null;
		int signalStrength = 0;
		this.netUtility = netUtility;
		if (isRunning){
			NetProphetLogger.logDebugging("startMeasuringTask", 
					"Measuring task is running");
			postMsg("Measuring task is still running");
			return;
		}
		
		if(netUtility != null){
			networkName = netUtility.getNetworkingFullName();
			networkType = netUtility.getNetworkingType();
			if(bandwidthCache.containsKey(networkName)){
				NetProphetLogger.logDebugging("startMeasuringTask", 
						"this network's bandwidth has been tested");
				postMsg("This network's bandwidth has been measured");
				return;
			}
			signalStrength = netUtility.getSignalStrength();
		}
		
		MeasureLocalBandwidthTask task = 
				new MeasureLocalBandwidthTask(networkName, networkType, signalStrength);
		task.start();
	}
	
	private void postMsg(String content){
		if(handler != null){
			try{
				Message msg = new Message();
				msg.what = InternalConst.MSGType.NETINFO_MSG;
				msg.obj = content;
				handler.sendMessage(msg);
			}
			catch(Exception e){
				NetProphetLogger.logError("LocalBandwidthMeasureTool", 
						"postMsg error: "+e.toString());
				e.printStackTrace();
			}
		}
	}
	
	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}
	
	public boolean isRunning(){
		return isRunning;
	}
	
	public class BandwidthTransmissionData{
		public String networkName;
		public String networkType;
		public int signalStrength;
		public String userID;
		public String phoneModel;
		
		public int bandwidth;      //KBits, -1 means bad networking condition.
		public int serverPingVal;  //ms, -1 means not found good server
		public String server;      //can be null if cannot find good server.		
		public BandwidthTransmissionData(String name, String type, int sig, String userID, String phoneModel){
			this.networkName = name;
			this.networkType = type;
			this.signalStrength = sig;
			this.userID = userID;
			this.phoneModel = phoneModel;
			
			this.bandwidth = 0;
			this.serverPingVal = 0;
			this.server = null;
		}
		
		public void setMeasurementResult(int bandwidth, int pingVal, String server){
			this.bandwidth = bandwidth;
			this.serverPingVal = pingVal;
			this.server = server;
		}
		
		//verify the networking parameters not changed
		public boolean verifyNetworkingCondition(String netName, String netType, int sig){
			if(!this.networkName.equals(netName) )
				return false;
			if(!this.networkType.equals(netType) )
				return false;
			if(this.signalStrength != sig)
				return false;
			return true;
		}
	}
	
	public class BandwidthResponse{
		public boolean result;
		public String err_msg;
		public String data;
		
		public BandwidthResponse(boolean rs, String err_msg, String data){
			this.result = rs;
			this.err_msg = err_msg;
			this.data = data;
		}
	}
	
	private class MeasureLocalBandwidthTask extends Thread{
		private String networkName;
		private String networkType;
		private int signalStrength;
		private String remoteURL;
		private String bwMeasureURLPath;
		private String bwMeasureURLPort;
		
		public MeasureLocalBandwidthTask(String name, String type, int strength){
			this.networkName = name;
			this.networkType = type;
			this.signalStrength = strength;
			NetProphetPropertyManager pm = NetProphetPropertyManager.getInstance();
			this.remoteURL = pm.getBandwidthMeasureURL();
			this.bwMeasureURLPath = pm.getBWMeasureURLPath();
			this.bwMeasureURLPort = pm.getBwMeasureURLPort();
		}
		
		@Override
		public void run() {
			try{
				isRunning = true;
				String userID = "non-android-user";
				try{
					Context context = NetProphet.getInstance().getContext();
					if(context != null){
						TelephonyManager mTelephony = 
								(TelephonyManager) (context.getSystemService(Context.TELEPHONY_SERVICE)); 
						userID = mTelephony.getDeviceId(); 
					}
				}
				catch(Exception e){
					NetProphetLogger.logError("MeasureLocalBandwidthTask",
							"failed to acquire userID: "+e.toString());
				}
				
				BandwidthTransmissionData data = null;
				
				if(netUtility != null){
					data = new BandwidthTransmissionData(
							netUtility.getNetworkingFullName(),
							netUtility.getNetworkingType(),
							netUtility.getSignalStrength(),
							userID,phoneModel);
				}
				else{
					data = new BandwidthTransmissionData(
						this.networkName, this.networkType, this.signalStrength, userID,phoneModel);
				}
				Gson gson = new GsonBuilder().create();
				String jsonObj = gson.toJson(data);
				
				postMsg("Start running bandwidth testing...");
				// 1. ask server for measurement permission.
				BandwidthResponse rs = sendCMDRequest("ask-permission", jsonObj);
				if (!rs.result){
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
							"not allowed to do MeasureLocalBandwidthTask");
					postMsg("Error: bw measuring task is not allowed");
					return ;
				}
				
				// 2. pull a list of testing servers.
				rs = sendCMDRequest("query-server-list", jsonObj);
				if(!rs.result){
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
							"failed to load bw measurement server list: "+rs.err_msg);
					postMsg("Error: failed to load bw measurement server list");
					return ;
				}
				gson = new Gson();
				String[] serverList = gson.fromJson(rs.data, String[].class);
				if(serverList==null || serverList.length==0){
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
							"bw measurement server list is empty ");
					postMsg("Error: failed to load bw measurement server list");
					return ;
				}
				// 3. find the nearest server.
				postMsg("Start finding the nearest testing server...");
				PingTool pingTool = new PingTool(3);
				float minVal = 1000;
				String nearestServer = "";
				for(String server : serverList){
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
							"  Start ping testing server: "+server);
					postMsg("    Start ping testing server: "+server);
					MeasureResult mrs = pingTool.doPing(server);
					if(mrs.lossRate==0.0 && mrs.avgDelay<minVal){
						minVal = mrs.avgDelay;
						nearestServer = server;
					}
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
							"   Ping delay: "+mrs.avgDelay+" ms");
					postMsg("        Ping delay: "+mrs.avgDelay+" ms");
				}
				if(nearestServer.equals("")){
					NetProphetLogger.logWarning("MeasureLocalBandwidthTask", 
							"networking signal is bad or all servers are down ");
					data.setMeasurementResult(-1, -1, null);
					gson = new GsonBuilder().create();
					jsonObj = gson.toJson(data);		
					rs = sendCMDRequest("post-result", jsonObj);
					return ;
				}
				NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
						"nearest server: "+ nearestServer+" delay: "+minVal);
				postMsg("Choose server: "+ nearestServer+" as testing server. Avg delay: "+minVal+" ms.");
				postMsg("Start sending/receving packets...");
				// 4. do measurement task.
				long[] bws = new long[10];
				long bw = 0;
				for(int i=0; i<10; i++){
					bws[i] = sendMeasureRequest(nearestServer);
					bw = selectStableBW(bws, i+1, 5);
					if( bw != 0) break;
					//System.out.println("VAL: "+bws[i] );
				}
				Arrays.sort(bws);
				if(bw == 0)
					bw = bws[2];
				NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
						"Final Delay:"+bw+"KBits");
				
				postMsg("Measured bandwidth is:"+bw+" KBits");
				// post results to remote server.
				
				try{
					if(netUtility != null){
						if(!data.verifyNetworkingCondition(
								netUtility.getNetworkingFullName(), 
								netUtility.getNetworkingType(), 
								netUtility.getSignalStrength())){
							NetProphetLogger.logError("MeasureLocalBandwidthTask", 
									"do not post measurement result to server because of networking state changed");
							return ;
						}
					}
				}
				catch(Exception e){
					NetProphetLogger.logError("MeasureLocalBandwidthTask", 
						"failed in verifying networking state");
				}
				data.setMeasurementResult((int)bws[2], (int)minVal, nearestServer);
				gson = new GsonBuilder().create();
				jsonObj = gson.toJson(data);		
				rs = sendCMDRequest("post-result", jsonObj);
				
				if(rs.result){
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
						"succeeded posting measurement result to server: ");
					postMsg("Bandwidth results have been sent to server.");
				}
				else{
					NetProphetLogger.logError("MeasureLocalBandwidthTask", 
							"failed to post measurement result to server: "+rs.err_msg);
					postMsg("Failed to send bandwidth results to server:"+rs.err_msg);
				}
				
			}
			catch(Exception e){
				NetProphetLogger.logError("MeasureLocalBandwidthTask", e.toString());
				e.printStackTrace();
			}
			finally{
				isRunning = false;
			}
			postMsg("Bandwidth measurement task is finished.");
		}
		
		private long selectStableBW(long[] bws, int size, int window){
			if(size < window)
				return 0;
			long[] newbws = new long[window];
			for(int i=0; i<window; i++)
				newbws[i] = bws[size-window+i];
			Arrays.sort(newbws);
			long sum = 0;
			long medium = newbws[window/2];
			for(int i=0; i<window; i++)
				sum += (newbws[i]-medium) * (newbws[i]-medium);
			double sqrt = Math.sqrt(sum/window);
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<window; i++){
				sb.append("  "+newbws[i]);
			}
			double cv = sqrt/(double)medium;
			sb.append(" std varience:"+sqrt+" coefficient varience:"+cv);
			NetProphetLogger.logError("selectStableBW", sb.toString());
			if(cv < 0.1)
				return medium;
			return 0;
		}
		
		private int sendMeasureRequest(String server) throws Exception {
			OkHttpClient client = new OkHttpClient();
			String url = String.format("http://%s:%s%s", 
					server, this.bwMeasureURLPort, this.bwMeasureURLPath);
			Request request = new Request.Builder().url(url).build();
			NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
					"sendMeasureRequest: "+ url);
			Call c = client.newCall(request);			
			Response response;
			
			try {
				response = c.execute();
				String str = response.body().string();
				NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
						"sendMeasureRequest response size:"+str.length());
				int size = str.length() * 8;
				long transDelay = c.getCallStatInfo().getFirstResponseTransDelay();
				long overallDelay = c.getCallStatInfo().getOverallDelay();
				NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
						"sendMeasureRequest: "+
						" transDelay:"+transDelay+" overallDelay:"+overallDelay);
				
				int rs = (int)((float)size / (float)(transDelay) );
				postMsg("    Received "+size+" bytes with delay:"+transDelay+" ms.");
				return rs;
			} 
			catch (IOException e) {
				NetProphetLogger.logError("MeasureLocalBandwidthTask.sendMeasureRequest", e.toString());
				postMsg("Error: "+e.toString());
				e.printStackTrace();
			}
			return 0;
		}
		
		private BandwidthResponse sendCMDRequest(String action, String jsonObj) throws Exception {
			NetProphetLogger.logDebugging("MeaureTask.sendRequest", 
					"action:"+action+" json:"+jsonObj);
			
			if(jsonObj == null)
				jsonObj = "{}";
			RequestBody body = RequestBody.create(
					MediaType.parse("application/json; charset=utf-8"), jsonObj);
			String url = String.format(this.remoteURL+"?action=%s", 
					action.toLowerCase().trim());
			Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
			OkHttpClient client = new OkHttpClient();
			Call c = client.newCall(request);
			try {
				Response response = c.execute();
				String str = response.body().string();
				NetProphetLogger.logDebugging("MeaureTask.sendRequest", 
						"url:"+url+" response:"+str);
				/*long transDelay = c.getCallStatInfo().getFirstResponseTransDelay();
				long overallDelay = c.getCallStatInfo().getOverallDelay();
				NetProphetLogger.logDebugging("MeaureTask.sendRequest", 
						"url:"+url+
						" transDelay:"+transDelay+" overallDelay:"+overallDelay);*/
				Gson gson = new Gson();
				BandwidthResponse rs = gson.fromJson(str, BandwidthResponse.class);
				return rs;
			} 
			catch (Exception e) {
				NetProphetLogger.logError("MeaureTask.sendRequest", e.toString());
				e.printStackTrace();
			}
			return null;
		}
		
	}
}
