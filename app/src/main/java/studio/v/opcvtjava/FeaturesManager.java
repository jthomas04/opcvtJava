package studio.v.opcvtjava;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.ORB;
import org.opencv.utils.Converters;

import java.util.List;

public class FeaturesManager {
    private ORB detector;
    private MatOfKeyPoint mKeypoints1;
    private MatOfKeyPoint mKeypoints2;

    public FeaturesManager(){
        detector = ORB.create();
        mKeypoints1 = new MatOfKeyPoint();
        mKeypoints2 = new MatOfKeyPoint();
    }

    public void getKeypointsM(FeaturesWithMat marker){
        detector.detect(marker.getMat(), mKeypoints1);
        marker.lKeypoints = mKeypoints1.toList();
        marker.keypoints = (short)marker.lKeypoints.size();
    }

    public void computeDesc1(FeaturesWithMat marker, List<KeyPoint> lKeypoints, boolean pointsGiven){
        detector.detectAndCompute(marker.getMat(), marker.mask , mKeypoints1, marker.descriptors, pointsGiven);
    }

}
