package com.psymaker.vibraimage.vibrama.ui;

import androidx.fragment.app.Fragment;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.psymaker.vibraimage.vibrama.AutoFitTextureView;
import com.psymaker.vibraimage.vibrama.DrawView;
import com.psymaker.vibraimage.vibrama.VibraimageActivity;
import com.psymaker.vibraimage.vibrama.VibraimageActivityBase;
import com.psymaker.vibraimage.vibrama.jni;
import com.psymaker.vibraimage.vibrama.med.R;

import java.io.File;

/**
 * Created by user on 11.09.2017.
 */

public class FragmentWeb extends Fragment {
    VibraimageActivityBase mApp;

    private WebView mView = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        VibraimageActivity app = (VibraimageActivity)getActivity();
        mApp = app.base;

        mView = (WebView) view.findViewById(R.id.id_webView);

        mView.getSettings().setAllowContentAccess(true);
        mView.getSettings().setAllowFileAccess(true);
        mView.getSettings().setAllowFileAccessFromFileURLs(true);

        mView.getSettings().setJavaScriptEnabled(true);
        mView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mView.getSettings().setUseWideViewPort(true);
        mView.getSettings().setLoadWithOverviewMode(true);
        mView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mView.setScrollbarFadingEnabled(false);
        mView.getSettings().setBuiltInZoomControls(true);
        mView.getSettings().setDisplayZoomControls(true);

        mView.setWebChromeClient(new WebChromeClient());
        mView.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view,url);
                //Calling an init method that tells the website, we're ready
              //  mView.loadUrl("javascript:m2Init()");
            }


        });

        mView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    WebView webView = (WebView) v;

                    switch(keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                                mApp.showFragment(R.id.vi_fragment);
                                return true;
                    }
                }

                return false;
            }
        });

        mView.clearCache(true);
        mView.clearHistory();
    }

    public void load(Uri uri)
    {
        if(mView != null)
        {
            String url = uri.toString();
            mView.loadUrl(url);
        }
    }



}
