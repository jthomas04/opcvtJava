package studio.v.opcvtjava;

import org.opencv.core.Mat;

public class NFMarker extends FeaturesWithMat{

    public NFMarker(Mat m){
        super(m);
    }

    public void analyze(FeaturesManager FM){
        FM.getKeypointsM(this);
        FM.computeDesc1(this, true);
    }

}
