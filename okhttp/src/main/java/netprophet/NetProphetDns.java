package netprophet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.Dns;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

/**
 * Created by xpan on 3/2/16.
 */
public class NetProphetDns implements Dns {
    final static private int maxSecondLevCacheEntry = 3000;
    final static private int SecondLevelCacheRecordCredibility = 5;
    final static private int hardDNSTimeout = 20; //20s
    final static private int DefaultTimeout = 1;
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
        List<InetAddress> cachedRecordList = new ArrayList<InetAddress>();
        StringBuilder errorMsg = new StringBuilder();
        if(searchSystemDNSCache(hostname,cachedRecordList, errorMsg)){
            System.err.println(hostname+" found record in system cache!");
            return cachedRecordList;
        }
        InetAddress cachedRecord = searchCache(defaultCache, hostname);
        if (cachedRecord != null){
            cachedRecordList.add(cachedRecord);
            System.err.println(hostname+" found record in default cache!");
            //System.err.flush();
            return cachedRecordList;
        }
        if (enableSecondLevelCache){
            cachedRecord = searchCache(secondLevelCache, hostname);
            if (cachedRecord != null){
                cachedRecordList.add(cachedRecord);
                System.err.println(hostname+" found record in second level cache!");
                executor.execute(new AsynLookupTask(hostname));
                return cachedRecordList;
            }
        }
        System.err.println("defaultCache Entry:"+defaultCache.getSize() +" secCacheEntry:"+secondLevelCache.getSize());
        System.err.println("do synchronous lookup!  "+hostname+ " "+enableSecondLevelCache);
        //System.err.flush();
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
    private int dnsDefaultTimeout;
    LinkedList<TimeoutExceptionItem> longTimeoutItems;
    private String dnsServer;
    private Resolver resolver, resolverWithLongTimeout ;
    private Map<Name, Set<Name>> host2DNSName;
    private boolean enableSecondLevelCache;
    ExecutorService executor = Executors.newFixedThreadPool(5);

    /*
     * If dns failed because of timeout, start an asynchronous dns
     *   lookup task. If succeeded, add one longTimeoutItems object
     *   in `longTimeoutItems`.
     *
     * */
    public void setDnsDefaultTimeout(int dnsDefaultTimeout) {
        this.dnsDefaultTimeout = dnsDefaultTimeout;
    }
    /*
     * TODO: this function is not fully implemented.
     *   Called every time when either of following changed:
     *    1. longTimeoutItems.
     *    2. dnsDefaultTimeout.
     *  */
    private void adjustTimeout(){
        if (longTimeoutItems.size() > 5) {
            long largestDelay = 0, smalledstDelay = hardDNSTimeout;
            for (TimeoutExceptionItem item : longTimeoutItems){
                if(item.delay > largestDelay)
                    largestDelay = item.delay;
                if(item.delay < smalledstDelay)
                    smalledstDelay = item.delay;
            }
            double delay = (largestDelay - smalledstDelay)*0.8 + smalledstDelay;
            int newDnsDefaultTimeout = (int)((delay+500)/1000);
            if (newDnsDefaultTimeout > hardDNSTimeout)
                newDnsDefaultTimeout = hardDNSTimeout;
            System.err.println("DNSTimeout has been updated from "+
                    dnsDefaultTimeout+" to "+newDnsDefaultTimeout);
            resolver.setTimeout(newDnsDefaultTimeout);
            longTimeoutItems.clear();
        }
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
        dnsDefaultTimeout = DefaultTimeout; //default: 10 s
        userDefinedTTL = 60 * 60; //default: 1 hour
        resolver = createNewResolverBasedOnDnsServer(dnsDefaultTimeout);
        resolverWithLongTimeout = createNewResolverBasedOnDnsServer(hardDNSTimeout);
        host2DNSName = createLRUMap(100);

        enableSecondLevelCache = EnableSecondLevelCache;
        longTimeoutItems = new LinkedList<TimeoutExceptionItem>();
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
            e.printStackTrace();
        }
        return null;
    }
    /*
    * Search the System Cache.
    */
    public boolean searchSystemDNSCache(String hostname,
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
                    //System.err.println("start");
                    addresses = (InetAddress[]) af.get(cacheEntry);
                    //System.err.println(addresses);
                }
                catch(java.lang.ClassCastException e){
                    errorMsg.append(((String)af.get(cacheEntry))+"error:"+e);
                    //System.err.println(af.get(cacheEntry)+"error:"+e);
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
            System.out.println("done DNS lookup in "+dnsDelay +" ms");

            //Cannot find the hostname, returns directly.
            if (records == null){
                if(dnsDelay > dnsDefaultTimeout){
                    //error caused by timeout
                    //start an asynchronous dns lookup.
                    executor.execute(new AsynLookupTask(hostname));
                }
                return null;
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
                System.out.println(
                        String.format("Name:%s IP:%s TTL:%d\n",
                            record.getName(),
                            ((ARecord) record).getAddress().toString(),
                            ((ARecord) record).getTTL())); 
            }
            storeRRsetToSecondLevCache(rawHostName, rrset);

            return rs;
        }
        catch(Exception e){
            System.err.println("error in doLookUp: "+e);
            e.printStackTrace();
            return null;
        }
    }


    private Name generateNameFromHost(String host){
        try {
            return Name.fromString(host + '.');
        }
        catch(Exception e){
            System.err.println("error in generateNameFromHost: "+e);
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
            System.err.println(e);
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
            System.err.println("failed to initiate Resolver:"+e);
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
    public class AsynLookupTask implements Runnable {
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
                System.out.println("ASYN done DNS lookup in "+dnsDelay +" ms "+isSucc);
                if(records==null)
                    return ;
                if (dnsDelay > dnsDefaultTimeout) {
                    longTimeoutItems.add(new TimeoutExceptionItem(
                            System.currentTimeMillis(), dnsDelay, hostname));
                    adjustTimeout();
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
                    System.out.println(
                            String.format("Name:%s IP:%s TTL:%d\n",
                                    record.getName(),
                                    ((ARecord) record).getAddress().toString(),
                                    ((ARecord) record).getTTL()));
                }
                storeRRsetToSecondLevCache(rawHostName, rrset);
            }
            catch(Exception e){
                System.err.println("error in doLookUp asynchronously: "+e);
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
            System.err.println("error in ModifyARecordTTL");
            e.printStackTrace();
        }
    }

}
