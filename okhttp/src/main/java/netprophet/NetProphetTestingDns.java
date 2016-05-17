package netprophet;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import netprophet.NetProphetPropertyManager.DNSServer;
import netprophet.PingTool.MeasureResult;
import okhttp3.Call;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal.NetProphetLogger;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.regex.Pattern;

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
import com.google.gson.JsonElement;

import static okhttp3.internal.Internal.logger;
/**
 * Created by xpan on 3/2/16.
 */
public class NetProphetTestingDns implements Dns {
    //put this in the configure file.
    final static private boolean EnableSecondLevelCache = true;
    final static private Pattern IPPATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    
    public List<InetAddress> lookup(String hostname) throws UnknownHostException
    {
        return synchronousLookup(hostname);
    }

    private int dnsTimeout;
    private String dnsServer;
    private Resolver resolver ;
    private String defaultBackupDNSServer;
    private boolean useBackupDNSServer;

    ExecutorService executor;
    ExecutorService listOperationExecutor;
    
    /*
     * If dns failed because of timeout, start an asynchronous dns
     *   lookup task. If succeeded, add one longTimeoutItems object
     *   in `longTimeoutItems`.
     *
     * */
    public void setDnsTimeout(int dnsDefaultTimeout) {
        this.dnsTimeout = dnsDefaultTimeout;
    }
    public int getDnsTimeout(){
    	return dnsTimeout;
    }
    
    public NetProphetTestingDns(){
        dnsServer = null;
        dnsTimeout = 10; //default: 10 s
        
        resolver = null;
        
        defaultBackupDNSServer = "8.8.8.8";
        useBackupDNSServer = false;
    }
 
    private List<InetAddress> synchronousLookup(String hostname){
        try {
        	NetProphetLogger.logDebugging("synchronousLookup",
            		"start DNS lookup "+hostname);
        	if(IPPATTERN.matcher(hostname).matches()){
        		return Arrays.asList(InetAddress.getAllByName(hostname));
        	}
            hostname = hostname.trim().toLowerCase();
            Lookup lookup = new Lookup(hostname, Type.A);
            resolver = createNewResolverBasedOnDnsServer();
            lookup.setResolver(resolver);
            lookup.setCache(null);
            long dnsStartTimeout = System.currentTimeMillis();
            Record[] records = lookup.run();
            long dnsDelay = System.currentTimeMillis() - dnsStartTimeout;
            
            
            if(records == null){
            	NetProphetLogger.logDebugging("synchronousLookup",
                		"done DNS lookup "+hostname+" in "+dnsDelay +
                		" records is null");
            	return new ArrayList<InetAddress>();
            }
            else{
            	 NetProphetLogger.logDebugging("synchronousLookup",
                 		"done DNS lookup "+hostname+" in "+dnsDelay +" ms"+
            			 " rs-size:"+records.length);
            }
            // Update the cache.
            ArrayList<InetAddress> rs = new ArrayList<InetAddress>(records.length);
            for (Record record : records){
                // create return value;
                ARecord ar = (ARecord)record;
                rs.add(ar.getAddress());
            }
            return rs;
        }
        catch(Exception e){
            e.printStackTrace();
            return new ArrayList<InetAddress>();
        }
    }

    private List<String> getLocalDNSAddress(){
        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
            Method method = SystemProperties.getMethod("get", new Class[]{String.class});
            ArrayList<String> servers = new ArrayList<String>();
            for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4",}) {
                String value = (String) method.invoke(null, name);
                if (value != null && !"".equals(value) && !servers.contains(value))
                    servers.add(value);
            }
            return servers;
        }
        catch(Exception e){
            //NetProphetLogger.logError("getLocalDNSAddress", e.toString());
            //e.printStackTrace();
        }
        return new ArrayList<String>();
    }

    /*
     * Create a resolver.
     *   dnsServer == null: use system default server
     *   dnsServer != null: use `dnsServer`
     * */
    private Resolver createNewResolverBasedOnDnsServer(){
        try{
        	SimpleResolver resolver = null;
            if (dnsServer == null){
            	List<String> servers = getLocalDNSAddress();
        		if(servers.size() > 0)
        			resolver = new SimpleResolver(servers.get(0));
        		else{
        			resolver = new SimpleResolver(defaultBackupDNSServer);
        			useBackupDNSServer = true;
        		}
            }
            else
                resolver = new SimpleResolver(dnsServer);
            resolver.setTimeout(dnsTimeout);
            NetProphetLogger.logDebugging("createNewResolverBasedOnDnsServer", 
            		"DNS server: "+resolver.getAddress().getHostString());
            return resolver;
        }
        catch(Exception e){
            logger.severe("failed to initiate Resolver:"+e);
            e.printStackTrace();
            return null;
        }
    }

   
    
   
    
}
