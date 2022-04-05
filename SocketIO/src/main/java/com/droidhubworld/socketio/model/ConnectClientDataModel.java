package com.droidhubworld.socketio.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectClientDataModel implements Parcelable {
    int port;
    String hostIpAddress;
    String extraData;//pass it as JSON Object OR normal string message
    boolean connectClient;

    public ConnectClientDataModel() {
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    public void setHostIpAddress(String hostIpAddress) {
        this.hostIpAddress = hostIpAddress;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public boolean isConnectClient() {
        return connectClient;
    }

    public void setConnectClient(boolean connectClient) {
        this.connectClient = connectClient;
    }

    protected ConnectClientDataModel(Parcel in) {
        port = in.readInt();
        hostIpAddress = in.readString();
        extraData = in.readString();
        connectClient = in.readByte() != 0;
    }

    public static final Creator<ConnectClientDataModel> CREATOR = new Creator<ConnectClientDataModel>() {
        @Override
        public ConnectClientDataModel createFromParcel(Parcel in) {
            return new ConnectClientDataModel(in);
        }

        @Override
        public ConnectClientDataModel[] newArray(int size) {
            return new ConnectClientDataModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(port);
        parcel.writeString(hostIpAddress);
        parcel.writeString(extraData);
        parcel.writeByte((byte) (connectClient ? 1 : 0));
    }
}
