package com.node.anonchat;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by alex_strange on 11/25/15.
 */
public class PreferenceStorage {
    static String preferencesIdentifier = "node.io.preferences";

    public static void storeUsername(String username){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("username", username);
        editor.apply();
    }

    public static void storeAvatar(String avatar){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("avatar", avatar);
        editor.apply();
    }


    public static void storeId(String username){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("id", username);
        editor.apply();
    }

    public static void storeLock(String locker){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("locker", locker);
        editor.apply();

        setLock(true);
    }

    public static void setLock(boolean lock){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("lock", lock);
        editor.apply();
    }


    public static void setCounter(int msg){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putInt("counter",msg);
        editor.apply();
    }

    public static int getCounter(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        int counter = prefs.getInt("counter",0);
        return counter;
    }

    public static void storeScreen(boolean screen){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("screenshot", screen);
        editor.apply();
    }

    //preferences

    public static void storenodos(boolean nodos){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("nodos", nodos);
        editor.apply();
    }

    public static void storetor(boolean tor){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("tor", tor);
        editor.apply();
    }

    public static void complex(boolean comp){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("complex", comp);
        editor.apply();
    }

    public static void storepassc(boolean passc){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("passc", passc);
        editor.apply();
    }

    public static void storeinterval(int interval){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putInt("interval", interval);
        editor.apply();
    }

    public static void storeptt(boolean ptt){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("ptt", ptt);
        editor.apply();
    }

    public static void storevideo(boolean video){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("video", video);
        editor.apply();
    }


    public static boolean getnodos(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean nodos = prefs.getBoolean("nodos", false);
        return nodos;
    }

    public static boolean getLock(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean lock = prefs.getBoolean("lock", false);
        return lock;
    }

    public static boolean gettor(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean tor = prefs.getBoolean("tor", false);
        return tor;
    }

    public static boolean getcomplex(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean comp = prefs.getBoolean("complex", false);
        return comp;
    }

    public static boolean getScreen(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean comp = prefs.getBoolean("screenshot", true);
        return comp;
    }

    public static boolean getpassc(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean passc = prefs.getBoolean("passc", false);
        return passc;
    }

    public static int getinterval(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        int interval = prefs.getInt("interval", 1);
        return interval;
    }

    public static boolean getptt(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean ptt = prefs.getBoolean("ptt", false);
        return ptt;
    }

    public static boolean getvideo(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean video = prefs.getBoolean("video", false);
        return video;
    }

    //end preferences
    public static String getId(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String userName = prefs.getString("id", null);
        return userName;
    }

    public static String getLocker(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String locker = prefs.getString("locker", null);
        return locker;
    }

    public static String getSchema(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String userName = prefs.getString("crunch", null);
        return userName;
    }

    public static void storeSchema(String username){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putString("crunch", username);
        editor.apply();
    }

    public static boolean getRadioAuto(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        boolean isauto = prefs.getBoolean("auto", true);
        return isauto;
    }

    public static void storeRadioAuto(boolean auto){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).
                edit();
        editor.putBoolean("auto", auto);
        editor.apply();
    }

    public static String getUsername(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String userName = prefs.getString("username", "anonymous");
        return userName;
    }

    public static String getUserAvatar(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String userAvatar = prefs.getString("avatar", "https://firebasestorage.googleapis.com/v0/b/anode-ed2b7.appspot.com/o/storage%2Fanon.jpg?alt=media&token=e34c1f1a-0e5f-4124-9939-c62a6e296653");
        return userAvatar;
    }


    public static Boolean shouldDoAutoLogin(){
        SharedPreferences prefs;
        prefs = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE);
        String userName = prefs.getString("username", null);
        if (userName != null && ! userName.isEmpty()){
            return true;
        }
        return false;

    }
    public static void clearUserSession(){
        SharedPreferences.Editor editor;
        editor = AppContext.getAppContext().getSharedPreferences(preferencesIdentifier,
                Context.MODE_PRIVATE).edit();
        editor.putString("id",null);
        editor.putString("username",null);
        editor.apply();
    }
}
