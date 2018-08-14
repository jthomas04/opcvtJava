package studio.v.opcvt;

/**
 * Created by JJ on 03/02/2018.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.ISurface;

import java.util.List;

import org.opencv.video.KalmanFilter;


public class VrActivity extends Activity implements SensorEventListener2 {
    org.rajawali3d.view.SurfaceView surfaceView;
    VRenderer vR;

    KalmanFilter kf;
    private SensorManager mSensorManager;
    private Sensor sensor;

    private final float[] mRotationMatrix = new float[16];
    private final float[] finalRotations = new float[16];

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mAccelerometerOldReading = new float[3];

    private final float alpha = 0.8f;
    private final float[] mGravityReading = new float[3];
    private final float[] mLAccelerationReading = new float[3];

    private final float[] maxLA = new float[3];
    private final float[] minLA = new float[3];

    private final float[] mMagnetometerReading = new float[3];
    private final float[] mGyroscopeReading = new float[3];

    private final float[] mRotationMatrix2 = new float[16];
    private final float[] mOrientationAngles = new float[3];

    private float[] eulers = new float[3];

    private double resultantTesla = 0;
    //Minimum and maximum values for overall Magnetic field magnitude.
    private final double maxTesla = 55;
    private final double minTesla = 35;

//    private final float[] maxLa = {0.07694663f, 0.07920146f, 0.03429985f};
//    private final float[] minLa = {-0.043663174f, -0.009892904f, -0.009892904f};
    private final float[] maxLa = {0.08f, 0.08f, 0.039f};
    private final float[] minLa = {-0.049f, -0.01f, -0.01f};

    private int sType;

    private int sensorStatus = 0;
    private final short ACCEL = 1;
    private final short GYRO = 2;
    private final short MAG = 4;
    private final short ORIENT = 8;
    private final short LINEAR = 16;
    private final short GRAVITY = 32;
    private final short AGM = ACCEL | GYRO | MAG;
    private final short AG = ACCEL | GYRO;
    private final short AM = ACCEL | MAG;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.obj_viewer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeSensors();

        surfaceView = (org.rajawali3d.view.SurfaceView)findViewById(R.id.sV);
        surfaceView.setFrameRate(60.0);
        surfaceView.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
        vR = new VRenderer(this);
        surfaceView.setSurfaceRenderer(vR);
        mRotationMatrix[ 0] = 1;
        mRotationMatrix[ 6] = 1;
        mRotationMatrix[ 9] = 1;
        mRotationMatrix[ 15] = 1;
//        mAccelerometerOldReading[0] = 0;
//        mAccelerometerOldReading[1] = 0;
//        mAccelerometerOldReading[2] = 0;

    }

    @Override
    public void onStop(){
        super.onStop();
        this.finish();
    }

    private void initializeSensors(){
        mSensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);

        SensorEventListener2 list = this;

        Log.w("SM", "Trying to get sensors");
        List <Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Sensor oSensor = null;
        String sName;
//        for(Sensor s: sensors){
//            sName = s.getName();
//            Log.w("Sensor Online", sName + "-" + s.getType());
//            if (sName == "ORIENTATION"){
//                oSensor = s;
//                Log.w("Sensors", "Found him!");
//            }
//        }

        Sensor accel, magneto, gyro, linearA, gravity;
        accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneto = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        linearA = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        if(gyro != null)
            mSensorManager.registerListener(list, gyro, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
        else
            Log.w("Sensors", "Gyroscope Not available");
        //Assuming all phones have an accelerometer.
        mSensorManager.registerListener(list, accel, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
        if (magneto != null) {
            mSensorManager.registerListener(list, magneto, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
//            if(sensor!= null){ //If orientation sensor not present, skip
//                mSensorManager.registerListener(list, sensors.get(3), SensorManager.SENSOR_DELAY_GAME);
//            }
//
// else{
//                Log.w("Sensors", "Orientation Sensor not available");
//            }
        } else {
            Log.e("OnCreate", "Couldn't initialize sensors.. No Magneto-meter present on device");
        }
        if(linearA != null){
            mSensorManager.registerListener(list, linearA, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
        }
        else{
            Log.w("Sensors", "Linear Acceleration sensor un-available");
        }

        if(gravity!= null){
            mSensorManager.registerListener(list, gravity, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
        }
        else{
            Log.w("Sensors", "Gravity sensor un-available");
        }

    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.w("SS", "Sensor event " + event.values[0]);
        //if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        sType = event.sensor.getType();
        //event.accuracy
        if (sType == Sensor.TYPE_ACCELEROMETER) {
            sensorStatus = sensorStatus | ACCEL;
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
            // Do some sensor average=ing with old values.
            System.arraycopy(mAccelerometerReading, 0, mAccelerometerOldReading, 0, mAccelerometerReading.length);

            //kf.


            Log.w("Gravity" , " X: " + mGravityReading[0] + " Y: " + mGravityReading[1] + " Z: " + mGravityReading[2]);
            // Remove the gravity contribution with the high-pass filter.
            //mLAccelerationReading[0] = event.values[0] - mGravityReading[0];
            //mLAccelerationReading[1] = event.values[1] - mGravityReading[1];
            //mLAccelerationReading[2] = event.values[2] - mGravityReading[2];

        }
        else if (sType == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
            resultantTesla = (double)(mMagnetometerReading[0] * mMagnetometerReading[0] +
                      mMagnetometerReading[1] * mMagnetometerReading[1] +
                      mMagnetometerReading[2] * mMagnetometerReading[2]
            );
            resultantTesla = Math.sqrt(resultantTesla);
            Log.w("Senor M", "Total uT = " + resultantTesla);
            if(resultantTesla <= maxTesla && resultantTesla >= minTesla){
                sensorStatus = sensorStatus | MAG;
            }
            else{
                Log.e("Magneto", "Magnetic interference detected!!!");
            }
        }
        else if(sType == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, mGyroscopeReading, 0, mGyroscopeReading.length);
            sensorStatus = sensorStatus | GYRO;
        }
        else if(sType == Sensor.TYPE_LINEAR_ACCELERATION){
            System.arraycopy(event.values,0, mLAccelerationReading, 0, mLAccelerationReading.length);
            sensorStatus = sensorStatus | LINEAR;
        }
        else if(sType == Sensor.TYPE_GRAVITY){
            System.arraycopy(event.values,0, mGravityReading, 0, mGravityReading.length);
            sensorStatus = sensorStatus | GRAVITY;
        }
//        Values for orientation sensor
//        if(event.values != null) {
//            eulers[0] = event.values[0];
//            eulers[1] = event.values[1];
//            eulers[2] = event.values[2];
//            //SensorManager.getOrientation(mRotationMatrix, event.values);
//            SensorManager.getRotationMatrixFromVector(
//                    mRotationMatrix, event.values);
//            if (mRotationMatrix == null) {
//
//            }
//        }
//    }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void updatePositonOrientation() {
       /* // Isolate the force of gravity with the low-pass filter.
        mGravityReading[0] = alpha * mGravityReading[0] + 0.2f * mAccelerometerReading[0];
        mGravityReading[1] = alpha * mGravityReading[1] + 0.2f * mAccelerometerReading[1];
        mGravityReading[2] = alpha * mGravityReading[2] + 0.2f * mAccelerometerReading[2];
        //Normalize accelerometer with Old Values.
        mAccelerometerReading[0] = (mAccelerometerOldReading[0] + mAccelerometerReading[0])/2;
        mAccelerometerReading[1] = (mAccelerometerOldReading[1] + mAccelerometerReading[1])/2;
        mAccelerometerReading[2] = (mAccelerometerReading[2] + mAccelerometerOldReading[2]/2);
        //switch based on which sensors where last updated!! */
       if((sensorStatus & LINEAR) == LINEAR){

       }
        if(sensorStatus == AM){
            // Update rotation matrix, which is needed to update orientation angles.
            mSensorManager.getRotationMatrix(mRotationMatrix, null, mGravityReading, mMagnetometerReading);

            /*
            Log.w("Sensor", " LA( X: " + mLAccelerationReading[0] + "Y: " +mLAccelerationReading[1] + "Z: " + mLAccelerationReading[2]);
            maxLA[0] = (maxLA[0] < mLAccelerationReading[0])?mLAccelerationReading[0]:maxLA[0];
            maxLA[1] = (maxLA[0] < mLAccelerationReading[1])?mLAccelerationReading[1]:maxLA[1];
            maxLA[2] = (maxLA[0] < mLAccelerationReading[2])?mLAccelerationReading[2]:maxLA[2];

            minLA[0] = (minLA[0] > mLAccelerationReading[0])?mLAccelerationReading[0]:minLA[0];
            minLA[1] = (minLA[1] > mLAccelerationReading[1])?mLAccelerationReading[0]:minLA[1];
            minLA[2] = (minLA[1] > mLAccelerationReading[2])?mLAccelerationReading[0]:minLA[2];

            for(int i = 0; i < 3; i++){
                mLAccelerationReading[i] = (mLAccelerationReading[i] < maxLa[i] && mLAccelerationReading[i] > minLa[i])?0:mLAccelerationReading[i];
            }

            Log.w("mLA ", " minLA = x: " + minLA[0] + " y: " + minLA[1] + " z: " + minLA[2] );
            Log.w("mLA ", " maxLA = x: " + maxLA[0] + " y: " + maxLA[1] + " z: " + maxLA[2] );
            */

        }
        else if (sensorStatus == AGM){
            //TODO integrate Gyro data
            //mSensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, mGyroscopeReading);
            Log.w("Sensor", " LA( X: " + mLAccelerationReading[0] + "Y: " +mLAccelerationReading[1] + "Z: " + mLAccelerationReading[2]);
        }
        else if(sensorStatus == AG){
            // TODO : properly use gyro data
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, mGyroscopeReading);
            Log.w("Sensor", " LA( X: " + mLAccelerationReading[0] + " Y: " +mLAccelerationReading[1] + " Z: " + mLAccelerationReading[2] + " )");
            //mSensorManager.getRotationMatrix(mRotationMatrix, ,)
        }

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
        // "mOrientationAngles" now has up-to-date information.

        sensorStatus = 0; //Reset Sensor status variable.

    }


    class VRenderer extends Renderer{

        private DirectionalLight mDirectionalLight;
        private Sphere a;
        private Matrix4 mat4;

        public VRenderer(Context context) {
            super(context);
            setFrameRate(60);
        }

        @Override
        protected void initScene() {
            mDirectionalLight = new DirectionalLight(1f, .2f, -1.0f);
            mDirectionalLight.setColor(0.8f, 0.8f, 0.8f);
            mDirectionalLight.setPower(2);
            getCurrentScene().addLight(mDirectionalLight);
            Material material = new Material();
            material.enableLighting(true);
            material.setDiffuseMethod(new DiffuseMethod.Lambert());
            material.setSpecularMethod(new SpecularMethod.Phong());
            material.setColorInfluence(0);
            Texture earthTexture = new Texture("Earth", R.drawable.earth);
            mat4 = new Matrix4(mRotationMatrix);
            try{
                material.addTexture(earthTexture);

            } catch (ATexture.TextureException error){
                Log.e(getLocalClassName() + ".initScene", error.toString());
            }
            a = new Sphere(1,30,30);
            a.setMaterial(material);
            a.setPosition(0,0,-5);
            a.setRotation(0,0,-90);
            getCurrentCamera().setPosition(0,0,0);
            getCurrentScene().addChild(a);

        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

        }

        @Override
        public void onTouchEvent(MotionEvent event) {

        }

        @Override
        public void onRender(final long elapsedTime, final double deltaTime) {
            super.onRender(elapsedTime, deltaTime);
            updatePositonOrientation();

            mat4 = new Matrix4(mRotationMatrix);

            //Quaternion qr = new Quaternion().fromEuler((double)eulers[2] * 180/Math.PI, (double)eulers[1] * 180/Math.PI, (double)eulers[0] * 180/ Math.PI);
            //mat4 = new Matrix4(qr);
            //a.setRotation(qr);
            mat4.rotate(new Vector3(0,0,1), 180);
            mat4.rotate(new Vector3(1,0,0),-45);
            //getCurrentCamera().setRotation(mat4); //.inverse());
            a.setRotation(mat4);
            //2, 1, 0 because X axis moves right positively.
            a.moveForward(mLAccelerationReading[2]);
            a.moveUp(mLAccelerationReading[1]);
            a.moveRight(mLAccelerationReading[0]);

//            getCurrentCamera().setCameraYaw(eulers[0]);
//            getCurrentCamera().setCameraPitch(eulers[2]);
//            getCurrentCamera().setCameraRoll(eulers[1]);
        }
    }
}