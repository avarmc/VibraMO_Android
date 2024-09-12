package com.psymaker.vibraimage.vibrama.pref;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.preference.*;
import android.util.AttributeSet;

import com.psymaker.vibraimage.vibrama.med.R;


public class PreferenceString extends EditTextPreference {

    public String mDst = "";

    public PreferenceString(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context,attrs,defStyleAttr,defStyleRes);
        loadScale(context,attrs);
        setLayoutResource(R.layout.preference_line);
    }
    public PreferenceString(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context,attrs,defStyleAttr);
        loadScale(context,attrs);
        setLayoutResource(R.layout.preference_line);
    }
    public PreferenceString(Context context, AttributeSet attrs)
    {
        super(context,attrs);
        loadScale(context,attrs);
        setLayoutResource(R.layout.preference_line);
    }
    public PreferenceString(Context context)
    {
        super(context);
        setLayoutResource(R.layout.preference_line);
    }

    protected  void loadScale(Context context, AttributeSet attrs)
    {
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.vicfg);

        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i)
        {
            int attr = a.getIndex(i);
            switch (attr)
            {
                 case R.styleable.vicfg_dst:
                    mDst = new String(a.getString(attr));
                    break;
            }
        }
        a.recycle();
    }

    public void setSummaryValue(String v) {
        super.setSummary(v);
    }
}
