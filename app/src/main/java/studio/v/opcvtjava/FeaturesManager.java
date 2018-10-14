package studio.v.opcvtjava;

import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.ORB;
import org.opencv.utils.Converters;

import java.util.LinkedList;
import java.util.List;

public class FeaturesManager {
    private String TAG = "Feat Mgr";

    private ORB detector;
    private BFMatcher matcher;
    private MatOfKeyPoint mKeypoints1;
    private MatOfKeyPoint mKeypoints2;
    private List<MatOfDMatch> matches;
    private List<DMatch> goodMatches;
    private DMatch [] dmatch;

    private double matchRatio = 0.0, dist, minDist = Double.MAX_VALUE, maxDist = 0;
    double matchRatioThreshold = 0.8;


    public FeaturesManager(){
        detector = ORB.create();
        matcher = BFMatcher.create();
        mKeypoints1 = new MatOfKeyPoint();
        mKeypoints2 = new MatOfKeyPoint();
        matches = new LinkedList<>();
        goodMatches = new LinkedList<>();
        dmatch = new DMatch[2];
    }

    public void getKeypointsM(FeaturesWithMat in){
        detector.detect(in.getMat(), mKeypoints1);
        in.lKeypoints = mKeypoints1.toList();
        in.keypoints = (short)in.lKeypoints.size();
    }

    public void computeDesc1(FeaturesWithMat fm, boolean pointsComputed){
        detector.detectAndCompute(fm.getMat(), fm.mask , mKeypoints1, fm.descriptors, pointsComputed);
    }

    public void getKeypointsI(FeaturesWithMat in){
        detector.detect(in.getMat(), mKeypoints2);
        in.lKeypoints = mKeypoints2.toList();
        in.keypoints = (short)in.lKeypoints.size();
    }

    public void computeDesc2(FeaturesWithMat fm, boolean pointsComputed){
        detector.detectAndCompute(fm.getMat(), fm.mask , mKeypoints2, fm.descriptors, pointsComputed);
    }

    public List<MatOfDMatch> matchFeatures(Mat desc1, Mat desc2){
        if(desc1.cols() != desc2.cols()){
            Log.e(TAG, "Can't match!!");
            return matches;
        }
        matcher.knnMatch(desc1, desc2, matches, 2);
        Log.e(TAG," matches = " + matches.size());
        return matches;
    }

    public List<DMatch> getGoodFeatures(List<MatOfDMatch> matches,  FeaturesWithMat image, FeaturesWithMat ref){
        if(matches.size() == 0) {
            Log.e(TAG, "no matches!");
            return goodMatches;
        }
        for(MatOfDMatch mdm: matches){
            dmatch = mdm.toArray();
            matchRatio = dmatch[0].distance/dmatch[1].distance;
            if(matchRatio < matchRatioThreshold){
                goodMatches.add(dmatch[0]);
                ref.lGoodKeypoints.add(ref.lKeypoints.get(dmatch[0].trainIdx).pt);
                image.lGoodKeypoints.add(image.lKeypoints.get(dmatch[0].queryIdx).pt);
            }
        }

        if(goodMatches.size() == 0){
            return goodMatches;
        }
        for( DMatch match: goodMatches){
            dist = match.distance;
            if( dist < minDist )
                minDist = dist;
            if( dist > maxDist )
                maxDist = dist;
        }
        Log.w(TAG + ":getGoodFeats"," Distances :: min :" + minDist + " max :" + maxDist + " of above good matches: " + goodMatches.size());
        return goodMatches;
    }

}
