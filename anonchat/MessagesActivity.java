package com.node.anonchat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.node.common.data.fixtures.MessagesFixtures;
import com.node.common.data.model.Message;
import com.node.common.data.model.User;
import com.node.utils.AppUtils;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import static com.node.common.data.fixtures.MessagesFixtures.getVoiceMessage;
import static com.node.gost.ulDrawHelper.toBase64;
import static com.node.utils.AppUtils.compress;

/*
 * Created by alex_strange on 04.04.17.
 */
public abstract class MessagesActivity extends AppCompatActivity
        implements MessagesListAdapter.SelectionListener,
        MessagesListAdapter.OnLoadMoreListener {

    public static int TOTAL_MESSAGES_COUNT = 0;

    protected final String senderId = PreferenceStorage.getId();
    protected ImageLoader imageLoader;
    protected MessagesListAdapter<Message> messagesAdapter;

    private Menu menu;
    private int selectionCount;
    private Date lastLoadedDate;

    public Database db;
    public String contactAddress;
    public String contactName;
    public String contactAvatar;
    public User user;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = Database.getInstance(this);

        contactAddress = getIntent().getStringExtra("id");

        contactName = getIntent().getStringExtra("name");

        contactAvatar = getIntent().getStringExtra("avatar");

        user = new User(contactAddress,contactName, contactAvatar, false);

        TOTAL_MESSAGES_COUNT = db.messagesCount(contactAddress,PreferenceStorage.getId());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            lastLoadedDate = sdf.parse(sdf.format(new Date()));
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
        }

        imageLoader = new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url, Object payload) {
                Picasso.with(MessagesActivity.this).load(url).into(imageView);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(messagesAdapter != null) {
            //messagesAdapter.clear();
        }

        if(messagesAdapter.getItemCount() == 0){
        Message temp = getlastmessage(contactAddress,contactName,contactAvatar);
        if(temp!= null) {
            messagesAdapter.addToStart(temp, true);
        }
            }
    }

    static String getRandomId() {
        return Long.toString(UUID.randomUUID().getLeastSignificantBits());
    }

    private Message getlastmessage(String address, String name, String avatar)
    {
        User usermessage;
        String[] temp = db.getLastMessage(contactAddress,PreferenceStorage.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date lastdate = null;
        try {
            lastdate = sdf.parse(sdf.format(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(temp!=null && temp.length > 0) {
            if(address.equals(temp[2])) {
                usermessage = new User(address, name, avatar, false);
                try {
                    lastdate = sdf.parse(temp[1]);
                } catch (ParseException ex) {
                    Log.v("Exception", ex.getLocalizedMessage());
                }
                lastLoadedDate = lastdate;
                if(temp[0].startsWith("image") || temp[0].startsWith("audio")) {
                    if(temp[0].startsWith("image")) {
                        Message mensaje = new Message(getRandomId(), usermessage, "(imagefile)", lastdate);
                        mensaje.setImage(new Message.Image(temp[0].substring(temp[0].indexOf("::") + 1, temp[0].length())));
                        return mensaje;
                    }else{

                        Message mensage = getVoiceMessage(usermessage, temp[0],10000);
                        mensage.setCreatedAt(lastdate);

                        return mensage;
                    }
                }else{
                    return new Message(getRandomId(), usermessage, temp[0], lastdate);
                }
            }else{
                usermessage = new User(PreferenceStorage.getId(), PreferenceStorage.getUsername(), PreferenceStorage.getUserAvatar(), true);
                try {
                    lastdate = sdf.parse(temp[1]);
                } catch (ParseException ex) {
                    Log.v("Exception", ex.getLocalizedMessage());
                }
                lastLoadedDate = lastdate;

                if(temp[0].startsWith("image") || temp[0].startsWith("audio")){
                    if(temp[0].startsWith("image")) {
                        Message mensaje = new Message(getRandomId(), usermessage, "(imagefile)", lastdate);
                        mensaje.setImage(new Message.Image(temp[0].substring(temp[0].indexOf("::") + 1, temp[0].length())));
                        return mensaje;
                    }else{
                      Message message = getVoiceMessage(new User(PreferenceStorage.getId(), PreferenceStorage.getUsername(),
                                PreferenceStorage.getUserAvatar(), false), temp[0],10000);
                        message.setCreatedAt(lastdate);
                        return message;
                    }
                }else{
                    return new Message(getRandomId(), usermessage, temp[0], lastdate);
                }
            }
        }else{
            try {
                lastLoadedDate = sdf.parse(sdf.format(new Date()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.chat_actions_menu, menu);
        onSelectionChanged(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:

                ArrayList<Message> messages = messagesAdapter.getSelectedMessages();

                messagesAdapter.copySelectedMessagesText(this, getMessageStringFormatter(), true);

                //messagesAdapter.deleteSelectedMessages();

                ClipboardManager clipboardManager = (ClipboardManager) getApplicationContext().getSystemService(this.CLIPBOARD_SERVICE);
                ClipData.Item clipitem = clipboardManager.getPrimaryClip().getItemAt(0);
                String text = clipitem.getText().toString();
                db.deleteMessage(contactAddress, PreferenceStorage.getId(), text);

                messagesAdapter.delete(messages);
                break;
            case R.id.action_copy:
                messagesAdapter.copySelectedMessagesText(this, getMessageStringFormatterUser(), true);
                AppUtils.showToast(this, R.string.copied_message, true);
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (selectionCount == 0) {
            super.onBackPressed();
        } else {
            messagesAdapter.unselectAllItems();
        }
        db.setMessagesReaded(contactAddress);
    }

    @Override
    public void onLoadMore(int page, int totalItemsCount) {
        Log.i("TAG", "onLoadMore: " + page + " " + totalItemsCount);
        TOTAL_MESSAGES_COUNT = db.messagesCount(contactAddress,PreferenceStorage.getId());
        if (totalItemsCount < TOTAL_MESSAGES_COUNT) {
            loadMessages();
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        this.selectionCount = count;
        menu.findItem(R.id.action_delete).setVisible(count > 0);
        menu.findItem(R.id.action_copy).setVisible(count > 0);
    }

    protected void loadMessages() {
        new Handler().postDelayed(new Runnable() { //imitation of internet connection
            @Override
            public void run() {
                ArrayList<Message> messages = MessagesFixtures.getBucketMessages(user, PreferenceStorage.getId(), lastLoadedDate, db);
                if(messages!= null && messages.size() > 0) {
                    lastLoadedDate = messages.get(messages.size() - 1).getCreatedAt();
                    messagesAdapter.addToEnd(messages, false);
                }
            }
        }, 1000);
    }

    private MessagesListAdapter.Formatter<Message> getMessageStringFormatter() {
        return new MessagesListAdapter.Formatter<Message>() {
            @Override
            public String format(Message message) {

                String text = message.getText();

                if (text == null) text = "[attachment]";

                if(text.equals("(imagefile)")) {
                    try {
                        return "image::" + toBase64(compress(message.getImageUrl()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(text.equals("(voice)")) return message.getVoice().getUrl();

                return  text;
            }
        };
    }

    private MessagesListAdapter.Formatter<Message> getMessageStringFormatterUser() {
        return new MessagesListAdapter.Formatter<Message>() {
            @Override
            public String format(Message message) {
                //String createdAt = new SimpleDateFormat("MMM d, EEE 'at' h:mm a", Locale.getDefault())
                  //      .format(message.getCreatedAt());

                String text = message.getText();
                if (text == null) text = "[attachment]";

                //return String.format(Locale.getDefault(), "%s: %s (%s)",
                  //    message.getUser().getName(), text, createdAt);
                return  text;
            }
        };
    }

}
