package studio.v.opcvtjava;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class BaseCVCameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final static String TAG = "BaseCVCameraActivity";
    private PermissionHandler PH;
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean cvStarted;
    private boolean paused = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.w(TAG, "OpenCv Loaded sucessfully");
                    onOpenCVReady();
                }break;
                default:
                {
                    super.onManagerConnected(status);
                }break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PH = new PermissionHandler(PermissionHandler.CamAndExtWrite, this, this, "I need permissions to run! You moron!!");
        createContentView();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView!=null){
            mOpenCvCameraView.disableView();
            //     mIsSLAMshutdown = true; NO SLAM ONLY FACE SLAM .
        }
        //this.finish();
        //System.exit(0);
    }
    @Override
    public void onPause(){
        super.onPause();
        if(mOpenCvCameraView!=null)
            mOpenCvCameraView.disableView();
        paused = true;
    }
    @Override
    public void onResume() {
        super.onResume();
        if (paused)
            mOpenCvCameraView.enableView();
        if(PH.checkAllPermisions(PermissionHandler.CamAndExtRead, this) && !cvStarted){
            startCV();
            //surfaceView.bringToFront();
            cvStarted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        int results = PH.processResults(requestCode, permissions, grantResults);
        // Just add this one line with the PermisssionHandler object now, shows your Toast(if no permissions granted) & returns of permissions granted.
        if (!cvStarted)
            startCV();
        //surfaceView.bringToFront();

    }

    protected void createContentView(){
        setContentView(R.layout.camera);
        mOpenCvCameraView = (JavaCameraView)findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    private void startCV(){
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCv Library not found. using OpenCv Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "Internal OpenCv Library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public CameraBridgeViewBase getCameraView(){
        return mOpenCvCameraView;
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }


    //Override this function to do whatever you need done to start the cameraView
    //Example: getCameraView().enableView()..... Also do all opencv dependant calls after this point
    protected void onOpenCVReady(){
        mOpenCvCameraView.enableView();
        Log.e(TAG , "onOpenCVReady(): You should override me!!");
    }

}
