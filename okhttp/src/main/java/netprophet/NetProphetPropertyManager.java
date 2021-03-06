package netprophet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import static okhttp3.internal.Internal.logger;

public class NetProphetPropertyManager {
	final private static String DNS_SERVER_LIST_FILE_NAME = "dnsserverlist.properties";
	final private static String CONFIGURE_FILE_NAME = "configuration.properties";
	private static NetProphetPropertyManager instance = null;
	public static NetProphetPropertyManager getInstance(){
		if(instance == null)
			instance = new NetProphetPropertyManager();
		return instance;
	};
	
	public class DNSServer{
		public String name;
		public String IP;
		public String location;
		public DNSServer(String name, String IP, String location ){
			this.name = name;
			this.IP = IP;
			this.location = location;
		}
		public String toString(){
			return String.format("DNSServer:%s %s in:%s", name, IP, location);
		}
	};
	private Properties properties;
	private String serverHost;
	private String serverPort;
	private String serverScheme;
	private boolean storeToRemoteServerEveryRequest; 
	private String remotePostReportPath;
	private String DNSTestingHostPath;
	private Map<String, DNSServer> dnsServerMap;
	private int DBSyncLimit;
	private int DBSyncPacketRecordSize;
	private boolean enableOptimization;
	private String connectionTestingURL;
	private String bandwidthMeasureURL;
	private String remotePostReportFullPath;
	private boolean allowDBSync;
	private boolean enableTestingMode;
	private String bwMeasureURLPath;
	private String bwMeasureURLPort;
	
	public String getBwMeasureURLPort() {
		return bwMeasureURLPort;
	}

	public void setBwMeasureURLPort(String bwMeasureURLPort) {
		this.bwMeasureURLPort = bwMeasureURLPort;
	}

	public String getBWMeasureURLPath() {
		return bwMeasureURLPath;
	}

	public void setBWMeasureURLPath(String bwMeasureURLPath) {
		this.bwMeasureURLPath = bwMeasureURLPath;
	}

	public String getBandwidthMeasureURL() {
		return bandwidthMeasureURL;
	}

	public void setBandwidthMeasureURL(String bandwidthMeasureURL) {
		this.bandwidthMeasureURL = bandwidthMeasureURL;
	}
	public boolean isEnableTestingMode() {
		return enableTestingMode;
	}

	public void setEnableTestingMode(boolean enableTestingMode) {
		this.enableTestingMode = enableTestingMode;
	}

	public boolean allowDBSync() {
		return allowDBSync;
	}

	public void setAllowDBSync(boolean allowDBSync) {
		this.allowDBSync = allowDBSync;
	}

	private NetProphetPropertyManager(){
		try {
			properties = properties(CONFIGURE_FILE_NAME);
			serverScheme = properties.getProperty("ServerScheme");
			serverHost = properties.getProperty("ServerHost");
			serverPort = properties.getProperty("ServerPort");
			remotePostReportPath = properties.getProperty("RemotePostReportPath");
			DNSTestingHostPath = properties.getProperty("DNSTestingHostPath");
			storeToRemoteServerEveryRequest =  
					Boolean.valueOf(properties.getProperty("StoreToRemoteServerEveryRequest"));
			DBSyncLimit = Integer.valueOf(properties.getProperty("DBSyncLimit"));
			DBSyncPacketRecordSize = Integer.valueOf(properties.getProperty("DBSyncPacketRecordSize"));
			enableOptimization = false;
			connectionTestingURL = properties.getProperty("ConnectionTestingURL");
			remotePostReportFullPath = String.format("%s://%s:%s%s", 
					serverScheme, serverHost, serverPort, remotePostReportPath);
			allowDBSync = Boolean.valueOf(properties.getProperty("AllowDBSynchronization"));
			enableTestingMode = false; //by default, it's disabled.
			bandwidthMeasureURL = properties.getProperty("BandwidthMeasurementURL");
			bwMeasureURLPath = properties.getProperty("BWMeasureURLPath");
			bwMeasureURLPort = properties.getProperty("BWMeasureURLPort");
			
			dnsServerMap = new HashMap<String, DNSServer>();
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						this.getClass().getClassLoader().getResourceAsStream(DNS_SERVER_LIST_FILE_NAME)));
			    String line;
			    while ((line = br.readLine()) != null) {
			       line = line.trim();
			       if (line.startsWith("#"))
			    	   continue;
			       if(line.length() == 0)
			    	   continue;
			       String[] tmp = line.split("\\s");
			       if (tmp.length != 3)
			    	   continue;
			       //logger.severe("DNS2:"+tmp[0]);
			       String name = tmp[0].trim();
			       String ip = tmp[1].trim();
			       String loc = tmp[2].trim();
			       dnsServerMap.put(name, new DNSServer(name, ip, loc));    
			       //logger.severe(dnsServerMap.get(name).toString());
			    }
			}
			catch(IOException ee){
				logger.log(Level.SEVERE, "Failed to read dns server list:"+ee);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to read properties: "+e);
			properties = null;
		}
	};
	
	public boolean isEnableOptimization() {
		return enableOptimization;
	}

	public void setEnableOptimization(boolean enableOptimization) {
		this.enableOptimization = enableOptimization;
	}

	public int getDBSyncPacketRecordSize() {
		return DBSyncPacketRecordSize;
	}

	public void setDBSyncPacketRecordSize(int dBSyncPacketRecordSize) {
		DBSyncPacketRecordSize = dBSyncPacketRecordSize;
	}

	public int getDBSyncLimit() {
		return DBSyncLimit;
	}
	public void setDBSyncLimit(int dBSyncLimit) {
		DBSyncLimit = dBSyncLimit;
	}


	
	public String getConnectionTestingURL() {
		return connectionTestingURL;
	}

	public void setConnectionTestingURL(String connectionTestingURL) {
		this.connectionTestingURL = connectionTestingURL;
	}

	public Map<String, DNSServer> getDNSServerMap(){
		return dnsServerMap;
	}
	
	public String getServerScheme() {
		return serverScheme;
	}
	
	public Properties getProperties() {
		return properties;
	}

	public String getServerHost() {
		return serverHost;
	}

	public String getServerPort() {
		return serverPort;
	}

	public boolean canStoreToRemoteServerEveryRequest() {
		return storeToRemoteServerEveryRequest;
	}

	public String getRemotePostReportPath() {
		return remotePostReportPath;
	}
	public void setRemotePostReportURL(String url){
		remotePostReportFullPath = url;
	}
	
	public String getRemotePostReportURL(){
		return remotePostReportFullPath;
			
	}
	public String getDNSServerTestingURLList(){
		return
			String.format("%s://%s:%s%s",
					serverScheme, serverHost, serverPort, DNSTestingHostPath);
	}

	private Properties properties(String propertyPath) throws IOException {
	    final Properties configProperties = new Properties();

	    // Attempt to read internal configuration
	    InputStream configStream = null;
	    try {
	        configStream = this.getClass().getClassLoader().getResourceAsStream(propertyPath);
	        configProperties.load(configStream);
	    }
	    catch(Exception e){
	    	logger.log(Level.SEVERE, "failed to read properties() because "+e);
	    }
	    finally {
	        if(configStream != null) {
	            configStream.close();
	        }
	        configStream = null;
	    }
	    
	    return configProperties;
	}
}
