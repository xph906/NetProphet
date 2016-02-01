package netprophet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import static okhttp3.internal.Internal.logger;

public class NetProphetPropertyManager {
	private static NetProphetPropertyManager instance = null;
	public static NetProphetPropertyManager getInstance(){
		if(instance == null)
			instance = new NetProphetPropertyManager();
		return instance;
	};
	
	private NetProphetPropertyManager(){
		try {
			properties = properties();
			serverScheme = properties.getProperty("ServerScheme");
			serverHost = properties.getProperty("ServerHost");
			serverPort = properties.getProperty("ServerPort");
			remotePostReportPath = properties.getProperty("RemotePostReportPath");
			storeToRemoteServerEveryRequest =  
					Boolean.valueOf(properties.getProperty("StoreToRemoteServerEveryRequest"));
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed read properties: "+e);
			properties = null;
		}
	};
	
	private Properties properties;
	private String serverHost;
	private String serverPort;
	private String serverScheme;
	private boolean storeToRemoteServerEveryRequest; 
	private String remotePostReportPath;
	
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
	
	public String getRemotePostReportURL(){
		return 
			String.format("%s://%s:%s%s", 
					serverScheme, serverHost, serverPort, remotePostReportPath);
	}

	private Properties properties() throws IOException {
	    final Properties configProperties = new Properties();

	    // Attempt to read internal configuration
	    InputStream configStream = null;
	    try {
	        configStream = this.getClass().getClassLoader().getResourceAsStream("configuration.properties");
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
