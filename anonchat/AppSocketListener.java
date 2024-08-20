package com.node.anonchat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.node.services.SocketEventConstants;
import com.node.services.SocketIOService;
import com.node.services.SocketListener;

import io.socket.client.Ack;
import io.socket.emitter.Emitter;

/**
 * Created by alex_strange on 15/10/2016
 */
public class AppSocketListener implements SocketListener{
    private static AppSocketListener sharedInstance;
    private SocketIOService socketServiceInterface;
    public SocketListener activeSocketListener;

    public void setActiveSocketListener(SocketListener activeSocketListener) {
        this.activeSocketListener = activeSocketListener;
        if (socketServiceInterface != null && socketServiceInterface.isSocketConnected()){
            onSocketConnected();
        }
    }

    public static AppSocketListener getInstance(){
        if (sharedInstance==null){
            sharedInstance = new AppSocketListener();
        }
        return sharedInstance;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            socketServiceInterface = ((SocketIOService.LocalBinder)service).getService();
            socketServiceInterface.setServiceBinded(true);
            socketServiceInterface.setSocketListener(sharedInstance);
            if (socketServiceInterface.isSocketConnected()){
                onSocketConnected();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            socketServiceInterface.setServiceBinded(false);
            socketServiceInterface=null;
            onSocketDisconnected();
        }
    };


    public void initialize(){
        Intent intent = new Intent(AppContext.getAppContext(), SocketIOService.class);
        AppContext.getAppContext().startService(intent);
        AppContext.getAppContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        //covenant: bind call service too here

        //finalize here
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(socketConnectionReceiver, new IntentFilter(SocketEventConstants.
                        socketConnection));
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(connectionFailureReceiver, new IntentFilter(SocketEventConstants.
                        connectionFailure));
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newMessageReceiver, new IntentFilter(SocketEventConstants.messageprivate));

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newContactReceiver, new IntentFilter(SocketEventConstants.contact));

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newFileReceiver, new IntentFilter(SocketEventConstants.eventfile));

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newImageReceiver, new IntentFilter(SocketEventConstants.eventimage));

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newBFReceiver, new IntentFilter(SocketEventConstants.eventbad_file));

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newPttReceiver, new IntentFilter(SocketEventConstants.eventptt));

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newRead, new IntentFilter(SocketEventConstants.read));

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                registerReceiver(newCall, new IntentFilter(SocketEventConstants.call));
    }

    private BroadcastReceiver socketConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           boolean connected = intent.getBooleanExtra("connectionStatus",false);
            if (connected){
                Log.i("AppSocketListener","Socket connected");
                onSocketConnected();
            }
            else{
                Log.i("AppSocketListener","Socket Disconnected");
                onSocketDisconnected();
            }
        }
    };

    private BroadcastReceiver connectionFailureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast toast = Toast.
                    makeText(AppContext.getAppContext(), "Please check your network connection",
                            Toast.LENGTH_SHORT);
            toast.show();
        }
    };

    private BroadcastReceiver newMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String message = intent.getStringExtra("message");
            onNewMessageReceived(userName,message);
        }
    };

    private BroadcastReceiver newRead = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            onNewRead(userName);
        }
    };

    private BroadcastReceiver newCall = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String channel = intent.getStringExtra("channel");
            onNewCall(userName, channel);
        }
    };

    private BroadcastReceiver newFileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String filename = intent.getStringExtra("filename");
            onNewFileReceived(userName,filename);
        }
    };

    private BroadcastReceiver newImageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String filename = intent.getStringExtra("filename");
            onNewImageReceived(userName,filename);
        }
    };

    private BroadcastReceiver newBFReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String filename = intent.getStringExtra("filename");
            onNewBFReceived(userName,filename);
        }
    };

    private BroadcastReceiver newPttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String filename = intent.getStringExtra("filename");
            onNewPttReceived(userName,filename);
        }
    };

    private BroadcastReceiver newContactReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String userName = intent.getStringExtra("username");
            String id = intent.getStringExtra("id");
            String avatar = intent.getStringExtra("avatar");
            onNewContactReceived(userName,id,avatar);
        }
    };

    public void destroy(){
        socketServiceInterface.setServiceBinded(false);
        AppContext.getAppContext().unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(socketConnectionReceiver);
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newMessageReceiver);
        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newContactReceiver);

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newFileReceiver);

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newImageReceiver);

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newBFReceiver);

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newPttReceiver);

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newRead);

        LocalBroadcastManager.getInstance(AppContext.getAppContext()).
                unregisterReceiver(newCall);

    }

    @Override
    public void onSocketConnected() {
        if (activeSocketListener != null) {
            activeSocketListener.onSocketConnected();
        }
    }

    @Override
    public void onSocketDisconnected() {
        if (activeSocketListener != null) {
            activeSocketListener.onSocketDisconnected();
        }
    }

    @Override
    public void onNewMessageReceived(String username, String message) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewMessageReceived(username, message);
        }
    }

    @Override
    public void onNewRead(String username) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewRead(username);
        }
    }

    public void onNewCall(String username, String channel) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewCall(username, channel);
        }
    }

    @Override
    public void onNewContactReceived(String username, String id, String avatar) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewContactReceived(username, id, avatar);
        }
    }

    @Override
    public void onNewImageReceived(String username, String filename) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewImageReceived(username, filename);
        }
    }

    @Override
    public void onNewBFReceived(String username, String filename) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewBFReceived(username, filename);
        }
    }

    @Override
    public void onNewFileReceived(String username, String filename) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewFileReceived(username, filename);
        }
    }

    @Override
    public void onNewPttReceived(String username, String filename) {
        if (activeSocketListener != null) {
            activeSocketListener.onNewPttReceived(username, filename);
        }
    }

    public void addOnHandler(String event,Emitter.Listener listener){
        socketServiceInterface.addOnHandler(event, listener);
    }

    public void emit(String event,Object[] args,Ack ack){
        socketServiceInterface.emit(event, args, ack);
    }

    public void emit (String event,Object... args){
        socketServiceInterface.emit(event, args);
    }

     void connect(){
        socketServiceInterface.connect();
    }

    public void disconnect(){
        socketServiceInterface.disconnect();
    }
    public void off(String event) {
        if (socketServiceInterface != null) {
            socketServiceInterface.off(event);
        }
    }

    public boolean isSocketConnected(){
        if (socketServiceInterface == null){
            return false;
        }
        return socketServiceInterface.isSocketConnected();
    }

    public void setAppConnectedToService(Boolean status){
        if ( socketServiceInterface != null){
            socketServiceInterface.setAppConnectedToService(status);
        }
    }

    public void restartSocket(){
        if (socketServiceInterface != null){
         socketServiceInterface.restartSocket();
        }
    }
    public void addNewMessageHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewMessageHandler();
        }
    }

    public void addNewRead(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewRead();
        }
    }

    public void addNewCall(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewCall();
        }
    }

    public void userJoined(){
        if (socketServiceInterface != null){
            socketServiceInterface.userjoined();
        }
    }

    public void userLeft(){
        if (socketServiceInterface != null){
            socketServiceInterface.userLeft();
        }
    }

    public void addNewContactRequest(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewContactRequest();
        }
    }

    public void addNewFileRequest(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewFileRequest();
        }
    }

    public void addNewImageRequest(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewImageRequest();
        }
    }

    public void addNewBFRequest(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewBFRequest();
        }
    }

    public void addNewPttRequest(){
        if (socketServiceInterface != null){
            socketServiceInterface.addNewPttRequest();
        }
    }

    public void removeNewMessageHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeMessageHandler();
        }
    }

    public void removeNewRead(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeRead();
        }
    }

    public void removeNewCall(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeNewCall();
        }
    }

    public void removeNewContactHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeContactHandler();
        }
    }

    public void removeNewFileHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeFileHandler();
        }
    }

    public void removeNewImageHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeImageHandler();
        }
    }

    public void removeNewBFHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.removeBFHandler();
        }
    }

    public void removeNewPttHandler(){
        if (socketServiceInterface != null){
            socketServiceInterface.removePttHandler();
        }
    }

    //nothing happens ;(...
    public void signOutUser(){
        AppSocketListener.getInstance().disconnect();
        removeNewMessageHandler();
        removeNewRead();
        removeNewCall();
        removeNewContactHandler();

        removeNewFileHandler();
        removeNewImageHandler();
        removeNewBFHandler();
        removeNewPttHandler();

        AppSocketListener.getInstance().connect();
    }
}
