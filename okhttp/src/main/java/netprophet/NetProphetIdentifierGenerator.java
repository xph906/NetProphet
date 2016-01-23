package netprophet;

public class NetProphetIdentifierGenerator {
	//Singleton pattern
	private static NetProphetIdentifierGenerator instance = null;
	public static NetProphetIdentifierGenerator getInstance() {
	  if(instance == null){
	    instance = new NetProphetIdentifierGenerator(System.currentTimeMillis());
	  }
	  return instance;
	}
	private long id;
	private NetProphetIdentifierGenerator(long seed){
	  this.id = seed+1;
	};
	
	public long getNextHTTPRequestID(){
		return this.id++;
	}
	
	//TODO: maybe we should differentiate request ID and trasanction ID?
	public long getNextTransactionID(){
		return this.id++;
	}
	
}
