/*
 * Chat.onion - P2P Instant Messenger
 *
 * http://play.google.com/store/apps/details?id=onion.chat
 * http://onionapps.github.io/Chat.onion/
 * http://github.com/onionApps/Chat.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package com.node.anonchat;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Database extends SQLiteOpenHelper {

    private static Database instance;
    private Context context;

    public Database(Context context) {
        super(context, "cnb.db", null, 1);
        this.context = context;
    }

    public static Database getInstance(Context context) {
        if (instance == null)
            instance = new Database(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE contacts (  _id INTEGER PRIMARY KEY, id TEXT UNIQUE, name TEXT, avatar TEXT DEFAULT 'https://firebasestorage.googleapis.com/v0/b/anode-ed2b7.appspot.com/o/storage%2Fanon.jpg?alt=media&token=e34c1f1a-0e5f-4124-9939-c62a6e296653', key_B TEXT DEFAULT 'none')");

        // index contacts by which tab they should appear on and by their names
        db.execSQL("CREATE INDEX contactindex ON contacts ( id, name, avatar )");

        // message table
        // _id: primary key
        // sender: 16 char onion address
        // receiver: 16 char onion address
        // content: message contents
        // time: message timestamp
        // pending: 1 if it is an outgoing message that still has to be sent, 0 if the message has already been sent, or if it has been received from someone else
        db.execSQL("CREATE TABLE messages ( _id TEXT, message TEXT, autor TEXT, CreatedAt TEXT, unread INTEGER DEFAULT 0)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // messages


    public synchronized Cursor getMessages(String a, String b, Date lastdate) {
        String DateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastdate);
        return getReadableDatabase().rawQuery("SELECT * FROM messages WHERE (((_id=? AND autor=?) OR (_id=? AND autor=?)) AND (datetime(?) > datetime(CreatedAt))) ORDER BY datetime(CreatedAt) DESC LIMIT 20", new String[]{a, b, a, a, DateandTime});
    }

    public synchronized String [] getLastMessage(String sender, String receiver) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM messages WHERE ((_id=? AND autor=?) OR (_id=? AND autor=?)) ORDER BY datetime(CreatedAt) DESC LIMIT 1", new String[]{sender, receiver, sender, sender});
        String[] temp = new String[3];
        if (cursor.getCount()>0) {
            cursor.moveToLast();
            String message = cursor.getString(cursor.getColumnIndex("message"));
            String date = cursor.getString(cursor.getColumnIndex("CreatedAt"));
            String autor = cursor.getString(cursor.getColumnIndex("autor"));
            temp[0] = message;
            temp[1] = date;
            temp[2] = autor;
        }else{
            temp = null;
        }
        cursor.close();
        return temp;
    }

    public synchronized int messagesCount(String sender, String receiver) {
        Cursor cursor = getReadableDatabase().query("messages", null, "((_id=? AND autor=?) OR (_id=? AND autor=?))", new String[]{sender, receiver, sender, sender}, null, null, null);
        int ammount = cursor.getCount();
        cursor.close();
        return ammount;
    }

    public synchronized  void inserMessage(String bucket, String autor, String message, Date date, int unread){
        long n;
        String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        {
            ContentValues v = new ContentValues();
            v.put("_id", bucket);
            v.put("autor", autor);
            v.put("message", message);
            v.put("CreatedAt", currentDateandTime);
            v.put("unread", unread);
            n = getWritableDatabase().insert("messages", null, v);
        }

    }

    public synchronized Cursor getContact(String id) {
        return getReadableDatabase().rawQuery("SELECT * FROM contacts WHERE (id=?)", new String[]{id});
    }

    public synchronized Cursor getContacts() {
        return getReadableDatabase().rawQuery("SELECT * FROM contacts", null);
    }

    public synchronized boolean RepeatedContact(String id) {
        Cursor cursor = getReadableDatabase().query("contacts", null, "id=?", new String[]{id}, null, null, null);
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }

    public synchronized boolean addContact(String id, String name, String avatar) {
        long n = 0;
        if(!RepeatedContact(id)) {
            {
                ContentValues v = new ContentValues();
                v.put("id", id);
                v.put("name", name);
                if(avatar.equals("none")) {
                    v.put("avatar", "https://firebasestorage.googleapis.com/v0/b/anode-ed2b7.appspot.com/o/storage%2Fanon.jpg?alt=media&token=e34c1f1a-0e5f-4124-9939-c62a6e296653");
                }else{
                    v.put("avatar", avatar);
                }
                n = getWritableDatabase().insert("contacts", null, v);
            }
            return true;
        }
        return false;
    }

    public synchronized String getContactName(String id) {
        String ret = "";
        Cursor cursor = getReadableDatabase().query("contacts", new String[]{"name"}, "id=?", new String[]{id}, null, null, null);
        if (cursor.moveToNext()) {
            ret = cursor.getString(0);
        }
        if (ret == null) ret = "";
        cursor.close();
        return ret;
    }

    //check if message is repeated, meaning exact!
    public synchronized boolean repeatedmessage(String id, String message) {
        Cursor cursor = getReadableDatabase().query("messages", null, "_id=? AND message=?", new String[]{id, message}, null, null, null);
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }

    public synchronized int unreadCount(String sender, String receiver) {
        Cursor cursor = getReadableDatabase().query("messages", null, "(((_id=? AND autor=?) OR (_id=? AND autor=?)) AND (unread = 1))", new String[]{sender, receiver, sender, sender}, null, null, null);
        int ammount = cursor.getCount();
        cursor.close();
        return ammount;
    }

    public synchronized void setMessagesReaded(String id) {
            ContentValues v = new ContentValues();
            v.put("unread", 0);
            getWritableDatabase().update("messages", v, "_id=?", new String[]{id});
        }

    public synchronized boolean deleteMessage(String sender, String receiver,String message) {
        int n = getWritableDatabase().delete("messages", "(((_id=? AND autor=?) OR (_id=? AND autor=?)) AND (message = ?))", new String[]{sender, receiver, sender, sender, message});
        return n > 0;
    }

}
