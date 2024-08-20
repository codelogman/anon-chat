package com.node.anonchat;

import android.app.Application;
import android.content.Context;

/**
 * Created by alex_strange on 20/10/16
 */
public class AppContext extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        AppContext.context = this;
        initializeSocket();

        //preferences default
        PreferenceStorage.storenodos(true);
        PreferenceStorage.storetor(false);
        PreferenceStorage.storepassc(false);
        PreferenceStorage.storeinterval(1);
        PreferenceStorage.storeptt(false);
        PreferenceStorage.storeRadioAuto(false);
        PreferenceStorage.storevideo(false);
        PreferenceStorage.storeScreen(true);
        //prefence default

    }

    public static Context getAppContext() {
        return AppContext.context;
    }

    public void initializeSocket(){
        AppSocketListener.getInstance().initialize();
    }

    public void destroySocketListener(){
        AppSocketListener.getInstance().destroy();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        destroySocketListener();
    }
}
