package com.psymaker.vibraimage.vibrama;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FileUtil {
    private static final String LOG_TAG = "FileUtil";
    private static FileUtil mInstance;

    private FileUtil() {
    }

    public static FileUtil getInstance() {
        if (mInstance == null) {
            synchronized (FileUtil.class) {
                if (mInstance == null) {
                    mInstance = new FileUtil();
                }
            }
        }
        return mInstance;
    }

    /**
     * Stores the given {@link Bitmap} to a path on the device.
     *
     * @param bitmap   The {@link Bitmap} that needs to be stored
     * @param filePath The path in which the bitmap is going to be stored.
     */
    public static boolean storeBitmap(Bitmap bitmap, String filePath) {
        boolean ok = false;
        File imageFile = new File(filePath);
        imageFile.getParentFile().mkdirs();
        try {
            OutputStream fout = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fout);
            fout.flush();
            fout.close();
            ok = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ok = false;
        } catch (IOException e) {
            e.printStackTrace();
            ok = false;
        }
        return ok;
    }

    public static void copyFile(String src,String dst) {
        try {
            File fs = new File(src);
            File fd = new File(dst);
            copyFile(fs,fd);

        } catch(IOException e) {
            Log.e(LOG_TAG, "File copy failed: " + e.toString());
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static void WriteString(String filename,String str) throws IOException {
        File file = new File(filename);
        FileOutputStream fOut = new FileOutputStream(file);
        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
        myOutWriter.append(str);

        myOutWriter.close();

        fOut.flush();
        fOut.close();
    }

    public static String readFileAsString(String filePath) {
        String result = "";
        File file = new File(filePath);
        int size = 0;
        if ( file.exists() ) {
            BufferedInputStream fis = null;
            try {

                fis = new BufferedInputStream( new FileInputStream(file) );

                while ((size = fis.available()) > 0) {
                    byte[] buffer = new byte[size];
                    fis.read(buffer,0,size);
                    result = result + (new String(buffer,"UTF-8"));
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            } finally {
                if (fis != null)
                    try {
                        fis.close();
                    } catch (IOException ignored) {
                    }
            }
            //result = new String(buffer);
        }
        return result;
    }


    public static String readAssetAsString(String filePath, Context mApp) {
        String result = "";

        String fn = getFn(filePath);

        BufferedInputStream fis = null;
        int size = 0;
        try {
            fis = new BufferedInputStream(mApp.getAssets().open(fn));

            while ((size = fis.available()) > 0) {
                byte[] buffer = new byte[size];
                fis.read(buffer, 0, size);
                result = result + (new String(buffer, "UTF-8"));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
        }

        return result;
    }

    public static void copyFileFromAssets(String fn,String toPath, Context mApp) {
        File fileTo = new File(toPath);
        copyFileFromAssets(fn,fileTo,mApp);
    }


    public static void copyFileFromAssets(String fn,File fileTo, Context mApp) {

        FileOutputStream fOut;

        try {
            fOut = new FileOutputStream(fileTo);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File write failed: " + e.toString());
            return;
        }

        BufferedInputStream fis = null;
        try {
            fis = new BufferedInputStream(mApp.getAssets().open(fn));
            byte[] buf = new byte[16*1024];
            int len;
            while (true) {
                len = fis.read(buf);
                if(len > 0)
                    fOut.write(buf,0,len);
                else
                    break;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "File IO failed: " + e.toString() +" "+fn);
        } finally {
            try {  if (fis != null) fis.close();  } catch (IOException ignored) { }
            try {  if (fOut != null) fOut.close();  } catch (IOException ignored) { }
        }

    }


    public static  String newExt(String fn,String ext)
    {
        return fn.substring(0, fn.lastIndexOf('.')+1)+ext;
    }


    public static  String getFn(String filePath)
    {
        String[] arr = filePath.split("/");
        String fn = arr[arr.length - 1];
        return fn;
    }



}