package netprophet;

// 28 fields in total.
public class NetProphetHTTPRequestInfoObject {
	// 1. General information
	private long reqID;
	private String url;
	private String method;
	/* Using TelephonyManager.getDeviceId() for now.
	 * It has known issues: http://android-developers.blogspot.be/2011/03/identifying-app-installations.html */
	private String userID;
	
	// 2. Redirection information
	/* Used to find previous/next request in a redirection chain
	 * 0 means no redirection. */
	private long prevReqID; 
	private long nextReqID;
	
	// 3. Timing information
	private long startTime;
	private long endTime;
	private long overallDelay;   /* endTime - startTime */
	private long dnsDelay;
	private long connDelay;
	private long handshakeDelay;
	private long tlsDelay;       /* connDelay = handshakeDelay + tlsDelay + minor_processing_delay */
	private long reqWriteDelay;
	private long serverDelay;    /* respFirstByteEndTime - handshakeDelay */
	private long TTFBDelay;      /* Time to First Byte delay */
	private long respTransDelay; /* respLastByteEndTime - respFirstByteEndTime */
	
	// 4. Catching information.
	private boolean useConnCache;
	private boolean useDNSCache;
	private boolean useRespCache;
	
	// 5. Request/Response information.
	private long respSize;
	private int HTTPCode;
	private int reqSize;  /* if post streaming data, the reqSize is 0 */
	
	// 6. Error information
	/* Note for a redirection chain, an error can only be at the last request
	 * So for all the requests in a redirection chain, the error related information
	 * will be the same: the call's error information (last request).
	 * */
	private boolean isFailedRequest;
	/* 
	 * NOERROR,
	 * DNS_ERR,
	 * CONN_ERR,
	 * REQ_SENT_ERR,
	 * RESP_RECV_ERR,
	 * HTTP_ERR,
	 * OTHER_ERR,
	 * IN_COMPLETE_TIMING,
	 * HTTP_SERVER_TIMEOUT_ERR, 
	 * NO_REQ
	 * */
	private String errorMsg;
	private String detailedErrorMsg;
	
	// 7. Transaction information
	private long transID;
	private int transType;
	
	// 8. Networking Information

	public NetProphetHTTPRequestInfoObject(long reqID, String url, String method, String userID,
			long prevReqID, long nextReqID, 
			long startTime, long endTime, long overallDelay, long dnsDelay,
			long connDelay, long handshakeDelay, long tlsDelay, long reqWriteDelay,
			long serverDelay, long TTFBDelay, long respTransDelay, 
			boolean useConnCache, boolean useDNSCache, boolean useRespCache,
			long respSize, int HTTPCode, int reqSize, 
			boolean isFailedRequest, String errorMsg, String detailedErrorMsg,
			long transID, int transType){
		this.reqID = reqID;
		this.url = url;
		this.method = method;
		this.userID = userID;
		this.prevReqID = prevReqID;
		this.nextReqID = nextReqID;
		this.startTime = startTime;
		this.endTime = endTime;
		this.overallDelay = overallDelay;
		this.dnsDelay = dnsDelay;
		this.connDelay = connDelay;
		this.handshakeDelay = handshakeDelay;
		this.tlsDelay = tlsDelay;
		this.reqWriteDelay = reqWriteDelay;
		this.serverDelay = serverDelay;
		this.TTFBDelay = TTFBDelay;
		this.respTransDelay = respTransDelay;
		this.useConnCache = useConnCache;
		this.useDNSCache = useDNSCache;
		this.useRespCache = useRespCache;
		this.respSize = respSize;
		this.HTTPCode = HTTPCode;
		this.reqSize = reqSize;
		this.isFailedRequest = isFailedRequest;
		this.errorMsg = errorMsg;
		this.detailedErrorMsg = detailedErrorMsg;
		this.transID = transID;
		this.transType = transType;
	}
	
	public long getReqID() {
		return reqID;
	}

	public void setReqID(long reqID) {
		this.reqID = reqID;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public long getPrevReqID() {
		return prevReqID;
	}

	public void setPrevReqID(long prevReqID) {
		this.prevReqID = prevReqID;
	}

	public long getNextReqID() {
		return nextReqID;
	}

	public void setNextReqID(long nextReqID) {
		this.nextReqID = nextReqID;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getOverallDelay() {
		return overallDelay;
	}

	public void setOverallDelay(long overallDelay) {
		this.overallDelay = overallDelay;
	}

	public long getDnsDelay() {
		return dnsDelay;
	}

	public void setDnsDelay(long dnsDelay) {
		this.dnsDelay = dnsDelay;
	}

	public long getConnDelay() {
		return connDelay;
	}

	public void setConnDelay(long connDelay) {
		this.connDelay = connDelay;
	}

	public long getHandshakeDelay() {
		return handshakeDelay;
	}

	public void setHandshakeDelay(long handshakeDelay) {
		this.handshakeDelay = handshakeDelay;
	}

	public long getTlsDelay() {
		return tlsDelay;
	}

	public void setTlsDelay(long tlsDelay) {
		this.tlsDelay = tlsDelay;
	}

	public long getReqWriteDelay() {
		return reqWriteDelay;
	}

	public void setReqWriteDelay(long reqWriteDelay) {
		this.reqWriteDelay = reqWriteDelay;
	}

	public long getServerDelay() {
		return serverDelay;
	}

	public void setServerDelay(long serverDelay) {
		this.serverDelay = serverDelay;
	}

	public long getTTFBDelay() {
		return TTFBDelay;
	}

	public void setTTFBDelay(long tTFBDelay) {
		TTFBDelay = tTFBDelay;
	}

	public long getRespTransDelay() {
		return respTransDelay;
	}

	public void setRespTransDelay(long respTransDelay) {
		this.respTransDelay = respTransDelay;
	}

	public boolean isUseConnCache() {
		return useConnCache;
	}

	public void setUseConnCache(boolean useConnCache) {
		this.useConnCache = useConnCache;
	}

	public boolean isUseDNSCache() {
		return useDNSCache;
	}

	public void setUseDNSCache(boolean useDNSCache) {
		this.useDNSCache = useDNSCache;
	}

	public boolean isUseRespCache() {
		return useRespCache;
	}

	public void setUseRespCache(boolean useRespCache) {
		this.useRespCache = useRespCache;
	}

	public long getRespSize() {
		return respSize;
	}

	public void setRespSize(long respSize) {
		this.respSize = respSize;
	}

	public int getHTTPCode() {
		return HTTPCode;
	}

	public void setHTTPCode(int hTTPCode) {
		HTTPCode = hTTPCode;
	}

	public int getReqSize() {
		return reqSize;
	}

	public void setReqSize(int reqSize) {
		this.reqSize = reqSize;
	}

	public boolean isFailedRequest() {
		return isFailedRequest;
	}

	public void setFailedRequest(boolean isFailedRequest) {
		this.isFailedRequest = isFailedRequest;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String getDetailedErrorMsg() {
		return detailedErrorMsg;
	}

	public void setDetailedErrorMsg(String detailedErrorMsg) {
		this.detailedErrorMsg = detailedErrorMsg;
	}
	
	public long getTransID() {
		return transID;
	}

	public void setTransID(long transID) {
		this.transID = transID;
	}

	public int getTransType() {
		return transType;
	}

	public void setTransType(int transType) {
		this.transType = transType;
	}
}
