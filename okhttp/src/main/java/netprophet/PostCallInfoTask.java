package netprophet;
import static okhttp3.internal.Internal.logger;

import java.io.IOException;
import java.util.logging.Level;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Internal.NetProphetLogger;

public class PostCallInfoTask implements Runnable{
    private String serverURL;
    private String contents;
    private OkHttpClient client;
    private MediaType jsonType;
    private String token, appName;
    
	public PostCallInfoTask(String contents, String url){
    	if(url == null){
    		//TODO: read from configure file
    		serverURL = "";
    	}
    	else {
    		this.serverURL = url;
    	}
    	this.contents = contents;
    	this.client = new OkHttpClient();
    	this.jsonType = MediaType.parse("application/json; charset=utf-8");
    	NetProphet netProphet = NetProphet.getInstance();
    	if(netProphet == null){
    		this.token = "00000000000000000000000000000000";
    		this.appName = "developement-testing";
    	}
    	else{
    		this.token = netProphet.getToken();
    		this.appName = netProphet.getAppName();
    	}
    }
	  
	@Override
	public void run() {
		//logger.log(Level.INFO, "prepares send json object: "+this.contents);
		RequestBody body = RequestBody.create(this.jsonType, this.contents);
		Request request = new Request.Builder()
			.url(this.serverURL)
			.post(body)
			.build();
		
		try {
			Response response = client.newCall(request).execute();
			String str = response.body().string();
			//logger.log(Level.INFO, 
			//		String.format("DEBUG postCallInfoToServerTask succeed: %s", str));
		} catch (IOException e) {
			NetProphetLogger.logError("PostCallInfoTask.run", e.toString());
		}
	}
	  
  }