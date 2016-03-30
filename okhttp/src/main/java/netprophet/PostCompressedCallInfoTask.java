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
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

public class PostCompressedCallInfoTask implements Runnable {
    private String serverURL;
    private String plaintext;
    private OkHttpClient client;
    private MediaType jsonType;
    private DatabaseHandler handler;
    private int tag;
    
    public PostCompressedCallInfoTask(String plaintext, String url, DatabaseHandler handler){
    	this.serverURL = url;
    	this.handler = handler;
    	this.plaintext = plaintext;
    	this.client = new OkHttpClient();
    	this.jsonType = MediaType.parse("application/json; charset=utf-8");
    	this.tag = plaintext.hashCode();
    }
    
	@Override
	public void run() {
		RequestBody body = RequestBody.create(this.jsonType, this.plaintext);
		Request request = null;
		try{
			request = new Request.Builder()
				.url(this.serverURL)
				.addHeader("Content-Encoding", "gzip")
				.post(forceContentLength(gzip(body)) )
				.build();
		}
		catch(Exception e){
			NetProphetLogger.logError("PostCompressedCallInfoTask.run1", e.toString());
			e.printStackTrace();
			handler.setSyncSuccessfulTag(false);
			return ;
		}
		
		try{
			handler.addPostTag(tag);
			int responseCode = -1;
			for(int count=0; count < 3; count++){
				try{
					Response response = client.newCall(request).execute();
					String str = response.body().string();
					responseCode = response.code();
					if(responseCode == 200)
					{
						NetProphetLogger.logDebugging("PostCompressedCallInfoTask.run", 
								"PostCompressedCallInfoTask succeed: "+str);	
						break;
					}
				}
				catch(Exception e){
					NetProphetLogger.logError("PostCompressedCallInfoTask.run2",
							String.format("PostCompressedCallInfoTask %s failed: %s (%d/3 times)",
									this.serverURL, e.toString(), count+1));
				}
			}
			if (responseCode != 200) {
				NetProphetLogger.logError("PostCompressedCallInfoTask.run2",
						"PostCompressedCallInfoTask failed three times. Give up!");
				handler.setSyncSuccessfulTag(false);
			}
		}
		finally{
			handler.deletePostTag(tag);
		}
	}
	
	private RequestBody gzip(final RequestBody body) {
		return new RequestBody() {
		  @Override 
		  public MediaType contentType() {
		    return body.contentType();
		  }
		
		  @Override
		  public long contentLength() {
		    return -1; // We don't know the compressed length in advance!
		  }
		
		  @Override
		  public void writeTo(BufferedSink sink) throws IOException {
		    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
		    body.writeTo(gzipSink);
		    gzipSink.close();
		  }
		};
	}
	
	private RequestBody forceContentLength(final RequestBody requestBody) throws IOException {
	    final Buffer buffer = new Buffer();
	    requestBody.writeTo(buffer);
	    return new RequestBody() {
	      @Override
	      public MediaType contentType() {
	        return requestBody.contentType();
	      }

	      @Override
	      public long contentLength() {
	        return buffer.size();
	      }

	      @Override
	      public void writeTo(BufferedSink sink) throws IOException {
	        sink.write(buffer.snapshot());
	      }
	    };
	  }

}
