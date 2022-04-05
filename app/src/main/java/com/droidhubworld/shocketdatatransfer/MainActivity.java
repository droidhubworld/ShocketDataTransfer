package com.droidhubworld.shocketdatatransfer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.droidhubworld.socketio.ConnectionData;
import com.droidhubworld.socketio.Logger;
import com.droidhubworld.socketio.TCPCommunicator;
import com.droidhubworld.socketio.TCPCommunicatorService;
import com.droidhubworld.socketio.TCPConstants;
import com.droidhubworld.socketio.callback.TCPCommunicatorListener;
import com.droidhubworld.socketio.callback.UDPBroadcastListener;
import com.droidhubworld.socketio.model.ConnectClientDataModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TCPCommunicatorListener, UDPBroadcastListener {
    private static final String TAG = MainActivity.class.getName();
    TextView systemIp, connectedClients, connectionMessage;
    EditText etServerIp;
    View connectionProgress;
    private final static int PORT = 49928;
    public boolean mBoundTcp = false;
    TCPCommunicator tcpCommunicator;
    InetAddress inetAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        connectionProgress = findViewById(R.id.connection_progress);
        systemIp = findViewById(R.id.tv_systemIp);
        connectedClients = findViewById(R.id.tv_connectedClients);
        connectionMessage = findViewById(R.id.tv_message);
        etServerIp = findViewById(R.id.et_server_ip);
        findViewById(R.id.btnStartService).setOnClickListener(this);
        findViewById(R.id.btnConnectToServer).setOnClickListener(this);
        findViewById(R.id.btnStop).setOnClickListener(this);
        findViewById(R.id.btnConnectClient).setOnClickListener(this);
        findViewById(R.id.btnSendTestMessage).setOnClickListener(this);

        tcpCommunicator = new TCPCommunicator.Builder(this, PORT)
                .setAutoConnectIpAddress("192.168.43.108")// if not set it will check over network and trying to connect is server is started on any IP
                .setAutoConnect(TCPConstants.CLIENT)// CLIENT for auto connect and SERVER for auto start server
                .setOnCallBackListener(this)
                .build();

        inetAddress = tcpCommunicator.systemAddress();
        if (inetAddress != null) {
            systemIp.setText("System Address : " + inetAddress.getHostAddress());
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStop:
                tcpCommunicator.disconnect();
                break;
            case R.id.btnConnectClient:
                if (inetAddress != null) {

                    ConnectClientDataModel connectClientDataModel = new ConnectClientDataModel();
                    connectClientDataModel.setPort(PORT);
                    connectClientDataModel.setHostIpAddress(inetAddress.getHostAddress());
                    connectClientDataModel.setConnectClient(true);
                    connectClientDataModel.setExtraData("Hello This Is Test Message");

                    tcpCommunicator.connectClientUsingUDP(connectClientDataModel);
                }
                break;
            case R.id.btnSendTestMessage:
                if (inetAddress != null) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("DATA", "Hi, This Is Test Message From client");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    tcpCommunicator.sendMessage( jsonObject);
                }
                break;
            case R.id.btnStartService:
                tcpCommunicator.startServer();
                break;
            case R.id.btnConnectToServer:
                if (etServerIp.getText().toString().trim().length() > 0) {
                    tcpCommunicator.connectToServer(etServerIp.getText().toString().trim(), PORT);
                } else {
                    Toast.makeText(this, "Enter IP Address", Toast.LENGTH_SHORT).show();
                    etServerIp.setError("Enter IP Address");
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBoundTcp = getApplicationContext().bindService(new Intent(getApplicationContext(), TCPCommunicatorService.class), tcpCommunicator.getServiceConnection(), Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(tcpCommunicator.getReceiver(), tcpCommunicator.getIntentFilter());
    }

    @Override
    protected void onStop() {
        if (mBoundTcp) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            // unbindService(tcpCommunicator.getServiceConnection());
            mBoundTcp = false;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tcpCommunicator.getReceiver());
        super.onStop();
    }

    @Override
    public void onTCPActionCall(ConnectionData data) {
        if (data.isServer()) {
            switch (data.getType()) {
                case TCPConstants.SERVER_START:
                    Logger.e(TAG, data.getMessage());
                    connectionProgress.setVisibility(View.GONE);

                    break;
                case TCPConstants.SERVER_CLOSED:
                    //connectionMessage.setText(null);
                    break;
                case TCPConstants.CLIENT_CONNECTED:

                    break;
                case TCPConstants.CLIENT_DISCONNECTED:

                    break;
                case TCPConstants.EXCEPTION:
                    connectionProgress.setVisibility(View.GONE);

                    break;
            }
            connectedClients.setText("Connected Clients : " + data.getConnectedClient());
            connectionMessage.append("\n" + data.getMessage());
        } else {
            switch (data.getType()) {
                case TCPConstants.CLIENT_CONNECTED:
                    connectionProgress.setVisibility(View.GONE);

                    break;
                case TCPConstants.CLIENT_DISCONNECTED:
                    //connectionMessage.setText(null);
                    break;
                case TCPConstants.EXCEPTION:
                    connectionProgress.setVisibility(View.GONE);
                    break;
                case TCPConstants.AUTO_CONNECTING:
                    connectionProgress.setVisibility(View.VISIBLE);
                    break;
            }
            connectedClients.setText(data.getConnectedTo());

            connectionMessage.append("\n" + data.getMessage());
        }
    }

    @Override
    public void onMessageReceived(String data) {
        Toast.makeText(this, ">>>>" + data, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUDPMessageReceived(String data) {
        Toast.makeText(this, ">>> " + data, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void obBroadcastSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void obBroadcastError(Throwable t) {
        Toast.makeText(this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }
}