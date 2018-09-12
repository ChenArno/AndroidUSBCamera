package com.jiangdg.usbcamera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.util.Log;

import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnCaptureListener;
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnEncodeResultListener;
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnPreViewResultListener;
import com.serenegiant.usb.common.UVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.util.List;

/**
 * Created by chencentury on 2018/4/22.
 */

public class XYHelper {
    private static final String TAG = "==========>";
    private UVCCameraHandler mCameraHandler;
    private CameraViewInterface mcamView;
    private UsbControlBlock mctrlBlock;
    private boolean isRequest = false;
    private String deviceName = null;
    private float banwidth = 0.65f;//双摄像头，0.65f  1f //最大0.75f

    //640*480 800*600
    private int previewWidth = 640;
    private int previewHeight = 480;
    private Activity mActivity;
    // 默认使用MJPEG  //UVCCamera 53行 https://github.com/saki4510t/UVCCamera/issues/231
    private static int FRAME_FORMAT_MJPEG = UVCCamera.FRAME_FORMAT_MJPEG;
    private static int FRAME_FORMAT_YUYV = UVCCamera.FRAME_FORMAT_YUYV;

    public XYHelper(Activity activity,CameraViewInterface camView){
        this.mActivity = activity;
        this.mcamView = camView;
        createUVCCamera();
    }

    public XYHelper(Activity activity,CameraViewInterface camView,int width,int height){
        this.mActivity = activity;
        this.mcamView = camView;
        createUVCCamera(width,height);
    }

    public void closeCamera() {
        if (this.mCameraHandler != null) {
            this.mCameraHandler.close();
        }
    }

    public void createUVCCamera() {
        if (this.mcamView == null)
            throw new NullPointerException("CameraViewInterface cannot be null!");

        // release resources for initializing camera handler
        if (this.mCameraHandler != null) {
            this.mCameraHandler.release();
            this.mCameraHandler = null;
        }
        // initialize camera handler
        this.mcamView.setAspectRatio(this.previewWidth / (float) this.previewHeight);
        this.mCameraHandler = UVCCameraHandler.createHandler(this.mActivity, this.mcamView, 2,
                this.previewWidth, this.previewHeight, this.FRAME_FORMAT_MJPEG,this.banwidth);
    }

    public void createUVCCamera(int width,int height) {
        if (this.mcamView == null)
            throw new NullPointerException("CameraViewInterface cannot be null!");

        // release resources for initializing camera handler
        if (this.mCameraHandler != null) {
            this.mCameraHandler.release();
            this.mCameraHandler = null;
        }
        // initialize camera handler
        this.mcamView.setAspectRatio(this.previewWidth / (float) this.previewHeight);
        this.mCameraHandler = UVCCameraHandler.createHandler(this.mActivity, this.mcamView, 2,
                width, height, FRAME_FORMAT_MJPEG,this.banwidth);
    }

    public void updateResolution(int width, int height) {
        if (this.previewWidth == width && this.previewHeight == height) {
            return;
        }
        this.previewWidth = width;
        this.previewHeight = height;
        if (this.mCameraHandler != null) {
            this.mCameraHandler.release();
            this.mCameraHandler = null;
        }
        this.mcamView.setAspectRatio(this.previewWidth / (float) this.previewHeight);
        this.mCameraHandler = UVCCameraHandler.createHandler(this.mActivity, this.mcamView, 2,
                this.previewWidth, this.previewHeight, FRAME_FORMAT_MJPEG);
        openAndPreview(this.mctrlBlock);
    }


    public void openAndPreview(UsbControlBlock ctrlBlock) {
        this.mctrlBlock = ctrlBlock;
        openCamera();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 休眠500ms，等待Camera创建完毕
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 开启预览
                startPreview();
                Log.i(TAG, "run: 开始播放");
            }
        }).start();
    }

    public void resetOpenAndPre(){
        openCamera();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 休眠500ms，等待Camera创建完毕
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 开启预览
                startPreview();
            }
        }).start();
    }

    public boolean checkSupportFlag(final int flag) {
        return this.mCameraHandler != null && this.mCameraHandler.checkSupportFlag(flag);
    }

    public int getModelValue(final int flag) {
        return this.mCameraHandler != null ? this.mCameraHandler.getValue(flag) : 0;
    }

    public int setModelValue(final int flag, final int value) {
        return this.mCameraHandler != null ? this.mCameraHandler.setValue(flag, value) : 0;
    }

    public int resetModelValue(final int flag) {
        return this.mCameraHandler != null ? this.mCameraHandler.resetValue(flag) : 0;
    }


    public void capturePicture(String savePath, OnCaptureListener listener) {
        if (this.mCameraHandler != null && this.mCameraHandler.isOpened()) {
            this.mCameraHandler.captureStill(savePath, listener);

        }
    }

    public void startRecording(RecordParams params, OnEncodeResultListener listener) {
        if (this.mCameraHandler != null && !isRecording()) {
            this.mCameraHandler.startRecording(params, listener);
        }
    }

    public void stopRecording() {
        if (this.mCameraHandler != null && isRecording()) {
            this.mCameraHandler.stopRecording();
        }
    }

    public boolean isRecording() {
        if (this.mCameraHandler != null) {
            return this.mCameraHandler.isRecording();
        }
        return false;
    }

    public boolean isCameraOpened() {
        if (this.mCameraHandler != null) {
            return this.mCameraHandler.isOpened();
        }
        return false;
    }

    public void release() {
        if (this.mCameraHandler != null) {
            this.mCameraHandler.release();
            this.mCameraHandler = null;
        }
    }

    public void setOnPreviewFrameListener(OnPreViewResultListener listener) {
        if (this.mCameraHandler != null) {
            this.mCameraHandler.setOnPreViewResultListener(listener);
        }
    }

    private void openCamera() {
        if (this.mCameraHandler != null) {
            this.mCameraHandler.open(this.mctrlBlock);
//            UVCCamera uvcCamera = new UVCCamera();
//            uvcCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH,UVCCamera.DEFAULT_PREVIEW_HEIGHT,UVCCamera.FRAME_FORMAT_MJPEG,0.6f);
        }
    }

    public void startPreview() {
        SurfaceTexture st = this.mcamView.getSurfaceTexture();
        if (this.mCameraHandler != null) {
            this.mCameraHandler.startPreview(st);
            this.isRequest = true;
        }
    }

    public void stopPreview() {
        if (this.mCameraHandler != null) {
            this.mCameraHandler.stopPreview();
            this.isRequest = false;
        }
    }

    public void startCameraFoucs() {
        if (this.mCameraHandler != null) {
            this.mCameraHandler.startCameraFoucs();
        }
    }

    public List<Size> getSupportedPreviewSizes() {
        if (this.mCameraHandler == null)
            return null;
        return this.mCameraHandler.getSupportedPreviewSizes();
    }

    public void setRequest(boolean request) {
        this.isRequest = request;
    }

    public boolean isRequest() {
        return this.isRequest;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

}
