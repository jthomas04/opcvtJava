package studio.v.opcvtjava.auriek;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.view.SurfaceView;

import studio.v.opcvtjava.R;

import static studio.v.opcvtjava.auriek.AureikLogger.KEY_AR_state;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_bitmap_sticker;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_file_string;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_mode_picked;
import static studio.v.opcvtjava.auriek.AureikLogger.UiThread;
import static studio.v.opcvtjava.auriek.AureikLogger.logIt;


public class AureikARHome extends Activity implements SensorEventListener2 {
    static final int PICKFILE_REQUEST_CODE = 12435;
    static final int STICKER_REQUEST_CODE = 15435;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 024513;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 024514;
    private static AlertDialog alertDialog;
    private static int modePicked = 0;
    private static Uri uriSticker, uriModel;
    private final float[] mRotationMatrixIn = new float[16];
    private final float[] mRotationMatrixOut = new float[16];
    Quaternion quaternion;
    private boolean permitted = false;
    private SurfaceView surfaceView;
    private SensorManager sensorManager;
    private BRenderer arRenderer;
    private int scrWidth, scrHeight;
    private float aspectRatio;
    private Matrix4 mRotationMatrix1;
    private float[] mRotationMatrix = new float[16];
    private Camera mCamera = null;
    private CameraView mCameraView = null;
    private Intent filePickIntent;
    private boolean ARstate = false;

    private double[] floatToDoubleArray(float[] floatArr) {
        double[] a = new double[floatArr.length];
        for (int i = 0; i < floatArr.length; i++) {
            a[i] = floatArr[i];
        }
        return a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_aureik_arhome);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        logIt("started Activity loaded layout", UiThread);
        surfaceView = findViewById(R.id.aureikARGLsurface);
        ImageButton imageButton = findViewById(R.id.aureikARButton);
        quaternion = new Quaternion();
        mRotationMatrix[0] = 1;
        mRotationMatrix[5] = 1;
        mRotationMatrix[10] = 1;
        mRotationMatrix[15] = 1;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                    ARstate = extras.getBoolean(KEY_AR_state);
                    if (!ARstate)
                        logIt("AR STATE IS FALSE when its supposed to be true make sure it doesn't happen", UiThread);
                    modePicked = extras.getInt(KEY_mode_picked);
                    String file = extras.getString(KEY_file_string);
                    String sticker = extras.getString(KEY_bitmap_sticker);
                    if(modePicked == 1){
                        uriSticker = Uri.parse(sticker);
                    }
                    else if (modePicked == 2) {
                        uriModel = Uri.parse(file);
                    }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            else
                permitted = true;
        }
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            logIt("Failed to get camera: " + e.getMessage(), UiThread);
            Toast.makeText(this, "Please provide Camera permissions", Toast.LENGTH_SHORT).show();
        }

        SurfaceViewINIT:
        {
            surfaceView.setFrameRate(60.0);
            surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            surfaceView.setTransparent(true);
            surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            surfaceView.setEGLContextClientVersion(2);
            surfaceView.setZOrderOnTop(false);
            logIt("default Surface view properties set", UiThread);
            switch (modePicked) {
                case 0: {
                    arRenderer = new BRenderer(this, 0);
                }
                break;
                case 1: {
                    Bitmap stickerImage;
                    try {
                        stickerImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriSticker);
                    } catch (Exception e) {
                        logIt("data from image picker not found", UiThread);
                        stickerImage = BitmapFactory.decodeResource(getResources(), R.drawable.addiee);
                    }
                    arRenderer = new BRenderer(this, 1, stickerImage);
                }
                break;
                case 2: {
                    if (permitted) {
                        try {
                            logIt("the uriModelis " + uriModel.toString(), UiThread);
                            arRenderer = new BRenderer(this, 2, uriModel);
                            parseUriModel(uriModel);
                        } catch (Exception e) {
                            logIt(e.getMessage(), UiThread);
                            arRenderer = new BRenderer(this, 0);
                        }
                    }
                }
                break;
                default:
                    arRenderer = new BRenderer(this, 0);
                    break;
            }
            arRenderer.setView(surfaceView);
            arRenderer.setAR(true);
            surfaceView.setSurfaceRenderer(arRenderer);

            if (mCamera != null) {
                mCameraView = new CameraView(this, mCamera);
                FrameLayout camera_view = findViewById(R.id.AureikcameraView);
                camera_view.addView(mCameraView);
            }
        }
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inflateADialog();
            }
        });
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            if (sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null) {
                Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
            } else if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
                Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME);
            } else {
                Toast.makeText(this, "No Rotation sensors you cannot run AR mode!", Toast.LENGTH_SHORT).show();
                Intent backToNormal = new Intent(this, AureikHome.class);
                backToNormal.putExtra(KEY_mode_picked, modePicked);
                if (uriSticker != null)
                    backToNormal.putExtra(KEY_bitmap_sticker, uriSticker.toString());
                if (uriModel != null) backToNormal.putExtra(KEY_file_string, uriModel.toString());
                backToNormal.putExtra(KEY_AR_state, false);
                startActivity(backToNormal);
            }
        }
    }

    private void inflateADialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_aureik_ar_options, null, false);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(AureikARHome.this);
        mBuilder.setView(v);
        mBuilder.create();
        mBuilder.setCancelable(true);
        mBuilder.show();
//        AlertDialogSETUP:
//        {
//            alertDialog = mBuilder.create();
//            alertDialog.setCancelable(true);
//            alertDialog.show();
//        }
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrixIn, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrixIn, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_Y, mRotationMatrixOut);
            quaternion = new Quaternion().fromMatrix(floatToDoubleArray(mRotationMatrixIn));
            arRenderer.setQuaternion(quaternion.inverse());
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void dialog_onarclick(View view) {
        logIt("awaiting User InPut", UiThread);
        switch (view.getId()) {
            case R.id.aureikMode0Button: {
                modePicked = 0;
                AureikARHome.this.recreate();
            }
            break;
            case R.id.aureikMode1Button: {
                modePicked = 1;
                Intent stickerPicker = new Intent(this, AureikSticker.class);
                stickerPicker.putExtra(KEY_AR_state, ARstate);
                stickerPicker.putExtra(KEY_mode_picked, modePicked);
                startActivity(stickerPicker);
                if (alertDialog != null && alertDialog.isShowing())
                    alertDialog.dismiss();
                AureikARHome.this.finish();
            }
            break;
            case R.id.aureikMode2Button: {
                modePicked = 2;
                filePickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                filePickIntent.addCategory(Intent.CATEGORY_OPENABLE);
                filePickIntent.setType("*/*");
                startActivityForResult(Intent.createChooser(filePickIntent, "Choose a file"), PICKFILE_REQUEST_CODE);
            }
            break;
            case R.id.aureikARModeButton:
                Intent normalMode = new Intent(AureikARHome.this, AureikHome.class);
                normalMode.putExtra(KEY_mode_picked, modePicked);
                if (uriSticker != null)
                    normalMode.putExtra(KEY_bitmap_sticker, uriSticker.toString());
                if (uriModel != null) normalMode.putExtra(KEY_file_string, uriModel.toString());
                normalMode.putExtra(KEY_AR_state, false);
                startActivity(normalMode);
                if (alertDialog != null && alertDialog.isShowing())
                    alertDialog.dismiss();
                AureikARHome.this.finish();
                return;
        }
//        alertDialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKFILE_REQUEST_CODE && (resultCode == RESULT_OK && data != null)) {
            uriModel = data.getData();
            this.recreate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permitted = true;
                } else {
                    Toast.makeText(this, "AR mod doesn't work withput Camera!! Give the permissions", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean val = super.onTouchEvent(event);
        arRenderer.onTouchEvent(event);
        return val;
    }

    private void parseUriModel(Uri uriModel) {
        AsyncObjLoader asyncObjLoader = new AsyncObjLoader(arRenderer, uriModel);
        try {
            asyncObjLoader.asyncParse(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}