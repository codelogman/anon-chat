package com.node.anonchat.styled;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.devlomi.record_view.OnBasketAnimationEnd;
import com.devlomi.record_view.OnRecordClickListener;
import com.devlomi.record_view.OnRecordListener;
import com.devlomi.record_view.RecordButton;
import com.devlomi.record_view.RecordView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.node.anonchat.AppSocketListener;
import com.node.anonchat.MessageActivity;
import com.node.anonchat.MessagesActivity;
import com.node.anonchat.PreferenceStorage;
import com.node.anonchat.R;
import com.node.anonchat.TypefaceSpan;
import com.node.common.data.fixtures.MessagesFixtures;
import com.node.common.data.model.Message;
import com.node.common.data.model.User;
import com.node.gost.ulDrawHelper;
import com.node.holders.IncomingVoiceMessageViewHolder;
import com.node.holders.OutcomingVoiceMessageViewHolder;
import com.node.ptt.Helper;
import com.node.services.SocketEventConstants;
import com.node.services.SocketListener;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.node.common.data.fixtures.MessagesFixtures.getImageMessage;
import static com.node.common.data.fixtures.MessagesFixtures.getTextMessage;
import static com.node.common.data.fixtures.MessagesFixtures.getVoiceMessage;
import static com.node.gost.ulDrawHelper.DES_DEncryption;
import static com.node.gost.ulDrawHelper.toBase64;
import static com.node.utils.AppUtils.compress;

public class StyledMessagesActivity extends MessagesActivity
        implements MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        DateFormatter.Formatter,
        MessagesListAdapter.OnMessageClickListener<Message>,
        SocketListener, MessageHolders.ContentChecker<Message> {

    public static void open(Context context, String id, String name, String avatar) {

        Intent myIntent = new Intent(context, StyledMessagesActivity.class);
        myIntent.putExtra("id", id);
        myIntent.putExtra("name", name);
        myIntent.putExtra("avatar", avatar);
        context.startActivity(myIntent);
    }

    private MessagesList messagesList;
    private FirebaseAuth mAuth;

    private String currentOutFile;
    private long duration;
    private MediaRecorder myAudioRecorder;

    private static final byte CONTENT_TYPE_VOICE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_styled_messages);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SpannableString s = new SpannableString("member: " + user.getName());
        s.setSpan(new TypefaceSpan(this, "Inconsolata-Regular.ttf"), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        (StyledMessagesActivity.this).getSupportActionBar().setTitle(s);


        mAuth = FirebaseAuth.getInstance();


        //record audio
        final RecordView recordView = (RecordView) findViewById(R.id.record_view);
        RecordButton recordButton = (RecordButton) findViewById(R.id.record_button);

        //IMPORTANT
        recordButton.setRecordView(recordView);

        recordView.setVisibility(View.GONE);

        recordView.setCustomSounds(R.raw.record_start,R.raw.record_finished,R.raw.record_error);

        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                //Start Recording..
                recordView.setVisibility(View.VISIBLE);
                Recording();
                //Toast.makeText(StyledMessagesActivity.this, "onStart", Toast.LENGTH_SHORT).show();
                Log.d("RecordView", "onStart");
            }

            @Override
            public void onCancel() {
                //On Swipe To Cancel
                stopRecording(true);
                Toast.makeText(StyledMessagesActivity.this, "Canceled ...", Toast.LENGTH_SHORT).show();
                Log.d("RecordView", "onCancel");

            }

            @Override
            public void onFinish(long recordTime) {
                //Stop Recording..
                String time = getHumanTimeText(recordTime);
                duration =recordTime;
                stopRecording(false);
                uploadPtt(currentOutFile);
                recordView.setVisibility(View.GONE);
                //Toast.makeText(StyledMessagesActivity.this, "onFinish " + time, Toast.LENGTH_SHORT).show();
                Log.d("RecordView", "onFinish");

                Log.d("RecordTime", time);
            }

            @Override
            public void onLessThanSecond() {
                //When the record time is less than One Second
                stopRecording(true);
                recordView.setVisibility(View.GONE);
                //Toast.makeText(StyledMessagesActivity.this, "onLessThanSecond", Toast.LENGTH_SHORT).show();
                Log.d("RecordView", "onLessThanSecond");
            }
        });

        recordView.setLessThanSecondAllowed(false);


        recordView.setSlideToCancelText("Slide To Cancel");

        //ListenForRecord must be false ,otherwise onClick will not be called
        recordButton.setOnRecordClickListener(new OnRecordClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(StyledMessagesActivity.this, "RECORD BUTTON CLICKED", Toast.LENGTH_SHORT).show();
                recordView.setVisibility(View.VISIBLE);
                Log.d("RecordButton","RECORD BUTTON CLICKED");
            }
        });

        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                recordView.setVisibility(View.GONE);
                //Toast.makeText(StyledMessagesActivity.this, "onAnimationEnd", Toast.LENGTH_SHORT).show();
                Log.d("RecordView", "Basket Animation Finished");
            }
        });

        recordView.setCancelBounds(8);//dp
        //record audio end

        messagesList = (MessagesList) findViewById(R.id.messagesList);
        initAdapter();

        MessageInput input = (MessageInput) findViewById(R.id.input);

        AppSocketListener.getInstance().setActiveSocketListener(this);

        input.setInputListener(this);
        input.setAttachmentsListener(this);
    }


    @SuppressLint("DefaultLocale")
    private String getHumanTimeText(long milliseconds) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }


    //record audio
    private void Recording() {

        Helper.getHelperInstance().makeHepticFeedback(
                getBaseContext());

        if (Helper.getHelperInstance().createRecordingFolder()) {

            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyyMMdd_HH_mm_ss");
            String currentTimeStamp = dateFormat
                    .format(new Date());

            currentOutFile = Helper.RECORDING_PATH
                    + "/recording_" + currentTimeStamp + ".3gp";

            myAudioRecorder = new MediaRecorder();
            myAudioRecorder
                    .setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder
                    .setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myAudioRecorder
                    .setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            myAudioRecorder.setOutputFile(currentOutFile);

            try {

                myAudioRecorder.prepare();
                myAudioRecorder.start();

            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    private void stopRecording(boolean canceled) {
        Helper.getHelperInstance().makeHepticFeedback(
                getBaseContext());
        try {

            if (null != myAudioRecorder) {
                myAudioRecorder.stop();
                myAudioRecorder.release();
                myAudioRecorder = null;

                if(canceled){
                    try {
                        File temp = new File(currentOutFile);
                        temp.delete();
                        currentOutFile="";
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadPtt(final String path) {
        if(!currentOutFile.isEmpty()) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReferenceFromUrl("gs://anode-ed2b7.appspot.com");

            signInAnonymously();

            final Uri file = Uri.fromFile(new File(path));
            StorageReference riversRef = storageRef.child("storage/" + file.getLastPathSegment());

            riversRef.putFile(file)
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        }
                    }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    User usuario = new User(PreferenceStorage.getId(), PreferenceStorage.getUsername(), PreferenceStorage.getUserAvatar(), true);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        db.inserMessage(contactAddress, PreferenceStorage.getId(), "audio::" + toBase64(compress(file.getLastPathSegment())), sdf.parse(sdf.format(new Date())), 0);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        messagesAdapter.addToStart(getVoiceMessage(usuario, "audio::" + toBase64(compress(file.getLastPathSegment())), duration), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        sendNotificationVoice("audio::" + toBase64(compress(file.getLastPathSegment())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        File temp = new File(path);
                        temp.delete();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }


    @Override
    public void onMessageClick(Message message) {
        String type = message.getText();
        if(type.startsWith("(voice)")) {
            MessageActivity.open(this, message.getVoice().getUrl());
        }
    }

    //end record audio
    @Override
    public boolean onSubmit(CharSequence input) {

        TOTAL_MESSAGES_COUNT++;
        User meId = new User(PreferenceStorage.getId(),PreferenceStorage.getUsername(),PreferenceStorage.getUserAvatar(),true);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            db.inserMessage(contactAddress, PreferenceStorage.getId(),input.toString(), sdf.parse(sdf.format(new Date())),0);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        messagesAdapter.addToStart(
                getTextMessage(meId,input.toString()), true);

        String encrypted = ulDrawHelper.DES_Encryption(input.toString());

        JSONObject objIn = new JSONObject();
        try {
            objIn.put("message", encrypted);
            objIn.put("to", contactAddress);
            objIn.put("from", PreferenceStorage.getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

            AppSocketListener.getInstance().emit(SocketEventConstants.messageprivate, objIn);

        return true;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    public static String getPath(final Uri uri, final Context context) {
        try {
            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * On activity result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == 9) {
            Uri selectedImage = data.getData();
            String path = getPath(selectedImage, getBaseContext());
            uploadfile(path);
        }
    }


    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                    }
                });
    }

    private void uploadfile(final String path) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://anode-ed2b7.appspot.com");
        final StyledMessagesActivity tempact = StyledMessagesActivity.this;

        signInAnonymously();

        final Uri file = Uri.fromFile(new File(path));
        final StorageReference riversRef = storageRef.child("storage/" + file.getLastPathSegment());


        riversRef.putFile(file)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    }
                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(getBaseContext(), "Fallo en envio " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Toast.makeText(getBaseContext(), "Imagen enviada! ", Toast.LENGTH_SHORT).show();

                User usuario = new User(PreferenceStorage.getId(),PreferenceStorage.getUsername(), PreferenceStorage.getUserAvatar(), true);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    db.inserMessage(contactAddress, PreferenceStorage.getId(),"image::" + toBase64(compress(taskSnapshot.getDownloadUrl().toString())), sdf.parse(sdf.format(new Date())),0);
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    messagesAdapter.addToStart(getImageMessage(usuario, toBase64(compress(taskSnapshot.getDownloadUrl().toString()))),true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sendNotificationImage("image::" + toBase64(compress(taskSnapshot.getDownloadUrl().toString())));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void sendNotificationImage(String fileName) {

        JSONObject objIn = new JSONObject();
        try {
            objIn.put("message", fileName);
            objIn.put("to", contactAddress);
            objIn.put("from", PreferenceStorage.getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
            AppSocketListener.getInstance().emit(SocketEventConstants.eventimage, objIn);
    }


    private void sendNotificationVoice(String fileName) {

        JSONObject objIn = new JSONObject();
        try {
            objIn.put("message", fileName);
            objIn.put("to", contactAddress);
            objIn.put("from", PreferenceStorage.getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AppSocketListener.getInstance().emit(SocketEventConstants.eventptt, objIn);
    }

    @Override
    public void onAddAttachments() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, 9);//one can be replaced with any action code
    }

    @Override
    public String format(Date date) {
        if (DateFormatter.isToday(date)) {
            return getString(R.string.date_header_today);
        } else if (DateFormatter.isYesterday(date)) {
            return getString(R.string.date_header_yesterday);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                return DateFormatter.format(sdf.parse(sdf.format(date)), DateFormatter.Template.STRING_DAY_MONTH_YEAR);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return "Lot of time ...";
    }

    @Override
    public boolean hasContentFor(Message message, byte type) {
        switch (type) {
            case CONTENT_TYPE_VOICE:
                return message.getVoice() != null
                        && message.getVoice().getUrl() != null
                        && !message.getVoice().getUrl().isEmpty();
        }
        return false;
    }

    private void initAdapter() {

        MessageHolders holders = new MessageHolders()
                .registerContentType(
                        CONTENT_TYPE_VOICE,
                        IncomingVoiceMessageViewHolder.class,
                        R.layout.item_custom_incoming_voice_message,
                        OutcomingVoiceMessageViewHolder.class,
                        R.layout.item_custom_outcoming_voice_message,
                        this);

        super.messagesAdapter = new MessagesListAdapter<>(super.senderId, holders, super.imageLoader);
        super.messagesAdapter.enableSelectionMode(this);
        super.messagesAdapter.setOnMessageClickListener(this);
        super.messagesAdapter.setLoadMoreListener(this);
        super.messagesAdapter.setDateHeadersFormatter(this);
        messagesList.setAdapter(super.messagesAdapter);
    }

    @Override
    public void onSocketConnected() {
    }

    @Override
    public void onSocketDisconnected() {
        AppSocketListener.getInstance().restartSocket();
    }

    @Override
    public void onNewMessageReceived(String username, String message) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Cursor dbcontact = db.getContact(username);
            dbcontact.moveToLast();
            Message textmessage;

            textmessage = getTextMessage(new User(username, dbcontact.getString(dbcontact.getColumnIndex("name")),
                    dbcontact.getString(dbcontact.getColumnIndex("avatar")), false), message);
            try {
                textmessage.setCreatedAt(sdf.parse(sdf.format(new Date())));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            messagesAdapter.addToStart(textmessage, true);
        db.setMessagesReaded(contactAddress);
    }

    @Override
    public void onNewContactReceived(String username, String id, String avatar) {

    }

    @Override
    public void onNewImageReceived(String username, String filename) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Cursor dbcontact = db.getContact(username);
            dbcontact.moveToLast();
            Message message;

            message = getImageMessage(new User(username, dbcontact.getString(dbcontact.getColumnIndex("name")),
                    dbcontact.getString(dbcontact.getColumnIndex("avatar")), false), filename.substring(filename.indexOf("::") + 1, filename.length()));
            try {
                message.setCreatedAt(sdf.parse(sdf.format(new Date())));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            messagesAdapter.addToStart(message, true);
        db.setMessagesReaded(contactAddress);
    }

    @Override
    public void onNewBFReceived(String username, String filename) {

    }

    @Override
    public void onNewFileReceived(String username, String filename) {

    }

    @Override
    public void onNewPttReceived(String username, String filename) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Cursor dbcontact = db.getContact(username);
            dbcontact.moveToLast();
            Message message;

            message = getVoiceMessage(new User(username, dbcontact.getString(dbcontact.getColumnIndex("name")),
                    dbcontact.getString(dbcontact.getColumnIndex("avatar")), false), filename,10000);
            try {
                message.setCreatedAt(sdf.parse(sdf.format(new Date())));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            messagesAdapter.addToStart(message, true);
        db.setMessagesReaded(contactAddress);
    }

    @Override
    public void onNewRead(String username) {

    }

    @Override
    public void onNewCall(String username, String channel) {

    }
}
