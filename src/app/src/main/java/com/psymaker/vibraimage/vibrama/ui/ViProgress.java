package com.psymaker.vibraimage.vibrama.ui;

import androidx.fragment.app.Fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.psymaker.vibraimage.vibrama.med.R;



public class ViProgress  extends Fragment {

    private ViProgressView mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mView = (ViProgressView) view.findViewById(R.id.progress_view);
    }

    public void check()
    {
        if(mView != null)
            mView.check();
    }

}
