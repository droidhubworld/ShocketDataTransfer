package com.droidhubworld.socketio;

public class EnumsAndStatics {
    public enum MessageTypes {
        MessageFromServer, MessageFromClient
    }

    public static final String MESSAGE_FROM = "messageFrom";
    public static final String MESSAGE_FROM_IP = "messageFromIp";
    public static final String CLIENT_INDEX = "clientIndex";

    public static MessageTypes getMessageTypeByString(String messageInString) {
        if (messageInString.equals(MessageTypes.MessageFromServer.toString()))
            return MessageTypes.MessageFromServer;
        if (messageInString.equals(MessageTypes.MessageFromClient.toString()))
            return MessageTypes.MessageFromClient;

        return null;
    }
}