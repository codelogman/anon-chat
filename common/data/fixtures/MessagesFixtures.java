package com.node.common.data.fixtures;

import android.database.Cursor;
import android.webkit.URLUtil;

import com.node.anonchat.Database;
import com.node.anonchat.PreferenceStorage;
import com.node.common.data.model.Message;
import com.node.common.data.model.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/*
 * Created by troy379 on 12.12.16.
 */
public final class MessagesFixtures extends FixturesData {

    private MessagesFixtures() {
        throw new AssertionError();
    }

    public static Message getImageMessage(User user, String url) {
        Message message = new Message(getRandomId(), user, "(imagefile)");
        message.setImage(new Message.Image(url));
        return message;
    }

    public static Message getVoiceMessage(User user, String url, long duration) {
        Message message = new Message(getRandomId(), user, "(voice)");
        message.setVoice(new Message.Voice(url, duration));
        return message;
    }

    public static Message getTextMessage(User user,String text) {
        return new Message(getRandomId(), user, text);
    }

    static String getRandomId() {
        return Long.toString(UUID.randomUUID().getLeastSignificantBits());
    }


    public static ArrayList<Message> getBucketMessages(User contact, String owner, Date lastloadedDate, Database db) {
        ArrayList<Message> messages = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Cursor cursor = null;
        try {
            cursor = db.getMessages(contact.getId(), owner, sdf.parse(sdf.format(lastloadedDate)));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (cursor.moveToFirst()) {
                do {
                    Message message;
                    String _id = cursor.getString(cursor.getColumnIndex("_id"));
                    String _message = cursor.getString(cursor.getColumnIndex("message"));
                    String _autor = cursor.getString(cursor.getColumnIndex("autor"));
                    Date CreatedAT = null;
                    try {
                        CreatedAT = sdf.parse((cursor.getString(cursor.getColumnIndex("CreatedAt"))));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    int unread = cursor.getInt(cursor.getColumnIndex("unread"));


                    if (_message.startsWith("image") || _message.startsWith("audio")){
                        if(!_autor.equals(PreferenceStorage.getId())) {
                            Cursor dbcontact = db.getContact(_autor);
                            dbcontact.moveToLast();
                            if(_message.startsWith("image")) {
                                message = getImageMessage(new User(_autor, dbcontact.getString(dbcontact.getColumnIndex("name")),
                                        dbcontact.getString(dbcontact.getColumnIndex("avatar")), false), _message.substring(_message.indexOf("::") + 1, _message.length()));
                                message.setCreatedAt(CreatedAT);
                            }else{
                                message = getVoiceMessage(new User(_autor, dbcontact.getString(dbcontact.getColumnIndex("name")),
                                        dbcontact.getString(dbcontact.getColumnIndex("avatar")), false), _message,10000);
                                message.setCreatedAt(CreatedAT);
                            }
                            dbcontact.close();
                        }else{
                            if(_message.startsWith("image")) {
                                message = getImageMessage(new User(_autor, PreferenceStorage.getUsername(),
                                        PreferenceStorage.getUserAvatar(), false), _message.substring(_message.indexOf("::") + 1, _message.length()));
                                message.setCreatedAt(CreatedAT);
                            }else{
                                message = getVoiceMessage(new User(_autor, PreferenceStorage.getUsername(),
                                        PreferenceStorage.getUserAvatar(), false), _message,10000);
                                message.setCreatedAt(CreatedAT);
                            }
                        }

                    }else{

                        if(!_autor.equals(PreferenceStorage.getId())) {
                            Cursor dbcontact = db.getContact(_autor);
                            dbcontact.moveToLast();
                            message = getTextMessage(new User(_autor, dbcontact.getString(dbcontact.getColumnIndex("name")),
                                    dbcontact.getString(dbcontact.getColumnIndex("avatar")), false), _message);
                            message.setCreatedAt(CreatedAT);
                            dbcontact.close();
                        }else{
                            message = getTextMessage(new User(_autor, PreferenceStorage.getUsername(),
                                    PreferenceStorage.getUserAvatar(), false), _message);
                            message.setCreatedAt(CreatedAT);
                        }
                    }

                    messages.add(message);
                }while(cursor.moveToNext());
            }
            cursor.close();

        return messages;
    }
}
