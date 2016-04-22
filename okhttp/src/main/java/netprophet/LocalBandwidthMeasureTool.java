package netprophet;

import static okhttp3.internal.Internal.logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import netprophet.PingTool.MeasureResult;
import android.R.array;
import android.os.Handler;
import android.os.Message;

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
	
	private LocalBandwidthMeasureTool(){
		bandwidthCache = new HashMap<String, Float>();	
		isRunning = false;
		handler = null;
	}
	
	public void startMeasuringTask(NetUtility netUtility){
		String networkName = null;
		String networkType = null;
		int signalStrength = 0;
		if (isRunning){
			NetProphetLogger.logDebugging("startMeasuringTask", 
					"Measuring task is running");
			postMsg("Measuring task is still running");
			return;
		}
		
		if(netUtility != null){
			networkName = netUtility.getNetworkingFullName();
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
	
	public class BandwidthEnvironmentData{
		public String networkName;
		public String networkType;
		public int signalStrength;
		public BandwidthEnvironmentData(String name, String type, int sig){
			this.networkName = name;
			this.networkType = type;
			this.signalStrength = sig;
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
				BandwidthEnvironmentData data = new BandwidthEnvironmentData(
						this.networkName, this.networkType, this.signalStrength);
				Gson gson = new GsonBuilder().create();
				String jsonObj = gson.toJson(data);
				
				postMsg("start running bandwidth testing...");
				// ask server for measurement permission.
				BandwidthResponse rs = sendCMDRequest("ask-permission", jsonObj);
				if (!rs.result){
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
							"not allowed to do MeasureLocalBandwidthTask");
					postMsg("Error: bw measuring task is not allowed");
					return ;
				}
				
				// pull a list of testing servers.
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
				// find the nearest server.
				PingTool pingTool = new PingTool(3);
				float minVal = 1000;
				String nearestServer = "";
				for(String server : serverList){
					NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
							"ping testing server:"+server);
					MeasureResult mrs = pingTool.doPing(server);
					if(mrs.lossRate==0.0 && mrs.avgDelay<minVal){
						minVal = mrs.avgDelay;
						nearestServer = server;
					}
					postMsg("ping server: "+server);
					postMsg("  avg delay: "+mrs.avgDelay+" ms");
				}
				if(nearestServer.equals("")){
					NetProphetLogger.logWarning("MeasureLocalBandwidthTask", 
							"networking signal is bad or all servers are down ");
					//TODO: paerhasp using such information
					return ;
				}
				NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
						"nearest server: "+ nearestServer+" delay: "+minVal);
				postMsg("nearest server: "+ nearestServer+" delay: "+minVal);
				
				// do measurement task.
				long[] bws = new long[5];
				for(int i=0; i<5; i++){
					bws[i] = sendMeasureRequest(nearestServer);
					//System.out.println("VAL: "+bws[i] );
				}
				Arrays.sort(bws);
				NetProphetLogger.logDebugging("MeasureLocalBandwidthTask", 
						"Final Delay:"+bws[2]+"KBits");
				
				postMsg("measured bandwidth:"+bws[2]+" KBits");
				// post results to remote server.
				postMsg("bw measurement finished");
			}
			catch(Exception e){
				NetProphetLogger.logError("MeasureLocalBandwidthTask", e.toString());
				e.printStackTrace();
			}
			finally{
				isRunning = false;
			}
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
				postMsg("  size:"+size+" delay:"+transDelay);
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
