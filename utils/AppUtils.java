package com.node.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/*
 * Created by troy379 on 04.04.17.
 */
public class AppUtils {

    public static void showToast(Context context, @StringRes int text, boolean isLong) {
        showToast(context, context.getString(text), isLong);
    }

    public static void showToast(Context context, String text, boolean isLong) {
        Toast.makeText(context, text, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    public static byte[] compress(String string) throws IOException {
        // string = "Lorem ipsum shizzle ma nizle";
        byte[] data = string.getBytes("UTF-8");
        ByteArrayOutputStream os = new ByteArrayOutputStream( data.length );
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write( data );
        gos.close();
        os.close();
        return os.toByteArray();
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

}