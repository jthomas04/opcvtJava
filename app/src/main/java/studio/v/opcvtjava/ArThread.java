package studio.v.opcvtjava;

/**
 * Created by JJ on 31/05/2017.
 */

import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class ArThread extends AsyncTask<Mat,Integer, Mat> {
    private Thread ito;
    private int nLog =0;
    private final String TAG = "thread-AR :: ";
    private volatile static boolean  running = false;
    private Object MC;

    private TL TL = new TL();
    private double last, now, diff;

    private List<Point> lPreviousP = new LinkedList();
    private Mat Homography = new Mat(), finalHomography = new Mat(), newHomography = new Mat();

    private Mat prevImage = new Mat(), mask = new Mat();

    public int state = ART.noHomo;
    public int i, nMatches, nGMatches, nFrameKps, nInliers, nFeatures;

    public ART t;
    public boolean trackerSucess = false;


    private Mat mRgba = new Mat(), mGray = new Mat();

    public void setMats(Mat rgba, Mat gray){
        //last = System.currentTimeMillis();
        mRgba = rgba.clone();
        mGray = gray.clone();
        //now = System.currentTimeMillis();
        //diff = now - last;
        //Log.w(TAG, "Time Taken Cloning : " + diff + "ms");
    }

    public void setExtras(Mat inmask, Mat previousImage, List<Point> previousPoints){
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

    public ArThread(Object mainObject, ART artObject){
        MC = mainObject;
        Log.w(TAG, "Class Intialized");
        t = artObject;
        state = ART.noHomo;
    }

    public void run(){
        setState(true, ART.noHomo);
        //Log.w(TAG, "Started running");
        //Log.w(TAG, "Is mask empty = " + mask.empty());
        //if(!mask.empty()){
            if (lPreviousP.size() == 0){
                Logit("No previous Points Computed!!");
            }
            else {
                TL.start();
                List<List<Point>> matchingPoints = new LinkedList<>();
                nGMatches = t.getMatchingPointsOPF(prevImage, mGray, lPreviousP, matchingPoints);
                TL.timeIt(TAG, "matching points with sparseOpticalFlow");
                if (nGMatches >= 4) {
                    setState(true, ART.tracking);
                    t.computeHomographyFromPoints(matchingPoints.get(0), matchingPoints.get(1));
                    TL.timeIt(TAG, "computing Homo from Points got from sOpF");
                    if (!Homography.empty()) {
                        Log.w(TAG, "I Got Homo form tracking matcher ");
                        state = ART.HomoReady; //ART.accumHomo;
                        //finalHomography = t.refineHomography(Homography, mGray);
                        return;
                    } else {
                        setState(true, ART.noHomo);
                        Log.w(TAG, "Homo from Points got from sOpF is Empty!");
                    }
                } else {
                    setState(true, ART.lostTrack);
                    Log.w(TAG, "The tracker lost track of the object");
                }
            }
        //}
        Log.w(TAG, "I could't use the tracking matcher");
        t.mask = new Mat();
        List<MatOfDMatch> mdms = t.matchFeatures(mGray);
        Log.w(TAG,"Keypoints = "  + t.lFrameKP.size());
        List<DMatch> gms = t.getGoodMatches(mdms);

        nGMatches = gms.size();
        nMatches = t.matches.size();
        nFrameKps = t.lFrameKP.size();
        nInliers = 0;

        //
        //if(nMatches >= 6){
        //    for(i = 0; i < nMatches; i++){
        //        Imgproc.circle(mRgba,t.lFrameKP.get(t.matches.get(i).queryIdx).pt, 10, sRed);
        //    }
        //}
        //

        if( (nGMatches >= 4) ) {
            //for (i = 0; i < nGMatches; i++) {
            //    Imgproc.circle(mRgba, t.lFrameKP.get(gms.get(i).queryIdx).pt, 10, ArActivity.sYellow);
            //}
            Homography = t.computeHomography();
            if (!Homography.empty()) {
                //for(i = 0; i<t.inlierP.size(); i++){
                //    Imgproc.circle(mRgba, t.inlierP.get(i), 10, sYellow);
                //}
                state = ART.HomoReady;
                finalHomography = t.refineHomography(Homography, mGray);
                prevImage = mGray.clone();
            } else {
                state = ART.HomoEmpty;
            }
        }
        else{
            state = ART.noHomo;
        }
        Log.w(TAG, "Finished");
        setState(false, state);
    }

    public Mat doInBackground(Mat... inputs){
        Mat toReturn = new Mat();
            setState(true, ART.noHomo);
            //Log.w(TAG, "Started running");
            //Log.w(TAG, "Is mask empty = " + mask.empty());
            //if (!mask.empty()) {
                //if(lPreviousP != null){
                    if (lPreviousP.size() == 0) {
                        Logit("No previous Points Computed!!");
                    } else {
                        //TL.start();
                        List<List<Point>> matchingPoints = new LinkedList<>();
                        nGMatches = t.getMatchingPointsOPF(prevImage, mGray, lPreviousP, matchingPoints); //This function returns number of matches it could track
                        //TL.timeIt(TAG, "matching points with sparseOpticalFlow");
                        if (nGMatches >= 4) {
                            setState(true, ART.tracking);
                            Homography = t.computeHomographyFromPoints(matchingPoints.get(0), matchingPoints.get(1));
                            // TL.timeIt(TAG, "computing Homo from Points got from sOpF");
                            if (!Homography.empty()){
                                Log.w(TAG, "I Got Homo form tracking matcher ");
                                setState(true, ART.HomoReady); //ART.accumHomo;
                                TL.timeIt(TAG, "trying refinement");
                                finalHomography = t.refineHomographyFromTracker(Homography, mGray, lPreviousP);
                                TL.timeIt(TAG, "finished refinement ");
                                if(!finalHomography.empty()){
                                    toReturn = finalHomography;
                                    setState(false, t.accumHomo );
                                    trackerSucess = true;
                                    return finalHomography;
                                }
                                else{
                                    setState(false, t.HomoReady);
                                    toReturn = Homography;
                                    trackerSucess = true;
                                    finalHomography = finalHomography.eye(Homography.size(), Homography.type());
                                    return toReturn;
                                }

                            }
                            else{
                                setState(true, ART.noHomo);
                                toReturn = Homography;
                                Log.w(TAG, "Homo from Points got from sOpF is Empty!");
                            }
                        } else {
                            setState(true, ART.lostTrack);
                            Log.w(TAG, "The tracker lost track of the object");
                        }
                    }
                //}
            //}
            Log.w(TAG, "I could't use the tracking matcher");
            t.mask = new Mat();
            List<MatOfDMatch> mdms = t.matchFeatures(mGray);
            Log.w(TAG, "Keypoints = " + t.lFrameKP.size());
            List<DMatch> gms = t.getGoodMatches(mdms);

            nGMatches = gms.size();
            nMatches = t.matches.size();
            nFrameKps = t.lFrameKP.size();
            nInliers = 0;

            //
            //if(nMatches >= 6){
            //    for(i = 0; i < nMatches; i++){
            //        Imgproc.circle(mRgba,t.lFrameKP.get(t.matches.get(i).queryIdx).pt, 10, sRed);
            //    }
            //}
            //

            if ((nGMatches >= 4)) {
                //for (i = 0; i < nGMatches; i++) {
                //    Imgproc.circle(mRgba, t.lFrameKP.get(gms.get(i).queryIdx).pt, 10, ArActivity.sYellow);
                //}
                Homography = t.computeHomography();
                if (!Homography.empty()) {
                    //for(i = 0; i<t.inlierP.size(); i++){
                    //    Imgproc.circle(mRgba, t.inlierP.get(i), 10, sYellow);
                    //}
                    state = ART.HomoReady;
                    finalHomography = t.refineHomography(Homography, mGray);
                    prevImage = mGray.clone();
                    toReturn = finalHomography;
                } else {
                    state = ART.HomoEmpty;
                    toReturn = Homography;
                }
            } else {
                state = ART.noHomo;
            }
            Log.w(TAG, "Finished");
            setState(false, state);
            //return toReturn;
//        }
//        catch (Error e){
//            e.printStackTrace();
//            Log.w(TAG, e.getMessage());
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            Log.w(TAG, e.getMessage());
//        }
        //finally {
            return toReturn;
        //}
    }


    private void setState(boolean tRunning, int State){
        running = tRunning;
        state = State;
        t.state = State;
    }

    public boolean isRunning(){
        return running;
    }

    private void Logit( String message){
        nLog++;
        Log.w(TAG, message + " Logged : " + nLog);
    }
}
