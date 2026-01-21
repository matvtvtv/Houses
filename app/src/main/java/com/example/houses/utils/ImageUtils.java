package com.example.houses.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {



        public static String uriToBase64(Context context, Uri uri) throws IOException {
            InputStream is = context.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            byte[] data = new byte[1024];
            int nRead;

            while ((nRead = is.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byte[] bytes = buffer.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }

        public static Bitmap base64ToBitmap(String base64) {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        }



    private static Bitmap scaleBitmap(Bitmap bitmap) {
        int maxSize = 1024;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float aspectRatio = (float) width / height;
        if (width > height) {
            width = maxSize;
            height = (int) (width / aspectRatio);
        } else {
            height = maxSize;
            width = (int) (height * aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}