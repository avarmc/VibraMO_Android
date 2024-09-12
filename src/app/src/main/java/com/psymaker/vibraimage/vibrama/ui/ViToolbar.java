package com.psymaker.vibraimage.vibrama.ui;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.psymaker.vibraimage.vibrama.med.R;
import com.psymaker.vibraimage.vibrama.VibraimageActivity;


public class ViToolbar  extends Fragment  implements View.OnClickListener {

    private static final String TAG = "ViToolbar";

    VibraimageActivity mApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (VibraimageActivity)getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_toolbar, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
///        view.findViewById(R.id.picture).setOnClickListener(this);

        Toolbar myToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if(myToolbar != null) {
            mApp.setSupportActionBar(myToolbar);
            ActionBar ab = mApp.getSupportActionBar();
            ab.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
          super.onPause();
    }

    @Override
    public void onClick(View view) {
        Log.i(TAG,"onClick");
        /*
        switch (view.getId()) {

            case R.id.picture: {
                takePicture();
                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }
        }
        */
    }
}
