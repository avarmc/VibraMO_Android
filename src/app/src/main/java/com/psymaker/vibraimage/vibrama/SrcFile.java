package com.psymaker.vibraimage.vibrama;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Size;
import android.view.Surface;
import android.view.View;

/**
 * Created by user on 20.11.2018.
 */

public class SrcFile extends VideoBase {

    private SrcFileAvi mSrc = new SrcFileAvi(this);;

    public float mTimeVideo = 0;

    public Size mVideoSize;

    public boolean mStarted = false;

    public SrcFile() {
    }

    public static String getFile() {
        return SrcFileAvi.mFilePath;
    }

    public static void setFile(String file) {
        SrcFileAvi.mFilePath = new String(file);
    }

    @Override
    public void cameraStop()
    {
        closeCamera();
    }

    @Override
    public void cameraStart() {
        openCamera();
    }

    @Override
    public void openCamera() {
        mCameraId = "*file";
        mState = STATE_OPENED;
        mStarted = true;
    }

    @Override
    public float getTime() {
        return mTimeVideo;
    }

    private void start() {

        if(mSrc != null) {
            mSrc.stop();
        }

        if(mDraw != null) {
            mDraw.clear();
        }
        mTextureView.setVisibility( View.INVISIBLE );

        mCameraId = "*file";
        mState = STATE_OPENED;
        mStarted = true;
        mTimeVideo = 0;

        jni.EnginePutIt("VI_VAR_RESET_TIMER",1);
        startBackgroundThread();


        try {
            mSrc.testExtractMpegFrames();
        } catch (Throwable th) {
            closeCamera();
        }

    }

    @Override
    public void closeCamera() {
        mCameraId = "";

        mStarted = false;
        mState = STATE_CLOSED;
        stopBackgroundThread();

        if(mSrc != null) {
            mSrc.stop();
        }

        if (null != mImageReader2) {
            mImageReader2.close();
            mImageReader2 = null;
        }
        mVideoSize = null;
        mTimeVideo = 0;
    }
    @Override
    public boolean checkState() {
        return true;
    }



    @Override
    public void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = mView.getActivity();
        synchronized (mCameraStateLock) {
            if (null == mTextureView || null == activity) {
                return;
            }

            // For still image captures, we always use the largest available size.

            // Find the rotation of the device relative to the native device orientation.
            int deviceRotation = 270; // activity.getWindowManager().getDefaultDisplay().getRotation();
            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);


            // Find the best preview size for these view dimensions and configured JPEG size.
            Size previewSize = mSrc.getFileSize();



            mTextureView.setAspectRatio(
                    previewSize.getWidth(), previewSize.getHeight());
            mDraw.setAspectRatio(
                    previewSize.getWidth(), previewSize.getHeight());


            // Find rotation of device in degrees (reverse device orientation for front-facing
            // cameras).
            int rotation = deviceRotation;

            Matrix matrixV = new Matrix();
            Matrix matrixD = new Matrix();

            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            //   RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
            RectF bufferRect = new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();


            mProc.onOrientationChanged(3);

            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrixV.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

            float scale = Math.min(
                    (float) viewWidth / previewSize.getWidth(),
                    (float) viewHeight / previewSize.getHeight());
            matrixV.postScale(scale, scale, centerX, centerY);

            matrixD.setRectToRect(new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight()), new RectF(0, 0, viewWidth, viewHeight), Matrix.ScaleToFit.FILL);
            matrixD.postRotate((rotation + 90) % 360, centerX, centerY);

            matrixV.postRotate(-rotation, centerX, centerY);


            if (Surface.ROTATION_270 != deviceRotation) {
                matrixD.postScale(1.0f, -1.0f, centerX, centerY);
                matrixV.postRotate(180, centerX, centerY);
                L("rotation 1");
            } else {
                L("rotation 2");
                matrixV.postRotate(180, centerX, centerY);
            }


            mDraw.setTransform(matrixD);
            mTextureView.setTransform(matrixV);

            // Start or restart the active capture session if the preview was initialized or
            // if its aspect ratio changed significantly.
            if (mPreviewSize == null || !checkAspectsEqual(previewSize, mPreviewSize)) {
                mPreviewSize = previewSize;
                if (mState != STATE_CLOSED ) {
                    start();
                }
            }
        }
    }



    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public void onBitmap( Bitmap bmp ) {
      //  synchronized (mDraw.mBitmapLock)
        {

//            Bitmap gray = toGrayscale(bmp);
//            mProc.onImage(gray);
//            gray.recycle();
            mProc.onTime( getTime() );
            mProc.onImage(bmp);
        }
    }
}
