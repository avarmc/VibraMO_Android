package com.psymaker.vibraimage.vibrama;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class Http {

    public Http(Handler h) {
       this.mMessageHandler = h;
    }
    private static String	mHttpRead = null;
    private static String	mHttpLocation = null;
    private static String	mHttpError = null;

    private int safelyConnectHTTP(String uri, HttpURLConnection connection) throws IOException {

        try {
            connection.connect();
        } catch (NullPointerException npe) {
            // this is an Android bug: http://code.google.com/p/android/issues/detail?id=16895
            throw new IOException(npe.toString());
        } catch (IllegalArgumentException iae) {
            // Also seen this in the wild, not sure what to make of it. Probably a bad URL
            throw new IOException(iae.toString());
        } catch (SecurityException se) {
            // due to bad VPN settings?
            throw new IOException(se.toString());
        } catch (IndexOutOfBoundsException ioobe) {
            // Another Android problem? https://groups.google.com/forum/?fromgroups#!topic/google-admob-ads-sdk/U-WfmYa9or0
            throw new IOException(ioobe.toString());
        }

        try {
            return connection.getResponseCode();
        } catch (NullPointerException npe) {
            throw new IOException(npe.toString());
        } catch (NumberFormatException nfe) {
            // Again seen this in the wild for bad header fields in the server response!
            throw new IOException(nfe.toString());
        }
    }

    public String readFileHTTP(String location)
    {
        mHttpError = null;
        mHttpRead = null;
        mHttpLocation = location;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                // Log.d(TAG, "VIAPP.readFileHTTP() run");
                String str = "";

                // Log.d(TAG, "VIAPP.readFileHTTP() 1 "+mHttpLocation);

                URL url = null;
                HttpURLConnection urlConnection = null;

                try {
                    url = new URL(mHttpLocation);
                    // Log.d(TAG, "VIAPP.readFileHTTP() 2 ");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    // Log.d(TAG, "VIAPP.readFileHTTP() 3 ");

                    // Log.d(TAG, "VIAPP.readFileHTTP() 4 "+urlConnection.toString());
                    safelyConnectHTTP(mHttpLocation,urlConnection);

                    // Log.d(TAG, "VIAPP.readFileHTTP() 5");
                    InputStream is = urlConnection.getInputStream();
                    // Log.d(TAG, "VIAPP.readFileHTTP() 6 ");
                    if(is == null)
                    {
                        // Log.d(TAG, "VIAPP.readFileHTTP() 7");
                        return;
                    }
                    InputStream in = new BufferedInputStream(is);
                    // Log.d(TAG, "VIAPP.readFileHTTP() 8 ");
                    int l,size = 0;
                    byte[] tmp = new byte[8192];
                    while ((l = in.read(tmp)) != -1) {
                        tmp[l] = 0;
                        size += l;
                        // Log.d(TAG, "VIAPP.readFileHTTP() read "+l+" size "+size+" str ["+(new String(tmp,"UTF-8") )+"]");
                        str += new String(Arrays.copyOfRange(tmp,0,l),"UTF-8");
                    }
                    mHttpRead = new String(str);
                    // Log.d(TAG, "VIAPP.readFileHTTP() 9");
                    urlConnection.disconnect();

                }
                catch (MalformedURLException e) {
                    mHttpError = e.getLocalizedMessage();
                    // Log.d(TAG, "VIAPP.readFileHTTP() "+e.getLocalizedMessage());
                    return;
                }
                catch (IOException e) {
                    mHttpError = e.getLocalizedMessage();
                    // Log.d(TAG, "VIAPP.readFileHTTP() "+e.getLocalizedMessage());
                    return;
                }


                // Log.d(TAG, "VIAPP.readFileHTTP() 2 ");
            }
        });

        thread.start();

        while( thread.isAlive() )
        {
            // Log.d(TAG, "VIAPP.readFileHTTP() sleep");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {

            }
        }


        if(mHttpError != null) {
            Alert(mHttpError);
        }
        // Log.d(TAG, "VIAPP.readFileHTTP() result "+mHttpRead);

        mHttpLocation = null;
        if(mHttpRead == null)
            return "";
        return new String(mHttpRead);
    }

    private Handler mMessageHandler = null;

    private void Alert( String str )
    {
        if(mMessageHandler == null)
            return;

        Message message = Message.obtain();
        message.obj = str;
        mMessageHandler.sendMessage(message);
    }
}
