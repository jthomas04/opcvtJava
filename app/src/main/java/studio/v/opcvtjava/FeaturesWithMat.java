package studio.v.opcvtjava;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FeaturesWithMat {
    private Mat m;

    short keypoints;
    Mat descriptors;
    Mat mask;
    List<KeyPoint> lKeypoints;
    List<Point> lMatchedKeypoints;
    List<Point> lGoodKeypoints;

//    public FeaturesWithMat(){
//        m = new Mat();
//    }

    public FeaturesWithMat(Mat marker){
        descriptors = new Mat();
        lKeypoints = new ArrayList<KeyPoint>();
        lMatchedKeypoints = new LinkedList<Point>();
        lGoodKeypoints = new ArrayList<>();
        m = marker;
        mask = new Mat();
    }

    public Mat getMat(){
        return m;
    }

    public void setMask(Mat maskToSet){
        mask = maskToSet;
    }


}
