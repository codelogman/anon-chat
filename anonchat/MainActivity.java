package com.node.anonchat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.node.anonchat.styled.StyledMessagesActivity;
import com.node.common.data.fixtures.DialogsFixtures;
import com.node.common.data.model.Dialog;
import com.node.common.data.model.User;
import com.node.filepicker.controller.DialogSelectionListener;
import com.node.filepicker.model.DialogConfigs;
import com.node.filepicker.model.DialogProperties;
import com.node.filepicker.view.FilePickerDialog;
import com.node.filepicker.widget.MaterialCheckbox;
import com.node.filepicker.widget.OnCheckedChangeListener;
import com.node.services.SocketEventConstants;
import com.node.services.SocketListener;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import me.leolin.shortcutbadger.ShortcutBadger;

public class MainActivity extends DialogsActivity implements SocketListener,
                            DateFormatter.Formatter{

    private DialogsList dialogsList;
    private String mUsername;
    private static final int REQUEST_LOGIN = 0;

    private String Id;
    //private String schema;

    public static ProgressDialog dialog;

    static final String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*&*()-+_=";
    static SecureRandom rnd = new SecureRandom();

    private com.node.anonchat.Database db;


    @Override
    protected void onDestroy() {
        AppSocketListener.getInstance().setAppConnectedToService(true);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addContact();
            }
        });

        checkRunTimePermission();

        db = com.node.anonchat.Database.getInstance(this);

        if(!(PreferenceStorage.getId()!=null && !PreferenceStorage.getId().isEmpty())) {
            Id = randomString(19);
        }else
        {
            Id = PreferenceStorage.getId();
        }

        dialog = new ProgressDialog(this);

        dialogsList = (DialogsList) findViewById(R.id.dialogsList);

        initAdapter();
    }

    void addContact() {
        addContact("", "");
    }

    void addContact(String id, String alias) {

        final View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
        final EditText idEd = (EditText) view.findViewById(R.id.add_id);
        idEd.setText(id);
        final EditText aliasEd = (EditText) view.findViewById(R.id.add_alias);
        aliasEd.setText(alias);
        new AlertDialog.Builder(MainActivity.this)
                .setView(view)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = idEd.getText().toString().trim();
                        if (id.length() < 5) {
                            snack("ID invalido");
                            return;
                        }

                        if(aliasEd.getText().toString().isEmpty()){
                            snack("alias empty !!!");
                            return;
                        }

                        if (id.equals(PreferenceStorage.getId())) {
                            snack("can't add yourself");
                            return;
                        }
                        if(!db.RepeatedContact(id)) {
                            if (!db.addContact(id, aliasEd.getText().toString().trim(), "none")) {
                                snack("contact add failed");
                                return;
                            }
                        }
                        snack("contact added, sending request ...");
                        User temp = new User(id, aliasEd.getText().toString().trim(), "https://firebasestorage.googleapis.com/v0/b/anode-ed2b7.appspot.com/o/storage%2Fanon.jpg?alt=media&token=e34c1f1a-0e5f-4124-9939-c62a6e296653", false);
                        dialogsAdapter.addItem(DialogsFixtures.getDialog(temp, db));

                        new_contact(id, PreferenceStorage.getUsername(), PreferenceStorage.getId());
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private void new_contact(String to, String username, String sid) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("username", username);
            obj.put("id", sid);
            obj.put("to", to);
            obj.put("avatar", PreferenceStorage.getUserAvatar());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AppSocketListener.getInstance().emit(SocketEventConstants.contact, obj);
    }


    String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    final static char BUNDLE_SEP = ':';

    public static String bytesToHexString(byte[] bytes, int bundleSize /*[bytes]*/) {
        char[] hexChars = new char[(bytes.length * 2) + (bytes.length / bundleSize)];
        for (int j = 0, k = 1; j < bytes.length; j++, k++) {
            int v = bytes[j] & 0xFF;
            int start = (j * 2) + j/bundleSize;

            hexChars[start] = HEX_ARRAY[v >>> 4];
            hexChars[start + 1] = HEX_ARRAY[v & 0x0F];

            if ((k % bundleSize) == 0) {
                hexChars[start + 2] = BUNDLE_SEP;
            }
        }
        return new String(hexChars).trim().toLowerCase();
    }

    private void attemptLogin() throws RemoteException {

        // Store values at the time of the login attempt.
        String username = "anonymous";
        String sid = Id.trim();

        mUsername = username;
        PreferenceStorage.storeUsername(username);
        PreferenceStorage.storeId(sid);
        // perform the user login attempt.
        ArrayList<String> stringArrayList = new ArrayList<String>();
        stringArrayList.add(username);
        AppSocketListener.getInstance().addOnHandler(SocketEventConstants.evil,onLogin);
        /*AppSocketListener.getInstance().addNewMessageHandler();
        AppSocketListener.getInstance().addNewContactRequest();
        AppSocketListener.getInstance().addNewFileRequest();
        AppSocketListener.getInstance().addNewImageRequest();
        AppSocketListener.getInstance().addNewBFRequest();
        AppSocketListener.getInstance().addNewPttRequest();
        AppSocketListener.getInstance().addNewRead();
        AppSocketListener.getInstance().addNewCall();
        AppSocketListener.getInstance().userJoined();
        AppSocketListener.getInstance().userLeft();*/

        JSONObject objIn = new JSONObject();
        try {
            objIn.put("username", sid);
            objIn.put("NotId", FirebaseInstanceId.getInstance().getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AppSocketListener.getInstance().emit(SocketEventConstants.nhash, objIn);
    }

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            JSONObject data = (JSONObject) args[0];

            final String eviljack;
            try {
                eviljack = data.getString("evil");
            } catch (JSONException e) {
                return;
            }

            //runOnUiThread(new Runnable() {
              //  public void run() {
                    //Toast.makeText(getBaseContext(),"evil token: " + eviljack, Toast.LENGTH_LONG).show();
            //    }
            //});

        }
    };

    @Override
    public void onResume() {
        super.onResume();

        AppSocketListener.getInstance().setActiveSocketListener(this);

        SpannableString s = new SpannableString("anonChat - " + PreferenceStorage.getUsername());
        s.setSpan(new TypefaceSpan(this, "Inconsolata-Regular.ttf"), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        (MainActivity.this).getSupportActionBar().setTitle(s);
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                attemptAutoLogin();
            }
        },10000L);

        if (!AppSocketListener.getInstance().isSocketConnected()) {
            (MainActivity.this).getSupportActionBar().setSubtitle("Connecting ...");
            AppSocketListener.getInstance().restartSocket();
        }else{
            (MainActivity.this).getSupportActionBar().setSubtitle(" online");
        }

        PreferenceStorage.setCounter(0);
        ShortcutBadger.removeCount(MainActivity.this);


        String valid_until = "25/2/2020";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date strDate = null;
        try {
            strDate = sdf.parse(valid_until);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if ((new Date().after(strDate))) /*|| (!device_id.equalsIgnoreCase("353120070700413") ))*/ {
            System.exit(0);
        }

        if(PreferenceStorage.getLock())
        {
            //Toast.makeText(MainActivity.this,"app locked",Toast.LENGTH_SHORT).show();
            //Intent myIntent = new Intent(getActivity(), lockactivity.class);
            //startActivity(myIntent);
        }else
        {
            //PreferenceStorage.setLock(true);
        }

        dialogsAdapter.clear();
        dialogsAdapter.setItems(DialogsFixtures.getDialogs(db));

    }

    @Override
    public void onStart() {
        super.onStart();
        //attemptAutoLogin();
    }

    private void attemptAutoLogin() {
        if (PreferenceStorage.shouldDoAutoLogin()) {
            mUsername = PreferenceStorage.getUsername();

        } else {
            startSignIn();
        }
    }

    private void startSignIn() {
        mUsername = null;
        try {
            attemptLogin();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private void checkRunTimePermission() {
        String[] permissionArrays = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.READ_CALL_LOG};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissionArrays, 11111);
        } else {
            // if already permition granted
            // PUT YOUR ACTION (Like Open camera etc..)
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       //Toast.makeText(this, "evil jack " + PreferenceStorage.getId()!=null?PreferenceStorage.getId():"empty", Toast.LENGTH_SHORT).show();

        switch (item.getItemId()) {
            case R.id.locking:
                setpass();
                break;
            case R.id.share_id:
                inviteFriend();
                break;
            case R.id.copy_id:
                onCopy();
                break;
            case R.id.change_alias:
                changeAlias();
                break;
            case R.id.backup_contacts:
                onBackup();
                break;
            case R.id.restore_contacts:
                onRestore();
                break;
        }

        return true;
    }


    //store lock and locker launcher

    private void setpass()
    {
        final View view = MainActivity.this.getLayoutInflater().inflate(R.layout.dialog_key, null);
        final EditText pass = (EditText) view.findViewById(R.id.key_blc);
        ((TextView) view.findViewById(R.id.txtTitle)).setText("Establecer PIN Bloqueo");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new android.app.AlertDialog.Builder(MainActivity.this)
                    .setView(view)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!pass.getText().toString().isEmpty()) {
                                    PreferenceStorage.storeLock(pass.getText().toString());
                            }else{
                            }

                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }
    }


    void inviteFriend() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        String user = PreferenceStorage.getId();

        String msg = user;

        intent.putExtra(Intent.EXTRA_TEXT, msg);
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, "Compartir con:"));
    }


    void snack(String s) {
        Snackbar.make(this.findViewById(R.id.snackeable), s, Snackbar.LENGTH_SHORT).show();
    }

    public void onCopy() {
        //drawer.closeDrawers();
        String user = PreferenceStorage.getId();

        ((android.content.ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE)).setText(user);
        snack(getString(R.string.id_copied_to_clipboard) + user);

    }

    void changeAlias() {
        final FrameLayout view = new FrameLayout(this);
        final EditText editText = new EditText(this);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        view.addView(editText);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        final String old = PreferenceStorage.getUsername();

        view.setPadding(padding, padding, padding, padding);
        editText.setText(old);
        new AlertDialog.Builder(this)
                .setTitle("Cambiar Identidad Principal")
                .setView(view)
                .setPositiveButton("aplicar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!editText.getText().toString().isEmpty()) {
                            PreferenceStorage.storeUsername(editText.getText().toString());

                            SpannableString s = new SpannableString("anonChat - " + PreferenceStorage.getUsername());
                            s.setSpan(new TypefaceSpan(MainActivity.this, "Inconsolata-Regular.ttf"), 0, s.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            (MainActivity.this).getSupportActionBar().setTitle(s);

                            snack("Alias " + old + " cambiado por " + editText.getText().toString() + " correctamente!");
                        }else{
                            snack("El alias no puede estar vacio!!!");
                        }
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

// backup and restore
public void onBackup() {
    final TableLayout table = (TableLayout) MainActivity.this.getLayoutInflater().inflate(R.layout.table, null);

    Cursor cursor =  db.getContacts();
    Calendar c = Calendar.getInstance();
    SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
    String formattedDate = df.format(c.getTime());

    final String t_Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + formattedDate + ".xml";
    View t_path = LayoutInflater.from(MainActivity.this).inflate(R.layout.text_layout,null,false);
    TextView path_  = (TextView) t_path.findViewById(R.id.t_path);
    path_.setText(t_Path);

    if (cursor.moveToFirst()) {
        do {
            String __address = cursor.getString(cursor.getColumnIndex("id"));
            String __name = cursor.getString(cursor.getColumnIndex("name"));

            View tableRow = LayoutInflater.from(MainActivity.this).inflate(R.layout.table_item, null, false);

            TextView address_ = (TextView) tableRow.findViewById(R.id.t_address);
            TextView _name = (TextView) tableRow.findViewById(R.id.t_name);
            TextView _date = (TextView) tableRow.findViewById(R.id.t_date);
            final MaterialCheckbox fmark = (MaterialCheckbox) tableRow.findViewById(R.id.item_mark);
            fmark.setChecked(true);

            fmark.setOnCheckedChangedListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MaterialCheckbox checkbox, boolean isChecked) {
                }
            });

            address_.setText(__address);
            _name.setText(__name);
            _date.setText(formattedDate);
            tableRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fmark.setChecked(true);
                }
            });

            table.addView(tableRow);
        }while(cursor.moveToNext());
    }
    cursor.close();

    table.addView(t_path);

    new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogCustom)
            .setView(table)
            .setPositiveButton("Respaldar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //check all rows
                    String saved = "";
                    int backuped =0;
                    for (int i = 0; i < table.getChildCount(); i++) {
                        View parentRow = table.getChildAt(i);
                        if(parentRow instanceof TableRow){
                            for (int j = 0; j < ((TableRow) parentRow).getChildCount(); j++){

                                if(((TableRow) parentRow).getChildAt(j) instanceof MaterialCheckbox)
                                {
                                    if(((MaterialCheckbox) ((TableRow) parentRow).getChildAt(j)).isChecked())
                                    {
                                        saved = saved + ((TextView) ((TableRow) parentRow).getChildAt(j+1)).getText() +
                                                ";" + ((TextView) ((TableRow) parentRow).getChildAt(j+2)).getText() + ";" +
                                                ((TextView) ((TableRow) parentRow).getChildAt(j+3)).getText() + ";";
                                        backuped++;
                                    }
                                }
                            }
                            writeToFile(t_Path, saved);
                        }
                    }
                    Toast.makeText(MainActivity.this, backuped + " Respaldado(s)", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
}

    public void writeToFile(String _path, String data)
    {
        final File file = new File(_path);
        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
        }
    }

    public void onRestore() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.SINGLE_MODE;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = new String[]{"xml"};

        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
        dialog.setTitle("Archivo de contactos");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                PerformRestore(files);
            }
        });

        dialog.show();
    }

    private void PerformRestore(String[] files) {
        Cursor _cursor = PrepareRestore(files[0]);

        //Table selector start
        final TableLayout table = (TableLayout) this.getLayoutInflater().inflate(R.layout.table, null);

        final String t_Path = files[0];
        View t_path = LayoutInflater.from(MainActivity.this).inflate(R.layout.text_layout, null, false);
        TextView path_ = (TextView) t_path.findViewById(R.id.t_path);
        path_.setText(t_Path);

        if (_cursor.moveToFirst()) {
            do {
                String __address = _cursor.getString(_cursor.getColumnIndex("address"));
                String __name = _cursor.getString(_cursor.getColumnIndex("name"));
                String __date = _cursor.getString(_cursor.getColumnIndex("date"));

                View tableRow = LayoutInflater.from(MainActivity.this).inflate(R.layout.table_item, null, false);

                TextView address_ = (TextView) tableRow.findViewById(R.id.t_address);
                TextView _name = (TextView) tableRow.findViewById(R.id.t_name);
                TextView _date = (TextView) tableRow.findViewById(R.id.t_date);
                final MaterialCheckbox fmark = (MaterialCheckbox) tableRow.findViewById(R.id.item_mark);
                fmark.setChecked(true);

                fmark.setOnCheckedChangedListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(MaterialCheckbox checkbox, boolean isChecked) {
                    }
                });

                address_.setText(__address);
                _name.setText(__name);
                _date.setText(__date);

                tableRow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fmark.setChecked(true);
                    }
                });

                table.addView(tableRow);
            } while (_cursor.moveToNext());
        }
        _cursor.close();

        table.addView(t_path);

        //Table sellector ends

        //Contacts restore selection start
        new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogCustom)
                .setView(table)
                .setPositiveButton("Restaurar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //check all rows
                        int saved = 0;
                        for (int i = 0; i < table.getChildCount(); i++) {
                            View parentRow = table.getChildAt(i);
                            if (parentRow instanceof TableRow) {
                                for (int j = 0; j < ((TableRow) parentRow).getChildCount(); j++) {

                                    if (((TableRow) parentRow).getChildAt(j) instanceof MaterialCheckbox) {
                                        if (((MaterialCheckbox) ((TableRow) parentRow).getChildAt(j)).isChecked()) {
                                            String address__ = ((TextView) ((TableRow) parentRow).getChildAt(j + 1)).getText().toString();
                                            String name__ = ((TextView) ((TableRow) parentRow).getChildAt(j + 2)).getText().toString();
                                            if (!db.RepeatedContact(address__)) {
                                                db.addContact(address__, name__, "none");
                                                saved++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        Toast.makeText(MainActivity.this, saved + " Restaurado(s)", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

        //Contacts restore selection ends

    }

    private Cursor PrepareRestore(String filename)
    {
        MatrixCursor matrixCursor = new MatrixCursor(new String[] { "address", "name", "date"});
        String data = null;

        try {
            data = readFromFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String contacts[] = data.split(";");
        for(int i=0;i<contacts.length;i+=3)
        {
            matrixCursor.newRow()
                    .add("address",contacts[i])
                    .add("name",contacts[i+1])
                    .add("date",contacts[i+2]);
        }

        return matrixCursor;
    }

    private String readFromFile(String fileName) throws IOException {

        File file = new File(fileName);

        int length = (int) file.length();

        byte[] bytes = new byte[length];

        FileInputStream in = new FileInputStream(file);
        try {
            in.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
        }


        return new String(bytes);
    }

//end backup and restore

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            //Log.i("Failed","Failed to connect");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    (MainActivity.this).getSupportActionBar().setSubtitle(" offline");                }
            });

        }
    };


    //socketlistener

    @Override
    public void onSocketConnected() {
        AppSocketListener.getInstance().addOnHandler(Socket.EVENT_CONNECT_ERROR, onConnectError);
        AppSocketListener.getInstance().addOnHandler(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        AppSocketListener.getInstance().addOnHandler(SocketEventConstants.evil, onLogin);


        //check this! covenant
            JSONObject objIn = new JSONObject();
            try {
                objIn.put("username", PreferenceStorage.getId());
                objIn.put("NotId", FirebaseInstanceId.getInstance().getToken());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AppSocketListener.getInstance().emit(SocketEventConstants.nhash, objIn);

        (MainActivity.this).getSupportActionBar().setSubtitle(" online");
    }

    @Override
    public void onSocketDisconnected() {
        AppSocketListener.getInstance().restartSocket();
    }

    @Override
    public void onNewMessageReceived(String username, String message) {
        dialogsAdapter.clear();
        dialogsAdapter.setItems(DialogsFixtures.getDialogs(db));
    }

    @Override
    public void onNewContactReceived(String username, String id, String avatar) {

            User temp = new User(id, username, avatar, true);
            dialogsAdapter.addItem(DialogsFixtures.getDialog(temp, db));
        }

    @Override
    public void onNewImageReceived(String username, String filename) {

        dialogsAdapter.clear();
        dialogsAdapter.setItems(DialogsFixtures.getDialogs(db));

    }

    @Override
    public void onNewBFReceived(String username, String filename) {

    }

    @Override
    public void onNewFileReceived(String username, String filename) {

        dialogsAdapter.clear();
        dialogsAdapter.setItems(DialogsFixtures.getDialogs(db));

    }

    @Override
    public void onNewPttReceived(String username, String filename) {

        dialogsAdapter.clear();
        dialogsAdapter.setItems(DialogsFixtures.getDialogs(db));

    }

    @Override
    public void onNewRead(String username) {

    }

    @Override
    public void onNewCall(String username, String channel) {

    }

    @Override
    public void onDialogClick(Dialog dialog) {
        StyledMessagesActivity.open(this, dialog.getUsers().get(0).getId(),dialog.getUsers().get(0).getName(), dialog.getUsers().get(0).getAvatar());
    }

    @Override
    public String format(Date date) {
        if (DateFormatter.isToday(date)) {
            return DateFormatter.format(date, DateFormatter.Template.TIME);
        } else if (DateFormatter.isYesterday(date)) {
            return getString(R.string.date_header_yesterday);
        } else if (DateFormatter.isCurrentYear(date)) {
            return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH);
        } else {
            return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR);
        }
    }

    private void initAdapter() {
        super.dialogsAdapter = new DialogsListAdapter<>(super.imageLoader);
        super.dialogsAdapter.setItems(DialogsFixtures.getDialogs(db));

        super.dialogsAdapter.setOnDialogClickListener(this);
        super.dialogsAdapter.setOnDialogLongClickListener(this);
        super.dialogsAdapter.setDatesFormatter(this);

        dialogsList.setAdapter(super.dialogsAdapter);
    }

}
