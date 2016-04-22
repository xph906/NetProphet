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
    private Resolver resolver, resolverWithLongTimeout ;
    private Map<Name, Set<Name>> host2DNSName;
    private boolean enableSecondLevelCache;
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
    }
 
    private List<InetAddress> synchronousLookup(String hostname){
        try {
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
            NetProphetLogger.logDebugging("synchronousLookup",
            		"done DNS lookup "+hostname+" in "+dnsDelay +" ms");

            if(records == null)
            	return new ArrayList<InetAddress>();
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



    /*
     * Create a resolver.
     *   dnsServer == null: use system default server
     *   dnsServer != null: use `dnsServer`
     * */
    private Resolver createNewResolverBasedOnDnsServer(){
        try{
            Resolver resolver = null;
            if (dnsServer == null)
                resolver = new SimpleResolver();
            else
                resolver = new SimpleResolver(dnsServer);
            resolver.setTimeout(dnsTimeout);
            return resolver;
        }
        catch(Exception e){
            logger.severe("failed to initiate Resolver:"+e);
            e.printStackTrace();
            return null;
        }
    }

   
    
   
    
}
