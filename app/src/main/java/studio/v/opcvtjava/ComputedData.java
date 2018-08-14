package studio.v.opcvt;

/**
 * Created by JJ on 03/04/2017.
 */

public class ComputedData { //This class will contain info from native code
    public int state;
    public float angleX;
    public float angleY;
    public float angleZ;
    public float cX;
    public float cY;
    public float cZ;

    public ComputedData(int s, float eX , float eY, float eZ, float tX, float tY, float tZ){
        state = s;
        angleX = eX;
        angleY = eY;
        angleX = eZ;
        cX = tX;
        cY = tY;
        cZ = tZ;
    }
    public ComputedData(int s){
        state = s;
        angleX = 0.0f;
        angleY = 0.0f;
        angleX = 0.0f;
        cX = 0.0f;
        cY = 0.0f;
        cZ = 0.0f;
    }
    public void setState(int i){
        state = i;
    }

}
