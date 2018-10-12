package studio.v.opcvtjava;

import org.opencv.core.Mat;

public class NFImage extends FeaturesWithMat {
    public NFImage(Mat mat){
        super(mat);
    }

    public void analyze(FeaturesManager FM){
        FM.getKeypointsI(this);
        FM.computeDesc2(this, true);
    }
}
