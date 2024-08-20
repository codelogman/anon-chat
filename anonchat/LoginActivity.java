package com.node.anonchat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.annotation.UiThread;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import com.node.services.SocketEventConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.socket.emitter.Emitter;
/**
 * A login screen that offers login via username.
 */
public class LoginActivity extends Activity {
    private EditText mUsernameView;
    private TextView Id;
    private TextView schema;
    private String mUsername;

    public static ProgressDialog dialog;

    static final String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*&*()-+_=";
    static SecureRandom rnd = new SecureRandom();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        // Set up te1he login form.
        mUsernameView = (EditText) findViewById(R.id.username_input);
        Id = (TextView) findViewById(R.id.txtId);
        schema = (TextView) findViewById(R.id.txtSchema);

        schema.requestFocus();

        if(!(PreferenceStorage.getId()!=null && !PreferenceStorage.getId().isEmpty())) {
            Id.setText(randomString(19));
        }else
        {
            Id.setText(PreferenceStorage.getId());
        }

        String temp = bytesToHexString(randomString(64).getBytes(),2);
        
        schema.setText(temp.substring(0,temp.length()-1));

        mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.username_input || id == EditorInfo.IME_NULL) {
                    try {
                        attemptLogin();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    attemptLogin();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        //dialog = new ProgressDialog(LoginActivity.this);

        //PreferenceStorage.storeLock(DES_Encryption("666"));

        checkRunTimePermission();
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

    String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    public String makeSHA1Hash(String input)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.reset();
        byte[] buffer = input.getBytes("UTF-8");
        md.update(buffer);
        byte[] digest = md.digest();

        String hexStr = "";
        for (int i = 0; i < digest.length; i++) {
            hexStr +=  Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return hexStr;
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

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() throws RemoteException {
        // Reset errors.
        mUsernameView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString().trim();
        String sid = Id.getText().toString().trim();
        String crunch = schema.getText().toString().trim();

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mUsernameView.setError("field required!");
            mUsernameView.requestFocus();
            return;
        }

        mUsername = username;
        PreferenceStorage.storeUsername(username);
        PreferenceStorage.storeId(sid);
        PreferenceStorage.storeSchema(crunch);
        // perform the user login attempt.
        ArrayList<String> stringArrayList = new ArrayList<String>();
        stringArrayList.add(username);
        AppSocketListener.getInstance().addOnHandler(SocketEventConstants.evil,onLogin);
        AppSocketListener.getInstance().addNewMessageHandler();
        AppSocketListener.getInstance().addNewContactRequest();
        AppSocketListener.getInstance().addNewFileRequest();
        AppSocketListener.getInstance().addNewImageRequest();
        AppSocketListener.getInstance().addNewBFRequest();
        AppSocketListener.getInstance().addNewPttRequest();
        AppSocketListener.getInstance().addNewRead();
        AppSocketListener.getInstance().addNewCall();
        AppSocketListener.getInstance().userJoined();
        AppSocketListener.getInstance().userLeft();

        //dialog.show(LoginActivity.this, "",
          //      "Storing Crunch evil to supernode ...", true,true);

        Toast.makeText(LoginActivity.this,
                "Storing Crunch evil to supernode ...", Toast.LENGTH_LONG).show();

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

            String eviljack;
            try {
                eviljack = data.getString("evil");
            } catch (JSONException e) {
                return;
            }

            //dialog.dismiss();
            PreferenceStorage.setLock(true);

            Intent intent = new Intent();
            intent.putExtra("username", mUsername);
            intent.putExtra("evil", eviljack);
            setResult(RESULT_OK, intent);
            finish();
            return;
        }
    };

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private static String DES_Encryption(String text)

    {

        Cipher ecipher;

        // Create a new DES key based on the 8 bytes in the secretKey array
        byte[] keyData = {(byte) 0x53, (byte) 0x33, (byte) 0x18, (byte) 0x79,
                (byte) 0x62, (byte) 0x11, (byte) 0x73, (byte) 0x19};

        try {

            SecretKeySpec key = new SecretKeySpec(keyData, "DES/CFB8/NoPadding");

            byte[] data = text.getBytes();

            // Setup the Initialization vector.
            byte[] iv1 = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

            AlgorithmParameterSpec paramSpec = new IvParameterSpec(keyData);

            ecipher = Cipher.getInstance("DES/CFB8/NoPadding");

            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

            byte[] output = ecipher.doFinal(data);


            return toBase64(output);

        } catch (GeneralSecurityException e) {
            return text;
            //throw new RuntimeException(e);
        } catch (Exception b) {
            return text;
        }
    }

    public static String toBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP).trim();
    }

}