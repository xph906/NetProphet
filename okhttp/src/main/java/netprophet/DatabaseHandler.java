package netprophet;


import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Internal.NetProphetLogger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static okhttp3.internal.Internal.logger;
/**
 * Created by dell on 2016/1/26.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "NetProphet.db";

    private static ReentrantLock db_lock = new ReentrantLock();
    
    protected ReentrantLock send_lock;
    protected ReentrantLock tagSetLock;
    
    private boolean isSynchronizing;
	private boolean isSyncSuccessful; //check if all requests successful.
    private boolean isPreparingRequest; //ensure to dump table only when all requests have been prepared.
    private HashSet<Integer> postTags;
    private long syncStartingTime;
    private boolean isSyncThreadRunning;
    private Context context;
    public boolean isSyncThreadRunning() {
		return isSyncThreadRunning;
	}

	private NetProphetPropertyManager propertyManager;
    
    private static DatabaseHandler instance = null;
    public static DatabaseHandler getInstance(Context context) {
        if(instance == null) {
            instance = new DatabaseHandler(context);
        }
        return instance;
    }

    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        send_lock = new ReentrantLock();
        tagSetLock = new ReentrantLock();
        postTags = null;
        isSynchronizing = false;
        isSyncSuccessful = true;
        isPreparingRequest = false;
        isSyncThreadRunning = false;
        syncStartingTime = 0;
        propertyManager = NetProphetPropertyManager.getInstance();
        try{	
	        if(!isTableExists(NetProphetData.RequestColumns.TABLE_NAME)){
	        	NetProphetLogger.logDebugging("DatabaseHandler", 
	        			NetProphetData.RequestColumns.TABLE_NAME + " table not exists");
	        	SQLiteDatabase db = this.getWritableDatabase();
	        	db.execSQL(createTable(NetProphetData.RequestColumns.TABLE_NAME, NetProphetData.RequestColumns.COLUMNS));
	        	db.close();
	        }
	        if(!isTableExists(NetProphetData.NetInfoColumns.TABLE_NAME)){
	        	NetProphetLogger.logDebugging("DatabaseHandler", 
	        			NetProphetData.NetInfoColumns.TABLE_NAME + " table not exists");
	        	SQLiteDatabase db = this.getWritableDatabase();
	        	db.execSQL(createTable(NetProphetData.NetInfoColumns.TABLE_NAME, NetProphetData.NetInfoColumns.COLUMNS));
	        	db.close();
	        }
	    }
        catch(Exception e){
        	NetProphetLogger.logError("DatabaseHandler", e.toString());
        	e.printStackTrace();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTable(NetProphetData.RequestColumns.TABLE_NAME, NetProphetData.RequestColumns.COLUMNS));
        db.execSQL(createTable(NetProphetData.NetInfoColumns.TABLE_NAME, NetProphetData.NetInfoColumns.COLUMNS));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }   
    private boolean isTableExists(String tbName){
    	boolean tableExists = false;
    	try{
    		SQLiteDatabase mDatabase = this.getReadableDatabase();
        	Cursor c = null;
        	/* get cursor on it */
        	try
        	{
        	    c = mDatabase.query(tbName, null,
        	        null, null, null, null, null);
        	    tableExists = true;
        	}
        	catch (Exception e) {
        	    /* fail */
        		NetProphetLogger.logError("isTableExists 1", e.toString());
        		e.printStackTrace();
        	}
    	}
    	catch(Exception e){
    		NetProphetLogger.logError("isTableExists 2", e.toString());
    		e.printStackTrace();
    	}
    	

    	return tableExists;
    }
    
    public boolean isSynchronizing() {
		return isSynchronizing;
	}

    private String createTable(String tableName, String[][] columns) {
        if (tableName == null || columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Invalid parameters for creating table " + tableName);
        } else {
            StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");

            stringBuilder.append(tableName);
            stringBuilder.append(" (");
            for (int n = 0, i = columns.length; n < i; n++) {
                if (n > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(columns[n][0]).append(' ').append(columns[n][1]);
            }
            return stringBuilder.append(");").toString();
        }
    }

    private void sendObjectsToRemoteDB(List objList){
    	if (objList.size() <= 0)
    		return ;
    	AsyncTaskManager taskManager = AsyncTaskManager.getInstance();
    	NetProphetPropertyManager propertyManager = NetProphetPropertyManager.getInstance();
    	Gson gson = new GsonBuilder().create();
		Vector arr = new Vector();
		arr.addAll(objList);
		String objStr = gson.toJson(arr);
		System.err.println("GSONSTRING: "+objStr);
		//DEBUG
		File file  = new File(context.getFilesDir(), "debug_json_str.txt");
		try {
			PrintWriter writter = new PrintWriter(new FileOutputStream(file, true));
			writter.println(objStr);
			writter.close();
		} catch (FileNotFoundException e) {
			NetProphetLogger.logError("sendObjectsToRemoteDB", e.toString());
			e.printStackTrace();
		}	
		//END DEBUG
		
		//addPostTag(objStr.hashCode());
		taskManager.postTask(new PostCompressedCallInfoTask(objStr, propertyManager.getRemotePostReportURL(),this));
		NetProphetLogger.logDebugging("sendObjectsToRemoteDB",
				"done sending "+objList.size()+" records to remote server");
    }
    
    public void setSyncSuccessfulTag(boolean tag){
    	this.isSyncSuccessful = tag;
    }
    
    protected void addPostTag(int hashcode) {
        tagSetLock.lock();
        if(postTags == null)
        	postTags = new HashSet<Integer>();
        postTags.add(hashcode);
        tagSetLock.unlock();
	}

    //BUG: if there are ten requests, deletePostTag(-1) is called after the first five
    //     finish calling deletePostTag(hashcode), while the rest five haven't called
    //     addPostTag(hashcode), the database will be cleared without confirming that 
    //     the rest five's records have been received.
    //     For now, we will let it go...
    protected void deletePostTag(int hashcode){
        boolean isPostTagSetEmpty = false; 
        int size = 0;
        tagSetLock.lock();
        if(postTags == null){
        	tagSetLock.unlock();
        	return ;
        }
        postTags.remove(hashcode);
        size = postTags.size();
        isPostTagSetEmpty = postTags.isEmpty();
        tagSetLock.unlock();
        
        if(isPostTagSetEmpty){
        	if(!isPreparingRequest && isSyncSuccessful){
        		clearDatabase(); 
        		NetProphetLogger.logDebugging("deletePostTag", 
        				"done synchronizing database and clearing tables: ");
        	}
        	else if(!isPreparingRequest){	
        		NetProphetLogger.logWarning("deletePostTag", 
        				"done synchronizing database but cannot clear database because some sync requests failed");
        	}
        	else{
        		NetProphetLogger.logWarning("deletePostTag", 
        				"done synchronizing database but cannot clear database because not all sync requests are prepared");
        	}
        	if(!isPreparingRequest){
        		isSynchronizing = false;
        		syncStartingTime = 0;
        		tagSetLock.lock();
        		postTags = null;
        		tagSetLock.unlock();
        	}
        }
        else{
        	NetProphetLogger.logWarning("deletePostTag", 
    				"done synchronizing a part of DB, "+size+" tasks remaining.");
        }
    }
    
    public void clearDatabase(){
    	db_lock.lock();
    	try{
    		SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE HTTPRequestInfo");
            db.execSQL("DROP TABLE NetworkInfo");

            db.execSQL(createTable(NetProphetData.RequestColumns.TABLE_NAME, NetProphetData.RequestColumns.COLUMNS));
            db.execSQL(createTable(NetProphetData.NetInfoColumns.TABLE_NAME, NetProphetData.NetInfoColumns.COLUMNS));
    	}catch(Exception e){
    		NetProphetLogger.logError("clearDatabase", e.toString());
            e.printStackTrace();
    	}finally{
    		db_lock.unlock();
    	}
    }
    
    public void initEverything(){
    	clearDatabase();
    	isSynchronizing = false;
    	syncStartingTime = 0;
    	isSyncSuccessful = true;
    	isPreparingRequest = false;
    	tagSetLock.lock();
    	postTags = null;
    	tagSetLock.unlock();
    }
    
    //TODO: this function can be removed in release version
    public String getDBSyncData(){
    	tagSetLock.lock();
    	int size = 0;
    	if(postTags != null)
    		size = postTags.size();
    	tagSetLock.unlock();
    	return String.format("isSynchronizing:%b  isSyncSuccessful:%b  isPreparingReq:%b  sizeofPostTags:%d", 
    			isSynchronizing, isSyncSuccessful, isPreparingRequest, size);
    }

    //This method will always be executed in another thread
    protected boolean synchronizeDatabase(){
    	//change this part of codes, 
    	//each time, read at most 1000 records into memory and sent to server
    	if(isSynchronizing == true){
    		long t = (System.currentTimeMillis() - syncStartingTime)/1000;
    		NetProphetLogger.logError("synchronizeDatabase", 
    				"the database is synchronizing now and has lasted for "+t+" seconds");
    		return false;
    	}
    	db_lock.lock();
    		
        try {
            String selectQuery = "SELECT * FROM " + NetProphetData.RequestColumns.TABLE_NAME;
            String selectQuery2 = "SELECT * FROM " + NetProphetData.NetInfoColumns.TABLE_NAME;
            SQLiteDatabase db = this.getReadableDatabase();
            //TODO: is transaction necessary for read operation?
            //db.beginTransaction();
            try {
                Cursor cursor = db.rawQuery(selectQuery, null);
                Cursor cursor2 = db.rawQuery(selectQuery2, null);
                isSynchronizing = true; 
                syncStartingTime = System.currentTimeMillis();
                //db.setTransactionSuccessful();
                try{
                	isSyncSuccessful = true;
                	isPreparingRequest = true;
                	int packetSize = propertyManager.getDBSyncPacketRecordSize();
                	if(cursor.moveToFirst()) {
                		while (!cursor.isAfterLast()) {
                			List<NetProphetHTTPRequestInfoObject> requestList = new ArrayList<NetProphetHTTPRequestInfoObject>();
                			do {
                				NetProphetHTTPRequestInfoObject infoObject = new NetProphetHTTPRequestInfoObject(
                                    cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                                    cursor.getLong(4), cursor.getLong(5), cursor.getLong(6), cursor.getLong(7),
                                    cursor.getLong(8), cursor.getLong(9), cursor.getLong(10), cursor.getLong(11),
                                    cursor.getLong(12), cursor.getLong(13), cursor.getLong(14), cursor.getLong(15),
                                    cursor.getLong(16), cursor.getInt(17) > 0, cursor.getInt(18) > 0, cursor.getInt(19) > 0,
                                    cursor.getLong(20), cursor.getInt(21), cursor.getInt(22), cursor.getString(28), cursor.getInt(23) > 0,
                                    cursor.getString(24), cursor.getString(25), cursor.getLong(26), cursor.getInt(27));
                				requestList.add(infoObject);
                			} while (cursor.moveToNext() && requestList.size() < packetSize);
                			//send  request;
                			sendObjectsToRemoteDB(requestList);
                		}
                	}
                	if (cursor2.moveToFirst()) {
                		while (!cursor2.isAfterLast()) {
                			List<NetProphetNetworkData> netInfoList = new ArrayList<NetProphetNetworkData>();
                			do {
                                NetProphetNetworkData netInfoObject = new NetProphetNetworkData(cursor2.getLong(0),
                                        cursor2.getString(1),cursor2.getString(2),cursor2.getInt(3),cursor2.getInt(4),
                                        cursor2.getInt(5),cursor2.getInt(6),cursor2.getInt(7),cursor2.getInt(8),cursor2.getInt(9));
                				netInfoList.add(netInfoObject);
                			} while (cursor2.moveToNext() && netInfoList.size() < packetSize);
                			//send  request;
                			sendObjectsToRemoteDB(netInfoList);
                		}
                    }
                	isPreparingRequest = false;
                	// prevent all sync tasks finish before isPreparingRequest setting to be true; 
                	deletePostTag(-1); 
                }
                catch(Exception e){
                	isPreparingRequest = false;
                	isSynchronizing = false; //if an error occurs, we consider synchronizing failed.
                	deletePostTag(-1);
                	NetProphetLogger.logError("synchronizeDatabase",e.toString());
                	e.printStackTrace();
                }

            } catch (Exception e) {
            	NetProphetLogger.logError("synchronizeDatabase",e.toString());
                e.printStackTrace();          
            } finally {
                //db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
            isPreparingRequest = false;
        }
    	//then drop the table and creates new table
    	//only when it's confirmed that the server has received the data.
    	return true;
    }
    
    /* NetProphetHTTPRequestInfoObject Database Interface */
    public void addRequestInfo(NetProphetHTTPRequestInfoObject infoObject) {
        db_lock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                checkRequestInfo(infoObject);

                values.put(NetProphetData.RequestColumns.REQUEST_ID, infoObject.getReqID());
                values.put(NetProphetData.RequestColumns.URL, infoObject.getUrl());
                values.put(NetProphetData.RequestColumns.METHOD, infoObject.getMethod());
                values.put(NetProphetData.RequestColumns.USER_ID, infoObject.getUserID());

                values.put(NetProphetData.RequestColumns.PREV_REQ_ID, infoObject.getPrevReqID());
                values.put(NetProphetData.RequestColumns.NEXT_REQ_ID, infoObject.getNextReqID());

                values.put(NetProphetData.RequestColumns.START_TIME, infoObject.getStartTime());
                values.put(NetProphetData.RequestColumns.END_TIME, infoObject.getEndTime());
                values.put(NetProphetData.RequestColumns.OVERALL_DELAY, infoObject.getOverallDelay());
                values.put(NetProphetData.RequestColumns.DNS_DELAY, infoObject.getDnsDelay());
                values.put(NetProphetData.RequestColumns.CONN_DELAY, infoObject.getConnDelay());
                values.put(NetProphetData.RequestColumns.HANDSHAKE_DELAY, infoObject.getHandshakeDelay());
                values.put(NetProphetData.RequestColumns.TLS_DELAY, infoObject.getTlsDelay());
                values.put(NetProphetData.RequestColumns.REQ_WRITE_DELAY, infoObject.getReqWriteDelay());
                values.put(NetProphetData.RequestColumns.SERVER_DELAY, infoObject.getServerDelay());
                values.put(NetProphetData.RequestColumns.TTFB_DELAY, infoObject.getTTFBDelay());
                values.put(NetProphetData.RequestColumns.RESP_TRANS_DELAY, infoObject.getRespTransDelay());
                
                values.put(NetProphetData.RequestColumns.RESP_TYPE, infoObject.getRespType());

                if (infoObject.isUseConnCache()) {
                    values.put(NetProphetData.RequestColumns.USE_CONN_CACHE, 1);
                } else {
                    values.put(NetProphetData.RequestColumns.USE_CONN_CACHE, 0);
                }
                if (infoObject.isUseDNSCache()) {
                    values.put(NetProphetData.RequestColumns.USE_DNS_CACHE, 1);
                } else {
                    values.put(NetProphetData.RequestColumns.USE_DNS_CACHE, 0);
                }
                if (infoObject.isUseRespCache()) {
                    values.put(NetProphetData.RequestColumns.USE_RESP_CACHE, 1);
                } else {
                    values.put(NetProphetData.RequestColumns.USE_RESP_CACHE, 0);
                }

                values.put(NetProphetData.RequestColumns.RESP_SIZE, infoObject.getRespSize());
                values.put(NetProphetData.RequestColumns.HTTP_CODE, infoObject.getHTTPCode());
                values.put(NetProphetData.RequestColumns.REQ_SIZE, infoObject.getReqSize());

                if (infoObject.isFailedRequest()) {
                    values.put(NetProphetData.RequestColumns.IS_FAILED_REQUEST, 1);
                } else {
                    values.put(NetProphetData.RequestColumns.IS_FAILED_REQUEST, 0);
                }
                values.put(NetProphetData.RequestColumns.ERROR_MSG, infoObject.getErrorMsg());
                values.put(NetProphetData.RequestColumns.DETAILED_ERROR_MSG, infoObject.getDetailedErrorMsg());
                values.put(NetProphetData.RequestColumns.TRANS_ID, infoObject.getTransID());
                values.put(NetProphetData.RequestColumns.TRANS_TYPE, infoObject.getTransType());

                db.insert(NetProphetData.RequestColumns.TABLE_NAME, null, values);

                db.setTransactionSuccessful();
            } catch (Exception e) {
            	NetProphetLogger.logError("addRequestInfo",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
        }
    }

    public void addRequestInfos(List<NetProphetHTTPRequestInfoObject> objList)
    {
        db_lock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                Iterator<NetProphetHTTPRequestInfoObject> objIter = objList.iterator();
                while(objIter.hasNext()) {
                    NetProphetHTTPRequestInfoObject infoObject = objIter.next();
                    ContentValues values = new ContentValues();
                    checkRequestInfo(infoObject);

                    values.put(NetProphetData.RequestColumns.REQUEST_ID, infoObject.getReqID());
                    values.put(NetProphetData.RequestColumns.URL, infoObject.getUrl());
                    values.put(NetProphetData.RequestColumns.METHOD, infoObject.getMethod());
                    values.put(NetProphetData.RequestColumns.USER_ID, infoObject.getUserID());

                    values.put(NetProphetData.RequestColumns.PREV_REQ_ID, infoObject.getPrevReqID());
                    values.put(NetProphetData.RequestColumns.NEXT_REQ_ID, infoObject.getNextReqID());

                    values.put(NetProphetData.RequestColumns.START_TIME, infoObject.getStartTime());
                    values.put(NetProphetData.RequestColumns.END_TIME, infoObject.getEndTime());
                    values.put(NetProphetData.RequestColumns.OVERALL_DELAY, infoObject.getOverallDelay());
                    values.put(NetProphetData.RequestColumns.DNS_DELAY, infoObject.getDnsDelay());
                    values.put(NetProphetData.RequestColumns.CONN_DELAY, infoObject.getConnDelay());
                    values.put(NetProphetData.RequestColumns.HANDSHAKE_DELAY, infoObject.getHandshakeDelay());
                    values.put(NetProphetData.RequestColumns.TLS_DELAY, infoObject.getTlsDelay());
                    values.put(NetProphetData.RequestColumns.REQ_WRITE_DELAY, infoObject.getReqWriteDelay());
                    values.put(NetProphetData.RequestColumns.SERVER_DELAY, infoObject.getServerDelay());
                    values.put(NetProphetData.RequestColumns.TTFB_DELAY, infoObject.getTTFBDelay());
                    values.put(NetProphetData.RequestColumns.RESP_TRANS_DELAY, infoObject.getRespTransDelay());
                    values.put(NetProphetData.RequestColumns.RESP_TYPE, infoObject.getRespType());

                    if (infoObject.isUseConnCache()) {
                        values.put(NetProphetData.RequestColumns.USE_CONN_CACHE, 1);
                    } else {
                        values.put(NetProphetData.RequestColumns.USE_CONN_CACHE, 0);
                    }
                    if (infoObject.isUseDNSCache()) {
                        values.put(NetProphetData.RequestColumns.USE_DNS_CACHE, 1);
                    } else {
                        values.put(NetProphetData.RequestColumns.USE_DNS_CACHE, 0);
                    }
                    if (infoObject.isUseRespCache()) {
                        values.put(NetProphetData.RequestColumns.USE_RESP_CACHE, 1);
                    } else {
                        values.put(NetProphetData.RequestColumns.USE_RESP_CACHE, 0);
                    }

                    values.put(NetProphetData.RequestColumns.RESP_SIZE, infoObject.getRespSize());
                    values.put(NetProphetData.RequestColumns.HTTP_CODE, infoObject.getHTTPCode());
                    values.put(NetProphetData.RequestColumns.REQ_SIZE, infoObject.getReqSize());

                    if (infoObject.isFailedRequest()) {
                        values.put(NetProphetData.RequestColumns.IS_FAILED_REQUEST, 1);
                    } else {
                        values.put(NetProphetData.RequestColumns.IS_FAILED_REQUEST, 0);
                    }
                    values.put(NetProphetData.RequestColumns.ERROR_MSG, infoObject.getErrorMsg());
                    values.put(NetProphetData.RequestColumns.DETAILED_ERROR_MSG, infoObject.getDetailedErrorMsg());
                    values.put(NetProphetData.RequestColumns.TRANS_ID, infoObject.getTransID());
                    values.put(NetProphetData.RequestColumns.TRANS_TYPE, infoObject.getTransType());

                    db.insert(NetProphetData.RequestColumns.TABLE_NAME, null, values);
                }

                db.setTransactionSuccessful();
            } catch (Exception e) {
            	NetProphetLogger.logError("addRequestInfos",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
        }
    }

    public void deleteRequestInfo(long req_id) {
        db_lock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete(NetProphetData.RequestColumns.TABLE_NAME, NetProphetData.RequestColumns.REQUEST_ID + " = ?",
                        new String[]{String.valueOf(req_id)});
                db.setTransactionSuccessful();
            } catch (Exception e) {
            	NetProphetLogger.logError("deleteRequestInfo",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
        }
    }

    @SuppressWarnings("finally")
	public long getRequestInfoCount()
    {
        db_lock.lock();
        long count = 0;
        try{
            SQLiteDatabase db = this.getReadableDatabase();
            db.beginTransaction();
            try{
                String selectQuery = "SELECT count(*) FROM " + NetProphetData.RequestColumns.TABLE_NAME;
                Cursor cursor = db.rawQuery(selectQuery, null);
                if(cursor.moveToFirst())
                {
                    count = cursor.getLong(0);
                }
            }catch (Exception e)
            {
            	NetProphetLogger.logError("getRequestInfoCount",e.toString());
                e.printStackTrace();
            }finally {
                db.endTransaction();
                db.close();
            }
        }finally {
            db_lock.unlock();
            return count;
        }
    }
    
    @SuppressWarnings("finally")
	private List<NetProphetHTTPRequestInfoObject> getAllRequestInfo() {
        db_lock.lock();
        List<NetProphetHTTPRequestInfoObject> requestList = new ArrayList<NetProphetHTTPRequestInfoObject>();
        try {
            String selectQuery = "SELECT * FROM " + NetProphetData.RequestColumns.TABLE_NAME;
            SQLiteDatabase db = this.getReadableDatabase();
            db.beginTransaction();
            try {
                Cursor cursor = db.rawQuery(selectQuery, null);
                db.setTransactionSuccessful();
                if (cursor.moveToFirst()) {
                    do {
                        NetProphetHTTPRequestInfoObject infoObject = new NetProphetHTTPRequestInfoObject(
                                cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                                cursor.getLong(4), cursor.getLong(5), cursor.getLong(6), cursor.getLong(7),
                                cursor.getLong(8), cursor.getLong(9), cursor.getLong(10), cursor.getLong(11),
                                cursor.getLong(12), cursor.getLong(13), cursor.getLong(14), cursor.getLong(15),
                                cursor.getLong(16), cursor.getInt(17) > 0, cursor.getInt(18) > 0, cursor.getInt(19) > 0,
                                cursor.getLong(20), cursor.getInt(21), cursor.getInt(22), cursor.getString(28), cursor.getInt(23) > 0,
                                cursor.getString(24), cursor.getString(25), cursor.getLong(26), cursor.getInt(27));

                        requestList.add(infoObject);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
            	NetProphetLogger.logError("getAllRequestInfo",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
            return requestList;
        }
    }
    private List<NetProphetHTTPRequestInfoObject> getAllRequestInfoAndDelete()
    {
        db_lock.lock();
        List<NetProphetHTTPRequestInfoObject> requestList = new ArrayList<NetProphetHTTPRequestInfoObject>();
        try {
            String selectQuery = "SELECT * FROM " + NetProphetData.RequestColumns.TABLE_NAME;
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                Cursor cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) {
                    do {
                        db.delete(NetProphetData.RequestColumns.TABLE_NAME, NetProphetData.RequestColumns.REQUEST_ID + " = ?",
                                new String[]{String.valueOf(cursor.getLong(0))});
                        NetProphetHTTPRequestInfoObject infoObject = new NetProphetHTTPRequestInfoObject(
                                cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                                cursor.getLong(4), cursor.getLong(5), cursor.getLong(6), cursor.getLong(7),
                                cursor.getLong(8), cursor.getLong(9), cursor.getLong(10), cursor.getLong(11),
                                cursor.getLong(12), cursor.getLong(13), cursor.getLong(14), cursor.getLong(15),
                                cursor.getLong(16), cursor.getInt(17) > 0, cursor.getInt(18) > 0, cursor.getInt(19) > 0,
                                cursor.getLong(20), cursor.getInt(21), cursor.getInt(22), cursor.getString(28), cursor.getInt(23) > 0,
                                cursor.getString(24), cursor.getString(25), cursor.getLong(26), cursor.getInt(27));

                        requestList.add(infoObject);
                    } while (cursor.moveToNext());
                    db.setTransactionSuccessful();
                }
            } catch (Exception e) {
            	NetProphetLogger.logError("getAllRequestInfoAndDelete",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        }finally {
            db_lock.unlock();
            return requestList;
        }

    }

    private void checkRequestInfo(NetProphetHTTPRequestInfoObject infoObject)
    {
        if(infoObject.getUrl() == null)
        {
            infoObject.setUrl("");
        }
        if(infoObject.getMethod() == null)
        {
            infoObject.setMethod("");
        }
        if(infoObject.getUserID() == null)
        {
            infoObject.setUserID("");
        }
        if(infoObject.getErrorMsg() == null)
        {
            infoObject.setErrorMsg("");
        }
        if(infoObject.getDetailedErrorMsg() == null)
        {
            infoObject.setDetailedErrorMsg("");
        }


    }

    /* NetProphetNetworkData Database Interface */
    public void addNetInfo(NetProphetNetworkData infoObject)
    {
        db_lock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();

                values.put(NetProphetData.NetInfoColumns.REQUEST_ID, infoObject.getReqID());
                values.put(NetProphetData.NetInfoColumns.NET_TYPE,infoObject.getNetworkType());
                values.put(NetProphetData.NetInfoColumns.NET_NAME,infoObject.getNetworkName());
                values.put(NetProphetData.NetInfoColumns.WIFI_LEVEL,infoObject.getWIFISignalLevel());
                values.put(NetProphetData.NetInfoColumns.CELL_LEVEL,infoObject.getCellSignalLevel());
                values.put(NetProphetData.NetInfoColumns.MCC,infoObject.getMCC());
                values.put(NetProphetData.NetInfoColumns.MNC,infoObject.getMNC());
                values.put(NetProphetData.NetInfoColumns.LAC,infoObject.getLAC());
                values.put(NetProphetData.NetInfoColumns.FIRST_MILE_LATENCY,infoObject.getFirstMileLatency());
                values.put(NetProphetData.NetInfoColumns.FIRST_MILE_PACKET_LOSS,infoObject.getFirstMilePacketLossRate());


                db.insert(NetProphetData.NetInfoColumns.TABLE_NAME, null, values);

                db.setTransactionSuccessful();
            } catch (Exception e) {
            	NetProphetLogger.logError("addNetInfo",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
        }
    }

    public void addNetInfos(List<NetProphetNetworkData> objList)
    {
        db_lock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                Iterator<NetProphetNetworkData> objIter = objList.iterator();
                while(objIter.hasNext()) {
                    NetProphetNetworkData infoObject = objIter.next();
                    ContentValues values = new ContentValues();

                    values.put(NetProphetData.NetInfoColumns.REQUEST_ID, infoObject.getReqID());
                    values.put(NetProphetData.NetInfoColumns.NET_TYPE,infoObject.getNetworkType());
                    values.put(NetProphetData.NetInfoColumns.NET_NAME,infoObject.getNetworkName());
                    values.put(NetProphetData.NetInfoColumns.WIFI_LEVEL,infoObject.getWIFISignalLevel());
                    values.put(NetProphetData.NetInfoColumns.CELL_LEVEL,infoObject.getCellSignalLevel());
                    values.put(NetProphetData.NetInfoColumns.MCC,infoObject.getMCC());
                    values.put(NetProphetData.NetInfoColumns.MNC,infoObject.getMNC());
                    values.put(NetProphetData.NetInfoColumns.LAC,infoObject.getLAC());
                    values.put(NetProphetData.NetInfoColumns.FIRST_MILE_LATENCY,infoObject.getFirstMileLatency());
                    values.put(NetProphetData.NetInfoColumns.FIRST_MILE_PACKET_LOSS,infoObject.getFirstMilePacketLossRate());
                    db.insert(NetProphetData.NetInfoColumns.TABLE_NAME, null, values);
                }

                db.setTransactionSuccessful();
            } catch (Exception e) {
            	NetProphetLogger.logError("addNetInfos",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
        }
    }

    public void deleteNetInfo(long req_id) {
        db_lock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete(NetProphetData.NetInfoColumns.TABLE_NAME, NetProphetData.NetInfoColumns.REQUEST_ID + " = ?",
                        new String[]{String.valueOf(req_id)});
                db.setTransactionSuccessful();
            } catch (Exception e) {
            	NetProphetLogger.logError("deleteNetInfo",e.toString());
                e.printStackTrace();   
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
        }
    }

    public long getNetInfoCount()
    {
        db_lock.lock();
        long count = 0;
        try{
            SQLiteDatabase db = this.getReadableDatabase();
            db.beginTransaction();
            try{
                String selectQuery = "SELECT count(*) FROM " + NetProphetData.NetInfoColumns.TABLE_NAME;
                Cursor cursor = db.rawQuery(selectQuery, null);
                if(cursor.moveToFirst())
                {
                    count = cursor.getLong(0);
                }
            }catch (Exception e)
            {
            	NetProphetLogger.logError("getNetInfoCount",e.toString());
                e.printStackTrace();
            }finally {
                db.endTransaction();
                db.close();
            }
        }finally {
            db_lock.unlock();
            return count;
        }
    }

    private List<NetProphetNetworkData> getAllNetInfo() {
        db_lock.lock();
        List<NetProphetNetworkData> requestList = new ArrayList<NetProphetNetworkData>();
        try {
            String selectQuery = "SELECT * FROM " + NetProphetData.NetInfoColumns.TABLE_NAME;
            SQLiteDatabase db = this.getReadableDatabase();
            db.beginTransaction();
            try {
                Cursor cursor = db.rawQuery(selectQuery, null);
                db.setTransactionSuccessful();
                if (cursor.moveToFirst()) {
                    do {
                        NetProphetNetworkData infoObject = new NetProphetNetworkData(cursor.getLong(0),
                                cursor.getString(1),cursor.getString(2),cursor.getInt(3),cursor.getInt(4),
                                cursor.getInt(5),cursor.getInt(6),cursor.getInt(7),cursor.getInt(8),cursor.getInt(9));

                        requestList.add(infoObject);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
            	NetProphetLogger.logError("getAllNetInfo",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
            return requestList;
        }
    }
    
    private List<NetProphetNetworkData> getAllNetInfoAndDelete() {
        db_lock.lock();
        List<NetProphetNetworkData> requestList = new ArrayList<NetProphetNetworkData>();
        try {
            String selectQuery = "SELECT * FROM " + NetProphetData.NetInfoColumns.TABLE_NAME;
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                Cursor cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) {
                    do {
                        db.delete(NetProphetData.NetInfoColumns.TABLE_NAME, NetProphetData.NetInfoColumns.REQUEST_ID + " = ?",
                                new String[]{String.valueOf(cursor.getLong(0))});
                        NetProphetNetworkData infoObject = new NetProphetNetworkData(cursor.getLong(0),
                                cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getInt(4),
                                cursor.getInt(5), cursor.getInt(6), cursor.getInt(7), cursor.getInt(8), cursor.getInt(9));
                        requestList.add(infoObject);
                    } while (cursor.moveToNext());
                    db.setTransactionSuccessful();
                }
            } catch (Exception e) {
            	NetProphetLogger.logError("getAllNetInfoAndDelete",e.toString());
                e.printStackTrace();
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
            return requestList;
        }
    }

    /* Task */
    public NetInfoSingleInsertTask getNetSingleInsertTask(NetProphetNetworkData infoObject)
    {
        return new NetInfoSingleInsertTask(infoObject);
    }
    public NetInfoBatchInsertTask getNetBatchInsertTask(List<NetProphetNetworkData> objList)
    {
        return new NetInfoBatchInsertTask(objList);
    }
    public class NetInfoSingleInsertTask implements Runnable{

        private NetProphetNetworkData infoObject;

        public NetInfoSingleInsertTask(NetProphetNetworkData obj)
        {
            this.infoObject = obj;
        }

        @Override
        public void run() {
            addNetInfo(infoObject);
        }
    }
    public class NetInfoBatchInsertTask implements Runnable{

        private List<NetProphetNetworkData> objectList;

        public NetInfoBatchInsertTask(List<NetProphetNetworkData> objList)
        {
            this.objectList = objList;
        }

        @Override
        public void run() {
            addNetInfos(objectList);
        }
    }
    
    public RequestSingleInsertTask getRequestSingleInsertTask(NetProphetHTTPRequestInfoObject infoObject)
    {
        return new RequestSingleInsertTask(infoObject);
    }
    public RequestBatchInsertTask getRequestBatchInsertTask(List<NetProphetHTTPRequestInfoObject> objList)
    {
        return new RequestBatchInsertTask(objList);
    }
    public DBSynchronizationTask getDBSyncTask(){
    	return new DBSynchronizationTask(this);
    }
    public class RequestSingleInsertTask implements Runnable{

        private NetProphetHTTPRequestInfoObject infoObject;

        public RequestSingleInsertTask(NetProphetHTTPRequestInfoObject obj)
        {
            this.infoObject = obj;
        }

        @Override
        public void run() {
            addRequestInfo(infoObject);
        }
    }
    public class RequestBatchInsertTask implements Runnable{

        private List<NetProphetHTTPRequestInfoObject> objectList;

        public RequestBatchInsertTask(List<NetProphetHTTPRequestInfoObject> objList)
        {
            this.objectList = objList;
        }

        @Override
        public void run() {
            addRequestInfos(objectList);
        }
    }
    
    public class DBSynchronizationTask implements Runnable{
    	private DatabaseHandler dbHandler;
    	private String url;
    	private long goodConnThreshold;
    	
    	public DBSynchronizationTask(DatabaseHandler handler){
    		dbHandler = handler;
    		url = propertyManager.getConnectionTestingURL();
    		goodConnThreshold = 1000;
    	}
		@Override
		public void run() {
			dbHandler.isSyncThreadRunning = true;
			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder().url(url).build();

			
			Call c = client.newCall(request);
			Response response;
			long delay = 0;
			for(int i=0; i<3; i++){
				try {
					long t1 = System.currentTimeMillis();
					response = c.execute();
					ResponseBody body = response.body();
					String str = body.string();
					delay = System.currentTimeMillis() - t1;
					if(response.code()==200 &&
						delay < goodConnThreshold){
						NetProphetLogger.logDebugging("DBSynchronizationTask", 
								"conn testing delay is:"+delay+"ms, okay to do synchronization");
						break;
					}
					
				} catch (Exception e) {
					NetProphetLogger.logError("DBSynchronizationTask", "failed connection testing:"+e);
				}
				NetProphetLogger.logDebugging("DBSynchronizationTask", 
						"conn testing delay is:"+delay+"ms, poor condition, wait for 5 mins and try again");
				SystemClock.sleep(7000);
			}
			
			if(delay >= goodConnThreshold){
				dbHandler.isSyncThreadRunning = false;
				return ;
			}
			if(!this.dbHandler.synchronizeDatabase()){
				NetProphetLogger.logError("DBSynchronizationTask", "failed to synchronize database");
			}
			dbHandler.isSyncThreadRunning = false;
		}
    }
}
