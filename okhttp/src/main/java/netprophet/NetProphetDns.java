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
public class NetProphetDns implements Dns {
    final static private int maxSecondLevCacheEntry = 3000;
    final static private int SecondLevelCacheRecordCredibility = 5;
    final static private int hardDNSTimeout = 20; //20s
    final static private int DefaultTimeout = 10;
    final static private int LongTimeoutItemThreshold = 3;
    final static private int UpdateDNSTimeoutThreshold = 20;
    final static private int DNSTestingHostNumber = 10;
    final static private Pattern IPPATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    
    //put this in the configure file.
    final static private boolean EnableSecondLevelCache = true;

    /*
     * 1. search system DNS cache
     * 2. search NetProphet DNS cache (defaultCache)
     *  a.  if find, return result;
     *  b.  if not:
     *    (a). in slow networking scenario, search secondLevelDNSCache
     *         (secondLevelCache). If find, then do asynchronous lookup.
     *    (b). in good networking scenario or fail to find from secondLevelDNSCache
     *         do synchronous lookup.
     * 3. after lookup is done,
     *    (a). update NetProphet DNS cache (defaultCache).
     *    (b). update second level cache (secondLevelDNSCache).
     */
    public List<InetAddress> lookup(String hostname) throws UnknownHostException
    {
    	if(IPPATTERN.matcher(hostname).matches()){
    		return Arrays.asList(InetAddress.getAllByName(hostname));
    	}
    	
        List<InetAddress> cachedRecordList = new ArrayList<InetAddress>();
        StringBuilder errorMsg = new StringBuilder();
        if(searchSystemDNSCache(hostname,cachedRecordList, errorMsg)){
        	//logger.info("  found record in system cache: "+hostname);
            return cachedRecordList;
        }
        InetAddress cachedRecord = searchCache(defaultCache, hostname);
        if (cachedRecord != null){
            cachedRecordList.add(cachedRecord);
            //logger.info("  found record in default cache: "+hostname);
            //System.err.flush();
            return cachedRecordList;
        }
        if (enableSecondLevelCache){
            cachedRecord = searchCache(secondLevelCache, hostname);
            if (cachedRecord != null){
                cachedRecordList.add(cachedRecord);
                //logger.info("  found record in second level cache: "+hostname);
                executor.execute(new AsynLookupTask(hostname));
                return cachedRecordList;
            }
        }
        //logger.info("defaultCache Entry:"+defaultCache.getSize() +" secCacheEntry:"+secondLevelCache.getSize());
        //logger.info("do synchronous lookup!  "+hostname);
        return synchronousLookup(hostname);
    }

    public InetAddress testCache(int cacheIndex, String hostname) throws UnknownHostException
    {
        if(cacheIndex == 1)
            return searchCache(defaultCache, hostname);
        else if(cacheIndex == 2)
            return searchCache(secondLevelCache, hostname);
        else
            return null;
    }

    /* Cache Key Rule:
     * The key for defaultCache is the Name of DNSJava,
     *   which can be hostname+'.' or some dns domain name.
     *   like www.sina.com.cn
     * The key for secondLevelCache is always hostname+'.'
     *
     * Three DNS Cache:
     * SystemCache: this cache is maintained by Android,
     *   It's searched by calling searchSystemDNSCache(...).
     * defaultCache: this cache is maintained by NetProphetDns object,
     *   It respects the TTL set by server.
     *   By default, all items searched by NetProphetDns will be
     *   cached in defaultCache.
     * secondLevelCache: this cache is maintained by NetPriphetDns
     *   object. The TTL is set by `userDefinedTTL`. All items will
     *   be cached here, but it's only cached when in slow scenario.
     */
    private Cache defaultCache;
    private Cache secondLevelCache; //this cache
    private long userDefinedTTL, userDefinedNegTTL;
    /* Default lookup server*/
    private int dnsTimeout;
    
    // Stores all delays larger than timeout. 
    // used to increase dnsDefaultTimeout.
    private LinkedList<TimeoutExceptionItem> longDnsDelayItems;	
    // Stores all delays, including the one larger than timeout.
    // used to decrease dnsDefaultTimeout.
    private LinkedList<TimeoutExceptionItem> dnsDelayItems; 
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
    
    /*
     * TODO: this function is used to update DNSTimeout, 
     *       it needs further design.
     *   Called every time when either of following changed:
     *    1. longDnsDelayItems.
     *    2. dnsDelayItems.
     *    2. dnsDefaultTimeout.
     *  */
    synchronized private void adjustTimeout(){
        if (longDnsDelayItems.size() >= LongTimeoutItemThreshold) {
            long largestDelay = 0, smalledstDelay = hardDNSTimeout;
            for (TimeoutExceptionItem item : longDnsDelayItems){
                if(item.delay > largestDelay)
                    largestDelay = item.delay;
                if(item.delay < smalledstDelay)
                    smalledstDelay = item.delay;
            }
            //double delay = (largestDelay - smalledstDelay)*0.8 + smalledstDelay;
            //int newDnsDefaultTimeout = (int)((delay+500)/1000);
            int newDnsDefaultTimeout = (int)((largestDelay+500)/1000);      
            if (newDnsDefaultTimeout > hardDNSTimeout)
                newDnsDefaultTimeout = hardDNSTimeout;
            if (newDnsDefaultTimeout < 3)
    			newDnsDefaultTimeout = 3;
           logger.info("DNSTimeout has been updated[1] from "+
        		   dnsTimeout+" to "+newDnsDefaultTimeout);
            resolver.setTimeout(newDnsDefaultTimeout);
            dnsTimeout = newDnsDefaultTimeout;
            longDnsDelayItems.clear();
        }
        else{
        	if (dnsDelayItems.size() > UpdateDNSTimeoutThreshold){
        		long largestDelay = 0;
        		for(TimeoutExceptionItem item : dnsDelayItems){
        			if(item.delay > largestDelay)
                        largestDelay = item.delay;
        		}
        		int newDnsDefaultTimeout = (int)((largestDelay+500)/1000) + 1;  
        		if (newDnsDefaultTimeout > hardDNSTimeout)
                    newDnsDefaultTimeout = hardDNSTimeout;
        		if (newDnsDefaultTimeout < 3)
        			newDnsDefaultTimeout = 3;
        		logger.info("DNSTimeout has been updated[2] from "+
        				dnsTimeout+" to "+newDnsDefaultTimeout);
                resolver.setTimeout(newDnsDefaultTimeout);
                dnsTimeout = newDnsDefaultTimeout;
                dnsDelayItems.clear();
        	}
        }
    }
    /* The two delay item list should be manipulated in synchronized methods */
    synchronized void addItemToDelayItems(TimeoutExceptionItem delayItem, 
    		TimeoutExceptionItem longDelayItem){
    	if (delayItem != null ) dnsDelayItems.add(delayItem);
    	if (longDelayItem != null ) longDnsDelayItems.add(longDelayItem); 
    }
    public LinkedList<TimeoutExceptionItem> getLongDnsDelayItems(){
    	return longDnsDelayItems;
    }
    public LinkedList<TimeoutExceptionItem> getdnsDelayItems(){
    	return dnsDelayItems;
    }
    /* TODO: this function is not fully implemented.
     * Empty longDnsDelayItems and dnsDelayItems
     */
    synchronized public void changeNetworkingState(){
    	longDnsDelayItems.clear();
    	dnsDelayItems.clear();
    	dnsTimeout = DefaultTimeout;
    }
    
    public void setEnableSecondLevelCache(boolean enableSecondLevelCache) {
        this.enableSecondLevelCache = enableSecondLevelCache;
    }

    public NetProphetDns(){
        defaultCache = new Cache();
        defaultCache.setMaxEntries(maxSecondLevCacheEntry);
        secondLevelCache = new Cache();
        secondLevelCache.setMaxEntries(maxSecondLevCacheEntry);
        dnsServer = null;
        dnsTimeout = DefaultTimeout; //default: 10 s
        userDefinedTTL = 60 * 60; //default: 1 hour
        resolver = createNewResolverBasedOnDnsServer(dnsTimeout);
        resolverWithLongTimeout = createNewResolverBasedOnDnsServer(hardDNSTimeout);
        host2DNSName = createLRUMap(100);

        enableSecondLevelCache = EnableSecondLevelCache;
        longDnsDelayItems = new LinkedList<TimeoutExceptionItem>();
        dnsDelayItems = new LinkedList<TimeoutExceptionItem>();
        
        executor = Executors.newFixedThreadPool(5);
        listOperationExecutor = Executors.newFixedThreadPool(10);
        
        //For debugging...
        //List<String> hosts = loadDNSServerTestingHostList();
        //findBestDNSServer(hosts);
    }

    /*
     * Search the NetProphet Cache.
     */
    private InetAddress searchCache(Cache cache, String hostname){
        try {
            Name rawHostName = generateNameFromHost(hostname);
            if (host2DNSName.containsKey(rawHostName)) {
                Set<Name> names = host2DNSName.get(rawHostName);
                for (Name name : names) {
                    RRset[] rs = cache.findAnyRecords(name, Type.A);
                    if(rs == null)
                        continue;
                    for (RRset s : rs) {
                        try {
                            Iterator<Record> iter = s.rrs();
                            InetAddress addr = InetAddress.getByAddress(iter.next().rdataToWireCanonical());
                            return addr;
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            RRset[] rs = cache.findAnyRecords(rawHostName, Type.A);
            if (rs == null)
                return null;
            for (RRset s : rs) {
                try {
                    Iterator<Record> iter = s.rrs();
                    InetAddress addr = InetAddress.getByAddress(iter.next().rdataToWireCanonical());
                    return addr;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
        catch(Exception e){
        	logger.severe("error in searchCache: "+e);
            e.printStackTrace();
        }
        return null;
    }
    /*
    * Search the System Cache.
    */
    static public boolean searchSystemDNSCache(String hostname,
                                        List<InetAddress> result, StringBuilder errorMsg) {
        try {
            String cacheName = "addressCache";
            Class<InetAddress> klass = InetAddress.class;
            Field acf = klass.getDeclaredField(cacheName);

            acf.setAccessible(true);
            Object addressCache = acf.get(null);
            Class cacheKlass = addressCache.getClass();
            Field cf = cacheKlass.getDeclaredField("cache");
            cf.setAccessible(true);
            Object realCacheClass = cf.get(addressCache);
            Class cacheClass = realCacheClass.getClass();
            Method snapshotMethod = cacheClass.getMethod("snapshot");
            snapshotMethod.setAccessible(true);

            Map<String, Object> cache = (Map<String, Object>) (snapshotMethod.invoke(realCacheClass));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> hi : cache.entrySet()) {
                String host = hi.getKey();
                host = host.trim().toLowerCase();
                if (!host.equals(hostname))
                    continue;

                Object cacheEntry = hi.getValue();
                Class cacheEntryKlass = cacheEntry.getClass();
                Field af = cacheEntryKlass.getDeclaredField("value");
                af.setAccessible(true);
                InetAddress[] addresses =null;
                try {
                    addresses = (InetAddress[]) af.get(cacheEntry);
                }
                catch(java.lang.ClassCastException e){
                    errorMsg.append(((String)af.get(cacheEntry))+"error:"+e);
                    return true;
                }
                for (InetAddress address : addresses) {
                    result.add(address);
                }
                return true;
            }
            return false;
        }
        catch(Exception e){
            //System.err.println("error in searchSystemDNSCache: "+e.toString());
            //e.printStackTrace();
            return false;
        }
    }

    private List<InetAddress> synchronousLookup(String hostname){
        try {
            hostname = hostname.trim().toLowerCase();
            Lookup lookup = new Lookup(hostname, Type.A);
            lookup.setResolver(resolver);
            lookup.setCache(defaultCache);
            long dnsStartTimeout = System.currentTimeMillis();
            Record[] records = lookup.run();
            long dnsDelay = System.currentTimeMillis() - dnsStartTimeout;
            logger.info("done DNS lookup "+hostname+" in "+dnsDelay +" ms");

            //Cannot find the hostname, returns directly.
            if (records == null){
                if(dnsDelay > dnsTimeout){
                    //error caused by timeout
                    //start an asynchronous dns lookup.
                    executor.execute(new AsynLookupTask(hostname));
                }
                return new ArrayList<InetAddress>();
            }
            listOperationExecutor.execute(
            		new DelayItemListOperationTask(
            				new TimeoutExceptionItem(System.currentTimeMillis(), dnsDelay, hostname), null));

            // Update the cache.
            ArrayList<InetAddress> rs = new ArrayList<InetAddress>(records.length);
            RRset rrset = new RRset();
            Name rawHostName = generateNameFromHost(hostname);
            for (Record record : records){
                // create return value;
                ARecord ar = (ARecord)record;
                rs.add(ar.getAddress());

                // update host2DNSName if rawHostName cannot generate real hostname
                if(!record.getName().equals(rawHostName)){
                    if(host2DNSName.containsKey(rawHostName)){
                        host2DNSName.get(rawHostName).add(record.getName());
                    }
                    else{
                        Set<Name> newSet = new HashSet<Name>();
                        newSet.add(record.getName());
                        host2DNSName.put(rawHostName, newSet);
                    }
                }

                // update secondLevelCache
                ARecord newARecord =  new ARecord(rawHostName, record.getDClass(),
                                userDefinedTTL, ((ARecord)record).getAddress());
                rrset.addRR(newARecord);
                /*logger.info(
                 *       String.format("Name:%s IP:%s TTL:%d\n",
                 *           record.getName(),
                 *           ((ARecord) record).getAddress().toString(),
                 *           ((ARecord) record).getTTL())); 
                 */
            }
            storeRRsetToSecondLevCache(rawHostName, rrset);
            return rs;
        }
        catch(Exception e){
            e.printStackTrace();
            return new ArrayList<InetAddress>();
        }
    }


    private Name generateNameFromHost(String host){
        try {
            return Name.fromString(host + '.');
        }
        catch(Exception e){
            logger.severe("error in generateNameFromHost: "+e);
            e.printStackTrace();
            return null;
        }
    }


    public String displaySysDNSCache()  {
        try {
            String cacheName = "addressCache";
            Class<InetAddress> klass = InetAddress.class;
            Field acf = klass.getDeclaredField(cacheName);

            acf.setAccessible(true);
            Object addressCache = acf.get(null);
            Class cacheKlass = addressCache.getClass();
            Field cf = cacheKlass.getDeclaredField("cache");
            cf.setAccessible(true);
            Object realCacheClass = cf.get(addressCache);
            Class cacheClass = realCacheClass.getClass();
            Method snapshotMethod = cacheClass.getMethod("snapshot");
            snapshotMethod.setAccessible(true);

            Map<String, Object> cache = (Map<String, Object>) (snapshotMethod.invoke(realCacheClass));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> hi : cache.entrySet()) {
                Object cacheEntry = hi.getValue();
                Class cacheEntryKlass = cacheEntry.getClass();
                Field expf = cacheEntryKlass.getDeclaredField("expiryNanos");
                expf.setAccessible(true);
                long expires = (Long) expf.get(cacheEntry);
                Field af = cacheEntryKlass.getDeclaredField("value");
                af.setAccessible(true);
                InetAddress[] addresses = (InetAddress[]) af.get(cacheEntry);
                List<String> ads = new ArrayList<String>(addresses.length);
                for (InetAddress address : addresses) {
                    ads.add(address.getHostAddress());
                }

                sb.append(hi.getKey() + " ADDR:" + ads + "\n");
            }
            return sb.toString();
        }
        catch(Exception e){
        	logger.severe("logger in displaying cache "+e);
            e.printStackTrace();
            return "error:"+e.toString();
        }
    }

    private static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
        return new LinkedHashMap<K, V>(maxEntries * 10 / 7, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /*
     * Create a resolver.
     *   dnsServer == null: use system default server
     *   dnsServer != null: use `dnsServer`
     * */
    private Resolver createNewResolverBasedOnDnsServer(int timeout){
        try{
            Resolver resolver = null;
            if (dnsServer == null)
                resolver = new SimpleResolver();
            else
                resolver = new SimpleResolver(dnsServer);
            resolver.setTimeout(timeout);
            return resolver;
        }
        catch(Exception e){
            logger.severe("failed to initiate Resolver:"+e);
            e.printStackTrace();
            return null;
        }
    }

    synchronized private void  storeRRsetToSecondLevCache(Name rawHostName, RRset rrset){
        if (secondLevelCache.getSize() > maxSecondLevCacheEntry)
            secondLevelCache.clearCache();
        if(rrset.size() > 0){
            secondLevelCache.flushName(rawHostName);
            secondLevelCache.addRRset(rrset, SecondLevelCacheRecordCredibility);
        }
    }

    private class TimeoutExceptionItem{
        public long timestamp;
        public long delay;
        public String hostname;

        public TimeoutExceptionItem(long timestamp, long delay, String hostname){
            this.timestamp = timestamp;
            this.delay = delay;
            this.hostname = hostname;
        }
    }
    private class AsynLookupTask implements Runnable {
        private String hostname;
        private long delay;

        public AsynLookupTask(String hostname){
            this.hostname = hostname;
            delay = 0;
        }

        @Override
        public void run() {
            try {
                hostname = hostname.trim().toLowerCase();
                Lookup lookup = new Lookup(hostname, Type.A);
                lookup.setResolver(resolverWithLongTimeout);
                lookup.setCache(defaultCache);
                long dnsStartTimeout = System.currentTimeMillis();
                Record[] records = lookup.run();
                long dnsDelay = System.currentTimeMillis() - dnsStartTimeout;
                boolean isSucc = !(records==null);
                logger.info("  Asyncronous DNS lookup done in "+dnsDelay +" ms. Successful:"+isSucc);
                if(records==null)
                    return ;
                if (dnsDelay > dnsTimeout) {
                    listOperationExecutor.execute(
                    	new DelayItemListOperationTask(
                    		new TimeoutExceptionItem(System.currentTimeMillis(), dnsDelay, hostname),
                   			new TimeoutExceptionItem(System.currentTimeMillis(), dnsDelay, hostname)));
                }
                else{
                	 listOperationExecutor.execute(
                    	new DelayItemListOperationTask(
                     		new TimeoutExceptionItem(System.currentTimeMillis(), dnsDelay, hostname),
                    		null));
                }
                
                // Update the cache.
                ArrayList<InetAddress> rs = new ArrayList<InetAddress>(records.length);
                RRset rrset = new RRset();
                Name rawHostName = generateNameFromHost(hostname);
                for (Record record : records){
                    // create return value;
                    ARecord ar = (ARecord)record;
                    rs.add(ar.getAddress());

                    // update host2DNSName if rawHostName cannot generate real hostname
                    if(!record.getName().equals(rawHostName)){
                        if(host2DNSName.containsKey(rawHostName)){
                            host2DNSName.get(rawHostName).add(record.getName());
                        }
                        else{
                            Set<Name> newSet = new HashSet<Name>();
                            newSet.add(record.getName());
                            host2DNSName.put(rawHostName, newSet);
                        }
                    }
                    // update secondLevelCache
                    ARecord newARecord =  new ARecord(rawHostName, record.getDClass(),
                            userDefinedTTL, ((ARecord)record).getAddress());
                    rrset.addRR(newARecord);
                }
                storeRRsetToSecondLevCache(rawHostName, rrset);
            }
            catch(Exception e){
                logger.severe("error in doLookUp asynchronously: "+e);
                e.printStackTrace();
            }
        }
    }
    
    private class DelayItemListOperationTask implements Runnable {
        private TimeoutExceptionItem delayItem;
        private TimeoutExceptionItem longDelayItem;    
        public DelayItemListOperationTask(TimeoutExceptionItem delayItem, TimeoutExceptionItem longDelayItem){
            this.delayItem = delayItem;
            this.longDelayItem = longDelayItem;
        }

        @Override
        public void run() {
            try {
                addItemToDelayItems(delayItem, longDelayItem);
                adjustTimeout();
            }
            catch(Exception e){
                logger.severe("error in DelayItemListOperationTask: "+e);
                e.printStackTrace();
            }
        }
    }
    
    /* 1. The value of maxDnsServerCount can be zero, meaning that it depends
     * on the dnsserverlist.properties. 
     * 2. In any case, the dns server list should include "local".
     * 3. The value of maxHostnameCount can be zero, meaning that it depends 
     * on the `hostnames`.
     * 4. The parameter `hostnames` can be zero, in that case, the hostnames will
     * be loaded from the server. The size of the hostnames will be limited by `maxHostnameCount`. 
     * */
    public void startDNSServerMeasurement(int maxDnsServerCount, int maxHostnameCount, List<String> hostnames){
    	DNSServerMeasurementTask task = new DNSServerMeasurementTask(maxDnsServerCount, maxHostnameCount, hostnames);
    	executor.submit(task);
    }
    
    private class DNSServerMeasurementTask implements Runnable {
        private List<String> hostnames;
        private int maxDnsServerCount;
        private int maxHostnameCount;
  
        /* 1. The value of maxDnsServerCount can be zero, meaning that it depends
         * on the dnsserverlist.properties. 
         * 2. In any case, the dns server list should include "local".
         * 3. The value of maxHostnameCount can be zero, meaning that it depends 
         * on the `hostnames`.
         * 4. The parameter `hostnames` can be zero, in that case, the hostnames will
         * be loaded from the server. The size of the hostnames will be limited by `maxHostnameCount`. 
         * */
        public DNSServerMeasurementTask(int maxDnsServerCount, int maxHostnameCount, List<String> hostnames){
            this.hostnames = hostnames;
            if(maxHostnameCount <= 0)
            	this.maxHostnameCount = 10000;
            else
            	this.maxHostnameCount = maxHostnameCount;
            
            if(maxDnsServerCount<=0)
            	this.maxDnsServerCount = 10000;
            else
            	this.maxDnsServerCount = maxDnsServerCount;
        }

        @Override
        public void run() {
            try {
                if(this.hostnames == null)
                	this.hostnames = loadDNSServerTestingHostList();
                if(this.hostnames == null){
                	NetProphetLogger.logError("DNSServerMeasurementTask", "hostnames are null");
                	return ;
                }
                while(hostnames.size() > maxHostnameCount){
                	hostnames.remove(hostnames.size()-1);
                }
            	
                NetProphetPropertyManager manager = NetProphetPropertyManager.getInstance();
            	Map<String, DNSServer> dnsServerMap = manager.getDNSServerMap();
                while(dnsServerMap.size() > maxDnsServerCount){
                	Iterator<String> it = dnsServerMap.keySet().iterator();
                	String s = it.next();
                	if(s.equals("local")){
                		s = it.next();
                	}
                	dnsServerMap.remove(s);
                }
                NetProphetLogger.logDebugging("DNSServerMeasurementTask", 
                		"start DNS server measurement");
                findBestDNSServer( hostnames, dnsServerMap);
                NetProphetLogger.logDebugging("DNSServerMeasurementTask", 
                		"finish DNS server measurement");
                
            }
            catch(Exception e){
                logger.severe("error in DelayItemListOperationTask: "+e);
                e.printStackTrace();
            }
        }
    }

    //now this function has no use, but it demonstrates how to modify superclass's private field.
    private void modifyARecordTTL(ARecord record){
        try {
            Class recordClass = record.getClass().getSuperclass();
            Field cf = recordClass.getDeclaredField("ttl");
            cf.setAccessible(true);
            cf.set(record, userDefinedTTL);
        }
        catch(Exception e){
            logger.severe("error in ModifyARecordTTL");
            e.printStackTrace();
        }
    }
    
    /*TODO: First fetch URLs from database.
     * only failed, load testing host from server.
     * */
    private List<String> loadDNSServerTestingHostList(){
    	NetProphetPropertyManager manager = NetProphetPropertyManager.getInstance();
    	String testHostListURL = manager.getDNSServerTestingURLList();
    	OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder().url(testHostListURL).build();
		Call c = client.newCall(request);
		Response response;
		List<String> hosts = new LinkedList<String>();
		try {
			response = c.execute();
			String contents = response.body().string();
			Gson g = new Gson();
			String[] urls = g.fromJson(contents, String[].class);
			for(String u : urls){
				try{
					URL url = new URL(u);
					hosts.add(url.getHost());
					if (hosts.size() > DNSTestingHostNumber)
						break;
				}
				catch(Exception e){
					logger.warning("error in parsing url: "+u);
				}
			}
			return hosts;
		} catch (Exception e) {
			return null;
		}
		
    }
    
    private boolean storeDNSServerMeasureData(String filename, 
    		Map<String, Map<String, List<Integer>>> dnsDelay, 
    		Map<String, Map<String, MeasureResult>> pingDelay){
    	if(dnsDelay==null || pingDelay==null){
    		NetProphetLogger.logError("storeDNSServerMeasureData", "data is empty");
    		return false;
    	}
    	try{
    		NetProphetLogger.logDebugging("storeDNSServerMeasureData", "start logging measurement results");
	    	//open file.
    		NetProphet netprophet = NetProphet.getInstance();
    		File dnsOutFile, pingOutFile;
    		if( netprophet!= null){
    			dnsOutFile = new File(netprophet.getContext().getFilesDir(), filename+"_dns.txt");
    			pingOutFile = new File(netprophet.getContext().getFilesDir(), filename+"_ping.txt");
    		}
    		else{
    			dnsOutFile = new File(filename+"_dns.txt");
    			pingOutFile = new File(filename+"_ping.txt");
    		}
	    	PrintWriter dnsWriter = new PrintWriter(dnsOutFile );
	    	PrintWriter pingWriter = new PrintWriter(pingOutFile);
	    	
	    	Set<String> servers = dnsDelay.keySet();
	    	//local has to exist
	    	Map<String, List<Integer>> data = dnsDelay.get("local");
	    	Set<String> hosts = data.keySet();
	    	StringBuilder header = new StringBuilder();
	    	header.append("delay");
	    	for(String host : hosts)
	    		header.append(","+host);
	    	dnsWriter.println(header.toString());
	    	pingWriter.println(header.toString());
	    	
	    	for(String server : servers){
	    		Map<String, List<Integer>> serverData = dnsDelay.get(server);
	    		Map<String, MeasureResult> pingData = pingDelay.get(server);
	    		//NetProphetLogger.logDebugging("findBestDNSServer", "DNS server: "+server);
	    		StringBuilder dnsDelayString = new StringBuilder();
	    		StringBuilder pingDelayString = new StringBuilder();
	    		dnsDelayString.append(server);
	    		pingDelayString.append(server);
	    		for(String host : hosts){
	    			MeasureResult pingVal = pingData.get(host);
	    			List<Integer> tmp = serverData.get(host);	
	    			Collections.sort(tmp);
	    			long dnsMedVal = tmp.get(tmp.size()/2);		
	    			dnsDelayString.append(","+dnsMedVal);
	    			pingDelayString.append(","+pingVal.avgDelay);
	    			NetProphetLogger.logDebugging("storeDNSServerMeasureData", 
	    					"  "+host+" : "+dnsMedVal+'/'+pingVal.avgDelay);
	    		}
	    		dnsWriter.println(dnsDelayString.toString());
		    	pingWriter.println(pingDelayString.toString());
	    	}
	    	
	    	dnsWriter.close();
	    	pingWriter.close();
	    	NetProphetLogger.logDebugging("storeDNSServerMeasureData", "finish logging measurement results");
    	}
    	catch(Exception e){
    		NetProphetLogger.logError("storeDNSServerMeasureData", e.toString());
    		e.printStackTrace();
    		return false;
    	}
    	
    	return true;
    }
    
    private void findBestDNSServer(List<String> hostnames, Map<String, DNSServer> dnsServerMap){
    	if(hostnames==null) {
    		NetProphetLogger.logError("findBestDNSServer", "hostnames are empty");
    		return;
    	}

    	Map<String, Map<String, List<Integer>>> results = new HashMap<String, Map<String, List<Integer>>>();
    	Map<String, Map<String, MeasureResult>> hostPingResults = new HashMap<String, Map<String, MeasureResult>>();
    	PingTool pingTool = new PingTool();
    	try{
	    	for(int i=0; i<3; i++){
	    		NetProphetLogger.logDebugging("findBestDNSServer", "start the "+i+" round");
	    		for(DNSServer server : dnsServerMap.values()){
	    			NetProphetLogger.logDebugging("findBestDNSServer", "  start testing :"+server);		
	    			Map<String, List<Integer>> serverData = results.get(server.name);
	    			Map<String, MeasureResult> pingData = hostPingResults.get(server.name);
	    			if (serverData == null){
	    				serverData = new HashMap<String, List<Integer>>();
	    				results.put(server.name, serverData);
	    			}
	    			if(pingData == null){
	    				pingData = new HashMap<String, MeasureResult>();
	    				hostPingResults.put(server.name, pingData);
	    			}
	    			
		    		for(String hostname : hostnames){
				        Lookup lookup = new Lookup(hostname, Type.A);
				        Resolver res = null;
				        if(hostname != "local")
				        	res = new SimpleResolver();
				        else
				        	res = new SimpleResolver(server.IP);
				        res.setTimeout(5);
				        lookup.setResolver(res);
				        lookup.setCache(null);
				        long dnsStartTimeout = System.currentTimeMillis();
				        Record[] records = lookup.run();
				        long dnsDelay = System.currentTimeMillis() - dnsStartTimeout;
				        boolean isSucc = !(records == null);
				        if (!isSucc) dnsDelay = 10000;
				        NetProphetLogger.logDebugging("findBestDNSServer",
				        		"    done DNS lookup "+hostname+" in "+dnsDelay +" ms" +" isSucc:"+isSucc);
				        if(records!=null && records.length >0 && pingData.get(hostname)==null){
				        	ARecord ar = (ARecord)records[0];
				        	MeasureResult pingRS = pingTool.doPing(ar.getAddress().getHostAddress());
				        	NetProphetLogger.logDebugging("findBestDNSServer", 
				        			"    done ping testing: host:"+hostname+
				        			" ip:"+pingRS.ip +
				        			" delay:"+pingRS.avgDelay);
				        	pingData.put(hostname, pingRS);
				        }
				        else if((records==null || records.length==0) && 
				        		pingData.get(hostname)==null){
				        	NetProphetLogger.logDebugging("findBestDNSServer", 
				        			"    done ping testing: host:"+hostname+
				        			" delay MAXIMUM");
				        	pingData.put(hostname, pingTool.getFailedMeasureResult(hostname));
				        }
				        
				        if(serverData.containsKey(hostname))
				        	serverData.get(hostname).add((int)dnsDelay);
				        else{
				        	List<Integer> l = new LinkedList<Integer>();
				        	l.add((int)dnsDelay);
				        	serverData.put(hostname, l);
				        }
		    		}
	    		}
	    		NetProphetLogger.logDebugging("findBestDNSServer", "finish the "+i+" round");
	    	}
	    	storeDNSServerMeasureData("DNSMeasurement", results, hostPingResults);
	    	/*
	    	Set<String> servers = results.keySet();
	    	for(String server : servers) {   		
	    		Map<String, List<Integer>> serverData = results.get(server);
	    		Map<String, MeasureResult> pingData = hostPingResults.get(server);
	    		Set<String> hosts = serverData.keySet();
	    		long val = 0;
	    		StringBuilder sb = new StringBuilder();
	    		NetProphetLogger.logDebugging("findBestDNSServer", "DNS server: "+server);
	    		for(String host : hosts){
	    			MeasureResult pingVal = pingData.get(host);
	    			List<Integer> data = serverData.get(host);
	    			
	    			Collections.sort(data);
	    			long dnsMedVal = data.get(data.size()/2);		
	    			NetProphetLogger.logDebugging("findBestDNSServer", 
	    					"  "+host+" : "+dnsMedVal+'/'+pingVal.avgDelay);
	    		}
	    	}*/
    	}
    	catch(Exception e){
    		logger.severe("errpr in findBestDNSServer: "+e);
    		e.printStackTrace();
    	}
    }
}
