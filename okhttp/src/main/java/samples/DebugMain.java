package samples;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import okhttp3.Call;
import okhttp3.Call.CallTiming;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.RequestTimingANP;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import static okhttp3.internal.Internal.logger;

public class DebugMain {

	public void makeRequest(String url) throws Exception{
		OkHttpClient client = new OkHttpClient().newBuilder()
				.build();
		
		// Create request for remote resource.
		Request request = new Request.Builder().url(url).build();

		// Execute the request and retrieve the response.
		Call c = client.newCall(request);
		Response response = c.execute();
		logger.log(Level.WARNING, "Load url: "+url);
		// Deserialize HTTP response to concrete type.
		long t3 = System.currentTimeMillis();
		ResponseBody body = response.body();
		long size1 = body.contentLength();
		String str = body.string();
		long size2 = str.length();
		long t5 = System.currentTimeMillis();
		logger.log(Level.WARNING, 
				String.format("Response length before string():%d length:%d",size1, size2) );
		
		logger.log(Level.WARNING, "response: "+response.isRedirect());
		CallTiming timingObj = c.getCallTiming();
		if(timingObj == null)
			throw new Exception("timing is null!");
		
		long t1 = timingObj.startTimeANP;
		long t2 = timingObj.getEndTimeANP();
		List<RequestTimingANP> timingsANP = timingObj.timingsANP;
		List<String> urlsANP = timingObj.urlsANP;
		logger.log(Level.WARNING, 
				String.format("Overall delay: %d", t2-t1));
		if(timingsANP.size() != urlsANP.size()){
			throw new Exception("the sizes of urlsANP and timingsANP are not the same ");
		}
		Iterator<String> urlIter = urlsANP.iterator();
		Iterator<RequestTimingANP> timingIter = timingsANP.iterator();
		while(urlIter.hasNext()){
			String curURL = urlIter.next();
			RequestTimingANP timing = timingIter.next();
			long dnsDelay = timing.getDnsEndTimeANP() - timing.getDnsStartTimeANP();
			long connSetupDelay = timing.getConnSetupEndTimeANP() - timing.getConnSetupStartTimeANP();
			long reqWriteDelay = timing.getReqWriteEndTimeANP() - timing.getReqWriteStartTimeANP();
			long respDelay = timing.getRespEndTimeANP() - timing.getReqWriteStartTimeANP();
			long TTFB = timing.getRespStartTimeANP() - timing.getReqWriteEndTimeANP();
			long respTransDelay = timing.getRespEndTimeANP() - timing.getRespStartTimeANP();
			long overallDelay = timing.getRespEndTimeANP() - timing.getReqStartTimeANP();
			logger.log(Level.WARNING,
					String.format(
							"accurateRespTime:%b overall:%dms dns:%dms, connSetup:%dms (handshake:%dms), " + 
									"server:%dms, resp:%dms (1.reqwrite:%dms 2.TTFB:%dms, 3.respTrans:%dms ) \n for URL:%s\n", 
							timing.isAccurateEndTimeANP(), overallDelay, dnsDelay, connSetupDelay, 
							timing.getHandshakeTimeANP(), timing.getEstimatedServerDelay(), respDelay, reqWriteDelay,  TTFB, respTransDelay, curURL));
		}
		
	}
	
	public  void sendGetRequest(String url) throws IOException{
		OkHttpClient client = new OkHttpClient().newBuilder()
				.build();
		// Create request for remote resource.
		Request request = new Request.Builder().url(url).build();

		// Execute the request and retrieve the response.
		Call c = client.newCall(request);
		Response response = c.execute();
		logger.log(Level.WARNING, "Load url: "+url);
		ResponseBody body = response.body();
		long size1 = body.contentLength();
		String str = body.string();
		long size2 = str.length();
		logger.log(Level.WARNING, 
				String.format("ResponseSize: directLength:%d fullLength:%d",size1, size2) );
		logger.log(Level.INFO,
				String.format("Response    : %s", str));
	}
	
	public void postFile(String url, File file ) throws Exception{
		final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");
		RequestBody requestBody = new MultipartBody.Builder()
        	.setType(MultipartBody.FORM)
        	.addFormDataPart("title", "Square Logo")
        	.addFormDataPart("photho", "logo-square.png",
        			RequestBody.create(MEDIA_TYPE_JPG, file))
        			.build();

		Request request = new Request.Builder()
        	.url(url)
        	.post(requestBody)
        	.build();
		
		OkHttpClient client = new OkHttpClient().newBuilder()
				.build();
		Call c = client.newCall(request);
		Response response = c.execute();
		if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
		String result = response.body().string();
		
		CallTiming timingObj = c.getCallTiming();
		if(timingObj == null)
			throw new Exception("timing is null!");
		
		long t1 = timingObj.startTimeANP;
		long t2 = timingObj.getEndTimeANP();
		List<RequestTimingANP> timingsANP = timingObj.timingsANP;
		List<String> urlsANP = timingObj.urlsANP;
		logger.log(Level.WARNING, 
				String.format("Overall delay: %d", t2-t1));
		if(timingsANP.size() != urlsANP.size()){
			throw new Exception("the sizes of urlsANP and timingsANP are not the same ");
		}
		Iterator<String> urlIter = urlsANP.iterator();
		Iterator<RequestTimingANP> timingIter = timingsANP.iterator();
		while(urlIter.hasNext()){
			String curURL = urlIter.next();
			RequestTimingANP timing = timingIter.next();
			long dnsDelay = timing.getDnsEndTimeANP() - timing.getDnsStartTimeANP();
			long connSetupDelay = timing.getConnSetupEndTimeANP() - timing.getConnSetupStartTimeANP();
			long reqWriteDelay = timing.getReqWriteEndTimeANP() - timing.getReqWriteStartTimeANP();
			long respDelay = timing.getRespEndTimeANP() - timing.getReqWriteStartTimeANP();
			long TTFB = timing.getRespStartTimeANP() - timing.getReqWriteEndTimeANP();
			long respTransDelay = timing.getRespEndTimeANP() - timing.getRespStartTimeANP();
			long overallDelay = timing.getRespEndTimeANP() - timing.getReqStartTimeANP();
			logger.log(Level.WARNING,
					String.format(
							"accurateRespTime:%b overall:%dms \n  dns:%dms \n  connSetup:%dms (handshake:%dms) " + 
									"\n  server:%dms \n  resp:%dms (1.reqwrite:%dms 2.TTFB:%dms, 3.respTrans:%dms ) \n for URL:%s\n", 
							timing.isAccurateEndTimeANP(), overallDelay, dnsDelay, connSetupDelay, 
							timing.getHandshakeTimeANP(), timing.getEstimatedServerDelay(), respDelay, reqWriteDelay,  TTFB, respTransDelay, curURL));
		}
		
		System.out.println(result);
	}
	
	public void postStream() throws Exception {
		final MediaType MEDIA_TYPE_MARKDOWN
	      = MediaType.parse("text/x-markdown; charset=utf-8");

	  	OkHttpClient client = new OkHttpClient();
	  	RequestBody requestBody = new RequestBody() {
	        @Override public MediaType contentType() {
	          return MEDIA_TYPE_MARKDOWN;
	        }

	        @Override public void writeTo(BufferedSink sink) throws IOException {
	          sink.writeUtf8("Numbers\n");
	          sink.writeUtf8("-------\n");
	          for (int i = 2; i <= 3997; i++) {
	            sink.writeUtf8(String.format(" * %s = %s\n", i, factor(i)));
	          }
	        }

	        private String factor(int n) {
	          for (int i = 2; i < n; i++) {
	            int x = n / i;
	            if (x * i == n) return factor(x) + " × " + i;
	          }
	          return Integer.toString(n);
	        }
	      };
	      
	      Request request = new Request.Builder()
	        .url("https://api.github.com/markdown/raw")
	        .post(requestBody)
	        .build();
	      
	      Call c= client.newCall(request);
	      Response response = c.execute();
	      if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
	      String str = response.body().string();
	      displayTimingInfo(c);      
	}
	
	public void asynGet(String url) throws Exception{
		OkHttpClient client = new OkHttpClient();

	    Request request = new Request.Builder()
	        .url(url)
	        .build();
	    
	    Call c = client.newCall(request);
	    c.enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				System.err.println("Exception: "+e.toString());
				try {
					displayTimingInfo(call);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}      
			}
		
			@Override
			public void onResponse(Call call, Response response)  {
				try {
					String contents = response.body().string();
					displayTimingInfo(call);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	    });
		  
	}

	private void displayTimingInfo(Call c) throws Exception{
		 CallTiming timingObj = c.getCallTiming();
			if(timingObj == null)
				throw new Exception("timing is null!");
			
			long t1 = timingObj.startTimeANP;
			long t2 = timingObj.getEndTimeANP();
			List<RequestTimingANP> timingsANP = timingObj.timingsANP;
			List<String> urlsANP = timingObj.urlsANP;
			logger.log(Level.WARNING, 
					String.format("Overall delay: %d", t2-t1));
			if(timingsANP.size() != urlsANP.size()){
				throw new Exception("the sizes of urlsANP and timingsANP are not the same ");
			}
			Iterator<String> urlIter = urlsANP.iterator();
			Iterator<RequestTimingANP> timingIter = timingsANP.iterator();
			while(urlIter.hasNext()){
				String curURL = urlIter.next();
				RequestTimingANP timing = timingIter.next();
				long dnsDelay = timing.getDnsEndTimeANP() - timing.getDnsStartTimeANP();
				long connSetupDelay = timing.getConnSetupEndTimeANP() - timing.getConnSetupStartTimeANP();
				long reqWriteDelay = timing.getReqWriteEndTimeANP() - timing.getReqWriteStartTimeANP();
				long respDelay = timing.getRespEndTimeANP() - timing.getReqWriteStartTimeANP();
				long TTFB = timing.getRespStartTimeANP() - timing.getReqWriteEndTimeANP();
				long respTransDelay = timing.getRespEndTimeANP() - timing.getRespStartTimeANP();
				long overallDelay = timing.getRespEndTimeANP() - timing.getReqStartTimeANP();
				logger.log(Level.WARNING,
						String.format(
								"accurateRespTime:%b overall:%dms \n  dns:%dms \n  connSetup:%dms (handshake:%dms) " + 
										"\n  server:%dms \n  resp:%dms (1.reqwrite:%dms 2.TTFB:%dms, 3.respTrans:%dms ) \n for URL:%s\n", 
								timing.isAccurateEndTimeANP(), overallDelay, dnsDelay, connSetupDelay, 
								timing.getHandshakeTimeANP(), timing.getEstimatedServerDelay(), respDelay, reqWriteDelay,  TTFB, respTransDelay, curURL));
			}
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		DebugMain client = new DebugMain();
		String url = "https://api.github.com/repos/square/okhttp/contributors";
		client.sendGetRequest(url);
		
		String hostOregan = "http://52.11.26.222:3000/";
		
		/*url = hostOregan + "get-mini-file";
		client.makeRequest(url);
		url = hostOregan + "get-small-file";
		client.makeRequest(url);
		url = hostOregan + "get-medium-file";
		client.makeRequest(url);
		url = hostOregan + "get-large-file";
		client.makeRequest(url);
		
		url = hostOregan + "sleep-50";
		client.makeRequest(url);
		url = hostOregan + "sleep-200";
		client.makeRequest(url);
		url = hostOregan + "sleep-500";
		client.makeRequest(url);
		url = hostOregan + "sleep-2000";
		client.makeRequest(url);
		
		
		client.makeRequest("http://www.sina.com.cn/");
		client.makeRequest("http://www.douban.com");
		client.makeRequest("http://www.cnn.com");
		client.makeRequest("https://www.facebook.com");
		client.makeRequest("https://www.baidu.com");
		
		url = hostOregan + "upload-photo";
		String photoPath;
		//String photoPath = "/Users/xpan/Documents/projects/NetProphet/tmp/largefile.jpg";
		photoPath = "/Users/xpan/Documents/projects/NetProphet/tmp/smallfile.jpeg";
		client.postFile(url, new File(photoPath));
		
		photoPath = "/Users/xpan/Documents/projects/NetProphet/tmp/largefile.jpg";
		client.postFile(url, new File(photoPath));*/
		
		//client.makeRequest("http://www.taobao.com");
		client.postStream();
		client.asynGet("http://www.taobao.com");
	}

}
