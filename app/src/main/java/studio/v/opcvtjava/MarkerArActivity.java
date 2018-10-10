package studio.v.opcvtjava;

import android.Manifest;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.utils.Converters;
import org.rajawali3d.view.ISurface;
import org.rajawali3d.view.SurfaceView;

import java.util.LinkedList;
import java.util.List;


public class MarkerArActivity  extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final String TAG = "MARAct";
    private Mat mRgba, mGray, mRgbaT, mRgbaF;

    private SurfaceView surfaceView;
    private BaseRenderer bR;
    private PermissionHandler PH;
    //Array containing Permissions : Camera & READ External Storage.
    private String [] camAndExtRead = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
    private boolean cvStarted = false;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.w(TAG, "OpenCv Loaded sucessfully");
                    mOpenCvCameraView.setMaxFrameSize(800,600);
                    mOpenCvCameraView.enableView();
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
        PH = new PermissionHandler(camAndExtRead, this, this, "I need permissions to run! You moron!!");

        setContentView(R.layout.obj_viewer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(android.view.SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        surfaceView = (SurfaceView) findViewById(R.id.sV);
        surfaceView.setFrameRate(60.0);

        //surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        //surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setTransparent(true);
        surfaceView.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);

        //surfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);

        bR = new BaseRenderer(this);
        surfaceView.setSurfaceRenderer(bR);

        bR.mRotationMatrix[ 0] = 1;
        bR.mRotationMatrix[ 6] = 1;
        bR.mRotationMatrix[ 9] = 1;
        bR.mRotationMatrix[ 15] = 1;

        surfaceView.setZOrderOnTop(true);
        //_glSurfaceView.setSurfaceRenderer(bR);


    }

//    @Override
//    protected void glSurfaceViewConfig() {
//        // Example which makes glSurfaceView transparent (along with setting scene.backgroundColor to 0x0)
//       _glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//       _glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
//       //_glSurfaceView.setZOrderOnTop(true);
//    }
//    @Override
//    protected void onCreateSetContentView()
//    {
//
//
//
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        int results = PH.processResults(requestCode, permissions, grantResults);
        // Just add this one line with the PermisssionHandler object now, shows your Toast & returns £ of permissions granted.
        if (!cvStarted)
            startCV();
        //surfaceView.bringToFront();

    }

    @Override
    public void onStop(){
        super.onStop();
        this.finish();
    }
    @Override
    public void onResume() {
        super.onResume();
        if(PH.checkAllPermisions(PermissionHandler.CamAndExtRead, this) && !cvStarted){
            startCV();
            //surfaceView.bringToFront();
            cvStarted = true;
        }
    }
    @Override
    public void onPause(){
        super.onPause();
        if(mOpenCvCameraView!=null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.w(TAG, "W: " + width + " H:"  + height);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        //mRRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8U);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat( height, width, CvType.CV_8UC4);
        //fMask = new Mat(height, width, CvType.CV_8U);

    }

    @Override
    public void onCameraViewStopped() {
        mOpenCvCameraView.disableView();
        mRgba.release();
        mGray.release();
        mRgbaT.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //mRgbaT = mRgba.t();
        mGray = inputFrame.gray();

        Mat thresholded = new Mat();
        Imgproc.adaptiveThreshold(mGray, thresholded, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 7, 7);
        //.threshold(mGray, thresholded, 0.5, 245, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);

        List<MatOfPoint> allContours = new LinkedList<>();
        MatOfPoint polygon = new MatOfPoint();
        List<MatOfPoint> polygons = new LinkedList<MatOfPoint>();
        List<Mat> allCurves = new LinkedList<>();
        //Converters.vector_vector_Point_to_Mat(allContours, allCurves);
        //Converters.Mat_to_vector_vector_Point2f();
        Mat hier = new Mat();
        Imgproc.findContours(thresholded, allContours, hier, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        for (MatOfPoint contour : allContours) {
            //Imgproc.approxPolyDP(contour, polygon, Imgproc.arcLength(contour, true)*0.02, true);

            if (polygon.size().height == 4 || polygon.size().width == 4 && Math.abs(Imgproc.contourArea(polygon)) > 1000 && Imgproc.isContourConvex(polygon)) {
                double maxCosine = 0;

            }

        }
        //Log.w(TAG, mRgba.toString());
        //Log.w(TAG, mRgbaT.toString());
        //Core.rotate(mRgbaT, mRgbaF,Core.ROTATE_90_CLOCKWISE);
        return mRgba;
    }

    private void startCV(){
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCv Library not found. using OpenCv Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "Internal OpenCv Library found inside pacakge. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
