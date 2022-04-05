package com.droidhubworld.socketio;

import com.droidhubworld.socketio.toolsnetwork.IPTools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClientCommunicator {
    private static final String TAG = TCPClientCommunicator.class.getName();
    private OnMessageReceived mMessageListener;
    /*private OnConnect mConnectListener;
    private OnDisconnect mDisconnectListener;*/
    private OnClientStatusListener mOnClientStatusListener;

    private PrintWriter mOut;
    private BufferedReader mIn;

    private volatile boolean mRun = false;
    private static final int mConnectionTimeout = 1000;
    private static Socket mSocket;


    public void connectInSeperateThread(final String ip, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connect(ip, port, false);
            }
        }).start();
    }

    public void connectInSeperateThread(final String ip, final String port) {
        connectInSeperateThread(ip, Integer.valueOf(port));
    }

    public void autoConnect(int port, boolean autoConnecting) {
        InetAddress inetAddress = IPTools.getLocalIPv4Address();
        String range = null;
        if (inetAddress != null) {
            String[] address = inetAddress.getHostAddress().split("\\.");
            range = address[0] + "." + address[1] + "." + address[2] + ".";
            Logger.e(TAG, range);
        }
        if (range == null) {
            return;
        }
        if (autoConnecting && mOnClientStatusListener != null) {
            mOnClientStatusListener.onAutoConnectionProcess(true);
        }
        for (int i = 1; i <= 255; i++) {
            String ip = range + i;
            try {
                if (i == 255) {
                    autoConnecting = false;
                }
                connect(ip, port, autoConnecting);
            } catch (Exception e) {
                if (autoConnecting && mOnClientStatusListener != null) {
                    mOnClientStatusListener.onAutoConnectionProcess(false);
                }
                Logger.e(TAG, ip + " : " + e.getMessage());
            }
        }
    }

    public void connectClient(String ip, int port) {
        connect(ip, port, false);
    }

    private void connect(String ip, int port, boolean autoConnecting) {
        mRun = true;
        String serverMessage;
        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress(InetAddress.getByName(ip), port), mConnectionTimeout);
            if (mOnClientStatusListener != null)
                mOnClientStatusListener.connected(mSocket, ip, port);

            try {
                mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
                while (mRun) {
                    serverMessage = mIn.readLine();
                    if (serverMessage != null && mMessageListener != null)
                        mMessageListener.messageReceived(serverMessage);
                    if (serverMessage == null)
                        mRun = false;
                }
            } finally {
                mSocket.close();
                if (mOnClientStatusListener != null)
                    mOnClientStatusListener.disconnected(ip, port);
            }

        } catch (Exception e) {
            //e.printStackTrace();
            if (mSocket != null && mSocket.isClosed()) {
                return;
            }
            if (!autoConnecting && mOnClientStatusListener != null) {
                mOnClientStatusListener.onException(e);
                mOnClientStatusListener.onAutoConnectionProcess(false);
            }
        }
    }

    public String getIpFromDns(String address) throws UnknownHostException {
        return InetAddress.getByName(address).getHostAddress();
    }

    public void send(String message) {
        if (mOut != null && !mOut.checkError()) {
            mOut.print(message);
            mOut.flush();
        }
    }

    public void sendLn(String message) {
        if (mOut != null && !mOut.checkError()) {
            mOut.println(message);
            mOut.flush();
        }
    }

    public void stopClient() {
        mRun = false;
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Boolean isConnected() {
        return mSocket != null ? mSocket.isConnected() : false;
    }


//----------------------------------------[Listeners]----------------------------------------//

    public void setOnMessageReceivedListener(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    /* public void setOnConnectListener(OnConnect listener) {
        mConnectListener = listener;
    }

    public void setOnDisconnectListener(OnDisconnect listener) {
        mDisconnectListener = listener;
    }*/
    public void setOnClientConnectionListener(OnClientStatusListener listener) {
        mOnClientStatusListener = listener;
    }


//----------------------------------------[Interfaces]----------------------------------------//

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

    /*public interface OnConnect {
       public void connected(Socket socket, String ip, int port);
   }

   public interface OnDisconnect {
       public void disconnected(String ip, int port);
   }*/
    public interface OnClientStatusListener {
        public void connected(Socket socket, String ip, int port);

        public void disconnected(String ip, int port);

        public void onException(Throwable t);

        public void onAutoConnectionProcess(boolean isRunning);
    }
}
