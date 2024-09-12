package com.psymaker.vibraimage.vibrama.ui;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.legacy.app.FragmentCompat;

import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.psymaker.vibraimage.vibrama.AutoFitTextureView;
import com.psymaker.vibraimage.vibrama.DrawView;
import com.psymaker.vibraimage.vibrama.ImageProc;
import com.psymaker.vibraimage.vibrama.OpenFileDialog;
import com.psymaker.vibraimage.vibrama.SrcCamera;
import com.psymaker.vibraimage.vibrama.SrcFile;
import com.psymaker.vibraimage.vibrama.VideoBase;
import com.psymaker.vibraimage.vibrama.med.R;
import com.psymaker.vibraimage.vibrama.VibraimageActivity;
import com.psymaker.vibraimage.vibrama.VibraimageActivityBase;
import com.psymaker.vibraimage.vibrama.jni;

import java.util.Comparator;


public class FragmentVI extends Fragment {

    private VibraimageActivityBase mApp;

    public VideoBase mVideo;







    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "VIFragment";



    /**
     * An {@link OrientationEventListener} used to determine when device rotation has occurred.
     * This is mainly necessary for when the device is rotated by 180 degrees, in which case
     * onCreate or onConfigurationChanged is not called as the view dimensions remain the same,
     * but the orientation of the has changed, and thus the preview rotation must be updated.
     */
    private OrientationEventListener mOrientationListener;

    private View.OnTouchListener     mTouchListener;



    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events of a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            if(mVideo != null) {
                synchronized (mVideo.mCameraStateLock) {
                    mVideo.mPreviewSize = null;
                }
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };



    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;
    public ImageProc mProc;
    public DrawView mDraw;





    //**********************************************************************************************



    public static void L(String str)
    {
        /* Log.i(TAG, str);*/
    }



    public static FragmentVI newInstance() {
        return new FragmentVI();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        VibraimageActivity app = (VibraimageActivity)getActivity();
        mApp = app.base;

        SrcFile.setFile( mApp.getStr("videoFile") );

        String camID = mApp.getStr("camera");
        if( camID.equals("*file"))
            mApp.putStr("camera", "*file" );

        return inflater.inflate(R.layout.fragment_vi, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        VibraimageActivity app = (VibraimageActivity)getActivity();
        mApp = app.base;

        if(mProc == null) {
            mProc = app.getImageProc();
        }

        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mDraw = (DrawView) view.findViewById(R.id.draw_view);
        mDraw.mApp = this;

        // Setup a new OrientationEventListener.  This is used to handle rotation events like a
        // 180 degree rotation that do not normally trigger a call to onCreate to do view re-layout
        // or otherwise cause the preview TextureView's size to change.
        mOrientationListener = new OrientationEventListener(getActivity(),
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if ( mTextureView != null && mTextureView.isAvailable() ) {
                    //if(mTextureView.mMeasure )
                    {
//                        configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
                        configureTransform();
                        mTextureView.mMeasure = false;
                    }
                }
            }
        };

        mTouchListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                L("Touch "+event.getAction());

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    jni.EnginePutIt("VI_FILTER_PAUSE",0);
                    jni.EnginePutIt("VI_VAR_RESET",1);
                }
                return true;
            }
        };

        mDraw.setOnTouchListener(mTouchListener);

        configureTransform();
    }




    public void cameraStop()
    {
        mProc.onPause();
        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }

        if(mVideo != null) {
            mVideo.cameraStop();
            mVideo = null;
        }
    }

    public void cameraStart()
    {
        int nTest= jni.seqGetTest();

        if( nTest != 0 )
            return;

        VideoBase.initCameras( getActivity() );

        String idDefCam = mApp.getStr("camera");

        if (!mTextureView.isAvailable()) {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        if(mVideo == null) {
            boolean pass = false;

            if(idDefCam != null && idDefCam.equals("*file")) {
                if(!SrcFile.getFile().isEmpty()) {
                    mVideo = new SrcFile();
                    mApp.putStr("videoFile", SrcFile.getFile());
                    pass = true;
                } else
                    mApp.putStr("camera","0");
            }

            if(!pass){
                mApp.putStr("videoFile","");
                mVideo = new SrcCamera();
            }

            mVideo.mApp = mApp;
            mVideo.mView = this;
            mVideo.mProc = mProc;
            mVideo.mDraw = mDraw;
            mVideo.mTextureView = mTextureView;
        }

        if(mVideo != null) {
            mVideo.cameraStart();
        }

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we should
        // configure the preview bounds here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            configureTransform();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        if (mOrientationListener != null && mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }

        cameraConfigure();

        mProc.onResume();
    }

    public void cameraRestart()
    {
        cameraStop();
        cameraStart();

    }

    public void cameraConfigure() {
        if(mVideo != null) {
            mVideo.configure();
        }
    }


    public void onResumeApp() {
        cameraStart();
    }


    public void onPauseApp() {

       cameraStop();

     super.onPause();

    }



    /**
     * Configure the necessary {@link android.graphics.Matrix} transformation to `mTextureView`,
     * and start/restart the preview capture session if necessary.
     * <p/>
     * This method should be called after the camera state has been initialized in
     * setUpCameraOutputs.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if( mVideo != null ) {
            mVideo.configureTransform(viewWidth,viewHeight);
        }
    }



    // Utility classes and methods:
    // *********************************************************************************************

    /**
     * Comparator based on area of the given {@link Size} objects.
     */
    public  static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * A dialog fragment for displaying non-recoverable errors; this {@ling Activity} will be
     * finished once the dialog has been acknowledged by the user.
     */
    public static class ErrorDialog extends DialogFragment {

        private String mErrorMessage;

        public ErrorDialog() {
            mErrorMessage = "Unknown error occurred!";
        }

        // Build a dialog with a custom message (Fragments require default constructor).
        public static ErrorDialog buildErrorDialog(String errorMessage) {
            ErrorDialog dialog = new ErrorDialog();
            dialog.mErrorMessage = errorMessage;
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(mErrorMessage)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }




    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show.
     */
    private void showToast(String text) {
        // We show a Toast by sending request message to mMessageHandler. This makes sure that the
        // Toast is shown on the UI thread.
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }


    /**
     * A {@link Handler} for showing {@link Toast}s on the UI thread.
     */
    private final Handler mMessageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };


    public void setCameraID(String sid) {  VideoBase.setCameraID(sid);  }
    public String getCameraID() {
        return VideoBase.getCameraID();
    }
    public String getCameraID(int n) {
        return VideoBase.getCameraID(n);
    }

    private boolean onCameraSet(int idCam) {
        String camID = getCameraID(idCam);

         mApp.putStr("camera", camID );

        cameraRestart();

        return false;
    }

    public boolean onCamera(final int idCam) {
        String camID = getCameraID(idCam);
        String filters[] = { ".*\\.mp4", ".*\\.avi" };
        if(camID.equals("*file")) {
            OpenFileDialog fileDialog = new OpenFileDialog(mApp.getApp())
                    .setFilter( filters )
                    .setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
                        @Override
                        public void OnSelectedFile(String fileName) {
                            Toast.makeText(mApp.getApp(), fileName, Toast.LENGTH_LONG).show();

                            SrcFile.setFile(fileName);
                            onCameraSet(idCam);
                        }
                    });
            fileDialog.show();
            return false;
        }

        return onCameraSet(idCam);
    }

    public void configureTransform() {
        View view = getView();
        if(view != null)
            configureTransform( view.getWidth(), view.getHeight() );
    }


}
