package com.droidhubworld.socketio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.droidhubworld.socketio.callback.UDPBroadcastListener;
import com.droidhubworld.socketio.model.ConnectClientDataModel;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPCommunicatorService extends Service {
    private static final String TAG = TCPCommunicatorService.class.getName();
    Handler handler;
    private final IBinder binder = new LocalBinder();
    static TCPCommunicatorService syncService;
    //    private static SharedPref pref;
    private Intent broadcastIntent;
    private int PORT = TCPConstants.PORT;
    private int mCurrentConnectedTCP = -1;
    private static TCPClientCommunicator mTcpClient;
    private static TCPServerCommunicator mTcpServer;
    private UIListenersThread uiListenersThread;

    public static TCPCommunicatorService getTcpService() {
        return syncService;
    }

    public static int getConnectedClientCount() {
        if (mTcpServer != null && mTcpServer.isServerRunning()) {
            return mTcpServer.getClientsCount();
        }
        return 0;
    }

    public static boolean isServerRunning() {
        if (mTcpServer != null) {
            return mTcpServer.isServerRunning();
        }
        return false;
    }

    public int getCurrentConnectedTCP() {
        return mCurrentConnectedTCP;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.syncService = this;
//        pref = new SharedPref(this);
        handler = new Handler();
        /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else {
//            startForeground(1, new Notification());
        }*/
    }

    public class LocalBinder extends Binder {
        public TCPCommunicatorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TCPCommunicatorService.this;
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.droidhubworld.socketio";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Logger.e(TAG, "onStartCommand");
        PORT = intent.getIntExtra("PORT", TCPConstants.PORT);
        uiListenersThread = new UIListenersThread();
        uiListenersThread.execute();
        receiveUdpMessage();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTcpClient != null && mTcpClient.isConnected())
            mTcpClient.stopClient();
        if (mTcpServer != null && mTcpServer.isServerRunning())
            mTcpServer.closeServer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void sendMessage(int clientIndex, JSONObject _obj) {
        try {
            if (mCurrentConnectedTCP == TCPConstants.CLIENT) {
                _obj.put(EnumsAndStatics.MESSAGE_FROM, EnumsAndStatics.MessageTypes.MessageFromClient);
                JSONObject obj = _obj;
                if (mTcpClient.isConnected()) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mTcpClient.sendLn(obj.toString());
                        }
                    }).start();

                }
            } else if (mCurrentConnectedTCP == TCPConstants.SERVER) {
                _obj.put(EnumsAndStatics.MESSAGE_FROM, EnumsAndStatics.MessageTypes.MessageFromServer);
                JSONObject obj = _obj;
                if (mTcpServer.isServerRunning()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (clientIndex != -1) {
                                mTcpServer.sendln(clientIndex, obj.toString());
                            } else {
                                mTcpServer.broadcastln(obj.toString());
                            }
                        }
                    }).start();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void sendMessageToAllClient(JSONObject _obj) {
        try {
            _obj.put(EnumsAndStatics.MESSAGE_FROM, EnumsAndStatics.MessageTypes.MessageFromServer);
            JSONObject obj = _obj;
            if (mTcpServer.isServerRunning()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTcpServer.broadcast(obj.toString(), null);
                    }
                }).start();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToAllClient(JSONObject _obj, String ignorClient) {
        try {
            _obj.put(EnumsAndStatics.MESSAGE_FROM, EnumsAndStatics.MessageTypes.MessageFromServer);
            JSONObject obj = _obj;
            if (mTcpServer.isServerRunning()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTcpServer.broadcast(obj.toString(), ignorClient);
                    }
                }).start();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void startTcp(Boolean isServer, String ip, int port) {

        if (isServer) {
            if (mTcpServer.isServerRunning()) {
                ConnectionData data = new ConnectionData();
                data.setServer(true);
                data.setType(TCPConstants.SERVER_RUNNING);
                data.setMessage(this.getResources().getString(R.string.server_is_running));
                sendToReceiver(data);
                return;
            }

            if (mCurrentConnectedTCP != -1) {
                ConnectionData data = new ConnectionData();
                data.setServer(true);
                data.setType(TCPConstants.CLIENT_IS_RUNNING);
                data.setMessage(this.getResources().getString(R.string.client_on));
                sendToReceiver(data);
                return;
            }

            if (port >= this.PORT) {
                new TcpServerThread().execute(port);
            } else {
                ConnectionData data = new ConnectionData();
                data.setServer(true);
                data.setType(TCPConstants.PORT_LOWER);
                data.setMessage(this.getResources().getString(R.string.low_port, String.valueOf(this.PORT)));
                sendToReceiver(data);
            }

        } else {
            if (mCurrentConnectedTCP == TCPConstants.CLIENT) {
                ConnectionData data = new ConnectionData();
                data.setServer(false);
                data.setType(TCPConstants.CLIENT_CONNECTED);
                data.setMessage(this.getResources().getString(R.string.client_is_connected));
                sendToReceiver(data);
                return;
            }
            if (mCurrentConnectedTCP != -1) {
                ConnectionData data = new ConnectionData();
                data.setServer(false);
                data.setType(TCPConstants.SERVER_IS_RUNNING);
                data.setMessage(this.getResources().getString(R.string.server_on));
                sendToReceiver(data);
                return;
            }

            if (uiListenersThread != null && uiListenersThread.isCancelled()) {
                uiListenersThread = new UIListenersThread();
                uiListenersThread.execute();
            }
            new TcpClientThread(ip, port, false).execute();
        }

    }

    public void startAutoTcp(Boolean isServer, String ip, int port) {

        if (isServer) {
            if (mTcpServer.isServerRunning()) {
                ConnectionData data = new ConnectionData();
                data.setServer(true);
                data.setType(TCPConstants.SERVER_RUNNING);
                data.setMessage(this.getResources().getString(R.string.server_is_running));
                sendToReceiver(data);
                return;
            }

            if (mCurrentConnectedTCP != -1) {
                ConnectionData data = new ConnectionData();
                data.setServer(true);
                data.setType(TCPConstants.CLIENT_IS_RUNNING);
                data.setMessage(this.getResources().getString(R.string.client_on));
                sendToReceiver(data);
                return;
            }

            if (port >= this.PORT) {
                new TcpServerThread().execute(port);
            } else {
                ConnectionData data = new ConnectionData();
                data.setServer(true);
                data.setType(TCPConstants.PORT_LOWER);
                data.setMessage(this.getResources().getString(R.string.low_port, String.valueOf(this.PORT)));
                sendToReceiver(data);
            }

        } else {
            if (mCurrentConnectedTCP == TCPConstants.CLIENT) {
                ConnectionData data = new ConnectionData();
                data.setServer(false);
                data.setType(TCPConstants.CLIENT_CONNECTED);
                data.setMessage(this.getResources().getString(R.string.client_is_connected));
                sendToReceiver(data);
                return;
            }
            if (mCurrentConnectedTCP != -1) {
                ConnectionData data = new ConnectionData();
                data.setServer(false);
                data.setType(TCPConstants.SERVER_IS_RUNNING);
                data.setMessage(this.getResources().getString(R.string.server_on));
                sendToReceiver(data);
                return;
            }

            /*if (uiListenersThread != null && uiListenersThread.isCancelled()) {
                uiListenersThread = new UIListenersThread();
                uiListenersThread.execute();
            }*/
            new TcpClientThread(ip, port, true).execute();
        }

    }

    //------------------------------------TCP Listeners------------------------------------//

    public class UIListenersThread extends AsyncTask<String, ConnectionData, Void> {
        @Override
        protected Void doInBackground(String... params) {
            mTcpServer = new TCPServerCommunicator();
            mTcpClient = new TCPClientCommunicator();


            mTcpServer.setOnServerStartListener(new TCPServerCommunicator.OnServerStart() {
                @Override
                public void serverStarted(int port) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(true);
                    data.setType(TCPConstants.SERVER_START);
                    data.setMessage("Server Started");
                    publishProgress(data);
                    data.setConnectedClient(mTcpServer.getClientsCount());
                    mCurrentConnectedTCP = TCPConstants.SERVER;
                }
            });
            mTcpServer.setOnServerClosedListener(new TCPServerCommunicator.OnServerClose() {
                @Override
                public void serverClosed(int port) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(true);
                    data.setType(TCPConstants.SERVER_CLOSED);
                    data.setMessage("Server Closed");
                    data.setConnectedClient(mTcpServer.getClientsCount());
                    publishProgress(data);
                    mCurrentConnectedTCP = -1;
                }
            });
            mTcpServer.setOnConnectListener(new TCPServerCommunicator.OnConnect() {
                @Override
                public void connected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex, int connectedClient) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(true);
                    data.setType(TCPConstants.CLIENT_CONNECTED);
                    data.setConnectedClient(connectedClient);
                    data.setMessage(TCPCommunicatorService.this.getResources().getString(R.string.client_cons) + localAddress + " Connected, Index: " + clientIndex);
                    publishProgress(data);
                }
            });

            mTcpServer.setOnDisconnectListener(new TCPServerCommunicator.OnDisconnect() {
                @Override
                public void disconnected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(true);
                    data.setType(TCPConstants.CLIENT_DISCONNECTED);
                    data.setMessage(TCPCommunicatorService.this.getResources().getString(R.string.client_cons) + clientIndex + " Disconnected");
                    data.setConnectedClient(mTcpServer.getClientsCount());
                    publishProgress(data);
                }
            });

            mTcpClient.setOnClientConnectionListener(new TCPClientCommunicator.OnClientStatusListener() {
                @Override
                public void connected(Socket socket, String ip, int port) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(false);
                    data.setType(TCPConstants.CLIENT_CONNECTED);
                    data.setMessage("Connected to: " + ip + ":" + port);
                    data.setConnectedTo("Connected to: " + ip + ":" + port);
                    publishProgress(data);
                    mCurrentConnectedTCP = TCPConstants.CLIENT;
                }

                @Override
                public void disconnected(String ip, int port) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(false);
                    data.setType(TCPConstants.CLIENT_DISCONNECTED);
                    data.setMessage("Disconnected: " + ip + ":" + port);
                    data.setConnectedTo("Disconnected to: " + ip + ":" + port);
                    publishProgress(data);
                    mCurrentConnectedTCP = -1;
                }

                @Override
                public void onException(Throwable t) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(false);
                    data.setType(TCPConstants.EXCEPTION);
                    data.setMessage(t.getLocalizedMessage());
                    publishProgress(data);
                }

                @Override
                public void onAutoConnectionProcess(boolean isRunning) {
                    ConnectionData data = new ConnectionData();
                    data.setServer(false);
                    data.setType(TCPConstants.AUTO_CONNECTING);
                    publishProgress(data);
                }
            });

            return null;
        }

        @Override
        protected void onProgressUpdate(ConnectionData... values) {
            super.onProgressUpdate(values);
            ConnectionData data = values[0];
            sendToReceiver(data);
        }
    }

    private void sendToReceiver(ConnectionData data) {
        if (data.isServer()) {
            /*switch (data.getType()) {
                case Constants.SERVER_START:
                    Logger.e(TAG, data.getMessage());

                    break;
                case Constants.SERVER_CLOSED:

                    break;
                case Constants.CLIENT_CONNECTED:

                    break;
                case Constants.CLIENT_DISCONNECTED:

                    break;
                case Constants.EXCEPTION:

                    break;
            }*/
            // Logger.e(TAG, " >>>>>>>>>>>>>>>>>>> " + data.getMessage());

        } else {
            switch (data.getType()) {
                case TCPConstants.CLIENT_CONNECTED:

                    break;
                case TCPConstants.CLIENT_DISCONNECTED:
                    //uiListenersThread.cancel(true);
                    break;
                case TCPConstants.EXCEPTION:

                    break;
            }
            //Logger.e(TAG, " >>>>>>>>>>>>>>>>>>> " + data.getMessage());
        }
        broadcastIntent = new Intent();
        broadcastIntent.setAction(TCPConstants.LOCAL_ACTION);
        Bundle bundle = new Bundle();
        bundle.putParcelable(TCPConstants.LOCAL_DATA, data);
        broadcastIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }
    //------------------------------------[TCP Server Thread]------------------------------------//

    public class TcpServerThread extends AsyncTask<Integer, String, Void> {

        @Override
        protected Void doInBackground(Integer... port) {
            mTcpServer.setOnMessageReceivedListener(new TCPServerCommunicator.OnMessageReceived() {
                @Override
                public void messageReceived(String message, int clientIndex) {
//                    publishProgress("Client " + clientIndex + ": " + message);

                    try {
                        JSONObject messageData = new JSONObject(message);
                        messageData.put(EnumsAndStatics.MESSAGE_FROM_IP, mTcpServer.getClients().get(clientIndex).getSocket().getInetAddress());
                        messageData.put(EnumsAndStatics.CLIENT_INDEX, clientIndex);

                        publishProgress(messageData.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Logger.e(TAG, e.getLocalizedMessage());
                    }


                }
            });

            mTcpServer.startServer(port[0]);
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            messageReceived(values[0]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Logger.e(TAG, "Server Cancelled : ");
            ConnectionData data = new ConnectionData();
            data.setServer(true);
            data.setType(TCPConstants.LOCAL_ERROR_ACTION);
            data.setMessage("Server Cancelled");
            sendToReceiver(data);
        }

    }


    //------------------------------------[TCP Client Thread]------------------------------------//
    public class TcpClientThread extends AsyncTask<String, String, Void> {
        String ipAddress;
        int port;
        boolean autoConnecting;

        public TcpClientThread(String ipAddress, int port, boolean autoConnecting) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.autoConnecting = autoConnecting;
        }

        @Override
        protected Void doInBackground(String... ip) {
            mTcpClient.setOnMessageReceivedListener(new TCPClientCommunicator.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    publishProgress(message);
                }
            });
            try {
                if (ipAddress == null) {
                    mTcpClient.autoConnect(port, autoConnecting);
                } else {
                    mTcpClient.connectClient(ipAddress, port);
                }
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            messageReceived(values[0]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Logger.e(TAG, "Client Cancelled");
            ConnectionData data = new ConnectionData();
            data.setServer(true);
            data.setType(TCPConstants.LOCAL_ERROR_ACTION);
            data.setMessage("Client Cancelled");
            sendToReceiver(data);
        }
    }

    private void messageReceived(String data) {

        broadcastIntent = new Intent();
        broadcastIntent.setAction(TCPConstants.LOCAL_MESSAGE_ACTION);
        Bundle bundle = new Bundle();
        bundle.putString(TCPConstants.NEW_MASSAGE, data);
        broadcastIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

    public void disconnect() {
        if (mCurrentConnectedTCP == TCPConstants.CLIENT)
            mTcpClient.stopClient();
        else if (mCurrentConnectedTCP == TCPConstants.SERVER)
            mTcpServer.closeServer();

        /*if (mTcpClient != null && mTcpClient.isConnected())
            mTcpClient.stopClient();
        if (mTcpServer != null && mTcpServer.isServerRunning())
            mTcpServer.closeServer();*/
    }

    /*
     * UDP BROADCAST
     * */
    private void receiveUdpMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Logger.e(TAG, "receiveUdpMessage PORT: " + PORT);

                    //Keep a socket open to listen to all the UDP trafic that is destined for this port
                    DatagramSocket socket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
                    socket.setBroadcast(true);

                    ConnectionData _data = new ConnectionData();
                    if ((mTcpServer != null && !mTcpServer.isServerRunning())) {
                        _data.setServer(true);
                    } else {
                        _data.setServer(false);
                    }
                    _data.setType(TCPConstants.READY_TO_RECEIVED_BROADCAST);
                    _data.setMessage("Ready to receive broadcast packets!");
                    sendToReceiver(_data);
                    while (true) {
                        Log.e(TAG, "Ready to receive broadcast packets!");
                        //Receive a packet
                        byte[] recvBuf = new byte[26000];
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

                        socket.receive(packet);

                        //Packet received
                        Log.e(TAG, "Packet received from: " + packet.getAddress().getHostAddress());
                        String data = new String(packet.getData()).trim();
                        Log.e(TAG, "Packet received; data: " + data);
                        if ((mTcpServer != null && !mTcpServer.isServerRunning())) {
                            udbMessageReceived(data);
                        }
                        recvBuf = new byte[26000];
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Oops ???? " + ex.getMessage());
                    ConnectionData _data = new ConnectionData();
                    if ((mTcpServer != null && !mTcpServer.isServerRunning())) {
                        _data.setServer(true);
                    } else {
                        _data.setServer(false);
                    }
                    _data.setType(TCPConstants.ERROR_ON_RECEIVED_BROADCAST);
                    _data.setMessage("Oops port is already in use");
                    sendToReceiver(_data);
                }
            }
        }).start();
    }

    private void udbMessageReceived(String data) {
        ConnectClientDataModel clientDataModel = new Gson().fromJson(data, ConnectClientDataModel.class);

        if (clientDataModel != null && clientDataModel.isConnectClient()) {
            if (mCurrentConnectedTCP == TCPConstants.CLIENT) {
                return;
            }
            startTcp(false, clientDataModel.getHostIpAddress(), clientDataModel.getPort());
        } else {
            broadcastIntent = new Intent();
            broadcastIntent.setAction(TCPConstants.LOCAL_USP_ACTION);
            Bundle bundle = new Bundle();
            bundle.putString(TCPConstants.LOCAL_DATA, data);
            broadcastIntent.putExtras(bundle);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        }
    }

    public void connectClientUsingBroadcast(ConnectClientDataModel data, @NonNull UDPBroadcastListener udpBroadcastListener) {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            String dataString = new Gson().toJson(data);

            mTcpClient.sendLn(dataString);

            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = dataString.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(), PORT);
            socket.send(sendPacket);
            if (udpBroadcastListener != null) {
                udpBroadcastListener.obBroadcastSuccess(getClass().getName() + "Broadcast packet sent to: " + getBroadcastAddress().getHostAddress());
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            if (udpBroadcastListener != null) {
                udpBroadcastListener.obBroadcastError(e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            if (udpBroadcastListener != null) {
                udpBroadcastListener.obBroadcastError(e);
            }
        }
    }

    public void connectClientUsingBroadcast(JSONObject data, @NonNull UDPBroadcastListener udpBroadcastListener) {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            String dataString = data.toString();

            mTcpClient.sendLn(dataString);

            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = dataString.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(), PORT);
            socket.send(sendPacket);
            if (udpBroadcastListener != null) {
                udpBroadcastListener.obBroadcastSuccess(getClass().getName() + "Broadcast packet sent to: " + getBroadcastAddress().getHostAddress());
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            if (udpBroadcastListener != null) {
                udpBroadcastListener.obBroadcastError(e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            if (udpBroadcastListener != null) {
                udpBroadcastListener.obBroadcastError(e);
            }
        }
    }

    public void sendBroadcast(ConnectClientDataModel data) {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {

          /*  UdpMessageModel dataObj = new UdpMessageModel();
            dataObj.setMessage("Server Call");
            dataObj.setPort(CommonUtils.PORT);
            dataObj.setTillId(BaseApp.getInstance().sharedPref().getString(SharedPref.TILL_AUTO_ID));
            dataObj.setTillName(BaseApp.getInstance().sharedPref().getString(SharedPref.CURRENT_TILL_NAME));*/
            String dataString = new Gson().toJson(data);

            mTcpClient.sendLn(dataString);

            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = dataString.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(), PORT);
            socket.send(sendPacket);
            System.out.println(getClass().getName() + "Broadcast packet sent to: " + getBroadcastAddress().getHostAddress());
        } catch (IOException e) {
            /*Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(TcpConstant.LOCAL_ACTION_ERROR);
            broadcastIntent.putExtra(TcpConstant.SEND_TO_FAILED_UDP, true);
            broadcastIntent.putExtra(TcpConstant.LOCAL_ERROR, "Network is unreachable");
            sendBroadcast(broadcastIntent);
            pref.setBoolean(SharedPref.SHOW_PORT_ERROR, true);*/
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            /*Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(TcpConstant.LOCAL_ACTION_ERROR);
            broadcastIntent.putExtra(TcpConstant.SEND_TO_FAILED_UDP, true);
            broadcastIntent.putExtra(TcpConstant.LOCAL_ERROR, e.getLocalizedMessage());
            sendBroadcast(broadcastIntent);
            pref.setBoolean(SharedPref.SHOW_PORT_ERROR, true);*/
            Log.e(TAG, "Exception: " + e.getMessage());

        }
    }


    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
}
