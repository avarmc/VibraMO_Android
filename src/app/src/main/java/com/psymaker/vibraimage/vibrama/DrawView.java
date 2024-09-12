package com.psymaker.vibraimage.vibrama;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import com.psymaker.vibraimage.vibrama.ui.FragmentVI;

/**
 * Created by user on 31.05.2017.
 */

public class DrawView extends View {
    private static final String TAG = "DrawView";


    static class theLock extends Object {
    }

    private int mRatioWidth = 10;
    private int mRatioHeight = 5;


    protected Matrix mMatrix;

    protected int deviceRotation = Surface.ROTATION_0;

    public Bitmap mBitmap;
    public theLock mBitmapLock = new theLock();

    public FragmentVI mApp;

    public void L(String str) {
     //   Log.i(TAG, str);
    }

    public DrawView(Context context) {
        super(context);
//        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
    }

    public void Init(FragmentVI mApp) {
        this.mApp = mApp;
    }

    public void clear() {
        synchronized (mBitmapLock) {
            if(mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }

        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        synchronized (mBitmapLock) {
            if (mBitmap == null || mApp.mVideo == null || mApp.mVideo.mPreviewSize == null)
                return;

            int vw = getWidth();
            int vh = getHeight();

            if( mMatrix != null && mBitmap != null  ) {
                canvas.drawBitmap(mBitmap, mMatrix, null);
            }
        }


/*
        Paint p = new Paint();
        p.setColor(Color.GREEN);

        canvas.drawLine(0, 0, getWidth(),0, p);
        canvas.drawLine(0, 0, 0, getHeight(), p);
        canvas.drawLine(0,  getHeight(), getWidth(), getHeight(), p);
        canvas.drawLine(getWidth(),  0, getWidth(), getHeight(), p);

        canvas.drawLine(0, 0, getWidth(), getHeight(), p);
        canvas.drawLine(0, getHeight(), getWidth(), 0, p);
*/

    }


    public void setAspectRatio(int width, int height) {

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        if (mRatioWidth == width && mRatioHeight == height) {
            return;
        }
        mRatioWidth = width;
        mRatioHeight = height;
        synchronized (mBitmapLock)
        {
            mBitmap = null;
        }
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed,l,t,r,b);
        L("onLayout "+l+" "+t+" "+r+" "+b + " ("+(r-l)+"x"+(b-t)+")");
    }

    public void setTransform(Matrix transform)
    {
        synchronized (mApp.mProc.lockObject) {
            mMatrix = new Matrix(transform);
        }
    }
}