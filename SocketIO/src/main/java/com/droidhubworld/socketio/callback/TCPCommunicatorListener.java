package com.droidhubworld.socketio.callback;

import com.droidhubworld.socketio.ConnectionData;

public interface TCPCommunicatorListener {
    void onTCPActionCall(ConnectionData data);
    void onMessageReceived(String data);
    void onUDPMessageReceived(String data);
}
