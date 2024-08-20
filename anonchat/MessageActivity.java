
package com.node.anonchat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import com.node.ptt.AppRTCAudioManager;
import com.node.ptt.PlayerVisualizerView;
import com.node.ptt.RecorderVisualizerView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Activity for single message view
 * 
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class MessageActivity extends Activity
{
    private String tmp = "";
    private FirebaseAuth mAuth;
    private ProgressBar progress;

    private MediaPlayer mMediaPlayer;
    private static final float VISUALIZER_HEIGHT_DIP = 50f;
    private LinearLayout mLinearLayout;
    private PlayerVisualizerView mVisualizerView;
    private Visualizer mVisualizer;
    private File radiofile;
    private RecorderVisualizerView visualizerView;

    private AppRTCAudioManager audioManager = null;


    public static void open(Context context, String message) {

        Intent myIntent = new Intent(context, MessageActivity.class);
        myIntent.putExtra("message", message);
        context.startActivity(myIntent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setFinishOnTouchOutside(false);
        }
        else
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        }

        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        //FILE_DOWNLOAD IMPLEMENTATION
        setContentView(R.layout.message);

        mAuth = FirebaseAuth.getInstance();

        progress = (ProgressBar) findViewById(R.id.segmented_progress_bar);
        progress.setMax(100);
        Button download_file = (Button) findViewById(R.id.download_file);
        String text = getIntent().getExtras().getString("message");

        mMediaPlayer = new MediaPlayer();

        mMediaPlayer
                .setOnCompletionListener(
                        new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mLinearLayout.setVisibility(View.GONE);
                                try {
                                    if (radiofile.exists()) {
                                        radiofile.delete();
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });

        mLinearLayout = (LinearLayout) findViewById(R.id.linearLayoutVisual);
        TextView texto = new TextView(this);

        TextView image = new TextView(this);

        image.setText("  ( X )");
        image.setGravity(Gravity.RIGHT | Gravity.TOP);
        image.setTextColor(Color.GRAY);
        image.setTextSize(14);

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mMediaPlayer!=null)
                    mMediaPlayer.reset();
                finish();
            }
        });

        texto.setText("press for pause  ---  (X) cancel");
        texto.setGravity(Gravity.CENTER | Gravity.TOP);
        texto.setTextColor(Color.GRAY);
        texto.setTextSize(9);
                // Create a VisualizerView to display the audio waveform for the current
        // settings
        mVisualizerView = new PlayerVisualizerView(MessageActivity.this);
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (VISUALIZER_HEIGHT_DIP * getResources()
                        .getDisplayMetrics().density)));
        mLinearLayout.addView(image);
        mLinearLayout.addView(texto);
        mLinearLayout.addView(mVisualizerView);

        mVisualizerView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mMediaPlayer!= null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                    } else {
                        mMediaPlayer.start();
                    }
                }
            }
        });

        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());

        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] bytes, int samplingRate) {
                        mVisualizerView.updateVisualizer(bytes);
                    }

                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] bytes, int samplingRate) {
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, false);

        mVisualizer.setEnabled(true);
        //visualizer record


        download_file.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                progress.setVisibility(View.VISIBLE);
                tmp = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + tmp;
                downloadfile(tmp);
            }
        });


        if(text.indexOf("file::") != -1)
        {
            download_file.setVisibility(View.VISIBLE);

            byte[] text_free = fromBase64(text.substring(text.indexOf("::") +1,text.length()));
            try {
                tmp = decompress(text_free);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((TextView) findViewById(R.id.message)).setTextColor(Color.GRAY);
            ((TextView) findViewById(R.id.message)).setText("external file : " + tmp);

        }else if(text.indexOf("audio::") != -1)
        {
            progress.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.message)).setVisibility(View.GONE);

            byte[] text_free = fromBase64(text.substring(text.indexOf("::") +1,text.length()));
            try {
                tmp = decompress(text_free);
            } catch (IOException e) {
                e.printStackTrace();
            }
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" +
                    tmp);
            if (file.exists()) {
                playSong(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + tmp);
            }else{
                downloadaudio(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + tmp);
            }

        }else
        {
        }
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    public static String decompress(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = compressed.length;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            string.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        return string.toString();
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

    @Override
    public void onBackPressed() {
        if(mMediaPlayer!=null)
            mMediaPlayer.reset();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
    }

    private void downloadfile(String path) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://anode-ed2b7.appspot.com");

        signInAnonymously();

        final Uri file = Uri.fromFile(new File(path));
        StorageReference riversRef = storageRef.child("storage/" + file.getLastPathSegment());
        //twice??
        File localFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getLastPathSegment());
        riversRef.getFile(localFile)
                .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        double Dprogress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        progress.setProgress((int)Dprogress);
                    }
                }).addOnPausedListener(new OnPausedListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onPaused(FileDownloadTask.TaskSnapshot taskSnapshot) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getBaseContext(), "Fallo en recoleccion " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MessageActivity.this, "file: " + tmp, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    private void playSong(String path) {

        mLinearLayout.setVisibility(View.VISIBLE);

        MediaPlayer mplayer = mMediaPlayer;

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(this, new Runnable() {
                    // This method will be called each time the audio state (number and
                    // type of devices) has been changed.
                    @Override
                    public void run() {
                        onAudioManagerChangedState();
                    }
                }
        );

        audioManager.init();

        if (null != mplayer) {
            mplayer.reset();
            try {
                mplayer.setDataSource(path);

                final File mfile = new File(path);

                mplayer.setOnCompletionListener(
                        new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mLinearLayout.setVisibility(View.GONE);
                                try {
                                    if (mfile.exists()) {
                                        mfile.delete();
                                        finish();
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });


                mplayer.prepare();
                mplayer.start();

            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void downloadaudio(final String path) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://anode-ed2b7.appspot.com");

        signInAnonymously();

        final Uri file = Uri.fromFile(new File(path));
        StorageReference riversRef = storageRef.child("storage/" + file.getLastPathSegment());
        //twice??

        File localFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getLastPathSegment());

        riversRef.getFile(localFile)
                .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    }
                }).addOnPausedListener(new OnPausedListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onPaused(FileDownloadTask.TaskSnapshot taskSnapshot) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(MessageActivity.this, "Fallo en recoleccion " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    playSong(path);
                }
        });

    }
}
