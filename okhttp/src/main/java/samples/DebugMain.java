package samples;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

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
import com.google.gson.Gson;

public class DebugMain {

	public static void getStringRequest(String url) throws Exception {
		OkHttpClient client = new OkHttpClient().newBuilder().build();

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
			return;
		}

		// Retrieve the response and display timing information
		ResponseBody body = response.body();
		String str = body.string();
		displayTimingInfo(c);

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

		OkHttpClient client = new OkHttpClient().newBuilder().build();
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
		
		OkHttpClient client = new OkHttpClient();
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

	public static void asyncGetStringRequest(String url) throws Exception {
		OkHttpClient client = new OkHttpClient();

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
					"    response info:\n    returncode:%d\n    returnsize:%d\n"
							+ "    errorMsg:%s\n    errorDetailedMsg:%s\n", 
							c.getCallStatInfo().getCodeANP(), 
							c.getCallStatInfo().getSizeANP(), 
							c.getCallStatInfo().getCallErrorMsg(), 
							c.getCallStatInfo().getDetailedErrorMsgANP()));
		}
	}

	public static void main(String[] args) throws Exception {
		String hostOregan = "52.11.26.222";
		String httpPort = "3000";
		String tcpPort = "3001"; //none http port
		//"http://52.11.26.222:3000/"
		String oreganURL = "http://" + hostOregan + ':' + httpPort + '/';
		String curDirPath = "/Users/a/Projects/Test/okhttp/";
		
		// OKHTTP default testing
		logger.log(Level.INFO, "Testing: OKHTTP default testing");
		String url = "https://api.github.com/repos/square/okhttp/contributors";
		DebugMain.getStringRequest(url);
		
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
