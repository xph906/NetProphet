package netprophet;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.RealCall;
import okhttp3.internal.Internal.NetProphetLogger;

public class NetProphetOptimizationTool {
	static private NetProphetOptimizationTool tool = null;
	static public NetProphetOptimizationTool getInstance(){
		if (tool == null)
			tool = new NetProphetOptimizationTool();
		return tool;
	}
	private NetProphetOptimizationTool(){
		
	}
	//TODO: Reham.
	public boolean needCompressedResponse(RealCall c){
		return true;
	}
	
	//Return Value: 0 means do not change existing setting
	public int getOptimizedDNSTimeout(){
		return 0; //0 let the DNS Component decide.
	}
	//TODO: Guangyao.
	//Return Value: 0 means do not change existing setting
	public int getOptimizedConnectionTimeout(){
		return 10000; //10s
	}
	//TODO: Guangyao.
	//Return Value:  0 means do not change existing setting
	public int getOptimizedRecvTimeout(){
		return 10000; //10s
	}
	
	public OptimiztionInterceptor newOptimiztionInterceptor(RealCall c){
		return new OptimiztionInterceptor(c);
	}
	
	public class OptimiztionInterceptor implements Interceptor {
		private RealCall call;
		public OptimiztionInterceptor(RealCall c){
			call = c;
		}
		public void setRealCall(RealCall c){
			this.call = c;
		}
		@Override 
		public Response intercept(Interceptor.Chain chain) throws IOException {
			Request request = chain.request();
			Response response = null;
			NetProphetPropertyManager manager = NetProphetPropertyManager.getInstance();
			if(!manager.isEnableOptimization()){
				NetProphetLogger.logDebugging("OptimizationInterceptor", "Optimization is not enabled:"+call.request().url());
				response = chain.proceed(request);
				return response;
			}
			
			Builder builder =  request.newBuilder();
			if(needCompressedResponse(call)){
				builder.addHeader("Accept-Encoding", "gzip");
				call.setUseGZIPOptimization(true);
			}
			
			Request newReq = builder.build();
			NetProphetLogger.logDebugging("OptimizationInterceptor", "OPTIMIZATION: add GZIP on Header:"+call.request().url());
			response = chain.proceed(newReq);		
		    return response;
		  }
		}
}
