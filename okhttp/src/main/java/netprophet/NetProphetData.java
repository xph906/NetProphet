package netprophet;


/**
 * Created by dell on 2016/1/26.
 */
public class NetProphetData {

    public static class NetInfoColumns{

        static final String TABLE_NAME = "NetworkInfo";

        static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY";
        static final String TYPE_TEXT = "TEXT";
        static final String TYPE_INT = "INT";

        static final String REQUEST_ID = "req_id";
        static final String NET_TYPE = "net_type";
        static final String NET_NAME = "net_name";
        static final String WIFI_LEVEL = "wifi_level";
        static final String CELL_LEVEL = "cell_level";
        static final String MCC = "mcc";
        static final String MNC = "mnc";
        static final String LAC = "lac";

        static final String FIRST_MILE_LATENCY = "first_mile_latency";
        static final String FIRST_MILE_PACKET_LOSS = "first_mile_packet_loss";

        public static final String[][] COLUMNS = new String[][]{{REQUEST_ID,TYPE_PRIMARY_KEY}, {NET_TYPE, TYPE_TEXT}, {NET_NAME, TYPE_TEXT},
                {WIFI_LEVEL,TYPE_INT},{CELL_LEVEL,TYPE_INT},{MCC,TYPE_INT},{MNC,TYPE_INT},{LAC,TYPE_INT},{FIRST_MILE_LATENCY,TYPE_INT},
                {FIRST_MILE_PACKET_LOSS,TYPE_INT}};


    }


    public static class RequestColumns{
        static final String TABLE_NAME = "HTTPRequestInfo";

        static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY";
        static final String TYPE_TEXT = "TEXT";
        static final String TYPE_INT = "INT";
        static final String TYPE_INTEGER = "INTEGER";
        static final String TYPE_BOOLEAN = "INTEGER(1)";


        // 1. General information
        static final String REQUEST_ID = "req_id";
        static final String URL = "url";
        static final String METHOD = "method";
        /* Using TelephonyManager.getDeviceId() for now.
         * It has known issues: http://android-developers.blogspot.be/2011/03/identifying-app-installations.html */
        static final String USER_ID = "user_id";


        // 2. Redirection information
	/* Used to find previous/next request in a redirection chain
	 * 0 means no redirection. */
        static final String PREV_REQ_ID = "prev_req_id";
        static final String NEXT_REQ_ID = "next_req_id";

        // 3. Timing information
        static final String START_TIME = "start_time";
        static final String END_TIME = "end_time";
        static final String OVERALL_DELAY ="overall_delay";
        static final String DNS_DELAY = "dns_delay";
        static final String CONN_DELAY = "conn_delay";
        static final String HANDSHAKE_DELAY = "handshake_delay";
        static final String TLS_DELAY = "tls_delay";
        static final String REQ_WRITE_DELAY = "req_write_delay";
        static final String SERVER_DELAY = "server_delay";
        static final String TTFB_DELAY = "ttfb_delay";
        static final String RESP_TRANS_DELAY = "resp_trans_delay";

        // 4. Catching information.
        static final String USE_CONN_CACHE = "use_conn_cache";
        static final String USE_DNS_CACHE = "use_dns_cache";
        static final String USE_RESP_CACHE = "use_resp_cache";

        // 5. Request/Response information.
        static final String RESP_SIZE = "resp_size";
        static final String HTTP_CODE = "http_code";
        static final String REQ_SIZE = "req_size";

        // 6. Error information
	/* Note for a redirection chain, an error can only be at the last request
	 * So for all the requests in a redirection chain, the error related information
	 * will be the same: the call's error information (last request).
	 * */
        static final String IS_FAILED_REQUEST = "is_failed_req";
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
        static final String ERROR_MSG = "error_msg";
        static final String DETAILED_ERROR_MSG = "detailed_error_msg";

        // 7. Transaction information
        static final String TRANS_ID = "trans_id";
        static final String TRANS_TYPE = "trans_type";

        // 8. Networking Information


        public static final String[][] COLUMNS = new String[][]{{REQUEST_ID,TYPE_PRIMARY_KEY}, {URL, TYPE_TEXT}, {METHOD, TYPE_TEXT},
                {USER_ID,TYPE_TEXT}, {PREV_REQ_ID,TYPE_INTEGER}, {NEXT_REQ_ID,TYPE_INTEGER}, {START_TIME,TYPE_INTEGER},
                {END_TIME,TYPE_INTEGER},{OVERALL_DELAY,TYPE_INTEGER},{DNS_DELAY,TYPE_INTEGER},{CONN_DELAY,TYPE_INTEGER},{HANDSHAKE_DELAY,TYPE_INTEGER},
                {TLS_DELAY,TYPE_INTEGER},{REQ_WRITE_DELAY,TYPE_INTEGER},{SERVER_DELAY,TYPE_INTEGER},{TTFB_DELAY,TYPE_INTEGER},
                {RESP_TRANS_DELAY,TYPE_INTEGER},{USE_CONN_CACHE,TYPE_BOOLEAN},{USE_DNS_CACHE,TYPE_BOOLEAN},{USE_RESP_CACHE,TYPE_BOOLEAN},
                {RESP_SIZE,TYPE_INTEGER},{HTTP_CODE,TYPE_INT},{REQ_SIZE,TYPE_INT},{IS_FAILED_REQUEST,TYPE_BOOLEAN},
                {ERROR_MSG,TYPE_TEXT},{DETAILED_ERROR_MSG,TYPE_TEXT},{TRANS_ID,TYPE_INTEGER},{TRANS_TYPE,TYPE_INT}};

    }



}
