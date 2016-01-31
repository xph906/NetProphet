package netprophet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by dell on 2016/1/26.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "NetProphet.db";

    private static ReentrantLock db_lock = new ReentrantLock();


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTable(NetProphetData.TABLE_NAME, NetProphetData.COLUMNS));
        //db.execSQL("CREATE INDEX request_id_index on " + NetProphetData.TABLE_NAME + "(" + NetProphetData.REQUEST_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addRequestInfo(NetProphetHTTPRequestInfoObject infoObject) {
        db_lock.lock();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                checkRequestInfo(infoObject);

                values.put(NetProphetData.REQUEST_ID, infoObject.getReqID());
                values.put(NetProphetData.URL, infoObject.getUrl());
                values.put(NetProphetData.METHOD, infoObject.getMethod());
                values.put(NetProphetData.USER_ID, infoObject.getUserID());

                values.put(NetProphetData.PREV_REQ_ID, infoObject.getPrevReqID());
                values.put(NetProphetData.NEXT_REQ_ID, infoObject.getNextReqID());

                values.put(NetProphetData.START_TIME, infoObject.getStartTime());
                values.put(NetProphetData.END_TIME, infoObject.getEndTime());
                values.put(NetProphetData.OVERALL_DELAY, infoObject.getOverallDelay());
                values.put(NetProphetData.DNS_DELAY, infoObject.getDnsDelay());
                values.put(NetProphetData.CONN_DELAY, infoObject.getConnDelay());
                values.put(NetProphetData.HANDSHAKE_DELAY, infoObject.getHandshakeDelay());
                values.put(NetProphetData.TLS_DELAY, infoObject.getTlsDelay());
                values.put(NetProphetData.REQ_WRITE_DELAY, infoObject.getReqWriteDelay());
                values.put(NetProphetData.SERVER_DELAY, infoObject.getServerDelay());
                values.put(NetProphetData.TTFB_DELAY, infoObject.getTTFBDelay());
                values.put(NetProphetData.RESP_TRANS_DELAY, infoObject.getRespTransDelay());

                if (infoObject.isUseConnCache()) {
                    values.put(NetProphetData.USE_CONN_CACHE, 1);
                } else {
                    values.put(NetProphetData.USE_CONN_CACHE, 0);
                }
                if (infoObject.isUseDNSCache()) {
                    values.put(NetProphetData.USE_DNS_CACHE, 1);
                } else {
                    values.put(NetProphetData.USE_DNS_CACHE, 0);
                }
                if (infoObject.isUseRespCache()) {
                    values.put(NetProphetData.USE_RESP_CACHE, 1);
                } else {
                    values.put(NetProphetData.USE_RESP_CACHE, 0);
                }

                values.put(NetProphetData.RESP_SIZE, infoObject.getRespSize());
                values.put(NetProphetData.HTTP_CODE, infoObject.getHTTPCode());
                values.put(NetProphetData.REQ_SIZE, infoObject.getReqSize());

                if (infoObject.isFailedRequest()) {
                    values.put(NetProphetData.IS_FAILED_REQUEST, 1);
                } else {
                    values.put(NetProphetData.IS_FAILED_REQUEST, 0);
                }
                values.put(NetProphetData.ERROR_MSG, infoObject.getErrorMsg());
                values.put(NetProphetData.DETAILED_ERROR_MSG, infoObject.getDetailedErrorMsg());
                values.put(NetProphetData.TRANS_ID, infoObject.getTransID());
                values.put(NetProphetData.TRANS_TYPE, infoObject.getTransType());

                db.insert(NetProphetData.TABLE_NAME, null, values);

                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Database", "Failed to insert request info!");
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

                    values.put(NetProphetData.REQUEST_ID, infoObject.getReqID());
                    values.put(NetProphetData.URL, infoObject.getUrl());
                    values.put(NetProphetData.METHOD, infoObject.getMethod());
                    values.put(NetProphetData.USER_ID, infoObject.getUserID());

                    values.put(NetProphetData.PREV_REQ_ID, infoObject.getPrevReqID());
                    values.put(NetProphetData.NEXT_REQ_ID, infoObject.getNextReqID());

                    values.put(NetProphetData.START_TIME, infoObject.getStartTime());
                    values.put(NetProphetData.END_TIME, infoObject.getEndTime());
                    values.put(NetProphetData.OVERALL_DELAY, infoObject.getOverallDelay());
                    values.put(NetProphetData.DNS_DELAY, infoObject.getDnsDelay());
                    values.put(NetProphetData.CONN_DELAY, infoObject.getConnDelay());
                    values.put(NetProphetData.HANDSHAKE_DELAY, infoObject.getHandshakeDelay());
                    values.put(NetProphetData.TLS_DELAY, infoObject.getTlsDelay());
                    values.put(NetProphetData.REQ_WRITE_DELAY, infoObject.getReqWriteDelay());
                    values.put(NetProphetData.SERVER_DELAY, infoObject.getServerDelay());
                    values.put(NetProphetData.TTFB_DELAY, infoObject.getTTFBDelay());
                    values.put(NetProphetData.RESP_TRANS_DELAY, infoObject.getRespTransDelay());

                    if (infoObject.isUseConnCache()) {
                        values.put(NetProphetData.USE_CONN_CACHE, 1);
                    } else {
                        values.put(NetProphetData.USE_CONN_CACHE, 0);
                    }
                    if (infoObject.isUseDNSCache()) {
                        values.put(NetProphetData.USE_DNS_CACHE, 1);
                    } else {
                        values.put(NetProphetData.USE_DNS_CACHE, 0);
                    }
                    if (infoObject.isUseRespCache()) {
                        values.put(NetProphetData.USE_RESP_CACHE, 1);
                    } else {
                        values.put(NetProphetData.USE_RESP_CACHE, 0);
                    }

                    values.put(NetProphetData.RESP_SIZE, infoObject.getRespSize());
                    values.put(NetProphetData.HTTP_CODE, infoObject.getHTTPCode());
                    values.put(NetProphetData.REQ_SIZE, infoObject.getReqSize());

                    if (infoObject.isFailedRequest()) {
                        values.put(NetProphetData.IS_FAILED_REQUEST, 1);
                    } else {
                        values.put(NetProphetData.IS_FAILED_REQUEST, 0);
                    }
                    values.put(NetProphetData.ERROR_MSG, infoObject.getErrorMsg());
                    values.put(NetProphetData.DETAILED_ERROR_MSG, infoObject.getDetailedErrorMsg());
                    values.put(NetProphetData.TRANS_ID, infoObject.getTransID());
                    values.put(NetProphetData.TRANS_TYPE, infoObject.getTransType());

                    db.insert(NetProphetData.TABLE_NAME, null, values);
                }

                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Database", "Failed to insert request info!");
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
                db.delete(NetProphetData.TABLE_NAME, NetProphetData.REQUEST_ID + " = ?",
                        new String[]{String.valueOf(req_id)});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Database", "Failed to delete request info!");
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
        }
    }

    public List<NetProphetHTTPRequestInfoObject> getAllRequestInfo() {
        db_lock.lock();
        List<NetProphetHTTPRequestInfoObject> requestList = new ArrayList<NetProphetHTTPRequestInfoObject>();
        try {
            String selectQuery = "SELECT * FROM " + NetProphetData.TABLE_NAME;
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
                                cursor.getLong(20), cursor.getInt(21), cursor.getInt(22), cursor.getInt(23) > 0,
                                cursor.getString(24), cursor.getString(25), cursor.getLong(26), cursor.getInt(27));

                        requestList.add(infoObject);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Database", "Failed to get all request info!");
            } finally {
                db.endTransaction();
                db.close();
            }
        } finally {
            db_lock.unlock();
            return requestList;
        }
    }

    public long getRequestInfoCount()
    {
        db_lock.lock();
        long count = 0;
        try{
            SQLiteDatabase db = this.getReadableDatabase();
            db.beginTransaction();
            try{
                String selectQuery = "SELECT count(*) FROM " + NetProphetData.TABLE_NAME;
                Cursor cursor = db.rawQuery(selectQuery, null);
                if(cursor.moveToFirst())
                {
                    count = cursor.getLong(0);
                }
            }catch (Exception e)
            {
                e.printStackTrace();
                Log.e("Database","Failed to get request count!");
            }finally {
                db.endTransaction();
                db.close();
            }
        }finally {
            db_lock.unlock();
            return count;
        }
    }

    public List<NetProphetHTTPRequestInfoObject> getAllRequestInfoAndDelete()
    {
        db_lock.lock();
        List<NetProphetHTTPRequestInfoObject> requestList = new ArrayList<NetProphetHTTPRequestInfoObject>();
        try {
            String selectQuery = "SELECT * FROM " + NetProphetData.TABLE_NAME;
            SQLiteDatabase db = this.getWritableDatabase();
            db.beginTransaction();
            try {
                Cursor cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) {
                    do {
                        db.delete(NetProphetData.TABLE_NAME, NetProphetData.REQUEST_ID + " = ?",
                                new String[]{String.valueOf(cursor.getLong(0))});
                        NetProphetHTTPRequestInfoObject infoObject = new NetProphetHTTPRequestInfoObject(
                                cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                                cursor.getLong(4), cursor.getLong(5), cursor.getLong(6), cursor.getLong(7),
                                cursor.getLong(8), cursor.getLong(9), cursor.getLong(10), cursor.getLong(11),
                                cursor.getLong(12), cursor.getLong(13), cursor.getLong(14), cursor.getLong(15),
                                cursor.getLong(16), cursor.getInt(17) > 0, cursor.getInt(18) > 0, cursor.getInt(19) > 0,
                                cursor.getLong(20), cursor.getInt(21), cursor.getInt(22), cursor.getInt(23) > 0,
                                cursor.getString(24), cursor.getString(25), cursor.getLong(26), cursor.getInt(27));

                        requestList.add(infoObject);
                    } while (cursor.moveToNext());
                    db.setTransactionSuccessful();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Database", "Failed to get all request info and delete!");
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
            Log.e("Database","URL IS NULL");
        }
        if(infoObject.getMethod() == null)
        {
            infoObject.setMethod("");
            Log.e("Database","METHOD IS NULL");
        }
        if(infoObject.getUserID() == null)
        {
            infoObject.setUserID("");
            Log.e("Database","USER_ID IS NULL");
        }
        if(infoObject.getErrorMsg() == null)
        {
            infoObject.setErrorMsg("");
            Log.e("Database","ERROR_MSG IS NULL");
        }
        if(infoObject.getDetailedErrorMsg() == null)
        {
            infoObject.setDetailedErrorMsg("");
            Log.e("Database","DETAILED_ERROR_MSG IS NULL");
        }


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

    public DatabaseSingleInsertTask getSingleInsertTask(NetProphetHTTPRequestInfoObject infoObject)
    {
        return new DatabaseSingleInsertTask(infoObject);
    }

    public DatabaseBatchInsertTask getBatchInsertTask(List<NetProphetHTTPRequestInfoObject> objList)
    {
        return new DatabaseBatchInsertTask(objList);
    }

    private class DatabaseSingleInsertTask implements Runnable{

        private NetProphetHTTPRequestInfoObject infoObject;

        public DatabaseSingleInsertTask(NetProphetHTTPRequestInfoObject obj)
        {
            this.infoObject = obj;
        }

        @Override
        public void run() {
            addRequestInfo(infoObject);
        }
    }

    private class DatabaseBatchInsertTask implements Runnable{

        private List<NetProphetHTTPRequestInfoObject> objectList;

        public DatabaseBatchInsertTask(List<NetProphetHTTPRequestInfoObject> objList)
        {
            this.objectList = objList;
        }

        @Override
        public void run() {
            addRequestInfos(objectList);
        }
    }
}
