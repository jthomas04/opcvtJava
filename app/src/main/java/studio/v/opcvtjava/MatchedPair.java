package studio.v.opcvtjava;

import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.MatOfDMatch;

import java.util.ArrayList;
import java.util.List;


public class MatchedPair{
    private final String TAG = "MatchedPair";

    FeaturesWithMat img, ref;

    private List<MatOfDMatch> matches;
    private List<DMatch> goodMatches;

    public MatchedPair(FeaturesWithMat marker, FeaturesWithMat image){
        ref = marker;
        img = image;

        matches = new ArrayList();
        goodMatches = new ArrayList();
        //match();
    }

    public short match(FeaturesManager fm){
        matches = fm.matchFeatures(img.descriptors, ref.descriptors);
        DMatch [] m2 = new DMatch[2];
        for(MatOfDMatch match: matches){
            m2 = match.toArray();
            ref.lMatchedKeypoints.add(ref.lKeypoints.get(m2[0].trainIdx).pt);
            img.lMatchedKeypoints.add(ref.lKeypoints.get(m2[0].queryIdx).pt);
        }
        goodMatches = fm.getGoodFeatures(matches, ref, img);
        return (short)goodMatches.size();
    }

}
 
 