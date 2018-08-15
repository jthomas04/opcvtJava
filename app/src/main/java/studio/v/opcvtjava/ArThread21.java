package studio.v.opcvtjava;

import android.os.AsyncTask;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by JJ on 06/01/2018.
 */

public class ArThread21 extends AsyncTask<Mat, Mat, Mat> {

    private int nLog =0;
    private final String TAG = "thread-AR :: ";
    private volatile static boolean  running = false;

    private TL TL = new TL();
    private double last, now, diff;

    private List<Point> lPreviousP = new LinkedList();
    private Mat Homography = new Mat(), finalHomography = new Mat(), newHomography = new Mat();

    private Mat prevImage = new Mat(), mask = new Mat();

    public int state = ART.noHomo;
    public int i, nMatches, nGMatches, nFrameKps, nInliers, nFeatures;

    public ART t;


    private Mat mRgba = new Mat(), mGray = new Mat();

    public void setMats(Mat rgba, Mat gray){
        //last = System.currentTimeMillis();
        mRgba = rgba.clone();
        mGray = gray.clone();
        //now = System.currentTimeMillis();
        //diff = now - last;
        //Log.w(TAG, "Time Taken Cloning : " + diff + "ms");
    }

    public synchronized void setExtras(Mat inmask, Mat previousImage, List<Point> previousPoints){
        mask = inmask.clone();
        prevImage = previousImage;
        lPreviousP = previousPoints;
    }


    public  Mat[] getMats(){
        Mat[] res = new Mat[2];
        res[0] = Homography;
        res[1] = finalHomography;
        return res;
    }

    public ArThread21(ART artObject){
        Log.w(TAG, "Class Intialized");
        t = artObject;
        state = ART.noHomo;
    }

    public Mat doInBackground(Mat... in) {
        mGray = in[0].clone();
        mRgba = in[1].clone();
        if (lPreviousP.size() == 0) {
            return new Mat();
        } else {
            List<List<Point>> matchingPoints = new LinkedList<>();
            nGMatches = t.getMatchingPointsOPF(prevImage, mGray, lPreviousP, matchingPoints); //This function returns number of matches it could track
            //TL.timeIt(TAG, "matching points with sparseOpticalFlow");
            if (nGMatches >= 4) {
                setState(true, ART.tracking);
                Homography = t.computeHomographyFromPoints(matchingPoints.get(0), matchingPoints.get(1));
            }
            if (Homography.empty()) Log.w(TAG, "NO HOMO!!!");
        }
        setState(false, t.noHomo);
        return new Mat();
    }

        private void setState(boolean tRunning, int State){
            running = tRunning;
            state = State;
        }

        public boolean isRunning(){
            return running;
        }
}
