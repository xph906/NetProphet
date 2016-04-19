package netprophet;

import java.util.List;

import android.content.Context;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.internal.Internal.NetProphetLogger;

public class NetProphetDebugObserver {
	public NetProphetDns netProphetDns;
	public DatabaseHandler dbHandler;
	
	public NetProphetDebugObserver(Context context){
		netProphetDns = OkHttpClient.getNetProphetDns();
		if(netProphetDns == null)
			netProphetDns = new NetProphetDns();
		if(context == null)
			dbHandler = null;
		else
			dbHandler = dbHandler.getInstance(context);
	}
	
	public String getNetProphetDnsInfo(){
		if (netProphetDns == null){
			return "netProphetDns is NULL";
		}
		return String.format(
				"CurrentDNSTimeout: %d, RecordedDNSItemSize:%d, ViolatedItemSize:%d ", 
				netProphetDns.getDnsTimeout(), netProphetDns.getdnsDelayItems().size(),
				netProphetDns.getLongDnsDelayItems().size());
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
	public void debugTestingDNSServer(int maxDNSServerCount, int maxHostnameCount, List<String> hostnames){
		netProphetDns.startDNSServerMeasurement(maxDNSServerCount, maxHostnameCount, hostnames);
	}
}
