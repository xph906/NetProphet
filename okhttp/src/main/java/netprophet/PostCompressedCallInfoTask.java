package netprophet;

import static okhttp3.internal.Internal.logger;

import java.io.IOException;
import java.util.logging.Level;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

public class PostCompressedCallInfoTask implements Runnable {
    private String serverURL;
    private String plaintext;
    private OkHttpClient client;
    private MediaType jsonType;
    
    public PostCompressedCallInfoTask(String plaintext, String url){
    	this.serverURL = url;
	
    	this.plaintext = plaintext;
    	this.client = new OkHttpClient();
    	this.jsonType = MediaType.parse("application/json; charset=utf-8");
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
			logger.severe("error in generating gzip request: "+e);
			e.printStackTrace();
		}
		
		try {
			Response response = client.newCall(request).execute();
			String str = response.body().string();
			logger.log(Level.INFO, 
					String.format("DEBUG PostCompressedCallInfoTask succeed: %s", str));
		} catch (IOException e) {
			logger.severe(
					String.format("PostCompressedCallInfoTask %s failed: %s",this.serverURL, e.toString()));
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
