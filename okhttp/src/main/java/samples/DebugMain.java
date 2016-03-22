package samples;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import netprophet.NetProphetPropertyManager;
import okhttp3.Call;
import okhttp3.Call.CallStatInfo;
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

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import com.google.gson.Gson;

public class DebugMain {

	public static void getStringRequest(String url) throws Exception {
		OkHttpClient client = new OkHttpClient(null).newBuilder().build();
		
		// Create request for remote resource.
		Request request = new Request.Builder().url(url).build();
		logger.log(Level.INFO, "Load url: " + url);

		// Execute the request and retrieve the response.
		Call c = client.newCall(request);
		Response response;
		try {
			response = c.execute();
		} catch (Exception e) {
			logger.log(Level.INFO, "  Call failed. Error Message:"
					+ c.getCallStatInfo().getCallErrorMsg());
			logger.log(Level.INFO, "        Detailed Information:"
					+ c.getCallStatInfo().getDetailedErrorMsgANP() + "\n");
			displayTimingInfo(c);
			return;
		}
 
		// Retrieve the response and display timing information
		ResponseBody body = response.body();
		String str = body.string();
		displayTimingInfo(c);
		logger.log(Level.INFO, "Done loading url: " + url+"\n");
	}

	public static void postJPGImage(String url, File file) throws Exception {
		final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("title", "Tesing jpg uploading")
				.addFormDataPart("photho", file.getName(),
						RequestBody.create(MEDIA_TYPE_JPG, file)).build();
		Request request = new Request.Builder().url(url).post(requestBody)
				.build();

		OkHttpClient client = new OkHttpClient(null).newBuilder().build();
		Call c = client.newCall(request);
		logger.log(Level.INFO, "Post image "+file.getName()+" to url: " + url);
		Response response = null;
		try {
			response = c.execute();
		} catch (Exception e) {
			logger.log(Level.INFO, "  Call failed. Error Message:"
					+ c.getCallStatInfo().getCallErrorMsg());
			logger.log(Level.INFO, "        Detailed Information: "
					+ c.getCallStatInfo().getDetailedErrorMsgANP() + "\n");
			return;
		}
		String str = response.body().string();
		displayTimingInfo(c);
	}

	public static void postStream(int size) throws Exception {
		final MediaType MEDIA_TYPE_MARKDOWN = MediaType
				.parse("text/x-markdown; charset=utf-8");
		final int postSize = size;
		
		OkHttpClient client = new OkHttpClient(null);
		RequestBody requestBody = new RequestBody() {
			@Override
			public MediaType contentType() {
				return MEDIA_TYPE_MARKDOWN;
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				sink.writeUtf8("Numbers\n");
				sink.writeUtf8("-------\n");
				for (int i = 2; i <= postSize; i++) {
					sink.writeUtf8(String.format(" * %s = %s\n", i, factor(i)));
				}
			}

			private String factor(int n) {
				for (int i = 2; i < n; i++) {
					int x = n / i;
					if (x * i == n)
						return factor(x) + " × " + i;
				}
				return Integer.toString(n);
			}
		};

		Request request = new Request.Builder()
				.url("https://api.github.com/markdown/raw").post(requestBody)
				.build();

		Call c = client.newCall(request);
		Response response = null;
		try {
			response = c.execute();
		} catch (Exception e) {
			logger.log(Level.INFO, "  Call failed. Error Message:"
					+ c.getCallStatInfo().getCallErrorMsg());
			logger.log(Level.INFO, "        Detailed Information: "
					+ c.getCallStatInfo().getDetailedErrorMsgANP() + "\n");
			return;
		}
		String str = response.body().string();
		displayTimingInfo(c);
	}

	public static void postJSON(String jsonOBJ, String url) throws Exception {
		logger.log(Level.INFO, "prepares send json object: "+jsonOBJ);
		RequestBody body = RequestBody.create(
				MediaType.parse("application/json; charset=utf-8"), jsonOBJ);
		Request request = new Request.Builder()
			.url(url)
			.post(body)
			.build();
		OkHttpClient client = new OkHttpClient(null);
		Call c = client.newCall(request);
		try {
			Response response = c.execute();
			String str = response.body().string();
			logger.log(Level.INFO, 
					String.format("postCallInfoToServerTask succeed: %s", str));
			
		} catch (IOException e) {
			logger.log(Level.WARNING, 
					String.format("postCallInfoToServerTask failed: %s", e.toString()));
		}
		
		displayTimingInfo(c);
	}
	
	public static void asyncGetStringRequest(String url) throws Exception {
		OkHttpClient client = new OkHttpClient(null);

		Request request = new Request.Builder().url(url).build();
		logger.log(Level.INFO, "Load url asynchronously: " + url);

		Call c = client.newCall(request);
		c.enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				logger.log(Level.INFO, "  Call failed. Error Message:"
						+ call.getCallStatInfo().getCallErrorMsg());
				logger.log(Level.INFO, "        Detailed Information:"
						+ call.getCallStatInfo().getDetailedErrorMsgANP() + "\n");
				return;
			}

			@Override
			public void onResponse(Call call, Response response) {
				try {
					String contents = response.body().string();
					displayTimingInfo(call);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	private static void displayTimingInfo(Call c) throws Exception {
		CallStatInfo timingObj = c.getCallStatInfo();
		c.storeCallStatInfo(true);
		
		if (timingObj == null)
			throw new Exception("timing is null!");

		long t1 = timingObj.getStartTimeANP();
		long t2 = timingObj.getEndTimeANP();
		logger.log(Level.INFO,
				String.format("Timing: call overall delay: %d ", t2 - t1));
		
		List<RequestTimingANP> timingsANP = timingObj.getTimingsANP();
		List<String> urlsANP = timingObj.getUrlsANP();
		// Debugging purpose
		if (timingsANP.size() != urlsANP.size()) {
			throw new Exception(
					"the sizes of urlsANP and timingsANP are not the same ");
		}
		
		// Display timing information
		Iterator<String> urlIter = urlsANP.iterator();
		Iterator<RequestTimingANP> timingIter = timingsANP.iterator();
		while (urlIter.hasNext()) {
			String curURL = urlIter.next();
			RequestTimingANP timing = timingIter.next();
			long dnsDelay = timing.getDnsEndTimeANP()
					- timing.getDnsStartTimeANP();
			long connSetupDelay = timing.getConnSetupEndTimeANP()
					- timing.getConnSetupStartTimeANP();
			long tlsConnDelay = timing.getTlsHandshakeTimeANP();
			long reqWriteDelay = timing.getReqWriteEndTimeANP()
					- timing.getReqWriteStartTimeANP();
			long respDelay = timing.getRespEndTimeANP()
					- timing.getReqWriteStartTimeANP();
			long TTFB = timing.getRespStartTimeANP()
					- timing.getReqWriteEndTimeANP();
			long respTransDelay = timing.getRespEndTimeANP()
					- timing.getRespStartTimeANP();
			long overallDelay = timing.getRespEndTimeANP()
					- timing.getReqStartTimeANP();

			logger.log(Level.INFO, String.format("  timing for url:%s", curURL));
			logger.log(
					Level.INFO,
					String.format(
							"    overall:%dms \n    dns:%dms \n    connSetup:%dms (handshake:%dms, tls:%dms) "
									+ "\n    server:%dms \n    resp:%dms (1.reqwrite:%dms 2.TTFB:%dms, 3.respTrans:%dms) ",
							overallDelay, dnsDelay, connSetupDelay,
							timing.getHandshakeTimeANP(), tlsConnDelay,
							timing.getEstimatedServerDelay(), respDelay,
							reqWriteDelay, TTFB, respTransDelay));
			logger.log(Level.INFO, String.format(
					"    response info:\n    returncode:%d\n    reqsize:%d\n    returnsize:%d\n"
							+ "    errorMsg:%s\n    errorDetailedMsg:%s\n", 
							c.getCallStatInfo().getFinalCodeANP(), 
							timing.getReqSizeANP(),
							c.getCallStatInfo().getSizeANP(), 
							c.getCallStatInfo().getCallErrorMsg(), 
							c.getCallStatInfo().getDetailedErrorMsgANP()));
		}
	}

	private static void printDNSCache(String cacheName) throws Exception {
	    Class<InetAddress> klass = InetAddress.class;
	    Field acf = klass.getDeclaredField(cacheName);
	    acf.setAccessible(true);
	    Object addressCache = acf.get(null);
	    Class cacheKlass = addressCache.getClass();
	    Field cf = cacheKlass.getDeclaredField("cache");
	    cf.setAccessible(true);
	    Map<String, Object> cache = (Map<String, Object>) cf.get(addressCache);
	    for (Map.Entry<String, Object> hi : cache.entrySet()) {
	        Object cacheEntry = hi.getValue();
	        Class cacheEntryKlass = cacheEntry.getClass();
	        Field expf = cacheEntryKlass.getDeclaredField("expiration");
	        expf.setAccessible(true);
	        long expires = (Long) expf.get(cacheEntry);
	        Field fs[] = cacheEntryKlass.getDeclaredFields();
	        //for (Field f : fs){
	        //	System.err.println(f.getName());
	        //}
	        Field af = cacheEntryKlass.getDeclaredField("addresses");
	        af.setAccessible(true);
	        InetAddress[] addresses = (InetAddress[]) af.get(cacheEntry);
	        List<String> ads = new ArrayList<String>(addresses.length);
	        for (InetAddress address : addresses) {
	            ads.add(address.getHostAddress());
	        }

	        System.out.println(hi.getKey() + " "+new Date(expires) +" " +ads);
	    }
	  }
	
	public static void main(String[] args) throws Exception {
		String hostOregan = "garuda.cs.northwestern.edu";
		String httpPort = "3000";
		String tcpPort = "3001"; //none http port
		//"http://52.11.26.222:3000/"
		String oreganURL = "http://" + hostOregan + ':' + httpPort + '/';
		String curDirPath = "/Users/xpan/Documents/projects/NetProphet/";
		OkHttpClient.initializeNetProphetDesktop();
	
		
	
		logger.log(Level.INFO, "Testing: OKHTTP default testing");
		String url = "https://api.github.com/repos/square/okhttp/contributors";
		//String url = "https://kat.cr/";
		DebugMain.getStringRequest(url);
		
		NetProphetPropertyManager manager = NetProphetPropertyManager.getInstance();
		//logger.log(Level.INFO, "server URL: "+manager.getRemotePostReportURL() +" "+manager.canStoreToRemoteServerEveryRequest());
		
		DebugMain.getStringRequest("http://news.sina.com.cn/o/2016-03-21/doc-ifxqnnkr9762064.shtml");
		DebugMain.getStringRequest("https://www.tmall.com");
		
		DebugMain.getStringRequest("http://www.cnn.com");
		DebugMain.getStringRequest("https://www.facebook.com");
		DebugMain.getStringRequest("http://51yes.com");
		
		/*
		String addressCache = "addressCache";
		printDNSCache(addressCache);
		 Cache cache = new Cache();
		 Resolver resolver = new SimpleResolver();
		 long t0 = System.currentTimeMillis();
		 String hn = "www.google.com";
		 Lookup lookup = new Lookup(hn, Type.A);
		 lookup.setResolver(resolver);
		 lookup.setCache(cache);
		 long t1 = System.currentTimeMillis();
		 Record[] records = lookup.run();
		 String address = ((ARecord) records[0]).getAddress().toString();
		 long t2 = System.currentTimeMillis();
		 t0 = t2 - t0;
		 t1 = t2 - t1;
		 System.err.println("delay:"+t0+" "+t1);
		 System.out.println(address);
		 Name n = null;
		 Cache secondCache = new Cache();
		 for (Record record : records){
			 //Record newRecord = new Record(record.getName(), record.getType(), record.getDClass(), 100000);
			 Class recordClass = record.getClass().getSuperclass();
			 Field cf = recordClass.getDeclaredField("ttl"); 
			 cf.setAccessible(true);
			 cf.set(record, 10000);
			 System.out.println(((ARecord)record).getAddress().toString()+" TTL:"+((ARecord)record).getTTL() +" name:"+record.getName());
			 ARecord newARecord = new ARecord(Name.fromString(hn+'.'), record.getDClass(), 5, ((ARecord)record).getAddress());
			 secondCache.addRecord(newARecord, 5, null);
			 
		 }
		 
		 t0 = System.currentTimeMillis();
		 lookup = new Lookup("www.cnn.com", Type.A);
		 lookup.setResolver(resolver);
		 lookup.setCache(cache);
		 t1 = System.currentTimeMillis();
		 records = lookup.run();
		 address = ((ARecord) records[0]).getAddress().toString();
		 t2 = System.currentTimeMillis() ;
		 t0 = t2 - t0;
		 t1 = t2 - t1;
		 System.err.println("delay2:"+t0+" "+t1);
		 DebugMain.getStringRequest("https://www.facebook.com");
		 RRset[] rs = secondCache.findRecords(Name.fromString(hn+'.'), Type.A);
		 for(RRset r : rs){
			 System.err.println("display cache: "+r);
		 }
		 */
		 /*DebugMain.getStringRequest("http://www.douban.com");
		 rs = secondCache.findRecords(Name.fromString(hn+'.'), Type.A);
		 for(RRset r : rs){
			 System.err.println("display cache: "+r);
		 }*/
		 
		 //printDNSCache(addressCache);
		//DebugMain.getStringRequest("https://www.baidu.com");
		
		// Testing error handling
		/*
		DebugMain.getStringRequest("http://www.snwx.com/book/5/5450/1661585.html");
		url = oreganURL + "404page";
		DebugMain.getStringRequest(url);
		url = "http://unknownaddress898989.com/" ;
		DebugMain.getStringRequest(url);
		url = "http://" + hostOregan + ':' + tcpPort + '/';
		DebugMain.getStringRequest(url);
		*/

		//DebugMain.getStringRequest("http://www.sina.com.cn/");
		DebugMain.getStringRequest("http://www.douban.com");
		//DebugMain.getStringRequest("http://www.cnn.com");
		//DebugMain.getStringRequest("https://www.facebook.com");
		//DebugMain.getStringRequest("https://www.baidu.com"); 
		DebugMain.getStringRequest("http://garuda.cs.northwestern.edu:3000/sleep-2000");
		DebugMain.getStringRequest("http://garuda.cs.northwestern.edu:3000/sleep-2000");
		DebugMain.getStringRequest("http://garuda.cs.northwestern.edu:3000/sleep-2000");
		DebugMain.getStringRequest("http://garuda.cs.northwestern.edu:3000/sleep-2000");
		DebugMain.getStringRequest("http://garuda.cs.northwestern.edu:3000/sleep-2000");
		//DebugMain.getStringRequest("https://www.douban.com/note/330868686/");
		DebugMain.getStringRequest("http://news.sina.com.cn/c/nd/2016-03-21/doc-ifxqnskh1078277.shtml");
		/*
		// Testing response transmission delay.
		logger.log(Level.INFO, "Testing: response transmission delay");
		url = oreganURL + "get-mini-file";
		DebugMain.getStringRequest(url);
		url = oreganURL + "get-small-file";
		DebugMain.getStringRequest(url);
		url = oreganURL + "get-medium-file";
		DebugMain.getStringRequest(url);
		url = oreganURL + "get-large-file";
		DebugMain.getStringRequest(url);

		// Testing server delay.
		logger.log(Level.INFO, "Testing: server delay");
		url = oreganURL + "sleep-50";
		DebugMain.getStringRequest(url);
		url = oreganURL + "sleep-200";
		DebugMain.getStringRequest(url);
		url = oreganURL + "sleep-500";
		DebugMain.getStringRequest(url);
		url = oreganURL + "sleep-2000";
		DebugMain.getStringRequest(url);

		// Testing real-world website
		logger.log(Level.INFO, "Testing: real-world website delay");
		DebugMain.getStringRequest("http://www.sina.com.cn/");
		DebugMain.getStringRequest("http://www.douban.com");
		DebugMain.getStringRequest("http://www.cnn.com");
		DebugMain.getStringRequest("https://www.facebook.com");
		DebugMain.getStringRequest("https://www.baidu.com");

		// Testing uploading images
		logger.log(Level.INFO, "Testing: post image delay");
		url = oreganURL + "upload-photo";
		String largePhotoPath = curDirPath+"tmp/largefile.jpg";
		DebugMain.postJPGImage(url, new File(largePhotoPath));
		String smallPhotoPath = curDirPath+"tmp/smallfile.jpg";
		DebugMain.postJPGImage(url, new File(smallPhotoPath));
		String unknowPhotoPath = curDirPath+"tmp/unknown.jpg";
		DebugMain.postJPGImage(url, new File(unknowPhotoPath));
		
		// Testing JSON posting
		logger.log(Level.INFO, "Testing: post JSON string");
		String jsonObj = "{'reqID':1453574957297,'url':'https://api.github.com/markdown/raw','method':'GET','userID':'Mac OS X','prevReqID':0,'nextReqID':0,'startTime':1453574955855,'endTime':1453574957294,'overallDelay':1439,'dnsDelay':34,'connDelay':787,'handshakeDelay':62,'tlsDelay':686,'reqWriteDelay':103,'serverDelay':410,'TTFBDelay':473,'respTransDelay':22,'useConnCache':false,'useDNSCache':false,'useRespCache':false,'respSize':25871,'HTTPCode':200,'reqSize':0,'isFailedRequest':false,'errorMsg':'NOERROR','detailedErrorMsg':'','transID':0,'transType':0}";	
		url = oreganURL + "post-callinfo";
		DebugMain.postJSON(jsonObj, url);
		
		// Testing streaming posting
		logger.log(Level.INFO, "Testing: post stream");
		DebugMain.postStream(1000);
		DebugMain.postStream(10000);
		
		// Testing redirection
		// Note sometimes taobao incurs TLS error.
		logger.log(Level.INFO, "Testing: redirection");
		DebugMain.getStringRequest("http://www.taobao.com");
		DebugMain.getStringRequest("http://www.yahoo.com");
		
		// Testing asynchronous get
		logger.log(Level.INFO, "Testing: asynchronous get");
		DebugMain.asyncGetStringRequest("https://www.yahoo.com");
		
		// Testing error handling
		DebugMain.getStringRequest("http://www.snwx.com/book/5/5450/1661585.html");
		url = oreganURL + "404page";
		DebugMain.getStringRequest(url);
		url = "http://unknownaddress898989.com/" ;
		DebugMain.getStringRequest(url);
		url = "http://" + hostOregan + ':' + tcpPort + '/';
		DebugMain.getStringRequest(url);
		*/

	}

}
