package com.psymaker.vibraimage.vibrama;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceScreen;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Created by user on 20.11.2018.
 */

public class VideoBase {

    /**
     * Camera state: Device is closed.
     */
    public static final int STATE_CLOSED = 0;

    /**
     * Camera state: Device is opened, but is not capturing.
     */
    public static final int STATE_OPENED = 1;

    /**
     * Camera state: Showing camera preview.
     */
    public static final int STATE_PREVIEW = 2;
    /**
     * Camera state: Waiting for 3A convergence before capturing a photo.
     */
    public static final int STATE_WAITING_FOR_3A_CONVERGENCE = 3;

    /**
     * Timeout for the pre-capture sequence.
     */
    public static final long PRECAPTURE_TIMEOUT_MS = 1000;

    /**
     * Tolerance when comparing aspect ratios.
     */
    public static final double ASPECT_RATIO_TOLERANCE = 0.005;

    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    /**
     * A lock protecting camera state.
     */
    public final Object mCameraStateLock = new Object();
    /**
     * The {@link Size} of camera preview.
     */

    public VibraimageActivityBase mApp;
    public Size mPreviewSize;

    public Fragment mView;
    public ImageProc mProc;
    public DrawView mDraw;
    public AutoFitTextureView mTextureView;

    public static long nFrame = 0;

    protected int mState = STATE_CLOSED;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    public static final int MAX_PREVIEW_WIDTH = 800;

    /**
     * ID of the current {@link CameraDevice}.
     */
    public static String mCameraId = null;
    public static String[] mCameras = null;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    public Handler mBackgroundHandler;
    /**
     * An additional thread for running tasks that shouldn't block the UI.  This is used for all
     * callbacks from the {@link CameraDevice} and {@link CameraCaptureSession}s.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A reference counted holder wrapping the {@link ImageReader} that handles Stream image captures.
     * This is used to allow us to clean up the {@link ImageReader} when all background tasks using
     * its {@link Image}s have completed.
     */
    public VideoBase.RefCountedAutoCloseable<ImageReader> mImageReader2;
    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * Stream image is ready to be saved.
     */
    public final ImageReader.OnImageAvailableListener mOnImageAvailableListener2
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();

            //    L("OnImageAvailableListener 2");
            if(mProc != null) {
                mProc.onTime( getTime() );
                mProc.onImage(image);
            }
            image.close();
            ++nFrame;
        }

    };


    /**
     * A {@link Handler} for showing {@link Toast}s on the UI thread.
     */
    private final Handler mMessageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Activity activity = mView.getActivity();
            if (activity != null) {
                Toast.makeText(activity, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public static void L(String str) {
        /* Log.i(TAG, str);*/
    }

    public void cameraStop() {
    }

    public void cameraStart() {
    }

    public void openCamera() {
    }

    public void closeCamera() {
    }

    public void cameraRestart() {
        cameraStop();
        cameraStart();
    }


    public void initCameras() {
        Activity activity = mView.getActivity();
        initCameras(activity);
    }

    public static void initCameras(Activity activity) {

        if (mCameras != null)
            return;

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        if (manager != null) {
            try {
                String[] cameras = manager.getCameraIdList();
                mCameras = new String[cameras.length + 1];
                for (int i = 0; i < cameras.length; ++i)
                    mCameras[i] = cameras[i];
                mCameras[cameras.length] = "*file";
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Return true if the given array contains the given integer.
     *
     * @param modes array to check.
     * @param mode  integer to get for.
     * @return true if the array contains the given integer, otherwise false.
     */
    public static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if the two given {@link Size}s have the same aspect ratio.
     *
     * @param a first {@link Size} to compare.
     * @param b second {@link Size} to compare.
     * @return true if the sizes have the same aspect ratio, otherwise false.
     */
    public static boolean checkAspectsEqual(Size a, Size b) {
        double aAspect = a.getWidth() / (double) a.getHeight();
        double bAspect = b.getWidth() / (double) b.getHeight();
        return Math.abs(aAspect - bAspect) <= ASPECT_RATIO_TOLERANCE;
    }

    public void configureTransform(int viewWidth, int viewHeight) {
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show.
     */
    public void showToast(String text) {
        // We show a Toast by sending request message to mMessageHandler. This makes sure that the
        // Toast is shown on the UI thread.
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }

    /**
     * Generate a string containing a formatted timestamp with the current date and time.
     *
     * @return a {@link String} representing a time.
     */
    public static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return sdf.format(new Date());
    }

    public static String getCameraID() {
        return mCameraId;
    }

    public static String getCameraID(int n) {
        if (n < 0 || n >= mCameras.length) return "";
        return mCameras[n];
    }

    public static void setCameraID(String sid) {
        mCameraId = new String(sid);
    }

    public static int compareSize(float ratio, Size s1, Size s2) {
        int dx = Math.abs(s1.getWidth() - s2.getWidth());
        int dy = Math.abs(s1.getHeight() - s2.getHeight());
        float r2 = 1.0f * s2.getWidth() / s2.getHeight();
        float dr = Math.abs(r2 - ratio) + 1;

        return Math.round(dx * dy * dr * dr);
    }

    public float getTime() { return -1; }

    public boolean checkState() {
        return true;
    }

    public boolean onCamera(int idCam) {
        if (mApp.mVi != null && mCameras != null && idCam < mCameras.length) {
            mApp.putStr("camera", mCameras[idCam]);
            cameraRestart();
        }
        return false;
    }

    /**
     * A wrapper for an {@link AutoCloseable} object that implements reference counting to allow
     * for resource management.
     */
    public static class RefCountedAutoCloseable<T extends AutoCloseable> implements AutoCloseable {
        private T mObject;
        private long mRefCount = 0;

        /**
         * Wrap the given object.
         *
         * @param object an object to wrap.
         */
        public RefCountedAutoCloseable(T object) {
            if (object == null) throw new NullPointerException();
            mObject = object;
        }

        /**
         * Increment the reference count and return the wrapped object.
         *
         * @return the wrapped object, or null if the object has been released.
         */
        public synchronized T getAndRetain() {
            if (mRefCount < 0) {
                return null;
            }
            mRefCount++;
            return mObject;
        }

        /**
         * Return the wrapped object.
         *
         * @return the wrapped object, or null if the object has been released.
         */
        public synchronized T get() {
            return mObject;
        }

        /**
         * Decrement the reference count and release the wrapped object if there are no other
         * users retaining this object.
         */
        @Override
        public synchronized void close() {
            if (mRefCount >= 0) {
                mRefCount--;
                if (mRefCount < 0) {
                    try {
                        mObject.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        mObject = null;
                    }
                }
            }
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        synchronized (mCameraStateLock) {
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    public void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                synchronized (mCameraStateLock) {
                    mBackgroundHandler = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void Sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return null;
    }


    public boolean isOption(String tag) {
        return false;
    }

    public void configure() {}

    public int[] getSupported(String tag) { return null; }
}
