package com.droidhubworld.socketio;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectionData implements Parcelable {
    boolean isServer;
    int type;
    String message;
    String connectedTo;
    int connectedClient;

    public ConnectionData() {
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConnectedTo() {
        return connectedTo;
    }

    public void setConnectedTo(String connectedTo) {
        this.connectedTo = connectedTo;
    }

    public int getConnectedClient() {
        return connectedClient;
    }

    public void setConnectedClient(int connectedClient) {
        this.connectedClient = connectedClient;
    }

    protected ConnectionData(Parcel in) {
        isServer = in.readByte() != 0;
        type = in.readInt();
        message = in.readString();
        connectedTo = in.readString();
        connectedClient = in.readInt();
    }

    public static final Creator<ConnectionData> CREATOR = new Creator<ConnectionData>() {
        @Override
        public ConnectionData createFromParcel(Parcel in) {
            return new ConnectionData(in);
        }

        @Override
        public ConnectionData[] newArray(int size) {
            return new ConnectionData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (isServer ? 1 : 0));
        parcel.writeInt(type);
        parcel.writeString(message);
        parcel.writeString(connectedTo);
        parcel.writeInt(connectedClient);
    }
}
