package com.node.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.node.Constants;
import com.node.anonchat.MainActivity;
import com.node.anonchat.PreferenceStorage;
import com.node.anonchat.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.leolin.shortcutbadger.ShortcutBadger;

public class SocketIOService extends Service {
    private SocketListener socketListener;
    private Boolean appConnectedToService;
    private Socket mSocket;
    private boolean serviceBinded = false;
    private com.node.anonchat.Database db;
    private final LocalBinder mBinder = new LocalBinder();

    private static final int NOTIFICATION_LED_OFF_MS = 1000;
    private static final int NOTIFICATION_LED_ON_MS = 300;
    private static final int NOTIFICATION_LED_COLOR = 0xff00ff00;

    private int badgeCount = 0;


    public void setAppConnectedToService(Boolean appConnectedToService) {
        this.appConnectedToService = appConnectedToService;
    }

    public void setSocketListener(SocketListener socketListener) {
        this.socketListener = socketListener;
    }

    public class LocalBinder extends Binder {
        public SocketIOService getService() {
            return SocketIOService.this;
        }
    }

    public void setServiceBinded(boolean serviceBinded) {
        this.serviceBinded = serviceBinded;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSocket();
        addSocketHandlers();
        addPongHandler();
        db = com.node.anonchat.Database.getInstance(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeSocketSession();
        Log.i("AppSocketListener","Relaunch Service SocketOServiceIOService");
        sendBroadcast(new Intent("YouWillNeverKillMe"));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return serviceBinded;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void initializeSocket() {
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = false;
            //options.reconnectionAttempts = 200;
            //options.reconnectionDelay = 5000;
            //options.reconnectionDelayMax = 6001;
            //options.timeout = 2000;

            mSocket = IO.socket(Constants.CHAT_SERVER_URL, options);
        } catch (Exception e) {
            Log.e("Error", "Exception in socket creation");
            throw new RuntimeException(e);
        }
    }

    private void closeSocketSession() {
        mSocket.disconnect();
        mSocket.off();
    }

    private void addSocketHandlers() {

        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Intent intent = new Intent(SocketEventConstants.socketConnection);
                intent.putExtra("connectionStatus", true);
                broadcastEvent(intent);
            }
        });

        mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Intent intent = new Intent(SocketEventConstants.socketConnection);
                intent.putExtra("connectionStatus", false);
                broadcastEvent(intent);
            }
        });


        mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Intent intent = new Intent(SocketEventConstants.connectionFailure);
                broadcastEvent(intent);
            }
        });

        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Intent intent = new Intent(SocketEventConstants.connectionFailure);
                broadcastEvent(intent);
            }
        });

        if (PreferenceStorage.shouldDoAutoLogin()) {
            addNewMessageHandler();
            addNewCall();
            addNewContactRequest();
            addNewFileRequest();
            addNewImageRequest();
            addNewBFRequest();
            addNewPttRequest();
            addNewRead();
            addPongHandler();
            userjoined();
            userLeft();
        }
        mSocket.connect();
    }


    public void userjoined() {
        mSocket.off(SocketEventConstants.userJoined);
        mSocket.on(SocketEventConstants.userJoined, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                JSONObject data = (JSONObject) args[0];
                String id;
                try {
                    id = data.getString("id");
                    //db.setContactStatus(id, Constants.CONNECTED);
                    //Put here sent images photos audio notification dosent
                    //doSendPendingMessages(id);
                } catch (JSONException e) {
                    return;
                }
            }
        });
    }

    private void doSendPendingMessages(String address) {
        Cursor cur = db.getReadableDatabase().query("messages", null, "pending=? AND receiver=?", new String[]{"1", address}, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String content = cur.getString(cur.getColumnIndex("content"));
                String type = cur.getString(cur.getColumnIndex("type"));
                //if (sendPending(address, content, type)) {
                    //db.markMessageAsSent(cur.getLong(cur.getColumnIndex("_id")));
                //}
            }
        }
        cur.close();
    }

    private boolean sendPending(String address, String message, String type) {
        JSONObject objIn = new JSONObject();
        try {
            objIn.put("message", message);
            objIn.put("to", address);
            objIn.put("from", PreferenceStorage.getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //send anyway user lefts because we have offline notifications
        /*if (db.getContactStatus(address).equals("connected")) {
            //put here all instances of messages, files, images, ptt ... notification dosent..
            switch (type) {
                case "msg_out":
                    mSocket.emit(SocketEventConstants.messageprivate, objIn);
                    break;
            }

            return true;
        }*/
        return false;
    }


    public void userLeft() {
        mSocket.off(SocketEventConstants.userLeft);
        mSocket.on(SocketEventConstants.userLeft, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                JSONObject data = (JSONObject) args[0];
                String id;
                try {
                    id = data.getString("id");
                    //db.setContactStatus(id, Constants.DISCONNECTED);
                } catch (JSONException e) {
                    return;
                }
            }
        });
    }

    public void addNewMessageHandler() {
        mSocket.off(SocketEventConstants.messageprivate);
        mSocket.on(SocketEventConstants.messageprivate, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String message;
                String from;
                String decrypted;
                try {
                    message = data.getString("message");
                    from = data.getString("from");
                    decrypted = DES_DEncryption(message);
                } catch (JSONException e) {
                    return;
                }

                if (isForeground("com.node.anonchat")) {

                    MediaPlayer mPlayer = MediaPlayer.create(getBaseContext(), R.raw.incoming2);
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            // TODO Auto-generated method stub
                            mp.release();
                        }
                    });
                    int MAX_VOLUME = 100;
                    final float volume = (float) (1 - (Math.log(MAX_VOLUME - 60) / Math.log(MAX_VOLUME)));
                    mPlayer.setVolume(volume, volume);
                    mPlayer.start();

                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, decrypted, sdf.parse(sdf.format(new Date())),1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(SocketEventConstants.messageprivate);
                        intent.putExtra("username", from);
                        intent.putExtra("message", decrypted);
                        broadcastEvent(intent);

                } else {

                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, decrypted, sdf.parse(sdf.format(new Date())), 1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        showNotificaitons(from, "Nuevos mensajes");
                        PreferenceStorage.setCounter(PreferenceStorage.getCounter()+1);
                        ShortcutBadger.applyCount(getApplicationContext(), PreferenceStorage.getCounter());

                }
            }
        });
    }

    public void addNewCall() {

        mSocket.off(SocketEventConstants.call);
        mSocket.on(SocketEventConstants.call
                , new Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {

                        JSONObject data = (JSONObject) args[0];
                        String from;
                        String channel;
                        try {
                            from = data.getString("from");
                            channel = data.getString("channel");
                        } catch (JSONException e) {
                            return;
                        }
                        //place_incoming_call(from, channel);
                    }
                });
    }

    public void addNewRead() {
        mSocket.off(SocketEventConstants.read);
        mSocket.on(SocketEventConstants.read, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String from;
                try {
                    from = data.getString("from");
                } catch (JSONException e) {
                    return;
                }

                if (isForeground("com.node.anonchat")) {

                    //db.markMessagesAsRead(from);
                    Intent intent = new Intent(SocketEventConstants.read);
                    intent.putExtra("username", from);
                    broadcastEvent(intent);
                } else {
                    //db.markMessagesAsRead(from);
                }
            }
        });
    }

    public void addNewContactRequest() {
        mSocket.off(SocketEventConstants.contact);
        mSocket.on(SocketEventConstants.contact, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String id;
                String username;
                String avatar;
                try {
                    id = data.getString("id");
                    username = data.getString("username");
                    avatar = data.getString("avatar");
                } catch (JSONException e) {
                    return;
                }

                MediaPlayer mPlayer = MediaPlayer.create(getBaseContext(), R.raw.pdrwht01);
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // TODO Auto-generated method stub
                        mp.release();
                    }
                });
                int MAX_VOLUME = 100;
                final float volume = (float) (1 - (Math.log(MAX_VOLUME - 60) / Math.log(MAX_VOLUME)));
                mPlayer.setVolume(volume, volume);
                mPlayer.start();

                if (isForeground("com.node.anonchat")) {
                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.RepeatedContact(id)) {
                        db.addContact(id, username, avatar);
                        Intent intent = new Intent(SocketEventConstants.contact);
                        intent.putExtra("username", username);
                        intent.putExtra("id", id);
                        intent.putExtra("avatar", avatar);
                        broadcastEvent(intent);
                    }
                } else {
                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.RepeatedContact(id)) {
                        db.addContact(id, username, avatar);
                        showNotificaitons(username, "Nuevo contacto");
                    }
                }
            }
        });
    }

    public void addNewFileRequest() {
        mSocket.off(SocketEventConstants.eventfile);
        mSocket.on(SocketEventConstants.eventfile, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String message;
                String from;
                try {
                    message = data.getString("message");
                    from = data.getString("from");
                } catch (JSONException e) {
                    return;
                }

                if (isForeground("com.node.anonchat")) {

                    MediaPlayer mPlayer = MediaPlayer.create(getBaseContext(), R.raw.incoming2);
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            // TODO Auto-generated method stub
                            mp.release();
                        }
                    });
                    int MAX_VOLUME = 100;
                    final float volume = (float) (1 - (Math.log(MAX_VOLUME - 60) / Math.log(MAX_VOLUME)));
                    mPlayer.setVolume(volume, volume);
                    mPlayer.start();

                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 0);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(SocketEventConstants.eventfile);
                        intent.putExtra("username", from);
                        intent.putExtra("filename", message);
                        broadcastEvent(intent);
                    }
                } else {
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 0);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        showNotificaitons(from, "Nuevo archivo recibido");
                    }
                }
            }
        });
    }

    public void addNewImageRequest() {
        mSocket.off(SocketEventConstants.eventimage);
        mSocket.on(SocketEventConstants.eventimage, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String message;
                String from;
                try {
                    message = data.getString("message");
                    from = data.getString("from");
                } catch (JSONException e) {
                    return;
                }

                if (isForeground("com.node.anonchat")) {
                    MediaPlayer mPlayer = MediaPlayer.create(getBaseContext(), R.raw.incoming2);
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            // TODO Auto-generated method stub
                            mp.release();
                        }
                    });
                    int MAX_VOLUME = 100;
                    final float volume = (float) (1 - (Math.log(MAX_VOLUME - 60) / Math.log(MAX_VOLUME)));
                    mPlayer.setVolume(volume, volume);
                    mPlayer.start();

                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(SocketEventConstants.eventimage);
                        intent.putExtra("username", from);
                        intent.putExtra("filename", message);
                        broadcastEvent(intent);
                    }
                } else {
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        showNotificaitons(from, "Nueva imagen recibida");
                    }
                }
            }
        });
    }

    public void addNewBFRequest() {
        mSocket.off(SocketEventConstants.eventbad_file);
        mSocket.on(SocketEventConstants.eventbad_file, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String message;
                String from;
                try {
                    message = data.getString("message");
                    from = data.getString("from");
                } catch (JSONException e) {
                    return;
                }

                if (isForeground("com.node.anonchat")) {
                    MediaPlayer mPlayer = MediaPlayer.create(getBaseContext(), R.raw.incoming2);
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            // TODO Auto-generated method stub
                            mp.release();
                        }
                    });
                    int MAX_VOLUME = 100;
                    final float volume = (float) (1 - (Math.log(MAX_VOLUME - 60) / Math.log(MAX_VOLUME)));
                    mPlayer.setVolume(volume, volume);
                    mPlayer.start();

                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 0);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(SocketEventConstants.eventbad_file);
                        intent.putExtra("username", from);
                        intent.putExtra("filename", message);
                        broadcastEvent(intent);
                    }
                } else {
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 0);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        showNotificaitons(from, "bad file recibido");
                    }
                }
            }
        });
    }

    public void addNewPttRequest() {
        mSocket.off(SocketEventConstants.eventptt);
        mSocket.on(SocketEventConstants.eventptt, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                JSONObject data = (JSONObject) args[0];
                String message;
                String from;
                try {
                    message = data.getString("message");
                    from = data.getString("from");
                } catch (JSONException e) {
                    return;
                }

                MediaPlayer mPlayer = MediaPlayer.create(getBaseContext(), R.raw.incoming);
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // TODO Auto-generated method stub
                        mp.release();
                    }
                });
                int MAX_VOLUME = 100;
                final float volume = (float) (1 - (Math.log(MAX_VOLUME - 60) / Math.log(MAX_VOLUME)));
                mPlayer.setVolume(volume, volume);
                mPlayer.start();

                if (isForeground("com.node.anonchat")) {
                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(SocketEventConstants.eventptt);
                        intent.putExtra("username", from);
                        intent.putExtra("filename", message);
                        broadcastEvent(intent);
                    }
                } else {
                    //
                    //notification: check if message have a exact copy of them, if its don't save apply don't care modal
                    //
                    if (!db.repeatedmessage(from, message)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        showNotificaitons(from, "Nuevo audio");
                    }
                }
            }
        });
    }

    public void removeMessageHandler() {
        mSocket.off(SocketEventConstants.messageprivate);
    }

    public void removeRead() {
        mSocket.off(SocketEventConstants.read);
    }

    public void removeNewCall() {
        mSocket.off(SocketEventConstants.call);
    }

    public void removeContactHandler() {
        mSocket.off(SocketEventConstants.contact);
    }

    public void removeFileHandler() {
        mSocket.off(SocketEventConstants.eventfile);
    }

    public void removeImageHandler() {
        mSocket.off(SocketEventConstants.eventimage);
    }

    public void removeBFHandler() {
        mSocket.off(SocketEventConstants.eventbad_file);
    }

    public void removePttHandler() {
        mSocket.off(SocketEventConstants.eventptt);
    }

    public void emit(String event, Object[] args, Ack ack) {
        mSocket.emit(event, args, ack);
    }

    public void emit(String event, Object... args) {
        try {
            mSocket.emit(event, args, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addOnHandler(String event, Emitter.Listener listener) {
        mSocket.on(event, listener);
    }

    public void addPongHandler() {
        mSocket.off(SocketEventConstants.ping);
        mSocket.on(SocketEventConstants.ping, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String ping = "";
                    try {
                        ping = data.getString("beat");
                    } catch (JSONException e) {
                        return;
                    }

                    if (ping.equals("one")) {
                        String sid = PreferenceStorage.getId();
                        if (!(sid == null)) {
                            JSONObject objIn = new JSONObject();
                            try {
                                objIn.put("username", sid);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            mSocket.emit("pong", objIn);
                            Log.i("SocketIOService","pong emmited!");
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException r) {
                    System.err.println("Not enough arguments received.");
                    return;
                }
            }
        });
    }

    public void connect() {
        mSocket.connect();
    }

    public void disconnect() {
        mSocket.disconnect();
    }

    public void restartSocket() {
        mSocket.off();
        mSocket.disconnect();
        addSocketHandlers();
    }

    public void off(String event) {
        mSocket.off(event);
    }

    private void broadcastEvent(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public boolean isSocketConnected() {
        if (mSocket == null) {
            return false;
        }
        return mSocket.connected();
    }

    public void showNotificaitons(String username, String message) {
        Intent toLaunch = new Intent(getApplicationContext(), MainActivity.class);
        toLaunch.putExtra("username", username);
        toLaunch.putExtra("message", message);
        toLaunch.setAction("android.intent.action.MAIN");
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, toLaunch,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(this)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(getApplicationContext().getResources().getColor(R.color.colorAccent))
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setContentTitle("anon")
                .setContentText(message)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_message_white_24dp)
                .build();

        n.ledARGB = NOTIFICATION_LED_COLOR;
        n.ledOnMS = NOTIFICATION_LED_ON_MS;
        n.ledOffMS = NOTIFICATION_LED_OFF_MS;
        n.flags |= Notification.FLAG_SHOW_LIGHTS;


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        notificationManager.notify(0, n);
    }

    public boolean isForeground(String myPackage) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }

    private static String DES_DEncryption(String data) {
        Cipher ecipher;

        // Create a new DES key based on the 8 bytes in the secretKey array
        byte[] keyData = {(byte) 0x51, (byte) 0x33, (byte) 0x18, (byte) 0x79,
                (byte) 0x62, (byte) 0x11, (byte) 0x73, (byte) 0x19};

        try {

            byte[] cipherBytes = fromBase64(data);

            SecretKeySpec key = new SecretKeySpec(keyData, "DES/CFB8/NoPadding");

            // Setup the Initialization vector.
            byte[] iv1 = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

            AlgorithmParameterSpec paramSpec = new IvParameterSpec(keyData);

            ecipher = Cipher.getInstance("DES/CFB8/NoPadding");

            ecipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

            byte[] output = ecipher.doFinal(cipherBytes);

            String PlainrStr = new String(output);

            return PlainrStr;

        } catch (GeneralSecurityException e) {
            return data;
            //throw new RuntimeException(e);
        } catch (IllegalArgumentException b) {
            return data;
        }
    }

    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }
}
