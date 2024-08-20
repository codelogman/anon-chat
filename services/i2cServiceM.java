package com.node.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Base64;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.node.anonchat.MainActivity;
import com.node.anonchat.PreferenceStorage;
import com.node.anonchat.R;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.leolin.shortcutbadger.ShortcutBadger;

import static android.content.Context.NOTIFICATION_SERVICE;

public class i2cServiceM extends FirebaseMessagingService {

    private static final int NOTIFICATION_LED_OFF_MS = 1000;
    private static final int NOTIFICATION_LED_ON_MS = 300;
    private static final int NOTIFICATION_LED_COLOR = 0xff00ff00;

    public i2cServiceM() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();
        com.node.anonchat.Database db = com.node.anonchat.Database.getInstance(getApplicationContext());

        String from = data.get("fromuser");
        String message = data.get("message");
        String name = data.get("name");
        String avatar = data.get("avatar");
        String type = data.get("type");

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {

            switch (type) {

            case "msg_in":
                    db.inserMessage(from, from, DES_DEncryption(message), sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nuevos mensajes]");
                break;
            case "file_in":
                db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nuevo archivo recibido]");
                break;
            case "img_in":
                db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nueva imagen recibida]");
                break;
            case "ptt":
                db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nueva transmision de radio]");
                break;
            case "contact":
                db.addContact(from, name, avatar);
                showNotificaitons("[Nuevo contacto]");
                break;
        }

        } catch (ParseException e) {
            e.printStackTrace();
        }


        PreferenceStorage.setCounter(PreferenceStorage.getCounter()+1);
        ShortcutBadger.applyCount(getApplicationContext(), PreferenceStorage.getCounter());
    }

    @Override
    public void handleIntent(Intent intent) {
        super.handleIntent(intent);

        com.node.anonchat.Database db = com.node.anonchat.Database.getInstance(getApplicationContext());
        String from = intent.getExtras().getString("fromuser");
        String message = intent.getExtras().getString("message");
        String name = intent.getExtras().getString("name");
        String avatar = intent.getExtras().getString("avatar");
        String type = intent.getExtras().getString("type");

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {

            switch (type) {

            case "msg_in":
                db.inserMessage(from, from, DES_DEncryption(message), sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nuevos mensajes]");
                break;
            case "file_in":
                db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nuevo archivo recibido]");
                break;
            case "img_in":
                db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nueva imagen recibida]");
                break;
            case "ptt":
                db.inserMessage(from, from, message, sdf.parse(sdf.format(new Date())), 1);
                showNotificaitons("[Nueva transmision de radio]");
                break;
            case "contact":
                db.addContact(from, name, avatar);
                showNotificaitons("[Nuevo contacto]");
                break;
        }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        PreferenceStorage.setCounter(PreferenceStorage.getCounter()+1);
        ShortcutBadger.applyCount(getApplicationContext(), PreferenceStorage.getCounter());

    }

    public void showNotificaitons(String message) {
        Intent toLaunch = new Intent(getApplicationContext(), MainActivity.class);
        toLaunch.putExtra("message", message);
        toLaunch.setAction("android.intent.action.MAIN");
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, toLaunch,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(this)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(getApplicationContext().getResources().getColor(R.color.colorAccent))
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle("anonymous")
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
