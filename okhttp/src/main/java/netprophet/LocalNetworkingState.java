package netprophet;
import static okhttp3.internal.Internal.logger;
import android.content.Context;

public class LocalNetworkingState {
	
	public class LocalNetworkingStateSnapshot{
        //TODO
        public LocalNetworkingStateSnapshot(String raw){
        }

        public synchronized void updateFields(String raw){
        }
    }
    public class SynchronizedBooleanData {
        private boolean val;
        public SynchronizedBooleanData(boolean val){
            this.val = val;
        }

        public synchronized void setVal(boolean val){
            this.val = val;
        }
        public synchronized boolean getVal(){
            return this.val;
        }

    };
	
	 private boolean isCurrentProcessRunningForeground(){
		 logger.info("bogus function isCurrentProcessRunningForeground");
		 return true;
	 }

	 public LocalNetworkingState(String type, String name, int updateInteval, int latencyUpdateTimeout, NetUtility util,
	                                Context context){
		 logger.info("bogus function isCurrentProcessRunningForeground");
	 }
	 
    public LocalNetworkingStateSnapshot getNetworkState(){
        return new LocalNetworkingStateSnapshot("");
    }

    public void startRepeatedRefresh(){
    }

    public void startOnetimeRefresh(){
    }

    public void stopRepeatedRefresh(){
    }

}
