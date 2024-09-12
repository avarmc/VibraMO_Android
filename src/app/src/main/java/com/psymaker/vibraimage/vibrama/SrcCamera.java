package com.psymaker.vibraimage.vibrama;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.psymaker.vibraimage.vibrama.ui.FragmentVI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by user on 20.11.2018.
 */

public class SrcCamera extends VideoBase {

    private static final String TAG = "SrcCamera";

    static class theLock extends Object {}
    static public theLock lockObject = new theLock();

    private Map<String,Integer> mOptionOff = new TreeMap<String,Integer>();

    /**
     * A counter for tracking corresponding {@link CaptureRequest}s and {@link CaptureResult}s
     * across the {@link CameraCaptureSession} capture callbacks.
     */
    private final AtomicInteger mRequestCounter = new AtomicInteger();

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);



    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the open {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link CameraCharacteristics} for the currently configured camera device.
     */
    private CameraCharacteristics mCharacteristics;



    /**
     * A reference counted holder wrapping the {@link ImageReader} that handles JPEG image
     * captures. This is used to allow us to clean up the {@link ImageReader} when all background
     * tasks using its {@link Image}s have completed.
     */
    private VideoBase.RefCountedAutoCloseable<ImageReader> mJpegImageReader;


    /**
     * Whether or not the currently configured camera device is fixed-focus.
     */
    private boolean mNoAFRun = false;

    /**
     * Number of pending user requests to capture a photo.
     */
    private int mPendingUserCaptures = 0;

    /**
     * Request ID to {@link SrcCamera.ImageSaver.ImageSaverBuilder} mapping for in-fragment_progress JPEG captures.
     */
    private final TreeMap<Integer, SrcCamera.ImageSaver.ImageSaverBuilder> mJpegResultQueue = new TreeMap<>();

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * Timer to use with pre-capture sequence to ensure a timely capture if 3A convergence is
     * taking too long.
     */
    private long mCaptureTimer;


    private boolean bReadParam = true;

    protected static int[]    colorTransformMatrix   = new int[]{258, 128, -119, 128, -10, 128, -40, 128, 209, 128, -41, 128, -1, 128, -74, 128, 203, 128};
    protected static float    multiplierR     = 1.6f;
    protected static float    multiplierG     = 1.0f;
    protected static float    multiplierB     = 2.4f;

    /**
     * {@link CameraDevice.StateCallback} is called when the currently active {@link CameraDevice}
     * changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here if
            // the TextureView displaying this has been set up.
            synchronized (mCameraStateLock) {
                mState = STATE_OPENED;
                mCameraOpenCloseLock.release();
                mCameraDevice = cameraDevice;

                // Start the preview session if the TextureView has been set up already.
                if (mPreviewSize != null && mTextureView.isAvailable()) {
                    createCameraPreviewSessionLocked();
                }
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            synchronized (mCameraStateLock) {
                mState = STATE_CLOSED;
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.e(TAG, "Received camera device error: " + error);
            synchronized (mCameraStateLock) {
                mState = STATE_CLOSED;
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
            }
            Activity activity = mView.getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * JPEG image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnJpegImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            dequeueAndSaveImage(mJpegResultQueue, mJpegImageReader);
        }

    };




    public  Integer  mCameraAfState = null;

    private int cameraFaceMode = -1;
    public Runnable cameraRestart =
            new Runnable() {
                public void run() {
                    cameraRestart();
                }
            };

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events for the preview and
     * pre-capture sequence.
     */
    private CameraCaptureSession.CaptureCallback mPreCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            Log.i("RESULT", "r: " + result);

            if(bReadParam)
                readParam();

            if(result == null)
                return;

            mCameraAfState = result.get(CaptureResult.CONTROL_AF_STATE);

            synchronized (mCameraStateLock) {

                switch (mState) {
                    case STATE_PREVIEW: {

                        if( jni.isStarted() == 1 && jni.EngineGetIt("VI_FACE_MODE") == 1 ) {
                            Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);

                            int orientation_offset = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);


                            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
                            if (faces != null)
                                Log.i("FACE", "faces: " + faces.length + " " + orientation_offset);

                            if (faces != null && mode != null && mCharacteristics != null) {


                                if (faces.length > 0) {
                                    Rect rMax = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

                                    for (int i = 0; i < faces.length; ++i) {
                                        Rect r = faces[i].getBounds();
                                        mProc.onFaceDetected(r, rMax);
                                    }
                                } else
                                    mProc.onFaceNotDetected();
                            }
                        };
                        break;
                    }
                    case STATE_WAITING_FOR_3A_CONVERGENCE: {
                        boolean readyToCapture = true;
                        if (!mNoAFRun) {
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                break;
                            }

                            // If auto-focus has reached locked state, we are ready to capture
                            readyToCapture =
                                    (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                            afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
                        }

                        // If we are running on an non-legacy device, we should also wait until
                        // auto-exposure and auto-white-balance have converged as well before
                        // taking a picture.
                        if (!isLegacyLocked()) {
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
                            if (aeState == null || awbState == null) {
                                break;
                            }

                            readyToCapture = readyToCapture &&
                                    aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED &&
                                    awbState == CaptureResult.CONTROL_AWB_STATE_CONVERGED;
                        }

                        // If we haven't finished the pre-capture sequence but have hit our maximum
                        // wait timeout, too bad! Begin capture anyway.
                        if (!readyToCapture && hitTimeoutLocked()) {
                            Log.w(TAG, "Timed out waiting for pre-capture sequence to complete.");
                            readyToCapture = true;
                        }

                        if (readyToCapture && mPendingUserCaptures > 0) {
                            // Capture once for each user tap of the "Picture" button.
                            while (mPendingUserCaptures > 0) {
                                captureStillPictureLocked();
                                mPendingUserCaptures--;
                            }
                            // After this, the camera will go back to the normal state of preview.
                            mState = STATE_PREVIEW;
                        }
                    }
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles the still JPEG and RAW capture
     * request.
     */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                     long timestamp, long frameNumber) {
            String currentDateTime = generateTimestamp();

            File jpegFile = new File(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "JPEG_" + currentDateTime + ".jpg");

            // Look up the ImageSaverBuilder for this request and update it with the file name
            // based on the capture start time.
            SrcCamera.ImageSaver.ImageSaverBuilder jpegBuilder;
            int requestId = (int) request.getTag();
            synchronized (mCameraStateLock) {
                jpegBuilder = mJpegResultQueue.get(requestId);
            }

            if (jpegBuilder != null) jpegBuilder.setFile(jpegFile);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            int requestId = (int) request.getTag();
            SrcCamera.ImageSaver.ImageSaverBuilder jpegBuilder;
            SrcCamera.ImageSaver.ImageSaverBuilder rawBuilder;
            StringBuilder sb = new StringBuilder();

            // Look up the ImageSaverBuilder for this request and update it with the CaptureResult
            synchronized (mCameraStateLock) {
                jpegBuilder = mJpegResultQueue.get(requestId);

                if (jpegBuilder != null) {
                    jpegBuilder.setResult(result);
                    sb.append("Saving JPEG as: ");
                    sb.append(jpegBuilder.getSaveLocation());
                }


                // If we have all the results necessary, save the image to a file in the background.
                handleCompletionLocked(requestId, jpegBuilder, mJpegResultQueue);

                finishedCaptureLocked();
            }

            showToast(sb.toString());
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureFailure failure) {
            int requestId = (int) request.getTag();
            synchronized (mCameraStateLock) {
                mJpegResultQueue.remove(requestId);
                finishedCaptureLocked();
            }
            showToast("Capture failed!");
        }

    };


    @Override
    public void cameraStop()
    {
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    public void cameraStart() {
        nFrame = 0;
        jni.EnginePutIt("VI_VAR_RESET_TIMER",1);

        startBackgroundThread();
        openCamera();
    }

    /**
     * Sets up state related to camera that is needed before opening a {@link CameraDevice}.
     */
    private boolean setUpCameraOutputs() {
        Activity activity = mView.getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            FragmentVI.ErrorDialog.buildErrorDialog("This device doesn't support Camera2 API.").
                    show(mApp.getApp().getSupportFragmentManager(), "dialog");
            return false;
        }
        try {
            boolean bCameraInList = false;
            String idDefCam = mApp.getStr("camera");


            initCameras();

            for (String cameraId : mCameras) {
                if(idDefCam.isEmpty() && cameraId.equals("1"))
                    idDefCam = "1";

                if (cameraId.equals(idDefCam))
                    bCameraInList = true;
            }

            // Find a CameraDevice that supports RAW captures, and configure state.
            for (String cameraId : mCameras) {
                if(bCameraInList && !cameraId.equals(idDefCam))
                    continue;

                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);


                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                Size largestJpeg = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new FragmentVI.CompareSizesByArea());

                Size captureSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        0, 0,
                        largestJpeg);

                synchronized (mCameraStateLock) {
                    // Set up ImageReaders for JPEG and RAW outputs.  Place these in a reference
                    // counted wrapper to ensure they are only closed when all background tasks
                    // using them are finished.
                    if (mJpegImageReader == null || mJpegImageReader.getAndRetain() == null) {
                        mJpegImageReader = new VideoBase.RefCountedAutoCloseable<>(
                                ImageReader.newInstance(largestJpeg.getWidth(),
                                        largestJpeg.getHeight(), ImageFormat.JPEG, /*maxImages*/5));
                    }
                    mJpegImageReader.get().setOnImageAvailableListener(
                            mOnJpegImageAvailableListener, mBackgroundHandler);


                    if (mImageReader2 == null || mImageReader2.getAndRetain() == null) {
                        mImageReader2 = new VideoBase.RefCountedAutoCloseable<>(ImageReader.newInstance(captureSize.getWidth(), captureSize.getHeight(),
                                ImageFormat.YUV_420_888, 2));
                    }
                    mImageReader2.get().setOnImageAvailableListener(
                            mOnImageAvailableListener2, mBackgroundHandler);

                    mCharacteristics = characteristics;
                    mCameraId = cameraId;
                }
                return true;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            mApp.putStr("camera","");
        }

        // If we found no suitable cameras for capturing RAW, warn the user.
        FragmentVI.ErrorDialog.buildErrorDialog("This device doesn't support capturing RAW photos").
                show(mApp.getApp().getSupportFragmentManager(), "dialog");
        return false;
    }

    @Override
    public void openCamera() {
        if(mDraw != null) {
            mDraw.clear();
        }
        mTextureView.setVisibility( View.VISIBLE );

        if (!setUpCameraOutputs()) {
            return;
        }

        Activity activity = mView.getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            // Wait for any previously running session to finish.
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String cameraId;
            Handler backgroundHandler;
            synchronized (mCameraStateLock) {
                cameraId = mCameraId;
                backgroundHandler = mBackgroundHandler;
            }

            // Attempt to open the camera. mStateCallback will be called on the background handler's
            // thread when this succeeds or fails.
            manager.openCamera(cameraId, mStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        } catch( SecurityException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            synchronized (mCameraStateLock) {

                // Reset state and clean up resources used by the camera.
                // Note: After calling this, the ImageReaders will be closed after any background
                // tasks saving Images from these readers have been completed.
                mPendingUserCaptures = 0;
                mState = STATE_CLOSED;
                if (null != mCaptureSession) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                if (null != mJpegImageReader) {
                    mJpegImageReader.close();
                    mJpegImageReader = null;
                }

                if (null != mImageReader2) {
                    mImageReader2.close();
                    mImageReader2 = null;
                }


            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }




    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void createCameraPreviewSessionLocked() {

        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            List<Surface> outputSurfaces = new LinkedList<>();
            outputSurfaces.add(surface);
            outputSurfaces.add(mJpegImageReader.get().getSurface());
            outputSurfaces.add(mImageReader2.get().getSurface());

            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReader2.get().getSurface());


            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            synchronized (mCameraStateLock) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }

                                // When the session is ready, we start displaying the preview.
                                mCaptureSession = cameraCaptureSession;

                                setupRequest();

                           }

                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed to configure camera.");
                        }
                    }, mBackgroundHandler
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    /**
     * Configure the given {@link CaptureRequest.Builder} to use auto-focus, auto-exposure, and
     * auto-white-balance controls if available.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @param builder the builder to configure.
     */
    private void setup3AControlsLocked(CaptureRequest.Builder builder) {
        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO);

        Float minFocusDist =
                mCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        mNoAFRun = (minFocusDist == null || minFocusDist == 0);

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO);
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
        }

        // If there is an auto-magical white balance control mode available, use it.
        if (contains(mCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }

    /**
     * If the given request has been completed, remove it from the queue of active requests and
     * send an {@link SrcCamera.ImageSaver} with the results from this request to a background thread to
     * save a file.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @param requestId the ID of the {@link CaptureRequest} to handle.
     * @param builder   the {@link SrcCamera.ImageSaver.ImageSaverBuilder} for this request.
     * @param queue     the queue to remove this request from, if completed.
     */
    private void handleCompletionLocked(int requestId, SrcCamera.ImageSaver.ImageSaverBuilder builder,
                                        TreeMap<Integer, SrcCamera.ImageSaver.ImageSaverBuilder> queue) {
        if (builder == null) return;
        SrcCamera.ImageSaver saver = builder.buildIfComplete();
        if (saver != null) {
            queue.remove(requestId);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(saver);
        }
    }

    /**
     * Check if we are using a device that only supports the LEGACY hardware level.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @return true if this is a legacy device.
     */
    private boolean isLegacyLocked() {
        return mCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    /**
     * Start the timer for the pre-capture sequence.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void startTimerLocked() {
        mCaptureTimer = SystemClock.elapsedRealtime();
    }

    /**
     * Check if the timer for the pre-capture sequence has been hit.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     *
     * @return true if the timeout occurred.
     */
    private boolean hitTimeoutLocked() {
        return (SystemClock.elapsedRealtime() - mCaptureTimer) > PRECAPTURE_TIMEOUT_MS;
    }

    public  static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public void configureTransformOld(int viewWidth, int viewHeight) {
        Activity activity = mView.getActivity();
        synchronized (mCameraStateLock) {
            if (null == mTextureView || null == activity) {
                return;
            }

            StreamConfigurationMap map = mCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // For still image captures, we always use the largest available size.
            Size largestJpeg = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());

            // Find the rotation of the device relative to the native device orientation.
            int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

            // Find the rotation of the device relative to the camera sensor's orientation.
            int totalRotation = sensorToDeviceRotation(mCharacteristics, deviceRotation);


            // Swap the view dimensions for calculation as needed if they are rotated relative to
            // the sensor.
            boolean swappedDimensions = totalRotation == 90 || totalRotation == 270;
            int rotatedViewWidth = viewWidth;
            int rotatedViewHeight = viewHeight;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedViewWidth = viewHeight;
                rotatedViewHeight = viewWidth;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }


            // Preview should not be larger than display size and 1080p.
            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            maxPreviewHeight = maxPreviewWidth*displaySize.y/displaySize.x;

            // Find the best preview size for these view dimensions and configured JPEG size.
            Size previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedViewWidth, rotatedViewHeight, /* maxPreviewWidth, maxPreviewHeight, */ 0,0,
                    largestJpeg);

//          previewSize = new Size((previewSize.getWidth()+31)&(~31),(previewSize.getHeight()+31)&(~31));
            if (swappedDimensions) {
                mTextureView.setAspectRatio(
                        previewSize.getHeight(), previewSize.getWidth());
                mDraw.setAspectRatio(
                        previewSize.getHeight(), previewSize.getWidth());

            } else {
                mTextureView.setAspectRatio(
                        previewSize.getWidth(), previewSize.getHeight());
                mDraw.setAspectRatio(
                        previewSize.getWidth(), previewSize.getHeight());
            }

            // Find rotation of device in degrees (reverse device orientation for front-facing
            // cameras).
            int rotation = (mCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT) ?
                    (360 + ORIENTATIONS.get(deviceRotation)) % 360 :
                    (360 - ORIENTATIONS.get(deviceRotation)) % 360;

            Matrix matrixV = new Matrix();
            Matrix matrixD = new Matrix();

            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();

            if( mCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT && deviceRotation == Surface.ROTATION_0)
                deviceRotation = Surface.ROTATION_180;

            mProc.onOrientationChanged(deviceRotation);

            if (Surface.ROTATION_90 == deviceRotation || Surface.ROTATION_270 == deviceRotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrixV.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                float scale = Math.min(
                        (float) viewHeight / previewSize.getHeight(),
                        (float) viewWidth / previewSize.getWidth());
                matrixV.postScale(scale, scale, centerX, centerY);

                matrixD.setRectToRect(new RectF(0, 0, previewSize.getWidth(),previewSize.getHeight()), new RectF(0, 0, viewWidth, viewHeight), Matrix.ScaleToFit.FILL);
                matrixD.postRotate((rotation+90)%360, centerX, centerY);

                matrixV.postRotate(-rotation, centerX, centerY);

                if(mCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT ) {
                    if(Surface.ROTATION_270 != deviceRotation) {
                        matrixD.postScale(-1.0f, 1.0f, centerX, centerY);
                        L("rotation 3");
                    } else
                    {
                        matrixD.postScale(1.0f, -1.0f, centerX, centerY);
                        L("rotation 4");
                    }
                } else
                {
                    if(Surface.ROTATION_270 != deviceRotation) {
                        matrixD.postScale(1.0f, -1.0f, centerX, centerY);
                        matrixV.postRotate(180, centerX, centerY);
                        L("rotation 1");
                    } else {
                        L("rotation 2");
                        matrixV.postRotate(180, centerX, centerY);
                    }
                }
            } else
            {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrixV.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                float scale = Math.min(
                        (float) viewWidth / previewSize.getHeight(),
                        (float) viewHeight / previewSize.getWidth());
                matrixV.postScale(scale, scale, centerX, centerY);

                matrixD.setRectToRect(new RectF(0, 0, previewSize.getWidth(),previewSize.getHeight()), new RectF(0, 0, viewHeight, viewWidth), Matrix.ScaleToFit.FILL);

                if(deviceRotation == Surface.ROTATION_180 ) {
                    matrixD.postScale(-1.0f, -1.0f, centerX, centerY);
                } else {
                    matrixD.postScale(-1.0f, -1.0f, centerX, centerY);
                }

                matrixV.postRotate(rotation, centerX, centerY);
            }


            mDraw.setTransform(matrixD);
            mTextureView.setTransform(matrixV);

            // Start or restart the active capture session if the preview was initialized or
            // if its aspect ratio changed significantly.
            if (mPreviewSize == null || !checkAspectsEqual(previewSize, mPreviewSize)) {
                mPreviewSize = previewSize;
                if (mState != STATE_CLOSED) {
                    createCameraPreviewSessionLocked();
                }
            }
        }
    }

    @Override
    public void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = mView.getActivity();
        synchronized (mCameraStateLock) {
            if (null == mTextureView || null == activity) {
                return;
            }

            StreamConfigurationMap map = mCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // For still image captures, we always use the largest available size.
            Size largestJpeg = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new FragmentVI.CompareSizesByArea());

            // Find the rotation of the device relative to the native device orientation.
            int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

            // Find the rotation of the device relative to the camera sensor's orientation.
            int totalRotation = sensorToDeviceRotation(mCharacteristics, deviceRotation);


            // Swap the view dimensions for calculation as needed if they are rotated relative to
            // the sensor.
            boolean swappedDimensions = totalRotation == 90 || totalRotation == 270;
            int rotatedViewWidth = viewWidth;
            int rotatedViewHeight = viewHeight;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedViewWidth = viewHeight;
                rotatedViewHeight = viewWidth;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }


            // Preview should not be larger than display size and 1080p.
            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            maxPreviewHeight = maxPreviewWidth*displaySize.y/displaySize.x;

            // Find the best preview size for these view dimensions and configured JPEG size.
            Size previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedViewWidth, rotatedViewHeight,
                    largestJpeg);

         //   Size previewSizeR = new Size(rotatedViewWidth,rotatedViewHeight);
//          previewSize = new Size((previewSize.getWidth()+31)&(~31),(previewSize.getHeight()+31)&(~31));
            if (swappedDimensions) {
                mTextureView.setAspectRatio(
                        previewSize.getHeight(), previewSize.getWidth());
                mDraw.setAspectRatio(
                        previewSize.getHeight(), previewSize.getWidth());

            } else {
                mTextureView.setAspectRatio(
                        previewSize.getWidth(), previewSize.getHeight());
                mDraw.setAspectRatio(
                        previewSize.getWidth(), previewSize.getHeight());
            }

            // Find rotation of device in degrees (reverse device orientation for front-facing
            // cameras).
            int rotation = (mCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT) ?
                    (360 + ORIENTATIONS.get(deviceRotation)) % 360 :
                    (360 - ORIENTATIONS.get(deviceRotation)) % 360;

            Matrix matrixV = new Matrix();
            Matrix matrixD = new Matrix();

            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();

            if( mCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT && deviceRotation == Surface.ROTATION_0)
                deviceRotation = Surface.ROTATION_180;

            mProc.onOrientationChanged(deviceRotation);

            if (Surface.ROTATION_90 == deviceRotation || Surface.ROTATION_270 == deviceRotation) {
                // undefined for portrait orientation
            } else
            {

                RectF rView = new RectF(0, 0, viewWidth, viewHeight);


                matrixD.setRectToRect(bufferRect, rView, Matrix.ScaleToFit.FILL);

                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrixV.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

                float scaleX =  (float) viewWidth / previewSize.getHeight();
                float scaleY =  (float) viewHeight / previewSize.getWidth();
                matrixV.postScale(scaleX, scaleY, centerX, centerY);

                float r = scaleX/scaleY;
                if(deviceRotation == Surface.ROTATION_180 ) {
                    matrixD.postScale(-1.0f, -r, centerX, centerY);
                } else {

                    matrixD.postScale(-1.0f, -r, centerX, centerY);
                }
                matrixD.postTranslate(0,-viewHeight*(1-r)/2);
                matrixV.postRotate(rotation, centerX, centerY);

            }


            mDraw.setTransform(matrixD);
            mTextureView.setTransform(matrixV);

            // Start or restart the active capture session if the preview was initialized or
            // if its aspect ratio changed significantly.
            if (mPreviewSize == null || !checkAspectsEqual(previewSize, mPreviewSize)) {
                mPreviewSize = previewSize;
                if (mState != STATE_CLOSED) {
                    createCameraPreviewSessionLocked();
                }
            }
        }
    }

    /**
     * Initiate a still image capture.
     * <p/>
     * This function sends a capture request that initiates a pre-capture sequence in our state
     * machine that waits for auto-focus to finish, ending in a "locked" state where the lens is no
     * longer moving, waits for auto-exposure to choose a good exposure value, and waits for
     * auto-white-balance to converge.
     */
    private void takePicture() {
        synchronized (mCameraStateLock) {
            mPendingUserCaptures++;

            // If we already triggered a pre-capture sequence, or are in a state where we cannot
            // do this, return immediately.
            if (mState != STATE_PREVIEW) {
                return;
            }

            try {
                // Trigger an auto-focus run if camera is capable. If the camera is already focused,
                // this should do nothing.
                if (!mNoAFRun) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                            CameraMetadata.CONTROL_AF_TRIGGER_START);
                }

                // If this is not a legacy device, we can also trigger an auto-exposure metering
                // run.
                if (!isLegacyLocked()) {
                    // Tell the camera to lock focus.
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                }

                // Update state machine to wait for auto-focus, auto-exposure, and
                // auto-white-balance (aka. "3A") to converge.
                mState = STATE_WAITING_FOR_3A_CONVERGENCE;

                // Start a timer for the pre-capture sequence.
                startTimerLocked();

                // Replace the existing repeating request with one with updated 3A triggers.
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mPreCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send a capture request to the camera device that initiates a capture targeting the JPEG and
     * RAW outputs.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void captureStillPictureLocked() {
        try {
            final Activity activity = mView.getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(mJpegImageReader.get().getSurface());



            // Use the same AE and AF modes as the preview.
            setup3AControlsLocked(captureBuilder);

            // Set orientation.
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            rotation = Surface.ROTATION_0;

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    sensorToDeviceRotation(mCharacteristics, rotation));

            // Set request tag to easily track results in callbacks.
            captureBuilder.setTag(mRequestCounter.getAndIncrement());

            CaptureRequest request = captureBuilder.build();

            // Create an ImageSaverBuilder in which to collect results, and add it to the queue
            // of active requests.
            ImageSaver.ImageSaverBuilder jpegBuilder = new ImageSaver.ImageSaverBuilder(activity)
                    .setCharacteristics(mCharacteristics);

            mJpegResultQueue.put((int) request.getTag(), jpegBuilder);

            mCaptureSession.capture(request, mCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called after a RAW/JPEG capture has completed; resets the AF trigger state for the
     * pre-capture sequence.
     * <p/>
     * Call this only with {@link #mCameraStateLock} held.
     */
    private void finishedCaptureLocked() {
        try {
            // Reset the auto-focus trigger in case AF didn't run quickly enough.
            if (!mNoAFRun) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

                mCaptureSession.capture(mPreviewRequestBuilder.build(), mPreCaptureCallback,
                        mBackgroundHandler);

                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the next {@link Image} from a reference counted {@link ImageReader}, retaining
     * that {@link ImageReader} until that {@link Image} is no longer in use, and set this
     * {@link Image} as the result for the next request in the queue of pending requests.  If
     * all necessary information is available, begin saving the image to a file in a background
     * thread.
     *
     * @param pendingQueue the currently active requests.
     * @param reader       a reference counted wrapper containing an {@link ImageReader} from which
     *                     to acquire an image.
     */
    private void dequeueAndSaveImage(TreeMap<Integer, ImageSaver.ImageSaverBuilder> pendingQueue,
                                     VideoBase.RefCountedAutoCloseable<ImageReader> reader) {
        synchronized (mCameraStateLock) {
            Map.Entry<Integer, ImageSaver.ImageSaverBuilder> entry =
                    pendingQueue.firstEntry();
            ImageSaver.ImageSaverBuilder builder = entry.getValue();

            // Increment reference count to prevent ImageReader from being closed while we
            // are saving its Images in a background thread (otherwise their resources may
            // be freed while we are writing to a file).
            if (reader == null || reader.getAndRetain() == null) {
                Log.e(TAG, "Paused the activity before we could save the image," +
                        " ImageReader already closed.");
                pendingQueue.remove(entry.getKey());
                return;
            }

            Image image;
            try {
                image = reader.get().acquireNextImage();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Too many images queued for saving, dropping image for request: " +
                        entry.getKey());
                pendingQueue.remove(entry.getKey());
                return;
            }

            builder.setRefCountedReader(reader).setImage(image);

            handleCompletionLocked(entry.getKey(), builder, pendingQueue);
        }
    }

    /**
     * Runnable that saves an {@link Image} into the specified {@link File}, and updates
     * {@link android.provider.MediaStore} to include the resulting file.
     * <p/>
     * This can be constructed through an {@link ImageSaverBuilder} as the necessary image and
     * result information becomes available.
     */
    public  static class ImageSaver implements Runnable {

        /**
         * The image to save.
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        /**
         * The CaptureResult for this image capture.
         */
        private final CaptureResult mCaptureResult;

        /**
         * The CameraCharacteristics for this camera device.
         */
        private final CameraCharacteristics mCharacteristics;

        /**
         * The Context to use when updating MediaStore with the saved images.
         */
        private final Context mContext;

        /**
         * A reference counted wrapper for the ImageReader that owns the given image.
         */
        private final VideoBase.RefCountedAutoCloseable<ImageReader> mReader;

        private ImageSaver(Image image, File file, CaptureResult result,
                           CameraCharacteristics characteristics, Context context,
                           VideoBase.RefCountedAutoCloseable<ImageReader> reader) {
            mImage = image;
            mFile = file;
            mCaptureResult = result;
            mCharacteristics = characteristics;
            mContext = context;
            mReader = reader;
        }

        @Override
        public void run() {
            boolean success = false;
            int format = mImage.getFormat();
            switch (format) {
                case ImageFormat.JPEG: {
                    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(mFile);
                        output.write(bytes);
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        closeOutput(output);
                    }
                    break;
                }
                case ImageFormat.RAW_SENSOR: {
                    DngCreator dngCreator = new DngCreator(mCharacteristics, mCaptureResult);
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(mFile);
                        dngCreator.writeImage(output, mImage);
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        closeOutput(output);
                    }
                    break;
                }
                default: {
                    Log.e(TAG, "Cannot save image, unexpected image format:" + format);
                    break;
                }
            }

            // Decrement reference count to allow ImageReader to be closed to free up resources.
            mReader.close();

            // If saving the file succeeded, update MediaStore.
            if (success) {
                MediaScannerConnection.scanFile(mContext, new String[]{mFile.getPath()},
                /*mimeTypes*/null, new MediaScannerConnection.MediaScannerConnectionClient() {
                            @Override
                            public void onMediaScannerConnected() {
                                // Do nothing
                            }

                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i(TAG, "Scanned " + path + ":");
                                Log.i(TAG, "-> uri=" + uri);
                            }
                        });
            }
        }

        /**
         * Builder class for constructing {@link ImageSaver}s.
         * <p/>
         * This class is thread safe.
         */
        public static class ImageSaverBuilder {
            private Image mImage;
            private File mFile;
            private CaptureResult mCaptureResult;
            private CameraCharacteristics mCharacteristics;
            private Context mContext;
            private VideoBase.RefCountedAutoCloseable<ImageReader> mReader;

            /**
             * Construct a new ImageSaverBuilder using the given {@link Context}.
             *
             * @param context a {@link Context} to for accessing the
             *                {@link android.provider.MediaStore}.
             */
            public ImageSaverBuilder(final Context context) {
                mContext = context;
            }

            public synchronized ImageSaverBuilder setRefCountedReader(
                    VideoBase.RefCountedAutoCloseable<ImageReader> reader) {
                if (reader == null) throw new NullPointerException();

                mReader = reader;
                return this;
            }

            public synchronized ImageSaverBuilder setImage(final Image image) {
                if (image == null) throw new NullPointerException();
                mImage = image;
                return this;
            }

            public synchronized ImageSaverBuilder setFile(final File file) {
                if (file == null) throw new NullPointerException();
                mFile = file;
                return this;
            }

            public synchronized ImageSaverBuilder setResult(final CaptureResult result) {
                if (result == null) throw new NullPointerException();
                mCaptureResult = result;
                return this;
            }

            public synchronized ImageSaverBuilder setCharacteristics(
                    final CameraCharacteristics characteristics) {
                if (characteristics == null) throw new NullPointerException();
                mCharacteristics = characteristics;
                return this;
            }

            public synchronized ImageSaver buildIfComplete() {
                if (!isComplete()) {
                    return null;
                }
                return new ImageSaver(mImage, mFile, mCaptureResult, mCharacteristics, mContext,
                        mReader);
            }

            public synchronized String getSaveLocation() {
                return (mFile == null) ? "Unknown" : mFile.toString();
            }

            private boolean isComplete() {
                return mImage != null && mFile != null && mCaptureResult != null
                        && mCharacteristics != null;
            }
        }
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,  int textureViewHeight, Size aspectRatio) {
        int scaleY = 1000*480/640;
        int scaleX = 1000;


        Size s = chooseOptimalSize(choices, textureViewWidth,textureViewHeight, 640*scaleX/1000,640*scaleY/1000, aspectRatio);
        if(s.getHeight() > 0 && s.getWidth() > 0)
            return s;
        s = chooseOptimalSize(choices, textureViewWidth,textureViewHeight, 800*scaleX/1000,800*scaleY/1000, aspectRatio);
        if(s.getHeight() > 0 && s.getWidth() > 0)
            return s;
        return chooseOptimalSize(choices, textureViewWidth,textureViewHeight, 800*scaleX/1000,800*scaleY/1000, aspectRatio);
    }

    /**
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,  int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        int nBest = -1;
        int rBest = 0x7fffffff;

        float ratio;
        if(textureViewWidth > 0) {
            ratio = 1.0f * textureViewWidth / textureViewHeight;
        } else {
            Point displaySize = new Point();
            mView.getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
            ratio = 1.0f * displaySize.x / displaySize.y;
        }
        if(ratio < 0.7f)
            ratio = 0.7f;

        Size test;
        if(maxWidth > 0) {
            test = new Size(maxWidth, maxHeight);
        } else {
            Point displaySize = new Point();
            mView.getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
            test = new Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_WIDTH*displaySize.y/displaySize.x);
        }

        for(int i = 0; i < choices.length; ++i)
        {
            if(maxWidth > 0 && choices[i].getWidth() > maxWidth)
                continue;
            if(maxHeight > 0 && choices[i].getHeight() > maxHeight)
                continue;

            int r = compareSize(ratio,test,choices[i]);
            if( r < rBest )
            {
                rBest = r;
                nBest = i;
            }
        }
        if(nBest < 0)
            return new Size(0,0);

        L("chooseOptimalSize "+choices[nBest]);

        return choices[nBest];
    }

    /**
     * Rotation need to transform from the camera sensor orientation to the device's current
     * orientation.
     *
     * @param c                 the {@link CameraCharacteristics} to query for the camera sensor
     *                          orientation.
     * @param deviceOrientation the current device orientation relative to the native device
     *                          orientation.
     * @return the total rotation from the sensor orientation to the current device orientation.
     */
    private static int sensorToDeviceRotation(CameraCharacteristics c, int deviceOrientation) {
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Get device orientation in degrees
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);

        // Reverse device orientation for front-facing cameras
        if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    /**
     * Cleanup the given {@link OutputStream}.
     *
     * @param outputStream the stream to close.
     */
    private static void closeOutput(OutputStream outputStream) {
        if (null != outputStream) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public boolean checkState() {
        if (mCameraAfState != null && mCameraAfState  == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)
            return false;
        return true;
    }

    private void readParam() {

        mOptionOff.clear();
        String tag;


        tag = "VI_FACE_MODE";
        try {
            int v = (mPreviewRequestBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) == CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE) ? 1:0;
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch ( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_EDGE_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.EDGE_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_NOISE_REDUCTION_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.NOISE_REDUCTION_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_LENS_OPTICAL_STABILIZATION_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_TONEMAP_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.TONEMAP_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_CONTROL_AF_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }


        if(isManualWhiteBalanceSupportedCamera2()) {

            tag = "CAM_CONTROL_AWB_MODE";
            try {
                if(mCharacteristics != null) {
                    int m[] = mCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
                    if (m != null && m.length > 1) {
                        int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AWB_MODE);
                        jni.EnginePutIt(tag, v);
                        mOptionOff.put(tag, 0);
                        if( v == 0 ) {
                            tag = "CAM_CONTROL_WB_KELVIN";
                            mOptionOff.put(tag, 0);
                        }
                    }
                }
            } catch (Exception e) {
                mOptionOff.put(tag, 1);
            }


        }


        tag = "CAM_CONTROL_AE_MODE";
        try {
            if(mCharacteristics != null) {
                int m[] = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
                if(m != null && m.length > 1) {
                    int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE);
                    jni.EnginePutIt(tag, v);
                    mOptionOff.put(tag, 0);
                }
            }
        } catch (Exception e) {
            mOptionOff.put(tag, 1);
        }

        tag = "CAM_CONTROL_EFFECT_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_CONTROL_AE_ANTIBANDING_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_CONTROL_SCENE_MODE";
        try {
            int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_SCENE_MODE);
            jni.EnginePutIt(tag,v);
            mOptionOff.put(tag,0);
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_CONTROL_AE_EXPOSURE_COMPENSATION";
        try {
            if(mCharacteristics != null) {
                Range<Integer> r = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                if(r != null && r.getLower() != r.getUpper()) {
                    int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION);
                    jni.EnginePutIt(tag, v);
                    mOptionOff.put(tag, 0);
                }
            }
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_CONTROL_AWB_LOCK";
        try {
            if( mCharacteristics != null && mCharacteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE)) {
                int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AWB_LOCK) ? 1 : 0;
                jni.EnginePutIt(tag, v);
                mOptionOff.put(tag, 0);
            }
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_CONTROL_AE_LOCK";
        try {
            if( mCharacteristics != null && mCharacteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE)) {
                int v = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AE_LOCK) ? 1 : 0;
                jni.EnginePutIt(tag, v);
                mOptionOff.put(tag, 0);
            }
        } catch( Exception e ){
            mOptionOff.put(tag,1);
        }

        tag = "CAM_LENS_FOCUS_DISTANCE";
        try {
            if( mCharacteristics != null ) {
                Float v;
                v = mCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                if( v != null && v > 0) {
                    v = mPreviewRequestBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE);
                    jni.EnginePutFt(tag, v);
                    mOptionOff.put(tag, 0);
                    mOptionOff.put("CAM_LENS_FOCUS_DISTANCE_AUTO", 0);
                }
            }
        } catch( Exception e ){
            mOptionOff.put(tag,1);
            mOptionOff.put("CAM_LENS_FOCUS_DISTANCE_AUTO", 1);
        }



        bReadParam = false;
    }


    private  <T> void setRequest(CaptureRequest.Key<T> key,T v) {
        if( mPreviewRequestBuilder == null)
            return;
        try {
            if(v instanceof Integer) {
                if(((Integer) v).longValue() != -1000000 ) {
                    mPreviewRequestBuilder.set(key, v);
                }
            } else {
                 mPreviewRequestBuilder.set(key, v);
            }
        } catch( Exception e ){}
    }



    private void setupRequest() {

        Integer exTime = jni.EngineGetIt("CAM_EXP_TIME");

        float fDist = jni.EngineGetFt("CAM_LENS_FOCUS_DISTANCE");
        int vAutoFDist = jni.EngineGetIt("CAM_LENS_FOCUS_DISTANCE_AUTO");
        boolean isAutoFDist = (vAutoFDist == 1);

        boolean isRealExposureTimeOnPreview = true;

        Integer focusMode = jni.EngineGetIt("CAM_CONTROL_AF_MODE");
        Integer flashMode = jni.EngineGetIt("CAM_FLASH_MODE");
        Integer aeMode   = jni.EngineGetIt("CAM_CONTROL_AE_MODE");
        Integer wbMode   = jni.EngineGetIt("CAM_CONTROL_AWB_MODE");
        Integer sceneMode = jni.EngineGetIt("CAM_CONTROL_SCENE_MODE");
        Integer ev     = jni.EngineGetIt("CONTROL_AE_EXPOSURE_COMPENSATION");
        Integer iso    = jni.EngineGetIt("CAM_CONTROL_ISO");
        Integer antibanding = jni.EngineGetIt("CAM_CONTROL_AE_ANTIBANDING_MODE");
        int vaeLock = jni.EngineGetIt("CAM_CONTROL_AE_LOCK");
        boolean aeLock = (vaeLock == 1);
        int vawbLock = jni.EngineGetIt("CAM_CONTROL_AWB_LOCK");
        boolean awbLock = (vawbLock == 1);
        Integer colorEffect = jni.EngineGetIt("CAM_CONTROL_EFFECT_MODE");
        Integer modeStab = jni.EngineGetIt("CAM_LENS_OPTICAL_STABILIZATION_MODE");
        Integer noiseMode = jni.EngineGetIt("CAM_NOISE_REDUCTION_MODE");
        Integer edgeMode = jni.EngineGetIt("CAM_EDGE_MODE");

        try {
            if (jni.EngineGetIt("VI_FACE_MODE") == 1) {
                mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                        CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                        CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF);
            }
        } catch( Exception e ){

        }

        setRequest(CaptureRequest.CONTROL_EFFECT_MODE, colorEffect);
        setRequest(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,modeStab);
        setRequest(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,antibanding);
        setRequest(CaptureRequest.NOISE_REDUCTION_MODE,noiseMode);
        setRequest(CaptureRequest.EDGE_MODE,edgeMode);

        setRequest(CaptureRequest.CONTROL_AE_MODE,aeMode);

        if(vaeLock >= 0)
            setRequest(CaptureRequest.CONTROL_AE_LOCK, aeLock);
        if(vawbLock >= 0)
            setRequest(CaptureRequest.CONTROL_AWB_LOCK, awbLock);

        if(aeLock && aeMode == 0)
        {
            setRequest(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev);
        }


        if ( wbMode == CaptureRequest.CONTROL_AWB_MODE_OFF) {
            setRequest(CaptureRequest.COLOR_CORRECTION_MODE,CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);

            float vKelvin = jni.EngineGetFt("CAM_CONTROL_WB_KELVIN");
            ColorSpaceTransform transform = null;
            try {
                transform = new ColorSpaceTransform(colorTransformMatrix);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            setRequest(CaptureRequest.COLOR_CORRECTION_GAINS, makeColorTemperature(vKelvin));
            if(transform != null)
                setRequest(CaptureRequest.COLOR_CORRECTION_TRANSFORM, transform);
        }
        setRequest(CaptureRequest.CONTROL_AWB_MODE,wbMode);

        if (sceneMode != CaptureRequest.CONTROL_MODE_AUTO)
        {
            setRequest(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
            setRequest(CaptureRequest.CONTROL_SCENE_MODE, sceneMode);
        }
        else
        {
            setRequest(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        }

        if(!isAutoFDist && fDist > 0 && vAutoFDist >= 0)
        {
            setRequest(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            setRequest(CaptureRequest.LENS_FOCUS_DISTANCE, fDist);
        } else
            setRequest(CaptureRequest.CONTROL_AF_MODE,focusMode);

/*
        if(!awbLock)
        {
            long exposureTime = exTime;
            long frameDuration = 0;
            int  sensorSensitivity = iso;

            //Exposure time longer than 1/15 gets preview very slow
            //Set custom exposure time/frame duration/ISO allows preview looks like real but on high fps.
            if(!isRealExposureTimeOnPreview)
            {
                if(exTime == 100000000L)
                {
                    exposureTime = 70000000L;
                    frameDuration = 70000000L;
                    sensorSensitivity = 500;
                }
                else if(exTime == 142857142L)
                {
                    exposureTime = 35000000L;
                    frameDuration = 39000000L;
                    sensorSensitivity = 1100;
                }
                else if(exTime >= 200000000L)
                {
                    exposureTime = 40000000L;
                    frameDuration = 40000000L;
                    sensorSensitivity = 1300;
                }
            }

            setRequest(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            setRequest(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            setRequest(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);

            if(frameDuration > 0)
                setRequest(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);

            if(sensorSensitivity > 0)
                setRequest(CaptureRequest.SENSOR_SENSITIVITY, sensorSensitivity);
        }
        */
//////////////////////////////////////////////////////////

        bReadParam = true;
        if(mCaptureSession != null) {
            try {
                //     setup3AControlsLocked(mPreviewRequestBuilder);
                mCaptureSession.stopRepeating();

                // Finally, we start displaying the camera preview.
                mCaptureSession.setRepeatingRequest(
                        mPreviewRequestBuilder.build(),
                        mPreCaptureCallback, mBackgroundHandler);
                mState = STATE_PREVIEW;
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public boolean isManualWhiteBalanceSupportedCamera2()
    {

        if (mCharacteristics != null
                && mCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) != null) //Disable manual WB for Nexus 6 - it manages WB wrong
        {
            int[] wb = mCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);

            //Only in AWB_MODE_OFF we can manually control color temperature of image data
            boolean wbOFF = false;
            for (int i = 0; i < wb.length; i++) {
                if (wb[i] == CaptureRequest.CONTROL_AWB_MODE_OFF) {
                    wbOFF = true;
                    break;
                }

            }
            return wbOFF;
        }

        return false;
    }

    private RggbChannelVector makeColorTemperature(float vKelvin ) {
        float R = 0;
        float G_even = 0;
        float G_odd  = 0;
        float B      = 0;

        float tmpKelvin = vKelvin/100;

        /*RED*/
        if(tmpKelvin <= 66)
            R = 255;
        else
        {
            double tmpCalc = tmpKelvin - 60;
            tmpCalc = 329.698727446 * Math.pow(tmpCalc, -0.1332047592);

            R = (float)tmpCalc;
            if(R < 0) R = 0.0f;
            if(R > 255) R = 255;
        }

        /*GREENs*/
        if(tmpKelvin <= 66)
        {
            double tmpCalc = tmpKelvin;
            tmpCalc = 99.4708025861 * Math.log(tmpCalc) - 161.1195681661;
            G_even = (float)tmpCalc;
            if(G_even < 0)
                G_even = 0.0f;
            if(G_even > 255)
                G_even = 255;
            G_odd = G_even;
        }
        else
        {
            double tmpCalc = tmpKelvin - 60;
            tmpCalc = 288.1221695283 * Math.pow(tmpCalc, -0.0755148492);
            G_even = (float)tmpCalc;
            if(G_even < 0)
                G_even = 0.0f;
            if(G_even > 255)
                G_even = 255;
            G_odd = G_even;
        }


        if(tmpKelvin <= 19)
        {
            B = 0.0f;
        }
        else
        {
            double tmpCalc = tmpKelvin - 10;
            tmpCalc = 138.5177312231 * Math.log(tmpCalc) - 305.0447927307;
            B = (float)tmpCalc;
            if(B < 0) B = 0;
        }

        R = (R/255) * multiplierR;

        G_even = (G_even/255) * multiplierG;
        G_odd = G_even;

        B = (B/255) * multiplierB;

        RggbChannelVector rggbVector = new RggbChannelVector(R, G_even, G_odd, B);

        return rggbVector;
    }

    @Override
    public CameraCharacteristics getCameraCharacteristics() {
        return mCharacteristics;
    }

    @Override
    public boolean isOption(String tag) {
        try {
            return mOptionOff.get(tag) == 0 ? true:false;
        } catch( Exception e ){

        }
        return false;
    }

    @Override
    public void configure() {
        setupRequest();
    }

    @Override
    public int[] getSupported(String tag) {

        if(mCharacteristics == null)
            return null;
        if( tag.equals("CAM_CONTROL_SCENE_MODE")) {
            return mCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
        }
        if( tag.equals("CAM_CONTROL_EFFECT_MODE")) {
            return mCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
        }
        if( tag.equals("CAM_CONTROL_AE_ANTIBANDING_MODE")) {
            return mCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
        }
        if( tag.equals("CAM_CONTROL_AE_MODE")) {
            return mCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        }
        if( tag.equals("CAM_CONTROL_AWB_MODE")) {
            return mCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        }

        if( tag.equals("CAM_CONTROL_AE_EXPOSURE_COMPENSATION")) {
            Range<Integer> r = mCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            if(r.getUpper() == r.getLower())
                return null;
            int[] a = new int[r.getUpper() - r.getLower() +1];
            for( int i = r.getLower(),pos = 0; i <= r.getUpper(); ++i,++pos)
                a[pos] = i;
            return a;
        }

        return null;
    }
}
