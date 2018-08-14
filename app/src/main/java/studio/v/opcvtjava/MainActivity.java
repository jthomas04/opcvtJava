package studio.v.opcvt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import android.content.ActivityNotFoundException;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;

public class MainActivity extends Activity {

    private final static String TAG = "M:Activity ";
    private static int REQUEST_CAMERA = 0, SELECT_FILE = 1, PIC_CROP = 2;
    private ImageView ivImage;
    private String userChoosenTask;
    private Uri imageUri;
    private Button tstBtn;
    private boolean opencvReady = false;
    private ART t;
    private Bitmap Bmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnSelect = (Button) findViewById(R.id.btnSelectPhoto);
        btnSelect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectItems();
            }
        });
        ivImage = (ImageView) findViewById(R.id.ivImage);
        tstBtn = (Button) findViewById(R.id.testBtn);

    }

    @Override
    public void onResume() { //ReSuMeD OnCe I GuEsS, BuT iT wAs MoRe lIkE a GoOd StArT.
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCv Library not found. using OpenCv Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "Internal OpenCv Library found inside pacakge. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    opencvReady = true;
                    t = new ART();
                    Log.i(TAG, "OpenCv Loaded sucessfully");
                }break;
                default:
                {
                    super.onManagerConnected(status);
                }break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if (userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    private void selectItems() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(MainActivity.this);

                if (items[item].equals("Take Photo")) {
                    userChoosenTask = "Take Photo";
                    if (result)
                        cameraIntent();

                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask = "Choose from Library";
                    if (result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = Uri.fromFile(getFile("opcvt"));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        //Log.d(TAG,imageUri.toString());
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if(tstBtn.getVisibility() == View.GONE)
                tstBtn.setVisibility(View.VISIBLE);
            if(requestCode==SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if(requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
            else if(requestCode == PIC_CROP)
                onCropImage(data);
        }
    }

    private File getFile(String path) {
        File folder;
        //if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
        folder = Environment.getExternalStoragePublicDirectory(path);
        if (!folder.exists()) {
            folder.mkdir();//if it doesn't exist the folder will be created
        }
        //}
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        String fpath = folder + "/" + imageFileName + ".jpg";
        File image_file = new File(fpath);
        return image_file;
    }

    private void onCaptureImageResult(Intent data) {
        if(data == null) {
            ivImage.setImageURI(imageUri);
            return;
        }
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (thumbnail != null) {
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 98, bytes);
        }
        File destination = getFile("opcvt");
        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageUri = Uri.fromFile(destination);
        Log.d(TAG,imageUri.toString());
        ivImage.setImageBitmap(thumbnail);
        Bmp = Bitmap.createBitmap(thumbnail);
    }

    private void onCropImage(Intent data){
        // get the returned data
        Bundle extras = data.getExtras();
        // get the cropped bitmap
        Bitmap selectedBitmap = extras.getParcelable("data");
        ivImage.setImageBitmap(selectedBitmap);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (selectedBitmap != null) {
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        }
        File destination = getFile("opcvt/markers");
        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageUri = Uri.fromFile(destination);
        ivImage.setImageBitmap(selectedBitmap);
        Bmp = Bitmap.createBitmap(selectedBitmap);
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                Log.v("GALLERY:", "Caught!");
                e.printStackTrace();
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        ivImage.setImageBitmap(bm);
        imageUri = data.getData();
        Bmp = Bitmap.createBitmap(bm);
    }

    public void cropit(View view) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // indicate image type and Uri
            cropIntent.setDataAndType(imageUri, "image/*");
            // set crop properties here
            cropIntent.putExtra("crop", true);
            // indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            // indicate output X and Y
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            // retrieve data on return
            cropIntent.putExtra("return-data", true);
            // start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException anfe) {
            // display an error message
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void accept(View view){
        Log.d(TAG, "OnClick Fired!!" );
        Intent cam = new Intent(this, ArActivity.class);
        cam.putExtra("iUri", imageUri);
        Log.d(TAG, "ACT2: Started...");
        startActivity(cam);

//		Intent vr=new Intent(this,AR.class);
//		Log.v("ACT2:","Started");
//		startActivity(vr);
//
        this.finish();

    }

    public void showKps(View view){
        if(!opencvReady) {
            Log.e(TAG, "OpenCV Library Not loaded & Ready!!");
            return;
        }
        t.loadMarker(Bmp);
        Mat markerFeatures = t.drawMarkerFeatures();
        Bmp = Bitmap.createBitmap(markerFeatures.cols(), markerFeatures.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(markerFeatures,Bmp);
        ivImage.setImageBitmap(Bmp);
    }

}


