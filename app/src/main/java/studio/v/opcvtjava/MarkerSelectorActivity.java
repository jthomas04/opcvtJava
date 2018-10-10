package studio.v.opcvtjava;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.CameraBridgeViewBase;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutionException;

public class MarkerSelectorActivity extends BaseCVCameraActivity {

    private static final String TAG = "MarkSelect";
    private CameraBridgeViewBase mCamera;
    private int W, H;
    private Mat mRGBA, mGRAY, mRGBAT, mRGBA2;
    //Create an array of similar Mats to intialize
    private Mat [] mats = {mRGBA, mRGBA2};

    private FeaturesManager FM;
    private NFMarker marker;
    private final byte markerSelected = 1;
    private final byte markerFinalized = 2;
    private final byte noMarker = -1;
    private final byte markerRejected = 0;
    private byte markerStatus = noMarker;
    private Button markerSelectBtn;

    private final Scalar sGreen = new Scalar(25, 255, 50);
    private final Scalar sOrange = new Scalar(255, 200, 25);
    private Scalar currentColor = sGreen;


    private Point centrePoint;
    private final int markerSize = 750;
    private Rect markerCorners;
    private checkMarker chkMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mCamera = this.getCameraView();
        markerSelectBtn = (Button)findViewById(R.id.markBtn);
    }

    @Override
    public void openCVReady(){
        mCamera.enableView();
        mCamera.enableFpsMeter();
        Log.w(TAG, "Circling!! Yay!");
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        W = width;
        H = height;
        mRGBAT = new Mat(height, width, CvType.CV_8UC4);
        mGRAY = new Mat(width, height, CvType.CV_8U);
        for(short i = 0; i < mats.length; i++){ //Loop initializing the mats
            mats[i] = new Mat(width, height, CvType.CV_8UC4);
        }

        FM = new FeaturesManager();
        centrePoint = new Point(W/2, (H-20)/2);
        markerCorners = computeMarkerCorners(width, height, markerSize, centrePoint);
        chkMarker = new checkMarker(FM);
    }

    @Override
    public void onCameraViewStopped() {
        mCamera.disableView();
        mGRAY.release();
        mRGBAT.release();
        for(Mat mat: mats){ //Loop initializing the mats
            mat.release();
        }
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inFrame){
        mRGBA = inFrame.rgba();
        mGRAY = inFrame.gray();

        if(markerStatus == markerSelected){
            boolean checkingMarker = (chkMarker.getStatus() == AsyncTask.Status.RUNNING);
            boolean finishedChecking = chkMarker.getStatus() == AsyncTask.Status.FINISHED;
            boolean notStarted = chkMarker.getStatus() == AsyncTask.Status.PENDING;
            if(notStarted){
                mRGBA2 = mRGBA.submat(markerCorners);
                mRGBA2.copyTo(marker.getMat());
                chkMarker.execute(marker);
                currentColor = sOrange;
            }
            else if(checkingMarker) {
                currentColor.val[0] = (currentColor.val[0]--)%255;
                currentColor.val[3] = (currentColor.val[3]++)%255;
            }
            else{
                if(finishedChecking){
                    currentColor = sOrange;
                    try {
                        markerStatus = (chkMarker.get() >= 10) ?markerFinalized: markerRejected;
                    }  catch (ExecutionException e) {
                        e.printStackTrace();
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    chkMarker = new checkMarker(FM);
                }
            }

        }
        Imgproc.drawMarker(mRGBA, centrePoint, currentColor, Imgproc.MARKER_SQUARE, markerSize, 1, Imgproc.LINE_8 );
        return mRGBA;
    }

    private Rect computeMarkerCorners(int width, int height, int radius, Point centre){
        double sqrt2 = Math.sqrt(2);
        double halfrt2 = sqrt2/2;
        double r = (double)radius;
        double side = (halfrt2 * r);
        int top = (int) ((height - side)/2);
        int bottom = (int) ((height + side)/2);
        int left = (int) (side/2); //Since co-ords start from TL (0,0)
        int right = (int) ((3*side/2)); // 1/2 side + 1 side = 3*sides /2
//        int [] TL = {left , top};
//        int [] TR = {right , top};
//        int [] BL = {left, bottom};
//        int [] BR = {right, bottom};
        return new Rect(top, left, right, bottom);
    }



    public void chooseAsMarker(View view){
        if(markerStatus == markerFinalized){
            markerStatus = noMarker;
            markerSelectBtn.setText("Re-select Marker");
        }
        else if(markerStatus == noMarker){
            markerStatus = markerSelected;
            markerSelectBtn.setText("Processing Marker");
        }
        else if(markerStatus == markerRejected){
            markerStatus = noMarker;
            markerSelectBtn.setText("Select Marker");
        }

    }

    private class checkMarker extends AsyncTask<NFMarker, Void, Short>  {
        private FeaturesManager FeMa;

        checkMarker(FeaturesManager fm){
            FeMa = fm;
        }

        @Override
        protected Short doInBackground(NFMarker... nfm) {
            nfm[0].analyze(FeMa);
            return nfm[0].keypoints;
        }

    };

}


