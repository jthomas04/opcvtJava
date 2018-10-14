package studio.v.opcvtjava;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.net.Uri;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


import min3d.core.RendererActivity;
//import studio.v.opcvtjava.ART;


import static android.R.attr.data;


public class MenuActivity extends AppCompatActivity {
    //For logging
    private static final String TAG = " MC:Activity ";


    public MenuActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picker);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.finish();
    }

    public void startAR(View view){
        Intent next = new Intent(this, ArActivity2.class);
        startActivity(next);

        this.finish();
    }

    public void startMarkerAR(View view){
        Intent next = new Intent(this, MarkerSelectorActivity.class);
        startActivity(next);

        this.finish();
    }

    public void startVR(View view){
        Intent next = new Intent(this, VrActivity.class);
        startActivity(next);

        this.finish();
    }
}




