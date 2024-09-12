package com.psymaker.vibraimage.vibrama;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureView {

    private static final String TAG = "AutoFitTextureView";

    private int mRatioWidth = 10;
    private int mRatioHeight = 5;


    public boolean mMeasure = true;

    public void L(String str)
    {
   //     Log.i(TAG, str);
    }

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        if (mRatioWidth == width && mRatioHeight == height) {
            return;
        }
        mRatioWidth = width;
        mRatioHeight = height;

        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.i(TAG,"onMeasure "+widthMeasureSpec+" "+heightMeasureSpec);
        mMeasure = true;
        int cw = getWidth();
        int ch = getHeight();

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int nw,nh;
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            nw = width;
            nh = height;
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                nw = width;
                nh = width * mRatioHeight / mRatioWidth;
            } else {
                nw = height * mRatioWidth / mRatioHeight;
                nh = height;
            }
        }

        setMeasuredDimension(nw, nh);

        if(cw != 0 && ch != 0 )
        {
      //      if( (nw != cw || nh != ch) && VibraimageActivity.mInstance.mVi != null )
      //          VibraimageActivity.mInstance.mVi.configureTransform(nw,nh);
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed,l,t,r,b);
        L("onLayout "+l+" "+t+" "+r+" "+b + " ("+(r-l)+"x"+(b-t)+")");
    }



}
