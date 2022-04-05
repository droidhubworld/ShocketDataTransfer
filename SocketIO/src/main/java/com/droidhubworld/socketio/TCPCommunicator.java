package com.droidhubworld.socketio;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.droidhubworld.socketio.callback.TCPCommunicatorListener;
import com.droidhubworld.socketio.callback.UDPBroadcastListener;
import com.droidhubworld.socketio.model.ConnectClientDataModel;
import com.droidhubworld.socketio.toolsnetwork.IPTools;

import org.json.JSONObject;

import java.net.InetAddress;

public class TCPCommunicator {
    private static final String TAG = TCPCommunicator.class.getName();
    private Context mContext;
    private String autoConnectIpAddress;
    private int port;
    private int autoConnect;
    private TCPCommunicatorListener listener;
    public TCPCommunicatorService mTcpService;
    private ServiceReceiver mReceiver;
    private UDPBroadcastListener udpListener;
    public boolean mBoundTcp = false;

    public TCPCommunicator(Context mContext, String autoConnectIpAddress, int port, int autoConnect, boolean autoStartService, TCPCommunicatorListener listener, UDPBroadcastListener udpListener) {
        this.mContext = mContext;
        this.autoConnectIpAddress = autoConnectIpAddress;
        this.port = port;
        this.autoConnect = autoConnect;
        this.listener = listener;
        this.udpListener = udpListener;
        mReceiver = new ServiceReceiver();
        if (autoStartService) {
            startService();
        }
    }

    public ServiceReceiver getReceiver() {
        return mReceiver;
    }

    public ServiceConnection getServiceConnection() {
        return mServiceConnection;
    }

    public static class Builder {
        private Context mContext;
        private String autoConnectIpAddress;
        private int port;
        private int autoConnect;
        private boolean autoStartService;
        private TCPCommunicatorListener listener;
        private UDPBroadcastListener udpListener;

        public Builder(@NonNull Context mContext, int port) {
            this.mContext = mContext;
            this.port = port;
        }

        public Builder setAutoConnectIpAddress(@Nullable String autoConnectIpAddress) {
            this.autoConnectIpAddress = autoConnectIpAddress;
            return this;
        }

        public Builder setAutoConnect(int autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        public Builder setAutoStartService(boolean autoStartService) {
            this.autoStartService = autoStartService;
            return this;
        }

        public Builder setOnCallBackListener(@NonNull TCPCommunicatorListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setOnUDPListener(@NonNull UDPBroadcastListener udpListener) {
            this.udpListener = udpListener;
            return this;
        }

        public TCPCommunicator build() {
            return new TCPCommunicator(this.mContext, this.autoConnectIpAddress, this.port, this.autoConnect, this.autoStartService, this.listener, this.udpListener);
        }
    }

    public void startService() {
        Intent tcpIntent = new Intent(mContext, TCPCommunicatorService.class);
        /*TCPCommunicatorService mService = new TCPCommunicatorService();
        if (!CommonUtils.isMyServiceRunning(this, mService.getClass())) {
            startService(tcpIntent);
        }*/
        tcpIntent.putExtra("PORT", port);
        mContext.startService(tcpIntent);
    }

    public void startServer() {
        if (mTcpService != null) {
            mTcpService.startTcp(true, null, port);
        }
    }

    public void connectToServer(String ip, int port) {
        if (mTcpService != null) {
            mTcpService.startTcp(false, ip, port);
        }
    }

    public void disconnect() {
        if (mTcpService != null)
            mTcpService.disconnect();
    }

    public void connectClientUsingUDP(ConnectClientDataModel dataModel) {
        if (mTcpService != null) {
            mTcpService.connectClientUsingBroadcast(dataModel, udpListener);
        }
    }

    public void connectClientUsingUDP(JSONObject dataModel) {
        if (mTcpService != null) {
            mTcpService.connectClientUsingBroadcast(dataModel, udpListener);
        }
    }

    public void sendMessageToAllClient(JSONObject _obj) {
        if (mTcpService != null) {
            mTcpService.sendMessageToAllClient(_obj);
        }
    }

    public void sendMessageToAllClient(JSONObject _obj, String ignorClient) {
        if (mTcpService != null) {
            mTcpService.sendMessageToAllClient(_obj, ignorClient);
        }
    }

    public void sendMessage(JSONObject _obj) {
        if (mTcpService != null) {
            mTcpService.sendMessage(-1, _obj);
        }
    }

    public void sendMessage(int clientIndex, JSONObject _obj) {
        if (mTcpService != null) {
            mTcpService.sendMessage(clientIndex, _obj);
        }
    }

    public IntentFilter getIntentFilter() {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TCPConstants.LOCAL_ACTION);
        mIntentFilter.addAction(TCPConstants.LOCAL_USP_ACTION);
        mIntentFilter.addAction(TCPConstants.LOCAL_MESSAGE_ACTION);
        return mIntentFilter;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            Logger.e(TAG, "onServiceConnected");
            if (name.getClassName().equals(TCPCommunicatorService.class.getName())) {
                TCPCommunicatorService.LocalBinder binder = (TCPCommunicatorService.LocalBinder) service;
                mTcpService = binder.getService();
                mBoundTcp = true;
                if (autoConnect == TCPConstants.CLIENT) {
                    mTcpService.startAutoTcp(false, autoConnectIpAddress, port);
                } else if (autoConnect == TCPConstants.SERVER) {
                    mTcpService.startAutoTcp(true, null, port);
                }
//                Logger.e(TAG, name.getClassName());

                /*autoStartConnection();
                if (mBoundTcp && mTcpService != null) {
                    if (mTcpService.getCurrentConnectedTCP() == Constants.SERVER) {
                        setupBadge(mTcpService.getConnectedClientCount(), true, true);
                        sendUdpMessage();
                    } else if (mTcpService.getCurrentConnectedTCP() == Constants.CLIENT) {
                        setupBadge(0, true, false);
                    } else {
                        setupBadge(0, false, false);
                    }
                }*/
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTcpService = null;
            mBoundTcp = false;
        }
    };

    /**
     * Receiver for broadcasts sent by {@link TCPCommunicatorService}.
     */
    private class ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Logger.e(TAG, " >>>>> " + intent.getAction());
            if (intent.getAction().equals(TCPConstants.LOCAL_ACTION)) {
                if (listener != null) {
                    ConnectionData data = bundle.getParcelable(TCPConstants.LOCAL_DATA);
                    listener.onTCPActionCall(data);
                }
            } else if (intent.getAction().equals(TCPConstants.LOCAL_USP_ACTION)) {
                if (listener != null) {
                    listener.onUDPMessageReceived(bundle.getString(TCPConstants.LOCAL_DATA));
                }
            } else if (intent.getAction().equals(TCPConstants.LOCAL_MESSAGE_ACTION)) {
//                localMessageAction(intent);
                Logger.e(TAG, "LOCAL_MESSAGE_ACTION");
                listener.onMessageReceived(bundle.getString(TCPConstants.NEW_MASSAGE));
            }
        }
    }

    public InetAddress systemAddress() {
        return IPTools.getLocalIPv4Address();
    }
}
