package studio.v.opcvtjava;

// Augmented Reality Tracking Class Written by JJ, uses ORB feature dectection, ORB Descriptor extractor, BF Based Matching & RANSAC to compute Homography
// Marker must be loaded into public ar.marker Mat externally after initialization
// Use analyzeMarker() to cetect & Compute marker keypoints & feature descriptors which can be accessed from markerKeypoints & MarkerDesc respectively

//USAGE:: Initialize Class, set marker via loadMarker / loadMarkerGrayscale calls, latter preferable
//NOTE:: Do Not Use multiple instances for multiple Objects, may Lag the whole system.
//Requires atleast 50mb of RAM to run.

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.ORB;
import org.opencv.features2d.GFTTDetector;
import org.opencv.features2d.FlannBasedMatcher;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.calib3d.Calib3d;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.video.Video;


import org.opencv.core.Scalar;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;

public class ART {

    private final static String TAG = "ART ::";
    private final static int detectorAlgo = FeatureDetector.ORB;
    private final static int extractorAlgo = DescriptorExtractor.OPPONENT_ORB;
    private int matcherAlgo = DescriptorMatcher.BRUTEFORCE_HAMMINGLUT;

    private final ORB detector;
    private final FeatureDetector gdt;
    //private final FastFeatureDetector FFD;
    private final FeatureDetector FD ;
    private final DescriptorExtractor DE;
    private final DescriptorMatcher DM;
    //private final FlannBasedMatcher FBM;

    private Mat hMask; //Matrix Returned from the Homogrphy.

    private Mat tmp_corners;

    private Mat markerMask;
    private Mat markerDesc;
    private Mat frameDesc;
    private MatOfKeyPoint markerKeypoints;
    private MatOfKeyPoint frameKeyPoints;
    private MatOfDMatch DMatches; //MMatches found in frame of the marker object
    private double maxDist = 2, minDist = Double.MAX_VALUE, dist = 0;
    private MatOfPoint2f p2fMarker;
    private MatOfPoint2f p2fFrame;
    private MatOfPoint3f markerKps;
    private List<Point3> lMatchedMarkerPoints;
    private List<Point> lMarkerP;

    private List<MatOfDMatch> lDMatches;
    private DMatch[] best2;

    private List<Mat> desc = new ArrayList<>();

    private Mat obj_corners;
    private MatOfPoint mIntSceneCorners;

    private static boolean intrinsicCalculated = false;

    public final static int accumRefinedHomo = 5;
    public final static int accumHomo = 4;
    public final static int tracking = 3;
    public final static int gotVectors = 2;
    public final static int HomoReady = 1;
    public final static int noHomo = 0;
    public final static int noVectors = -1;
    public final static int HomoEmpty = -2;
    public final static int lostTrack = -3;

    public Mat marker;

    public Mat mask;
    public Mat EM;


    public List<DMatch> matches;
    public List<KeyPoint> lMarkerKP;
    public List<KeyPoint> lFrameKP;
    public List<KeyPoint> lFastKP;
    public List<Point> matchedMarkerP;
    public List<Point> matchedFrameP;
    public List<Point> inlierP;

    //public List<Mat> rotations;
    //public List<Mat> translations;
    //public List<Mat> normals;

    public List<List<Point>> matchingPoints;

    public static float focalXY = (float)786.42938232; //Assumed constant which will have to changed with Camera Calibration
    public static Mat intrinsicMat;
    public static MatOfDouble distCoeff;


    public Mat rv;
    public Mat tv;

    public int maxFeatures = 500;
    public int maxIters = 2000;
    public double matchRatio = 2.8;
    public double matchThreshold = 15.0;
    public double ransacThreshold = 3.0;
    public double ransacConfidence = 0.994;


    public int nMarkerKps = 0;
    public int state = noHomo;

    double now, last, diff;
    TL tl;

    public ART(){
        tl = new TL();
        FD = FeatureDetector.create(detectorAlgo);
        //FFD = FastFeatureDetector.create();
        detector = ORB.create();

        gdt = FeatureDetector.create(FeatureDetector.FAST);


        DE = DescriptorExtractor.create(extractorAlgo);
        DM = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMINGLUT);
        //FBM = FlannBasedMatcher.create();

        marker = new Mat();
        markerMask = new Mat();
        mask = new Mat();
        hMask = new Mat();
        EM = new Mat();
        obj_corners = new Mat(4,1,CvType.CV_32FC2);
        tmp_corners  = new Mat();

        markerKeypoints = new MatOfKeyPoint();
        frameKeyPoints = new MatOfKeyPoint();
        markerDesc = new Mat();
        frameDesc = new Mat();
        matches = new ArrayList<DMatch>();
        DMatches = new MatOfDMatch();
        lMarkerKP = new ArrayList<KeyPoint>();
        lFrameKP = new LinkedList<KeyPoint>();
        matchedMarkerP = new ArrayList<Point>(100);
        matchedFrameP = new ArrayList<Point>(100);
        lMarkerP = new ArrayList<Point>();
        inlierP = new ArrayList<Point>();
        p2fMarker = new MatOfPoint2f();
        p2fFrame = new MatOfPoint2f();

        lDMatches = new ArrayList<MatOfDMatch>();
        best2 = new DMatch[2];

        //rotations = new ArrayList<Mat>();
        //translations = new ArrayList<Mat>();
        //normals = new ArrayList<Mat>();

        rv = new Mat();
        tv = new Mat();

        matchingPoints = new LinkedList<List<Point>>();

        intrinsicMat = new Mat(3, 3, CvType.CV_32F);
        distCoeff = new MatOfDouble();
        markerKps =  new MatOfPoint3f();

    }

    public int getMarkerFeatures(){
        detector.detect(marker, markerKeypoints);
        lMarkerKP = markerKeypoints.toList();
        return lMarkerKP.size();
    }

    public void loadMarkerGrayScale(Bitmap bmp)
    {
        Mat mMarker;
        mMarker = new Mat();
        marker = new Mat();
        Utils.bitmapToMat(bmp, mMarker, true);
        Imgproc.cvtColor(mMarker, marker, Imgproc.COLOR_RGBA2GRAY, 1);
        Log.i(TAG, marker.toString() + " " + bmp.getHeight() + " " + bmp.getWidth() );
        setMarkerData();

    }

    public void loadMarker(Bitmap bmp){
        long diff, now, last;
        marker = new Mat();
        last = System.currentTimeMillis();
        Utils.bitmapToMat(bmp, marker, true);
        now = System.currentTimeMillis();
        diff = now - last;
        Log.w(TAG, "Time taken for  bitmapToMat = " + diff +"ms");
        setMarkerData();
    }

    public void setMarker(Mat m){
        marker = m;
        setMarkerData();
    }

    private void setMarkerData(){
        obj_corners.put(0, 0, new double[] {0,0});
        obj_corners.put(1, 0, new double[] {marker.cols(),0});
        obj_corners.put(2, 0, new double[] {marker.cols(), marker.rows()});
        obj_corners.put(3, 0, new double[] {0,marker.rows()});
    }

    private void detectAndCompute(Mat gSrc, MatOfKeyPoint mkp, Mat descriptors){
        last = System.currentTimeMillis();
        FD.detect(gSrc, mkp);
        now = System.currentTimeMillis();
        diff = now - last;
        last = now;
        Log.w(TAG, "Time Taken ORB D:  " + diff);
        DE.compute(gSrc, mkp, descriptors);
        now = System.currentTimeMillis();
        diff = now - last;
        last = now;
        Log.w(TAG, "Time Taken Opponent ORB D:  " + diff);
        //descriptors.convertTo(descriptors, CvType.CV_8U);
    }

    public void analyzeMarker(){
        detector.detectAndCompute(marker, markerMask, markerKeypoints, markerDesc, false);
        //detectAndCompute(marker, markerKeypoints, markerDesc);
        lMarkerKP = markerKeypoints.toList();
        for(int i = 0; i < lMarkerKP.size(); i++){
            lMarkerP.add(lMarkerKP.get(i).pt);
        }
    }

    public Mat drawMarkerFeatures() {
        Mat res = marker.clone();
        detector.detectAndCompute(marker, markerMask, markerKeypoints, markerDesc, false);
        lMarkerKP = markerKeypoints.toList();
        KeyPoint[] kps = markerKeypoints.toArray(); // TODO: Remove for loop & this unless debugging required
        for (int i = 0; i < kps.length; i++) {
            Imgproc.circle(res, kps[i].pt, 5, new Scalar(255, 255, 255, 255));
        }
        return res;
    }

    public void detectFeatures(Mat gSrc, MatOfKeyPoint mkp){
        FD.detect(gSrc, mkp);
    }

    public List<KeyPoint> getFastFeatures(Mat gSrc){
        tl.start();
        frameKeyPoints = new MatOfKeyPoint();
        gdt.detect(gSrc, frameKeyPoints, mask);
        //detector.detect(gSrc, frameKeyPoints, mask);

        lFastKP =  frameKeyPoints.toList();
        tl.timeIt(TAG, "Good features to track function");
        return lFastKP;
    }

    public List <MatOfDMatch> matchFeatures(Mat image){
        lDMatches.clear();
        //if(lFrameKP.size() > 0) lFrameKP.clear();
        DMatches = new MatOfDMatch();
        last = System.currentTimeMillis();
        detector.detectAndCompute(image, mask, frameKeyPoints, frameDesc, false);
        now = System.currentTimeMillis();
        diff = now - last;
        last = now;
        Log.w(TAG, "Time Taken ORB:  " + diff);

        lFrameKP = frameKeyPoints.toList();
        //Log.w(TAG,"Keypoints = "  + lFrameKP.size());
        //frameDesc.convertTo(frameDesc, CvType.CV_32F);
        //markerDesc.convertTo(markerDesc, CvType.CV_32F);

        //Test to see if first Frame so as not to crash knn match
        if(frameDesc.cols() != markerDesc.cols())
            return lDMatches;
        DM.knnMatch(frameDesc, markerDesc, lDMatches, 2);
        //DM.match(frameDesc, markerDesc, DMatches);
        now = System.currentTimeMillis();
        diff = now - last;
        last = now;
        Log.w(TAG, "Time Taken Matching:  " + diff);
        return lDMatches;
    }

    public List<DMatch> getGoodMatches(List<MatOfDMatch> ldm){
        //Reset all variables, so accumulation doesn't occur.
        matchedFrameP.clear();
        matchedMarkerP.clear();
        matches.clear();
        List<DMatch> goodMatches = new ArrayList<>();
        if(ldm.size() == 0){
            return goodMatches;
        }
        double matchRatio = 0.0, matchRatioThreshold = 0.73;
        for(MatOfDMatch mdm : ldm){
            best2 =  mdm.toArray();
            matchRatio = best2[0].distance / best2[1].distance;
            if(matchRatio < matchRatioThreshold){
                goodMatches.add(best2[0]);
                //matches.add(best2[0]);
            }
        }
        matches = goodMatches;
        //matches = DMatches.toList();
        if(matches.size() == 0){
            return matches;
        }
        for( DMatch match: matches){
            dist = match.distance;
            if( dist < minDist )
                minDist = dist;
            if( dist > maxDist )
                maxDist = dist;
        }
        Log.w(TAG," Distances :: min :" + minDist + " max :" + maxDist);
        if( matches.size() > 0) {
            for (int i = 0; i < matches.size(); i++) {
                //if (matches.get(i).distance <= Math.max(matchRatio * minDist, matchThreshold)) {
                    //goodMatches.add(matches.get(i));
                //Log.w(TAG,"Keypoints = "  + lFrameKP.size() + " " + goodMatches.get(i).queryIdx );
                    matchedFrameP.add(lFrameKP.get(goodMatches.get(i).queryIdx).pt);
                    matchedMarkerP.add(lMarkerKP.get(goodMatches.get(i).trainIdx).pt);
                //}
            }
        }
        now = System.currentTimeMillis();
        diff = now - last;
        last = now;
        //Log.w(TAG, "Time Taken Good Matching:  " + diff);
        return goodMatches;
    }

    public Mat computeHomography(){
        //Reset variables, so accumulation doesn't occur
        inlierP.clear();

        hMask = new Mat();
        p2fFrame.fromList(matchedFrameP);
        p2fMarker.fromList(matchedMarkerP);
        Mat h =  Calib3d.findHomography(p2fMarker, p2fFrame, Calib3d.RANSAC, ransacThreshold, hMask, maxIters, ransacConfidence);
        for(int i = 0; i < hMask.rows(); i++){
            double inlier = hMask.get(i,0)[0];
            if(inlier >= 1.0) {
                inlierP.add(matchedFrameP.get(i));
            }
        }

        now = System.currentTimeMillis();
        diff = now - last;
        last = now;
       // Log.w(TAG, "Homo Computing:  " + diff);
        return h;
    }

    public Mat computeHomographyFromPoints(List<Point> inMarkerP, List<Point> inFrameP){
        int i;
        inlierP.clear();

        hMask = new Mat();

        p2fFrame.fromList(inFrameP);
        p2fMarker.fromList(inMarkerP);
        Mat h =  Calib3d.findHomography(p2fMarker, p2fFrame); //, Calib3d.RANSAC, ransacThreshold, hMask, maxIters, ransacConfidence);

//        for(i = 0; i < hMask.rows(); i++){
//           double inlier = hMask.get(i,0)[0];
//            if(inlier >= 1.0) {
//              inlierP.add(matchedFrameP.get(i));
//            }
//        }

        return h;
    }

    public Mat refineHomography(Mat H, Mat image){
        Size image_size = image.size();
        Size h_size= H.size();
        Mat refinedHomo = new Mat(h_size, H.type());
        Mat revHomo = new Mat(h_size, H.type());
        List<Point> tmpMFP = new ArrayList(matchedFrameP);
        List<Point> tmpMMP = new ArrayList(matchedMarkerP);
        List<Point> tmpIP = new ArrayList<>(inlierP);


        revHomo = revHomo.eye(h_size, H.type());

        Mat warpedImg = new Mat(image_size, image.type());
        Imgproc.warpPerspective(image, warpedImg, H, image_size);

        List<MatOfDMatch> mdms = matchFeatures(warpedImg);
        List<DMatch> gms = getGoodMatches(mdms);
        if(gms.size() >= 4){
            revHomo = computeHomography();

        }
        if(revHomo.empty()){
            return revHomo.eye(h_size, H.type());
        }

        last = System.currentTimeMillis();

        H.copyTo(refinedHomo);
        refinedHomo.mul(revHomo);

        matchedFrameP = tmpMFP;
        matchedMarkerP = tmpMMP;
        inlierP = tmpIP;

        now = System.currentTimeMillis();
        diff = now - last;
        Log.w(TAG, "Homo Refining:  " + diff);
        return refinedHomo;
    }

    public Mat refineHomographyFromTracker(Mat H, Mat image, List<Point> inPoints){
        Size image_size = image.size();
        Size h_size= H.size();
        Mat refinedHomo = new Mat(h_size, H.type());
        Mat revHomo = new Mat(h_size, H.type());

        revHomo = revHomo.eye(h_size, H.type());

        Mat warpedImg = new Mat(image_size, image.type());
        Imgproc.warpPerspective(image, warpedImg, H, image_size);

        //List<KeyPoint> nKps = t.getFastFeatures(warpedImg); no idea why i wrote this
        List<List<Point>> fKps = new LinkedList<List<Point>>();
        fKps = matchingPoints;
        //for(kp : nKps)
        //    fKps.add(nKps.pt);
        if( getMatchingPointsOPF(image, warpedImg, inPoints, matchingPoints) >= 4){
            revHomo = computeHomographyFromPoints(matchingPoints.get(0), matchingPoints.get(1));
            if(revHomo.empty()){
                matchingPoints = fKps;
                return revHomo;
            }
            else{
                H.copyTo(refinedHomo);
                refinedHomo.mul(revHomo);
            }
        }
        matchingPoints = fKps;
        return refinedHomo;
    }

    public Mat transformPointsFromHomography(Mat H){
        Mat scene_corners = new Mat(4,1,CvType.CV_32FC2);
        Core.perspectiveTransform(obj_corners,scene_corners, H);
        return scene_corners;
    }

    public boolean checkIfConvex(Mat inCorners){
        // Convert the scene corners to integer format, as required
        // by the Imgproc.isContourConvex function.
        inCorners.convertTo(tmp_corners, CvType.CV_32S);
        mIntSceneCorners = new MatOfPoint(tmp_corners);

        // Check whether the corners form a convex polygon. If not,
        // (that is, if the corners form a concave polygon), the
        // detection result is invalid because no real perspective can
        // make the corners of a rectangular image look like a concave
        // polygon!
        return Imgproc.isContourConvex(mIntSceneCorners);
    }

    public Mat makeMask(Mat sceneCorners, Size maskSize){
        int i = 0;
        tl.start();
//        double maxX = 0.0, maxY = 0.0, minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
//        double currX = 0.0, currY = 0.0;
        Mat mask = new Mat(maskSize, CvType.CV_8U);
        //mask.setTo(new Scalar(0));
//
        double[][] mPoint = new double[4][2];
        Point[] corners = new Point[4];

//        mPoint[0] = sceneCorners.get(0,0);
//        mPoint[1] = sceneCorners.get(1,0);
//        mPoint[2] = sceneCorners.get(3,0);
//        mPoint[3] = sceneCorners.get(3,0);
//
//        corners[0] = new Point(mPoint[0][0], mPoint[0][1]);
//        corners[1] = new Point(mPoint[1][0], mPoint[1][1]);
//        corners[2] = new Point(mPoint[2][0], mPoint[2][1]);
//        corners[3] = new Point(mPoint[3][0], mPoint[3][1]);

        for(i = 0; i < sceneCorners.rows(); i++ ){
            mPoint[i] = sceneCorners.get(i, 0);
            corners[i] = new Point(mPoint[i][0],mPoint[i][1]);
        }
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

        //Point normalizedPoint1 = new Point(Math.max(corners[0].x,0), Math.max(corners[0].y,0) );
        //Point normalizedPoint2 = new Point(Math.min(corners[2].x,maskSize.width), Math.min(corners[2].y,maskSize.height));
        //Rect rectangleOfI = new Rect(normalizedPoint1, normalizedPoint2);
        //Mat ROI = new Mat(mask, rectangleOfI);
        //ROI.setTo(new Scalar(255, 255, 255));

        MatOfPoint mpCorners = new MatOfPoint();
        mpCorners.fromArray(corners); //(sceneCorners);


        //Point center = new Point(sceneCorners.get(0,0)/2, maxY/2);
        //Imgproc.floodFill(mask, fMask, center, new Scalar(255,255,255));
        Imgproc.fillConvexPoly(mask, mpCorners, new Scalar(255));

        tl.timeIt(TAG, " making mask ");
        return mask;
    }

    public Mat makeEnvelopeMask(Mat sceneCorners, Size maskSize){
        Mat Mask= new Mat(maskSize, CvType.CV_8U);
        return mask;
    }

    public List<Point> getInliers(Mat inlierMask){
        List<Point> inliers = new LinkedList<>();
        for(int i = 0; i < inlierMask.rows(); i++){
            double inlier = inlierMask.get(i,0)[0];
            if(inlier >= 1.0) {
                inliers.add(matchedFrameP.get(i));
            }
        }
        return inliers;
    }

    public boolean findPose(float fx, float fy, float height, float width){
        boolean res;
        last = System.currentTimeMillis();
        if(!intrinsicCalculated){
            intrinsicMat.put(0, 0, fx);
            intrinsicMat.put(1, 1, fy);
            intrinsicMat.put(0, 2, width/2);
            intrinsicMat.put(1, 2, height/2);
            intrinsicMat.put(2,2, (float)1);
            intrinsicCalculated = true;
        }
        p2fFrame.fromList(matchedFrameP);
        int nMatchedPoints = matchedMarkerP.size();
        lMatchedMarkerPoints = new ArrayList<Point3>(nMatchedPoints);
        Point pt;
        double x = 0, y = 0;
        for(Point mmp : matchedMarkerP){
            pt =  mmp;
            x = pt.x;
            y = pt.y;
            lMatchedMarkerPoints.add(new Point3(x, y, 1));
        }
        markerKps.fromList(lMatchedMarkerPoints);
        Log.w(TAG, " status = pose computing ");
        res = Calib3d.solvePnP(markerKps, p2fFrame, intrinsicMat, distCoeff, rv, tv);
        now = System.currentTimeMillis();
        diff = now - last;
        Log.w(TAG, "Time taken solvingPnp : " + diff + "ms");
        return res;
    }

    public float [] computeRotationMat(Mat rv){
        Mat RM = new Mat();
        float [] result = new float[9];
        Mat YZNegative = new Mat(1,3, rv.type());
        YZNegative.put(0,1, -1.0);
        YZNegative.put(0,3,-1.0);
        rv = rv.mul(YZNegative);
        Calib3d.Rodrigues(rv, RM);
        RM.get(0,0, result);
        return result;
    }

    public float[] computeEulers(Mat rv){
        Mat RM = new Mat();
        Calib3d.Rodrigues(rv, RM);
        float[] angles = new float[3];
        int ver=0;
        angles[0] =(float) (Math.atan((RM.get(1,0)[ver]/RM.get(0,0)[ver])));
        angles[1] =(float) (Math.atan (  ( (RM.get(2,0)[ver])*-1 )*Math.sqrt(Math.pow((RM.get(2,1)[ver]),2)+Math.pow((RM.get(2,2)[ver]),2))));
        angles[2] =(float) (Math.atan(RM.get(2,1)[ver])/RM.get(2,2)[ver]);
        return angles;
    }


    //Tries to track input points From previous frame and returns the number it could track
    public int getMatchingPointsOPF(Mat prevSrc, Mat mSrc, List<Point> inPoints, List <List <Point> > result ){
        List <Point> nextPoints = new ArrayList<Point>();
        MatOfPoint2f m2fNextPoints = new MatOfPoint2f(), m2fPrevPoints = new MatOfPoint2f();
        m2fPrevPoints.fromList(inPoints);
        MatOfByte status = new MatOfByte();
        MatOfFloat errors = new MatOfFloat();
        Video.calcOpticalFlowPyrLK(prevSrc, mSrc, m2fPrevPoints, m2fNextPoints, status, errors);
        byte[] bStatus = status.toArray();
        List<Point> newPoints = m2fNextPoints.toList();
        List<Point> res1 = new LinkedList<>();
        List<Point> res2 = new LinkedList<>();
        byte b = 0;
        for(int i = 0; i < bStatus.length ; i++) {
            b = bStatus[i];
            if(b == 1 ){
                res1.add(inPoints.get(i));
                res2.add(newPoints.get(i));
            }
        }
        result.add(res1);
        result.add(res2);


        return res1.size(); //TODO return an 0 or full size of list.
    }

    public int getRelativePose(List<Point> lp1, List<Point> lp2, double f, Point pp, Mat R, Mat t ){
        tl.start();
        Mat p1 = Converters.vector_Point_to_Mat(lp1);
        Mat p2 = Converters.vector_Point_to_Mat(lp2);
        //TODO:: use second variant of findEssentialMat
        EM = Calib3d.findEssentialMat(p1, p2,f,pp,Calib3d.RANSAC,0.85,1.5);
        if(EM.empty()) return noVectors;
        int r = Calib3d.recoverPose(EM, p1, p2, R, t, f, pp);
        tl.timeIt(TAG,"Recovering pose using E Mat");
        return r;
    }


}
