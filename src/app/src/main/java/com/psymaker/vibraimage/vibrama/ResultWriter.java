package com.psymaker.vibraimage.vibrama;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.psymaker.vibraimage.vibrama.med.R;
import com.psymaker.vibraimage.vibrama.ui.FragmentWeb;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.TreeMap;

/**
 * Created by user on 11.09.2017.
 */

public class ResultWriter {

    private static final String LOG_TAG = "ResultWriter";
    private static TreeMap<String, File> mSrcFiles = new TreeMap<String, File>();
    private static String lastFile;
    private VibraimageActivityBase mApp;
    private static float mProgress = -1;

    public ResultWriter(VibraimageActivityBase mApp) {
        this.mApp = mApp;
    }

    public static float getProgress() {
        return mProgress;
    }


    public void export_file(final String src_xml_file, final String src_html_file, final String dst_html_file, final int bOverwrite) {

        synchronized (mApp.mProc.lockObject)
        {
            mProgress = 0;
            mApp.mProc.mSkip++;
            lastFile = null;
        }


        Thread thread = new Thread(new Runnable() {
            public void run() {

                String str_xml = readFileAsString(src_xml_file);

                if(src_html_file.endsWith(".html"))
                    export_html(src_html_file,dst_html_file,new String(str_xml));
                else
                    export_xml( src_html_file, dst_html_file,str_xml );

                lastFile = dst_html_file;

                openResults();

                synchronized (mApp.mProc.lockObject) {
                    mProgress = -1;
                    mApp.mProc.mSkip--;
                }
            }
        });

        thread.start();

    }

    private static void copyFile(String src,String dst) {
        try {
            File fs = new File(src);
            File fd = new File(dst);
            copyFile(fs,fd);

        } catch(IOException e) {
            Log.e("Exception", "File copy failed: " + e.toString());
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
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

    private void export_xml(String src_base, String dst_base, String str_xml)
    {
        String fnXls = newExt(dst_base,"html");
        String fnXml = newExt(dst_base,"xml");
        String fnXls1 = getFn(src_base);

        copyFileFromAssets(fnXls1 ,fnXls);

        try {
            WriteString(fnXml,str_xml);
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    private void WriteString(String filename,String str) throws IOException {

        try {
            File file = new File(filename);
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(str);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException ex)
         {
                ex.printStackTrace();
            }
    }

    private String replace1(String str,String from,String to) {
        int pos = str.indexOf(from);
        if(pos < 0)
            return str;
        StringBuilder result = new StringBuilder(str.length()+to.length());
        result.append( str.substring(0,pos) );
        result.append( to );
        result.append( str.substring(pos+from.length()) );
        return result.toString();
    }
    private void export_html(final String src_html_file, final String dst_html_file, String str_xml)
    {
        mApp.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mApp.getApp(), mApp.getString(R.string.processing_measure)+" "+dst_html_file, Toast.LENGTH_LONG).show();
            }
        });


        String str_tmp = readAssetAsString(src_html_file);

        str_xml = str_xml.replaceAll("[\r\n\t]", "");
        str_xml = str_xml.replace("\"", "\\\"");
      //  str_tmp = str_tmp.replace("{%XML_TEXT%}", str_xml);
        str_tmp = replace1(str_tmp,"{%XML_TEXT%}", str_xml);
        str_tmp = str_tmp.replace("{%SELF%}", "file://"+dst_html_file);

        try {

            WriteString(dst_html_file,str_tmp);

            lastFile = new String(dst_html_file);

            mProgress = 1.0f;

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void startFile(String name)
    {
        if(name != null)
            openInBrowser(new File(name));
    }


    private String newExt(String fn,String ext)
    {
        return fn.substring(0, fn.lastIndexOf('.')+1)+ext;
    }

    public String get_mime_type(String url) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(url);
        String mime = null;
        if (ext != null) {
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }
        return mime;
    }
    private void openInBrowser(File file) {
        final Uri uri = Uri.fromFile(file);

        if( mApp.getApp().getSupportFragmentManager() == null)
            return;
        FragmentWeb web = (FragmentWeb) mApp.getApp().getSupportFragmentManager().findFragmentById(R.id.vi_fragment_web);
        if(web == null)
            return;
        web.load(uri);
        mApp.showFragment(R.id.vi_fragment_web);


    }

    private String getFn(String filePath)
    {
        String[] arr = filePath.split("/");
        String fn = arr[arr.length - 1];
        return fn;
    }

    private String readAssetAsString(String filePath) {
        String result = "";

        String fn = getFn(filePath);

        BufferedInputStream fis = null;
        int size = 0;
        try {
            fis = new BufferedInputStream(mApp.getApp().getAssets().open(fn));

            while ((size = fis.available()) > 0) {
                byte[] buffer = new byte[size];
                fis.read(buffer, 0, size);
                result = result + (new String(buffer, "UTF-8"));
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, e.toString());
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
        }

        return result;
    }

    private void copyFileFromAssets(String fn,String toPath) {

        File fileTo = new File(toPath);
        FileOutputStream fOut;

        try {
            fOut = new FileOutputStream(fileTo);
        } catch (FileNotFoundException e) {
            Log.e("Exception", "File write failed: " + e.toString());
            return;
        }

        BufferedInputStream fis = null;
        int size = 0;
        try {
            fis = new BufferedInputStream(mApp.getApp().getAssets().open(fn));

            while ((size = fis.available()) > 0) {
                byte[] buffer = new byte[size];
                fis.read(buffer, 0, size);
                fOut.write(buffer);
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, e.toString());
        } finally {
            try {  if (fis != null) fis.close();  } catch (IOException ignored) { }
            try {  if (fOut != null) fOut.close();  } catch (IOException ignored) { }
        }

    }


    private static String readFileAsString(String filePath) {
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
                Log.d(LOG_TAG, e.toString());
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

    public static boolean canOpenResults() {
        if(ResultWriter.lastFile == null)
            return false;
        return lastFile.length() > 0;
    }

    public void openResults() {

        mApp.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mApp.getApp(), mApp.getString(R.string.ready_measure)+" "+lastFile, Toast.LENGTH_SHORT).show();
                if( jni.EngineGetIt("VI_INFO_XLS_OPEN") != 0 )
                    startFile(lastFile);
            }
        });
    }
}
