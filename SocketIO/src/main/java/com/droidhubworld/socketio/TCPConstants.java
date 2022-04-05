package com.droidhubworld.socketio;

public class TCPConstants {

    public static final int CLIENT = 0;
    public static final int SERVER = 1;

    public static int PORT = 1024;

    public static final int SERVER_START = 1;
    public static final int SERVER_RUNNING = 2;
    public static final int SERVER_CLOSED = 3;
    public static final int CLIENT_CONNECTED = 4;
    public static final int CLIENT_DISCONNECTED = 5;
    public static final int CLIENT_IS_RUNNING = 10;
    public static final int SERVER_IS_RUNNING = 11;
    public static final int PORT_LOWER = 12;
    public static final int EXCEPTION = 404;
    public static final int AUTO_CONNECTING = 405;
    public static final int LOCAL_ERROR_ACTION = 406;
    public static final int READY_TO_RECEIVED_BROADCAST = 101;
    public static final int ERROR_ON_RECEIVED_BROADCAST = 102;

    public static final String LOCAL_DATA = "com.droidhubworld.socketio.DATA";

    /*
    * ACTIONS
    * */
    public static final String NEW_MASSAGE =  "com.droidhubworld.socketio.new_local_message";

    public static final String LOCAL_ACTION = "com.droidhubworld.socketio.localAction";
//    public static final String LOCAL_SERVER_ACTION = "com.droidhubworld.socketio.localServerAction";
    public static final String LOCAL_USP_ACTION = "com.droidhubworld.socketio.localUdpAction";
    public static final String LOCAL_MESSAGE_ACTION = "com.droidhubworld.socketio.localserveractionmessage";
//    public static final String LOCAL_CLIENT_ACTION = "com.droidhubworld.socketio.localClientAction";
}
