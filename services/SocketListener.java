package com.node.services;

/**
 * Created by alex_strange on 11/16/15.
 */
public interface SocketListener {
    void onSocketConnected();
    void onSocketDisconnected();
    void onNewMessageReceived(String username,String message);
    void onNewContactReceived(String username,String id, String avatar);

    void onNewImageReceived(String username,String filename);
    void onNewBFReceived(String username,String filename);
    void onNewFileReceived(String username,String filename);
    void onNewPttReceived(String username,String filename);
    void onNewRead(String username);
    void onNewCall(String username, String channel);
}
