package com.psymaker.vibraimage.vibrama.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.psymaker.vibraimage.vibrama.med.R;
import com.psymaker.vibraimage.vibrama.jni;

/**
 * Created by user on 18.07.2017.
 */

public class ViProgressView extends View {
    static class theLock extends Object {}
    static public theLock lockObject = new theLock();

    private Bitmap buffCanvasBitmap;
    private Canvas buffCanvas;

    private Paint mPaintB = new Paint();
    private Paint mPaintQ = new Paint();
    private Paint mPaintM = new Paint();
    private Paint mPaintW = new Paint();

    private float mM = -1;
    private float mQV = -1;
    private int mQT = -1;

    private Bitmap mState;


    public ViProgressView(Context context) {
        super(context);
        init();
    }
    public ViProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public ViProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public ViProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mPaintB.setColor(Color.BLACK);
     //   mPaintB = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintB.setTextSize(50);
    //    mPaintB.setStyle(Paint.Style.STROKE);

        mPaintM.setColor(Color.GREEN);
        mPaintW.setColor(Color.WHITE);
        mPaintQ.setColor(Color.GREEN);
    }

    public void check() {
        boolean changed = false;
        synchronized (lockObject) {
            float progressM = jni.EngineGetFt("VI_INFO_M_PROGRESS");
            float progressQV = jni.EngineGetFt("VI_INFO_CHQ_TEST_VALUE");
            int progressQT = jni.EngineGetIt("VI_INFO_CHQ_TEST_TYPE");

            if (progressM != mM) {
                changed = true;
                mM = progressM;
            }
            if (progressQV != mQV) {
                changed = true;
                mQV = progressQV;
            }
            if (progressQT != mQT) {
                mQT = progressQT;
                changed = true;
                switch(mQT)
                {
                    default: mState = null; break;
                    case 1:
                        mState = BitmapFactory.decodeResource(getResources(), R.drawable.qt1, null);
                        break;
                    case 2:
                        mState = BitmapFactory.decodeResource(getResources(), R.drawable.qt2, null);
                        break;
                    case 3:
                        mState =  BitmapFactory.decodeResource(getResources(), R.drawable.qt3, null);
                        break;
                }
            }
        }
        if(!changed) {
            int ext = jni.EngineGetIt("VI_INFO_TIME_VIDEO_EXT");
            if (ext == 1 )
            {
                changed = true;
            }
        }
        if(changed)
            postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();

        if(buffCanvasBitmap == null || buffCanvasBitmap.getWidth() != w || buffCanvasBitmap.getHeight() != h) {
            buffCanvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            buffCanvas = new Canvas();
            buffCanvas.setBitmap(buffCanvasBitmap);
        }

        buffCanvas.drawRect(0,0,w,h,mPaintW);

        synchronized (lockObject) {
            if (w > h)
                onDrawH( w, h);
            else
                onDrawV( w, h);
        }


        canvas.drawBitmap(buffCanvasBitmap, 0, 0, null);
    }

    protected void onDrawH(int w,int h) {
        int x0 = h;
        int h1a = 0, h1b = h/2;
        int h2a = h1b, h2b = h;

        if( mState != null )
        {
            buffCanvas.drawBitmap(mState,0.0f,0.0f,null);
         //   x0 += h;
        }

        int nw = w-x0;

        if(mM > 0) {
            buffCanvas.drawRect(x0, h1a, x0 + (int) (mM * nw), h1b, mPaintM);
        }

        if(mQV > 0) {
            if(mQV > 80)
                mPaintQ.setColor( Color.GREEN );
            else
            if(mQV > 50)
                mPaintQ.setColor( Color.YELLOW );
            else
                mPaintQ.setColor( Color.RED );

            buffCanvas.drawRect(x0, h2a, x0+(int) (0.01f * mQV * nw), h2b, mPaintQ);

        }

        int ext = jni.EngineGetIt("VI_INFO_TIME_VIDEO_EXT");
        if( ext == 1) {
            String tVideo = String.format("%.1f", jni.EngineGetFt("VI_INFO_TIME_VIDEO"));
            buffCanvas.drawText(tVideo, w - mPaintB.measureText(tVideo) - 10, (h - 20) / 2, mPaintB);
        }

    }

    protected void onDrawV(int w,int h) {
        int y0 = w;
        int w1a = 0, w1b = w/2;
        int w2a = w1b, w2b = w;


        if( mState != null )
        {
            buffCanvas.drawBitmap(mState,0.0f,0.0f,mPaintM);
          //  y0 += w;
        }

        int nh = h-y0;

        if(mM > 0)
            buffCanvas.drawRect(w1a,y0+nh-1,w1b,y0+nh-1-(int)(mM*nh),mPaintM);

        if(mQV > 0) {
            if(mQV > 80)
                mPaintQ.setColor( Color.GREEN );
            else
            if(mQV > 50)
                mPaintQ.setColor( Color.YELLOW );
            else
                mPaintQ.setColor( Color.RED );

            buffCanvas.drawRect(w2a,y0+nh,w2b,y0+nh-1-(int)(0.01f * mQV*nh),mPaintQ);

        }
        if( jni.EngineGetIt("VI_INFO_TIME_VIDEO_EXT") == 1) {
            String tVideo = String.format("%.1f", jni.EngineGetFt("VI_INFO_TIME_VIDEO"));
            buffCanvas.drawText(tVideo, 0, (h - 5), mPaintB);
        }
    }

}
