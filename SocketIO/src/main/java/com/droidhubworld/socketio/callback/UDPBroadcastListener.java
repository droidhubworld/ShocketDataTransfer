package com.droidhubworld.socketio.callback;

public interface UDPBroadcastListener {
    void obBroadcastSuccess(String message);
    void obBroadcastError(Throwable t);
}
