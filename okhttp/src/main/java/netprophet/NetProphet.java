package netprophet;

import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import java.util.logging.Level;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.OkHttpClient.Builder;
import okhttp3.internal.Internal.NetProphetLogger;
import okhttp3.internal.InternalCache;
import android.content.Context;
import android.net.ConnectivityManager;
import static okhttp3.internal.Internal.logger;

public class NetProphet {
	private static NetProphet netProphet = null;
	private static Context context = null;
	/*
	 * This function has been called in the beginning of application.
	 */
	public static void initializeNetProphet(Context context, boolean enableOptimization){
		NetProphet.context = context;
		OkHttpClient.initializeNetProphet(context, enableOptimization);
		DatabaseHandler.getInstance(context);
		NetUtility.getInstance(context, null);
		NetProphetPropertyManager.getInstance();
		logger.setLevel(Level.ALL);
	}
	/*
	 * This function is for testing/debugging on desktop.
	 */
	public static void initializeNetProphetDesktop(boolean enableOptimization){
		OkHttpClient.initializeNetProphetDesktop(enableOptimization);
	}

	public static NetProphet getInstance(){
		if(context == null)
			return null;
		if(netProphet == null)
			netProphet = new NetProphet();
		return netProphet;
	}
	
	private DatabaseHandler dbHandler;
	private NetUtility netUtility;
	private NetProphetPropertyManager propertyManager;
	private NetProphet(){
		if(context == null){
			NetProphetLogger.logError("NetProphet", "failed to initialize NetProphet: context is null.");
			return ;
		}
		dbHandler = DatabaseHandler.getInstance(context);
		netUtility = NetUtility.getInstance(context, null);
		netUtility.setmNetProphet(this);
		propertyManager = NetProphetPropertyManager.getInstance();
	}
	
	public void debugDBSynchronization(int count){
		long reqCount = dbHandler.getRequestInfoCount();
		if(reqCount >= count){
			NetProphetLogger.logDebugging("NetProphet", 
					"start to synchronize DB :"+reqCount+" records");
			dbHandler.synchronizeDatabase();
		}
		else{
			NetProphetLogger.logDebugging("NetProphet", 
					"too early to do DB synchronization because of "+reqCount+" records");
		}
	}
	public String getDBInfo(){
		long reqCount = dbHandler.getRequestInfoCount();
		long netinfoCount = dbHandler.getNetInfoCount();
		String syncInfo = dbHandler.getDBSyncData();
		return String.format("DBInfo: reqRecordCount:%d  NetInfoRecordCount:%d\n SyncInfo:%s",
				reqCount, netinfoCount, syncInfo);
	}
	
	protected void networkingChanged(int type, String name){
		if(type == ConnectivityManager.TYPE_WIFI){
			NetProphetLogger.logDebugging("networkingChanged", "networking changed to WIFI");
			//TODO: make sure the WIFI works properly, then do DB synchronization
			long reqCount = dbHandler.getRequestInfoCount();
			if(reqCount >= propertyManager.getDBSyncLimit()){
				NetProphetLogger.logDebugging("networkingChanged",
						"start to synchronize DB :"+reqCount+" records");
				dbHandler.synchronizeDatabase();
			}
			else{
				NetProphetLogger.logDebugging("networkingChanged",
						"too early to do DB synchronization because of "+reqCount+" records");
			}
		}
		else if(type==ConnectivityManager.TYPE_MOBILE ||
				type==ConnectivityManager.TYPE_MOBILE_DUN ||
				type==ConnectivityManager.TYPE_MOBILE_HIPRI ||
				type==ConnectivityManager.TYPE_MOBILE_MMS ||
				type==ConnectivityManager.TYPE_MOBILE_SUPL ){
			NetProphetLogger.logDebugging("networkingChanged", "networking changed to MOBILE");
		}
		else if(type == -1){ /*No connection */
			NetProphetLogger.logDebugging("networkingChanged", "no connection.");
		}
		else{
			NetProphetLogger.logError("networkingChanged", "unknown networking type: "+type);
		}
	}
	
}
