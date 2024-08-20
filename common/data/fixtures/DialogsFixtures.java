package com.node.common.data.fixtures;

import android.database.Cursor;
import android.util.Log;
import android.webkit.URLUtil;

import com.node.anonchat.Database;
import com.node.anonchat.PreferenceStorage;
import com.node.common.data.model.Dialog;
import com.node.common.data.model.Message;
import com.node.common.data.model.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/*
 * Created by Anton Bevza on 07.09.16.
 */
public final class DialogsFixtures extends FixturesData {
    private DialogsFixtures() {
        throw new AssertionError();
    }

    public static ArrayList<Dialog> getDialogs(Database db) {
        ArrayList<Dialog> chats = new ArrayList<>();
        ArrayList<User> users = getUsers(db);


        for (int i = 0; i < users.size(); i++) {
            chats.add(getDialog(users.get(i),db));
        }

        return chats;
    }

    public static Dialog getDialog(User user, Database db) {
        ArrayList<User> users = new ArrayList<>();
        users.add(user);
        return new Dialog(
                user.getId(),
                user.getName(),
                user.getAvatar(),
                users,
                getlastmessage(user.getId(),user.getName(),user.getAvatar(), db),db.unreadCount(user.getId(), PreferenceStorage.getId()));
    }


    private static Message getlastmessage(String address, String name, String avatar, Database db)
    {
        User usermessage = new User(address, name, avatar, false);
        String[] temp = db.getLastMessage(address, PreferenceStorage.getId());
        if(temp != null && temp.length > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date lastdate = new Date();
            try {
                lastdate = sdf.parse(temp[1]);
            } catch (ParseException ex) {
                Log.v("Exception", ex.getLocalizedMessage());
            }

            //check this
            if(temp[0].startsWith("image") || temp[0].startsWith("audio")){
                if(temp[0].startsWith("image")) {
                    Message mensaje = new Message(address, usermessage, "(imagefile)", lastdate);
                    mensaje.setImage(new Message.Image(temp[0].substring(temp[0].indexOf("::") + 1, temp[0].length())));
                    return mensaje;
                }else{
                    Message mensaje = new Message(address, usermessage, "(voice)", lastdate);
                    mensaje.setVoice(new Message.Voice(temp[0],10000));
                    return mensaje;
                }
            }else{
                return new Message(address, usermessage, temp[0], lastdate);
            }

        }else{
            return null;
        }
    }

    private static ArrayList<User> getUsers(Database db) {
        ArrayList<User> users = new ArrayList<>();

        Cursor cursor =  db.getContacts();

        if (cursor.moveToFirst()) {
            do {
                User user;
                String _id = cursor.getString(cursor.getColumnIndex("id"));
                String _name = cursor.getString(cursor.getColumnIndex("name"));
                String _avatar = cursor.getString(cursor.getColumnIndex("avatar"));
                user = getUser(_id, _name, _avatar);
                users.add(user);
            }while(cursor.moveToNext());
        }
        cursor.close();

        return users;
    }

    private static User getUser(String id, String name, String avatar) {
        return new User(
                id,
                name,
                avatar,
                true);
    }

    private static Message getMessage(final Date date) {
        return new Message(
                getRandomId(),
                getUser("","",""),
                getRandomMessage(),
                date);
    }
}
