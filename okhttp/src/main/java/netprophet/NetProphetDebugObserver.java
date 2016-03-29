package netprophet;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

public class NetProphetDebugObserver {
	public NetProphetDns netProphetDns;
	public NetProphetDebugObserver(){
		netProphetDns = OkHttpClient.getNetProphetDns();
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
}
