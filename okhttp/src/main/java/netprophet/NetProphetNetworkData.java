package netprophet;

public class NetProphetNetworkData {
	private long reqID;
	private String networkType;   //"WIFI" or "mobile"
	private String networkName;   // LIMIT 16
	private int WIFISignalLevel;  // [0 - 4]
	private int cellSignalLevel;  // [0 - 4]
	private int MCC;              // mobile network code
	private int MNC;              // mobile country code
	private int LAC;              // local area code
	
	private int firstMileLatency; // ms
	private int firstMilePacketLossRate; // e.g., 40 -> 40%
	
	public NetProphetNetworkData(long reqID, String type, String name, int wifiSignal, int cellSignal, 
			int mcc, int mnc, int lac, 
			int firstMileLat, int firstMilePLR){
		this.reqID = reqID;
		this.networkName = name;
		this.networkType = type;
		this.WIFISignalLevel = wifiSignal;
		this.cellSignalLevel = cellSignal;
		this.MCC = mcc;
		this.MNC = mnc;
		this.LAC = lac;
		this.firstMileLatency = firstMileLat;
		this.firstMilePacketLossRate = firstMilePLR;
	}
	
	public String toString(){
		return String.format("REQ_ID:%d\n TYPE:%s NAME:%s\n" + 
				"WIFISIG:%d CELLSIG:%d\n"+
				"MCC:%d MNC:%d\n"+
				"LAC:%d", (long)reqID, networkType, networkName,  WIFISignalLevel,
				cellSignalLevel, MCC, MNC, LAC);
	}
	
	public String getNetworkType() {
		return networkType;
	}
	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}
	public String getNetworkName() {
		return networkName;
	}
	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}
	public int getWIFISignalLevel() {
		return WIFISignalLevel;
	}
	public void setWIFISignalLevel(int wIFISignalLevel) {
		WIFISignalLevel = wIFISignalLevel;
	}
	public int getCellSignalLevel() {
		return cellSignalLevel;
	}
	public void setCellSignalLevel(int cellSignalLevel) {
		cellSignalLevel = cellSignalLevel;
	}
	public int getMCC() {
		return MCC;
	}
	public void setMCC(int mCC) {
		MCC = mCC;
	}
	public int getMNC() {
		return MNC;
	}
	public void setMNC(int mNC) {
		MNC = mNC;
	}
	public int getLAC() {
		return LAC;
	}
	public void setLAC(int lAC) {
		LAC = lAC;
	}
	public long getReqID() {
		return reqID;
	}

	public void setReqID(long reqID) {
		this.reqID = reqID;
	}

	public int getFirstMileLatency() {
		return firstMileLatency;
	}

	public void setFirstMileLatency(int firstMileLatency) {
		this.firstMileLatency = firstMileLatency;
	}

	public int getFirstMilePacketLossRate() {
		return firstMilePacketLossRate;
	}

	public void setFirstMilePacketLossRate(int firstMilePacketLossRate) {
		this.firstMilePacketLossRate = firstMilePacketLossRate;
	}
	
	
}
