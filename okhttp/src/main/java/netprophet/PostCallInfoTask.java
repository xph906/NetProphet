package netprophet;
import static okhttp3.internal.Internal.logger;

import java.io.IOException;
import java.util.logging.Level;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostCallInfoTask implements Runnable{
    private String serverURL;
    private String contents;
    private OkHttpClient client;
    private MediaType jsonType;
    
	public PostCallInfoTask(String contents, String url){
    	if(url == null){
    		//TODO: read from configure file
    		serverURL = "";
    	}
    	else {
    		this.serverURL = url;
    	}
    	this.contents = contents;
    	this.client = new OkHttpClient(null);
    	this.jsonType = MediaType.parse("application/json; charset=utf-8");
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
			logger.log(Level.WARNING, 
					String.format("postCallInfoToServerTask %s failed: %s",this.serverURL, e.toString()));
		}
	}
	  
  }