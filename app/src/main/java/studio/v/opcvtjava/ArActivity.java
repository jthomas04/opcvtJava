package studio.v.opcvt;

import min3d.core.RendererActivity;
import min3d.core.Object3dContainer;
import min3d.objectPrimitives.Box;
import min3d.vos.Light;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.opengl.GLSurfaceView;


import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.KeyPoint;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.utils.Converters;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Point;

import org.opencv.core.Scalar;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by JJ on 23/03/2017.
 * Finished on 01:20, Yes I know tat took a little time but took a lot of time starting it
 * 1:26, I'm Still adding Sublimal messages you WoN't PrObAbLy ReAd
 * 1:56, need to fInD CaBlE to DeBuG, sO tIrEd & DoWn, eVeN tOdAy YoU sUsPeCt Me WhEn I rEaLlY hAvE nOtHiNg.... :| :| :(
 * 2:59 Almost 3'o Clock, YoUr FaSt AsLeEp, BuT i FiNaLlY gOt ThE mOdUlE's MoSt ImPoRtAnT pArT WorKiNg.
 *  06/04/2017 Switched back to FAST feature detection & yes cleaning uo the sublimal messages
 * 2:15 Trying native code for better performance...... tHiS mUsIc Is CrAzY oN eArS nEeD bEtTeR tRaCkS
 * I'm GoNnA gEt A hEaDAcHe ToMoRrOw.
 */

public class ArActivity extends RendererActivity implements CvCameraViewListener2 {
    private static final String TAG = " MC2:Activity "; //For all logging purposes

    private CameraBridgeViewBase mOpenCvCameraView; // Stores reference Camera view of OpenCV, YES JUST A REFERENCE VEEJAY NOT THE ACTUAL THING!!
    private RelativeLayout rl; //ThIs iS aLsO aN rEfErEnCe AKA a Pointer from The  old C Days, yes JAVA is the SaMe UgLy PoInTeRs In A NiCe FlOwErY PaCkAgE, LiKe CeRtAiN pEoPlE WhO SuGaRiFy!!
    protected GLSurfaceView glv;
    private ImageView debugView; //TODO: Debuging only remove, UnEcEsSaRy, Just There for my ObLiGaTiOn, YoU KnOw LiKe sOcIaL oBlIgAtIoNs.....

    private int nMatches = 0, nGMatches = 0, nFrameKps = 0, nInliers = 0; // ExTrA UnEcEsSaRy MoNkEyS, ThEsE all IdIoTiC CoUnTeRs ThEy JuSt CoUnT, bEtTeR wAy NoW wItH @i.
    private boolean firstRun = true;
    private boolean homoCompute = false;
    private boolean vectorsComputed = false;
    private int finalState = ART.noHomo;
    private int currState = ART.noHomo;

    public float frameWidth = 0, frameHeight = 0;
    private float xEuler = 0.0f, yEuler = 0.0f, zEuler = 0.0f;
    private float XC = 0.0f, YC = 0.0f, ZC = 0.0f;
    private float [] eulers = new float[3];

    private Bitmap bmp;
    private Mat mRgba;
    private Mat mRRgba;  //Extra Mat to apply Mask on.
    private Mat mGray;
    private Mat mRgbaF;
    private Mat mRgbaT; //Used to fix Camera Orientation, YoU kNoW sO mUnK tAiL iS dOwN & hEaD uP.
    //CORRECTION :: Currently used to store zeros

    private Mat fMask;
    private Mat prevImage;
    Mat[] res = new Mat[2];
    private Mat oldHomography;
    private Mat Homography;
    private Mat finalHomography;
    private Mat old_scene_corners;
    private List<Point> prevPoints;

    private Object3dContainer _cube;

    private String message;
    private ART t;//Very Important, ART Class for Augmented Reality Tracking
    private ArThread art;
    private ComputedData data;

    public static int thread = 0;

    static Scalar sOrange = new Scalar(255, 127, 0, 255);
    static Scalar sYellow = new Scalar(255, 255, 0, 255);
    static Scalar sRed = new Scalar(255, 0, 0, 255);
    static Scalar sGreen = new Scalar(0, 0, 255, 255);

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
        Log.w(TAG, "super.onCreate Finished, It Started working Veejay....");
        Uri uri = (Uri) getIntent().getExtras().get("iUri");
        loadBitmap(uri);
        // TODO: Remove after debug these 2 lines
        //debugView = (ImageView) findViewById(R.id.iv); //Debug view
        //debugView.setImageBitmap(bmp);
    }

    private void loadBitmap(Uri uri){ // Function to load & decode Bitmap
        InputStream is;
        try {
            is = getContentResolver().openInputStream(uri);
        }
        catch (FileNotFoundException e) {
            Log.w(TAG, "Input Stream could not be opened!");
            return;
        }
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bmp = BitmapFactory.decodeStream(is, null, op);
        Log.w(TAG, bmp.toString());
        try {
            if (is != null) {
                is.close();
            }
        }
        catch (IOException e){
            Log.w(TAG,"Uri to file not valid");
            e.printStackTrace();
        }
    }
    public Bitmap scaledwn( Bitmap bmp) {
        Bitmap dwn =Bitmap.createScaledBitmap(bmp,512,512,true);
        return dwn;
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
    }
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCv Library not found. using OpenCv Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "Internal OpenCv Library found inside pacakge. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        Log.w(TAG, "W: " + width + " H:"  + height);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8U);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, height, CvType.CV_8UC4);
        fMask = new Mat(height, width, CvType.CV_8U);
        prevImage = new Mat();
        prevPoints = new LinkedList<>();
        frameHeight = height;
        frameWidth = width;
        Bitmap minimarker= scaledwn(bmp); //Is this really necessary VJ??? If it is Maybe join inside the load Bitmap code

        t = new ART();
        art = new ArThread(this, t);
        //t.loadMarkerGrayScale(bmp);
        t.loadMarker(bmp);
        t.analyzeMarker();

        //t.getMarkerFeatures(t.marker.getNativeObjAddr(), false);

        t.nMarkerKps = t.lMarkerKP.size(); //Okay listen up this is pointer in memory (data Section) To Pointer in memory (Code section)

        //Utils.matToBitmap(t.marker, bmp);
        //debugView.setImageBitmap(bmp);
    }
    public void onCameraViewStopped(){
        mOpenCvCameraView.disableView();
        mRgba.release();
        mGray.release();
        mRgbaT.release();
        t = null;
    }

    //Executed on each frame got from the camera. Must return a Mat to display on screen.
    //@RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    public Mat onCameraFrame(CvCameraViewFrame inFrame){

        mRgba = inFrame.rgba();
        mGray = inFrame.gray();

//        /*
//        List<KeyPoint> KPS = t.getFastFeatures(mGray);
//        Log.w(TAG, "KPS =" + KPS.size());
//
//        for(KeyPoint kp : KPS)
//            prevPoints.add(kp.pt);
//        if(firstRun)
//            firstRun = false;
//        else{
//            List<List<Point>> matchingPoints = new LinkedList<>();
//            nGMatches = t.getMatchingPointsOPF(prevImage, mGray, prevPoints, matchingPoints); //This function returns number of matches it could track
//            //TL.timeIt(TAG, "matching points with sparseOpticalFlow");
//            if (nGMatches >= 4) {
//                //setState(true, ART.tracking);
//                Homography = t.computeHomographyFromPoints(matchingPoints.get(0), matchingPoints.get(1));
//            }
//            if (Homography.empty()) Log.w(TAG, "NO HOMO!!!");
//            else finalState = t.HomoReady;
//        }
//        prevImage = mGray.clone();
//        */
        currState = art.state; //This is shown on screen.

        //UPDATE:: Changed from using art.isRunning() function (see if speed-ups the overall process).
        boolean artrun = (art.getStatus() == AsyncTask.Status.RUNNING); //Check if thread is running
        if(!artrun){
            finalState = art.state;
            Log.w(TAG, " AR Thread Final State = " + finalState);
            if(finalState == ART.HomoReady || finalState == ART.accumHomo) {
                nMatches = art.nMatches;
                nGMatches = art.nGMatches;
                nInliers = art.nInliers;
                homoCompute = true;
                res = art.getMats();
                if(finalState==t.accumHomo && Homography!=null){
                    Homography = Homography.mul(res[0]);
                    finalHomography = finalHomography.mul(res[1]);
                }
                else{
                    Homography = res[0];
                    finalHomography = res[1];
                }


                //Compute Vectors from matching Point Lists
                //vectorsComputed = t.findPose(t.focalXY, t.focalXY, frameHeight, frameWidth);
                Mat RM = new Mat(), TM = new Mat();
                Point CP = new Point(mRgba.size().width / 2, mRgba.size().height / 2);
                if(art.trackerSucess){
                    vectorsComputed = (t.getRelativePose(t.matchingPoints.get(0), t.matchingPoints.get(1), ART.focalXY, CP, RM, TM)!=ART.noVectors);
                }
                else{
                    vectorsComputed =  t.findPose(t.focalXY, t.focalXY, frameHeight, frameWidth);
                }
                Log.w(TAG, " RM = " + RM.get(0, 0)[0] + " TM = " + TM.get(0, 0)[0]);
                if (vectorsComputed && !art.trackerSucess) { //If true vectors are in t.rv & t.tv
                    finalState = t.gotVectors;
                    eulers = t.computeEulers(t.rv);
                    xEuler = eulers[0];
                    yEuler = eulers[1];
                    zEuler = eulers[2];
                    XC = (float) t.tv.get(0, 0)[0];
                    YC = (float) t.tv.get(1, 0)[0];
                    ZC = (float) t.tv.get(2, 0)[0];
                } else {
                    finalState = t.noVectors;
                }
            }

            //t.mask = fMask; //Set  mask before using GFTT to detect features.

            art = new ArThread(this, t);
            //if(!fMask.empty())
            if(!firstRun)
                art.setExtras(fMask, prevImage, new LinkedList<Point>(prevPoints)); //Set the stored mask, frame & points for tracking.
            else
                firstRun = false;
            art.setMats(mRgba, mGray);

            art.execute(new Mat());
            prevImage = mGray; //Store frame for tracking algorithm.
            prevPoints.clear(); //Clear the current list.
            List<KeyPoint> detectedKeypoints;
            if(!firstRun)
                detectedKeypoints = t.getFastFeatures(t.marker);
            else
                detectedKeypoints = t.getFastFeatures(mGray);
            for(KeyPoint kp : detectedKeypoints)
                prevPoints.add(kp.pt);
            //TL.timeIt(TAG, " adding to list");
            Log.w(TAG," KPS = " + detectedKeypoints.size());
//
        }
        if(homoCompute){
            Mat overMat = mRgba.clone();

            //UPDATE:: Removed for helper functions from ART object. Looks cleaner now
            //Mat obj_corners = new Mat(4,1,CvType.CV_32FC2);

            Mat scene_corners = t.transformPointsFromHomography(Homography); //Transform object corners with Homography 1
            Mat scene_corners2 = t.transformPointsFromHomography(finalHomography);  // Transform object corners with refined Homography

            old_scene_corners = scene_corners;


            //obj_corners.put(0, 0, new double[] {0,0});
            //obj_corners.put(1, 0, new double[] {t.marker.cols(),0});
            //obj_corners.put(2, 0, new double[] {t.marker.cols(), t.marker.rows()});
            //obj_corners.put(3, 0, new double[] {0,t.marker.rows()});

            //Compute the most probable perspective transformation
            //out of several pairs of corresponding points.
            //Imgproc.getPerspectiveTransform(obj_corners, scene_corners);
            //Core.perspectiveTransform(obj_corners,scene_corners, Homography);
            //Core.perspectiveTransform(obj_corners,scene_corners2, finalHomography);


            // Check whether the corners form a convex polygon. If not,
            // (that is, if the corners form a concave polygon), the
            // detection result is invalid because no real perspective can
            // make the corners of a rectangular image look like a concave
            // polygon!
            if (!t.checkIfConvex(scene_corners)){
                // The corners do not form a convex polygon, use old corners
                scene_corners = old_scene_corners;
                Log.w(TAG, "Homography Estimation Wrong!!");
            }
            else{
                old_scene_corners = scene_corners; //Store for next faulty frame;
                firstRun = false;
                Imgproc.warpPerspective(t.marker, overMat, finalHomography, overMat.size());
                Core.addWeighted(mRgba, 0.6, overMat, 0.4, 0.1, mRgba);
            }
            if(!firstRun){ // if not first-time Scene Corners are computed.
                drawLinesAroundMarker(scene_corners, scene_corners2);

                if(scene_corners2.get(0,0)[0] == 0 ){
                    //fMask = t.makeMask(scene_corners, mRgba.size() );
                    oldHomography = Homography;
                }
                else{
                    //fMask = t.makeMask(scene_corners2, mRgba.size() );
                    oldHomography = finalHomography;
                }
                mRRgba = mRgba;
                mRgbaT = Mat.zeros(mRgbaT.size(), mRgbaT.type());
                //mRgba.copyTo(mRRgba, fMask);
                showStats(currState, mRRgba);
                return  mRRgba;
            }

        }
        //Rotate mRgba 90 degrees
        //Core.transpose(mGray, mRgbaT); // Fixes orientation, openCv orients camera left by 90 degrees
        //Imgproc.resize(mGray, mRgbaF, mRgbaF.size(), 0, 0, 0);
        //Core.flip(mRgbaF, mRgba, 1); Only For Potrait... You seeeeeeee WhEn YoU tAkE a SeLfIe MuNk, HeAd uP fEeT dOwN/ vIcE-vErSa On ThE tReE

        //Mat tmp = mGray.clone();
        //Imgproc.blur(tmp, mGray, new Size(0.5, 0.5));
        /* */

//        List<MatOfDMatch> mdms = t.matchFeatures(mGray);
//        List<DMatch> gms = t.getGoodMatches(mdms);
//
//        nGMatches = gms.size();
//        nMatches = t.matches.size();
//        nFrameKps = t.lFrameKP.size();
//        nInliers = 0;
//
//        //
//        //if(nMatches >= 6){
//        //    for(i = 0; i < nMatches; i++){
//        //        Imgproc.circle(mRgba,t.lFrameKP.get(t.matches.get(i).queryIdx).pt, 10, sRed);
//        //    }
//        //}
//        //
//
//        if( (nGMatches >= 4) ) {
//            for(i = 0; i < nGMatches; i ++){
//                    Imgproc.circle(mRgba, t.lFrameKP.get(gms.get(i).queryIdx).pt, 10, sYellow);
//            }
//            Homography = t.computeHomography();
//            if (!Homography.empty()) {
//                //for(i = 0; i<t.inlierP.size(); i++){
//                //    Imgproc.circle(mRgba, t.inlierP.get(i), 10, sYellow);
//                //}
//                t.state = t.HomoReady;
//                nInliers = t.inlierP.size();
//                finalHomography = t.refineHomography(Homography, mGray);
//                Mat overMat = mRgba.clone();
//                Imgproc.warpPerspective(t.marker, overMat, finalHomography, overMat.size());
//                Core.addWeighted(mRgba, 0.6, overMat, 0.4, 0.1, mRgba);
//
//                Mat obj_corners = new Mat(4,1,CvType.CV_32FC2);
//                Mat scene_corners = new Mat(4,1,CvType.CV_32FC2);
//                Mat scene_corners2 = new Mat(4,1,CvType.CV_32FC2);
//                Mat old_scene_corners = scene_corners;
//
//                obj_corners.put(0, 0, new double[] {0,0});
//                obj_corners.put(1, 0, new double[] {t.marker.cols(),0});
//                obj_corners.put(2, 0, new double[] {t.marker.cols(), t.marker.rows()});
//                obj_corners.put(3, 0, new double[] {0,t.marker.rows()});
//
//                //Compute the most probable perspective transformation
//                //out of several pairs of corresponding points.
//                //Imgproc.getPerspectiveTransform(obj_corners, scene_corners);
//                Core.perspectiveTransform(obj_corners,scene_corners, Homography);
//                Core.perspectiveTransform(obj_corners,scene_corners2, finalHomography);
//
//                // Convert the scene corners to integer format, as required
//                // by the Imgproc.isContourConvex function.
//                Mat tmp_corners = new Mat();
//                scene_corners.convertTo(tmp_corners, CvType.CV_32S);
//
//                MatOfPoint mIntSceneCorners = new MatOfPoint(tmp_corners);
//
//                // Check whether the corners form a convex polygon. If not,
//                // (that is, if the corners form a concave polygon), the
//                // detection result is invalid because no real perspective can
//                // make the corners of a rectangular image look like a concave
//                // polygon!
//                if (!(Imgproc.isContourConvex(mIntSceneCorners))) {
//                    // The corners do not form a convex polygon, use old corners
//                    scene_corners = old_scene_corners;
//                }
//                else{
//                    old_scene_corners = scene_corners; //Store for next faulty frame;
//                    firstRun = false;
//                }
//                if(!firstRun){
//                    Imgproc.line(mRgba, new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)), new Scalar(255, 255, 0),2);
//                    Imgproc.line(mRgba, new Point(scene_corners.get(1,0)), new Point(scene_corners.get(2,0)), new Scalar(255, 255, 0),2);
//                    Imgproc.line(mRgba, new Point(scene_corners.get(2,0)), new Point(scene_corners.get(3,0)), new Scalar(255, 255, 0),2);
//                    Imgproc.line(mRgba, new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)), new Scalar(255, 255, 0),2);
//
//                    Imgproc.line(mRgba, new Point(scene_corners2.get(0,0)), new Point(scene_corners2.get(1,0)), new Scalar(0, 255, 0),1);
//                    Imgproc.line(mRgba, new Point(scene_corners2.get(1,0)), new Point(scene_corners2.get(2,0)), new Scalar(0, 255, 0),1);
//                    Imgproc.line(mRgba, new Point(scene_corners2.get(2,0)), new Point(scene_corners2.get(3,0)), new Scalar(0, 255, 0),1);
//                    Imgproc.line(mRgba, new Point(scene_corners2.get(3,0)), new Point(scene_corners2.get(0,0)), new Scalar(0, 255, 0),1);
//                }
//            }
//             else{
//                t.state = t.HomoEmpty;
//                nInliers = 0;
//            }
//            if(t.findPose( t.focalXY, t.focalXY, frameHeight, frameWidth)){ //If true vectors are in t.rv & t.tv
//                t.state = t.gotVectors;
//                eulers = t.computeEulers(t.rv);
//                xEuler = eulers[0];
//                yEuler = eulers[1];
//                zEuler = eulers[2];
//                XC = (float)t.tv.get(0,0)[0];
//                YC = (float)t.tv.get(1,0)[0];
//                ZC = (float)t.tv.get(2,0)[0];
//            }
//            else{
//                t.state = t.noVectors;
//            }
//        }
//        else{
//            t.state = t.noHomo;
//        }
//        */
        showStats(currState, mRgba);

        return mRgba;
    }

    @Override //Settin pixel format to be transparent & such.
    protected void glSurfaceViewConfig(){
        glv = this.glSurfaceView();
        glv.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glv.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        glv.setZOrderOnTop(true); // Good Ol' Z-order did the trick
        glv.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
    }
    @Override //Function Over-ridden to make AR Adjustments & prevent min3d from just keeping the RenderSurface view.
    protected void onCreateSetContentView()
    {
        setContentView(R.layout.camera);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        rl = (RelativeLayout) findViewById(R.id.relLayout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        rl.addView(glv);
        Log.w(TAG, "RenderActivity got hacked Veejay, Over-ridden function got used, TaKe ThAt YoU sTuPiD MiN3D mUnK.!!");
    }

    @Override
    public void initScene()
    {   scene.lights().add(new Light());
        scene.backgroundColor().setAll(0x0);
        _cube = new Box(0.1f, 0.1f, 0.1f);
        scene.addChild(_cube);}
    @Override
    public void updateScene()
    {
        _cube.rotation().x = (xEuler*180/(float)Math.PI);
        _cube.rotation().y = (yEuler*180/(float)Math.PI);
        _cube.rotation().z = (zEuler*180/(float)Math.PI);
        _cube.position().x = XC;
        _cube.position().z =  ZC/1000-5;
        _cube.position().y = -YC/10;

        //Log.w(TAG, "Cube Position : " + _cube.position().x + " " + _cube.position().y + " " + _cube.position().z);
    }

    public void showStats( int state, Mat src){
        Scalar color = null;
        switch(state){
            case 0:
                message = "No Homo Computed";
                color = sRed;
                break;
            case 1:
                message = "Homo Ready";
                color = sYellow;
                break;
            case 2:
                message = "Vectors Ready";
                color = sYellow;
                break;
            case 3:
                message = "Tracking Object";
                color = sGreen;
                break;
            case 4:
                message = "Accumulative Homo";
                color = sGreen;
                break;
            case -1:
                message = "No Vectors";
                color = sRed;
                break;
            case -2:
                message = "Homo Empty";
                color = sRed;
                break;
            case -3:
                message = "Lost Track";
                color = sRed;
                break;
        }
        Imgproc.putText(mRgba, message, new Point(80, 40), Core.FONT_HERSHEY_PLAIN, 2.0, color);
        message = "# of Matches: " + nMatches + " G.M's :" + nGMatches + " Inliers :" + nInliers + " | Fr : " + nFrameKps + " Mark :" + t.nMarkerKps;
        Imgproc.putText(mRgba, message, new Point(0,65), Core.FONT_HERSHEY_PLAIN, 1.0, sRed);
        if(state == t.gotVectors || vectorsComputed){
            message = " RX:" + xEuler + " RY:" + yEuler + " RZ:" + zEuler;
            Imgproc.putText(mRgba, message, new Point(0,105), Core.FONT_HERSHEY_PLAIN, 1.0, sYellow);
            message = " TX:" + XC + " TY:" + YC + " TZ:" + ZC;
            Imgproc.putText(mRgba, message, new Point(0,125), Core.FONT_HERSHEY_PLAIN, 1.0, sYellow);
        }
    }

    public void placeObject(View view){
        Intent loadObject = new Intent(this, ObjViewer.class);
        startActivity(loadObject);
    }

    private void drawLinesAroundMarker(Mat scene_corners, Mat scene_corners2){
        Imgproc.line(mRgba, new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)), new Scalar(255, 255, 0),2);
        Imgproc.line(mRgba, new Point(scene_corners.get(1,0)), new Point(scene_corners.get(2,0)), new Scalar(255, 255, 0),2);
        Imgproc.line(mRgba, new Point(scene_corners.get(2,0)), new Point(scene_corners.get(3,0)), new Scalar(255, 255, 0),2);
        Imgproc.line(mRgba, new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)), new Scalar(255, 255, 0),2);

        Imgproc.line(mRgba, new Point(scene_corners2.get(0,0)), new Point(scene_corners2.get(1,0)), new Scalar(0, 255, 0),1);
        Imgproc.line(mRgba, new Point(scene_corners2.get(1,0)), new Point(scene_corners2.get(2,0)), new Scalar(0, 255, 0),1);
        Imgproc.line(mRgba, new Point(scene_corners2.get(2,0)), new Point(scene_corners2.get(3,0)), new Scalar(0, 255, 0),1);
        Imgproc.line(mRgba, new Point(scene_corners2.get(3,0)), new Point(scene_corners2.get(0,0)), new Scalar(0, 255, 0),1);

    }

//    private Mat makeRectMask(Mat sceneCorners, Size maskSize){
//        int i = 0;
//        TL.start();
//        double maxX = 0.0, maxY = 0.0, minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
//        double currX = 0.0, currY = 0.0;
//        Mat mask = new Mat(maskSize, CvType.CV_8U).zeros(maskSize, CvType.CV_8U);
//        Mat fMask = new Mat();
//
//        double[][] mPoint = new double[4][2];
//        Point[] corners = new Point[4];
//
//        for(i = 0; i < sceneCorners.rows(); i++ ){
//           mPoint[i] = sceneCorners.get(i, 0);
//        }
//        for(i = 0; i < 4; i++ ){
//            currX  = mPoint[i][0];
//            currY = mPoint[i][1];
//            if(currX > maxX) maxX = currX;
//            if(currY > maxY) maxY = currY;
//            if(currX < minX) minX = currX;
//            if(currY < minY) minY = currY;
//        }
//
//        minX -= 25;
//        minY -= 25;
//        maxX += 25;
//        maxY += 25;
//
//        corners[0] = new Point(minX,minY);
//        corners[1]= new Point(maxX, minY);
//        corners[2] = new Point (maxX ,maxY);
//        corners[3] = new Point(minY, maxY);
//
//        //Point normalizedPoint1 = new Point(Math.max(corners[0].x,0), Math.max(corners[0].y,0) );
//        //Point normalizedPoint2 = new Point(Math.min(corners[2].x,maskSize.width), Math.min(corners[2].y,maskSize.height));
//        //Rect rectangleOfI = new Rect(normalizedPoint1, normalizedPoint2);
//        //Mat ROI = new Mat(mask, rectangleOfI);
//        //ROI.setTo(new Scalar(255, 255, 255));
//        MatOfPoint mpCorners = new MatOfPoint(corners);
//        Point center = new Point(maxX/2, maxY/2);
//        //Imgproc.floodFill(mask, fMask, center, new Scalar(255,255,255));
//
//        Imgproc.fillConvexPoly(mask, mpCorners, new Scalar(255, 255, 255));
//
//        TL.timeIt(TAG, " making mask ");
//        return mask;
//    }
}
