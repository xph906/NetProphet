package netprophet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import java.util.logging.Level;

import okhttp3.internal.Internal.NetProphetLogger;
import static okhttp3.internal.Internal.logger;

/*
 *  android.permission.INTERNET
 *  android.permission.ACCESS_COARSE_LOCATION
 *  android.permission.ACCESS_FINE_LOCATION
 *  android.permission.ACCESS_WIFI_STATE
 *  android.permission.ACCESS_NETWORK_STATE
 *  android.permission.READ_PHONE_STATE
 *  android.permission.GET_TASKS
 * */
public class NetUtility extends PhoneStateListener {
    private State mState;
    private String mType;
    private String mName;
    private int mWIFISignalLevel;
    private int mCellSignalLevel;
    private int mMCC;   // mobile network code for GSM, system ID for CDMA
    private int mMNC;   // mobile country code for GSM, network operator fo CDMA
    private int mLAC;   // local area code for GSM, network ID for CDMA
    private String mIMEI;
    private int mCellularType;
    private CellularType mGeneralizedCellularType;
    
    private Context mContext;
    private boolean mListening; //whether monitoring connection changes
    private boolean mIsFailover;
    private NetworkInfo.DetailedState mDetailedState;
    private String mReason;
    private NetworkInfo mNetworkInfo, mOtherNetworkInfo;
    private ConnectivityBroadcastReceiver mReceiver;
    private Handler handler;
    private int mCellSignalStrength;
    private int mWIFISignalStrength;
    private TelephonyManager mTelManager;
    private SignalStrength mSignalStrength;
    private LocalNetworkingState mWIFINetState;
    private LocalNetworkingState mCellNetState;
    private NetProphet mNetProphet;

    private static NetUtility mNetUtility = null;
    public static NetUtility getInstance(Context cx, Handler handler){
    	if (mNetUtility==null){
    		mNetUtility = new NetUtility(cx, handler);
    		logger.severe("ATTENTION: create new instance!!!");
    	}
    	return mNetUtility;
    }
    
    public NetUtility(Context cx, Handler handler){
        this.mContext = cx;
        this.mState = State.UNKNOWN;
        this.mListening = false;
        this.mNetworkInfo = null;
        this.mOtherNetworkInfo = null;
        this.mIsFailover = false;
        this.mReceiver = new ConnectivityBroadcastReceiver();
        this.handler = handler;
        this.mName = null;
        this.startListening();
        this.mCellSignalStrength = 0;
        this.mWIFISignalStrength = 0;
        this.mNetProphet = null;
        mTelManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mTelManager.listen(this, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        //10min and 60s
        mWIFINetState = new LocalNetworkingState("WIFI", this.mName, 10, 60, this,cx);
        mCellNetState = new LocalNetworkingState("mobile", this.mName, 10, 60, this,cx);
        
        mGeneralizedCellularType = CellularType.GSM;
        getLAC(); //update cellular type

    }

    public State getNetworkingState(){
        return mState;
    }
    public String getNetworkingType() {
        return mType;
    }
    public String getNetworkingName() {
        return mName;
    }

    public enum CellularType {
        CDMA,
        GSM
    };

    public enum State {
        UNKNOWN,
        CONNECTED,
        NOT_CONNECTED,
        CONNECTING
    };

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                mListening == false) {
                return;
            }

            //handle no connectivity
            boolean noConnectivity =
                    intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            mReason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            if (noConnectivity) {

                logger.log(Level.INFO, "Network disconnected: ");
                mState = State.NOT_CONNECTED;
                mName = null;
                String msg_obj = String.format(
                        "NetworkingState Updated: %s (%s)\n", mState, mReason == null ? "" : mReason);
                postMessage(InternalConst.MSGType.NETINFO_MSG, msg_obj);
                mWIFINetState.stopRepeatedRefresh();
                mCellNetState.stopRepeatedRefresh();
                if(mNetProphet != null)
                	mNetProphet.networkingChanged(-1, "Network Disconnected");
                return;
            }

            ConnectivityManager cm =
                    (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            mNetworkInfo = cm.getActiveNetworkInfo();
            if (mNetworkInfo == null) return;
            mIsFailover =
                    intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);

            // get type and name
            String subType = mNetworkInfo.getSubtypeName();
            mType = mNetworkInfo.getTypeName();
            boolean isConnected = mNetworkInfo.isConnected();
            boolean isConnecting = mNetworkInfo.isConnectedOrConnecting();
            if(isConnected){
                logger.log(Level.INFO, "Network connected: "+mType+"/"+subType);
                mState = State.CONNECTED;
            }
            else if(isConnecting){
                logger.log(Level.INFO, "Network connecting: "+mType+'/'+subType);
                mState = State.CONNECTING;
            }
            try {
                // update type and name
                if (isConnected && mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    mName = fetchWIFIName();
                    subType = mName;
                    mCellNetState.stopRepeatedRefresh();
                    mWIFINetState.startRepeatedRefresh();
                    if(mNetProphet != null)
                    	mNetProphet.networkingChanged(mNetworkInfo.getType(), mName);
                } else if (isConnected) {
                    mName = subType;
                    mCellularType = mTelManager.getNetworkType();
                    mWIFINetState.stopRepeatedRefresh();
                    mCellNetState.startRepeatedRefresh();
                    if(mNetProphet != null)
                    	mNetProphet.networkingChanged(ConnectivityManager.TYPE_MOBILE, mName);
                }
            }
            catch(Exception e){
                System.err.println("error in  ConnectivityBroadcastReceiver:"+e);
                e.printStackTrace();
            }

            //update log information
            StringBuilder sb = new StringBuilder();
            sb.append(String.format( "NetworkingState Updated: %s\n", mState));
            sb.append(String.format( "  Type: %s \n  Subtype %s\n",  mType, subType));
            postMessage(InternalConst.MSGType.NETINFO_MSG, sb.toString());
        }
    };

    public NetProphet getmNetProphet() {
		return mNetProphet;
	}

	public void setmNetProphet(NetProphet mNetProphet) {
		this.mNetProphet = mNetProphet;
	}

	@Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        mSignalStrength = signalStrength;
    }

    public LocalNetworkingState.LocalNetworkingStateSnapshot getRefreshedFirstMileLatency(){
        if(mState == State.NOT_CONNECTED)
            return null;
        else if(mType.equals("mobile"))
            return mCellNetState.getNetworkState();
        else if(mType.equals("WIFI"))
            return mWIFINetState.getNetworkState();
        return null;
    }

    public void refreshFirstMileLatency(){
        if(mState == State.NOT_CONNECTED)
            return ;
        else if(mType.equals("mobile"))
            mCellNetState.startOnetimeRefresh();
        else if(mType.equals("WIFI"))
            mWIFINetState.startOnetimeRefresh();
    }

  //NEW
    public String getIMEI(){
        try {
            mIMEI = mTelManager.getDeviceId();
        }
        catch(Exception e){
            logger.severe("failed to get IMEI");
        }
        return mIMEI;
    }

    //NEW
    public int getLAC(){
        try {
            //For CDMA, we get Network ID here
            if(mGeneralizedCellularType == CellularType.GSM) {
                GsmCellLocation location = (GsmCellLocation) mTelManager.getCellLocation();
                mLAC = location.getLac();
            }
            else{
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) mTelManager.getCellLocation();
                mLAC = cdmaCellLocation.getNetworkId();
            }
            return mLAC;
        }
        catch(Exception e){
            try{
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) mTelManager.getCellLocation();
                mLAC = cdmaCellLocation.getNetworkId();
                mGeneralizedCellularType = CellularType.CDMA;
            }
            catch(Exception err){
                logger.severe("failed to get LAC");
            }
        }
        return 0;
    }

    //NEW
    //MCC for CDMA is system ID
    public int getMCC(){
        try {
            String networkOperator = mTelManager.getNetworkOperator();
            if(mGeneralizedCellularType == CellularType.GSM){
                if (networkOperator != null) {
                    mMCC = Integer.valueOf(networkOperator.substring(0, 3));
                    return mMCC;
                }
            }
            else{
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) mTelManager.getCellLocation();
                return cdmaCellLocation.getSystemId();
            }
        }
        catch(Exception e){
            logger.severe("failed to get mcc");
        }
        return 0;
    }

    //NEW
    //MNC for CDMA is NetworkOperator
    public int getMNC(){
        try {

            String networkOperator = mTelManager.getNetworkOperator();
            if(mGeneralizedCellularType == CellularType.GSM) {
                if (networkOperator != null) {
                    mMNC = Integer.valueOf(networkOperator.substring(3));
                    return mMNC;
                }
            }
            else{
                mMNC = Integer.valueOf(networkOperator);
                return mMNC;
            }
        }
        catch(Exception e){
            logger.severe("failed to get MNC ");
        }
        return 0;
    }

    public int getWIFISignalStrength(){
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        int numberOfLevels = 5;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        mWIFISignalStrength = wifiInfo.getRssi();
        mWIFISignalLevel  = WifiManager.calculateSignalLevel(mWIFISignalStrength, numberOfLevels);
        logger.info(String.format("WIFI signal strength: %d (%d)",
                mWIFISignalStrength, mWIFISignalLevel));
        return mWIFISignalLevel;
    }

    public String toString(){
        try {
            return String.format("State : %s\nType  : %s\nName  : %s\nWIFISig: %d\nCELLSig: %d\n" +
                            "LAC   :%d\nMNC   :%d\nMCC   :%d\nIMEI:   %s\n ",
                    getNetworkingState().toString(), getNetworkingType().toString(), getNetworkingName(),
                    getWIFISignalStrength(), getCellSignalStrength(),
                    getLAC(), getMNC(), getMCC(), getIMEI());
        }
        catch(Exception e){
            logger.severe("error in toString "+e);
            e.printStackTrace();
            return "";
        }
    }
    public synchronized void startListening() {
        if (!mListening) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mReceiver, filter);
            mListening = true;
        }
    }

    public synchronized void stopListening() {
        if (mListening) {
            mContext.unregisterReceiver(mReceiver);
            mContext = null;
            mNetworkInfo = null;
            mOtherNetworkInfo = null;
            mIsFailover = false;
            mReason = null;
            mListening = false;
        }
    }

    private String fetchWIFIName(){
        WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED ||
                        state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }

  //TODO: Needs further updates!!!
    public int getCellSignalStrength()
    {
        int dbm = 0;
        try {
            if (mCellularType == TelephonyManager.NETWORK_TYPE_LTE) {
                String ssignal = mSignalStrength.toString();
                String[] parts = ssignal.split(" ");
                int asu = Integer.parseInt(parts[8]);
                dbm = asu * 2 - 113;
            } else if (mCellularType == TelephonyManager.NETWORK_TYPE_CDMA
                    || mCellularType == TelephonyManager.NETWORK_TYPE_1xRTT ||
                    mCellularType == TelephonyManager.NETWORK_TYPE_EHRPD ) {
                dbm = mSignalStrength.getCdmaDbm();
            } else if (mCellularType == TelephonyManager.NETWORK_TYPE_EVDO_0
                    || mCellularType == TelephonyManager.NETWORK_TYPE_EVDO_A
                    || mCellularType == TelephonyManager.NETWORK_TYPE_EVDO_B) {

                dbm = mSignalStrength.getEvdoDbm();
            } else if (mSignalStrength.isGsm()) {
                int asu = mSignalStrength.getGsmSignalStrength();
                dbm = asu * 2 - 113;
            } else {
                if(mGeneralizedCellularType == CellularType.GSM){
                    int asu = mSignalStrength.getGsmSignalStrength();
                    dbm = asu * 2 - 113;
                }
                else
                    dbm = mSignalStrength.getCdmaDbm();
            }
        }
        catch(Exception e){
        	NetProphetLogger.logError("getCellSignalStrength", e.toString());
        }

        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mCellSignalLevel  = WifiManager.calculateSignalLevel(dbm, 5);
        return mCellSignalLevel;
    }


    //TODO: not implemented
    public String getDetailedState(){
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        //NetworkInfo.DetailedState state =
        return "";
    }

    private void postMessage(int msgType, String content){
        if(handler == null)
            return;
        Message msg = new Message();
        msg.what = msgType;
        msg.obj = content;
        handler.sendMessage(msg);
        NetProphetLogger.logDebugging("postMessage", content);
    }

}
