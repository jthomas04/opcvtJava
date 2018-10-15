package studio.v.opcvtjava.auriek;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import studio.v.opcvtjava.R;

import static org.opencv.core.Core.FILLED;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;
import static org.opencv.imgproc.Imgproc.INTER_MAX;
import static org.opencv.imgproc.Imgproc.LINE_8;
import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.convexHull;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.threshold;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_AR_state;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_bitmap_sticker;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_mode_picked;
import static studio.v.opcvtjava.auriek.AureikLogger.UiThread;
import static studio.v.opcvtjava.auriek.AureikLogger.logIt;

public class AureikSticker extends Activity implements View.OnClickListener {
    private final int dilateSize = 3;
    private final String prefix = "IN" + "ADDIEDEVJON";
    private final int REQUESTCAMERACODE = 4254162, REQUESTGALLERYCODE = 4254163;
    Button pickImage, confirmImage, stickerize;
    ImageView imageView;
    Bitmap sticker = null;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.w("OPENCV", "OpenCv Loaded sucessfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private boolean AR = false;
    private String fileDir;
    private Uri path;
    private Uri homeUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aureik_sticker);
        pickImage = findViewById(R.id.AureikStickerPICKIMAGEButton);
        confirmImage = findViewById(R.id.AureikStickerDONEIMAGEButton);
        imageView = findViewById(R.id.AureikStickerImageView);
        stickerize = findViewById(R.id.AureikStickerSTICKERIZEIMAGEButton);
        Bundle bundle = getIntent().getExtras();
        try {
            AR = bundle.getBoolean(KEY_AR_state);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logIt("AR MODE IS" + AR, UiThread);
        pickImage.setOnClickListener(this);
        confirmImage.setOnClickListener(this);
//        imageView.setOnClickListener(this);
        stickerize.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.AureikStickerPICKIMAGEButton:
                setDisplayPicture();
                break;
            case R.id.AureikStickerDONEIMAGEButton:
                passToHome(homeUri);
                break;
            case R.id.AureikStickerSTICKERIZEIMAGEButton:
            case R.id.AureikStickerImageView:
                if (sticker != null) {
                    new StickerTask(this).execute(sticker);
                }
                break;
        }
    }

    private Bitmap generateSticker(Bitmap sticker) {
        List<MatOfPoint> contours;
        Mat elementDilate;
        Mat hierarchy;
        MatOfInt hull;
        Mat src;
        Mat gray;
        src = new Mat(sticker.getHeight(), sticker.getWidth(), CV_8UC4);
        gray = new Mat(src.size(), CV_8U);
        Mat temp = new Mat(src.size(), src.type());
        Mat output;
        Bitmap Op;
        hull = new MatOfInt();
        contours = new ArrayList<>();
        elementDilate = getStructuringElement(MORPH_ELLIPSE, new Size(2 * dilateSize + 1, 2 * dilateSize + 1), new Point(dilateSize, dilateSize));
        hierarchy = new Mat();
        Utils.bitmapToMat(sticker, src);
        Imgproc.cvtColor(src, gray, COLOR_RGBA2GRAY);
        threshold(gray, gray, 120, 256, Imgproc.THRESH_BINARY_INV);
        Imgproc.dilate(gray, gray, elementDilate);
        Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 1, 1);
        Imgproc.morphologyEx(gray, gray, MORPH_CLOSE, elementDilate);
        Imgproc.findContours(gray, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
        Rect boundingRectangle;
        MatOfPoint pickedContour = null;
        int largestContourIndex = -1;
        double lCountourSize = Double.MIN_VALUE;
        for (int i = 0; i < contours.size(); i++) {
            if (contourArea(contours.get(i)) > lCountourSize) {
                lCountourSize = contourArea(contours.get(i));
                largestContourIndex = i;
            }
        }
        pickedContour = contours.get(largestContourIndex);
        convexHull(pickedContour, hull);
        MatOfPoint mopOut = new MatOfPoint();
        mopOut.create((int) hull.size().height, 1, CvType.CV_32SC2);
        for (int i = 0; i < hull.size().height; i++) {
            int index = (int) hull.get(i, 0)[0];
            double[] point = new double[]{
                    pickedContour.get(index, 0)[0], pickedContour.get(index, 0)[1]
            };
            mopOut.put(i, 0, point);
        }
        drawContours(gray, contours, largestContourIndex, new Scalar(255, 255, 255, 255), FILLED, LINE_8, hierarchy, INTER_MAX, new Point());
        boundingRectangle = boundingRect(mopOut);
        Rect outputRect = new Rect(boundingRectangle.x + 2, boundingRectangle.y + 2, boundingRectangle.width - 3, boundingRectangle.height - 4);
        src.copyTo(temp, gray);
        output = temp.submat(outputRect);
        Op = Bitmap.createBitmap(output.width(), output.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(output, Op);
        return Op;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // todo use appropriate resultCode in your case
        if (resultCode == FragmentActivity.RESULT_OK) {
//            if (data.getData() != null) {
//                // this case will occur in case of picking image from the Gallery,
//                // but not when taking picture with a camera
//                try {
//                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(AureikSticker.this.getContentResolver(), data.getData());
//                    // do whatever you want with the Bitmap ....
//                    sticker = bitmap;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                // this case will occur when taking a picture with a camera
//                Bitmap bitmap = null;
//                Cursor cursor = AureikSticker.this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                        new String[]{MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED,
//                                MediaStore.Images.ImageColumns.ORIENTATION}, MediaStore.Images.Media.DATE_ADDED,
//                        null, "date_added DESC");
//                if (cursor != null && cursor.moveToFirst()) {
//                    Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
//                    String photoPath = uri.toString();
//                    cursor.close();
//                    if (photoPath != null) {
//                        bitmap = BitmapFactory.decodeFile(photoPath);
//                        sticker = bitmap;
//                    }
//                }
//
//                if (bitmap == null) {
//                    // for safety reasons you can
//                    // use thumbnail if not retrieved full sized image
//                    bitmap = (Bitmap) data.getExtras().get("data");
//                    sticker = bitmap;
//                }
//                // do whatever you want with the Bitmap ....
//            }
            if (requestCode == REQUESTGALLERYCODE) {
                Uri selectedImage = data.getData();
                homeUri = selectedImage;
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                if (c != null) {
                    c.moveToFirst();
                    int columnIndex = c.getColumnIndex(filePath[0]);
                    String picturePath = c.getString(columnIndex);
                    c.close();
                    sticker = (BitmapFactory.decodeFile(picturePath));
                }
            } else if (requestCode == REQUESTCAMERACODE) {
                try {
                    sticker = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
            imageView.setImageBitmap(sticker);
        }
    }


    private void passToHome(Uri homeUri) {
        Intent boi;
        if (!AR)
            boi = new Intent(this, AureikHome.class);
        else
            boi = new Intent(this, AureikARHome.class);
        boi.putExtra(KEY_bitmap_sticker, homeUri.toString());
        boi.putExtra(KEY_mode_picked, 1);
        startActivity(boi);
        this.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //Temporarily unused generator of normal maps for some extra 3d ness
    private Bitmap generateNormalBitmap(Bitmap diffuseTex) {
        Mat diffuse = new Mat();
        Mat normal = new Mat();
        Utils.bitmapToMat(diffuseTex, diffuse);
        Imgproc.threshold(diffuse, normal, 0, 256, THRESH_BINARY);
        Mat kernal = getStructuringElement(MORPH_ELLIPSE, new Size(21, 21), new Point(10, 10));
        erode(normal, normal, kernal);
        Imgproc.blur(normal, normal, new Size(10, 10));
        Bitmap b = Bitmap.createBitmap(normal.width(), normal.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(normal, b);
        return b;
    }

    private File saveImg(@Nullable Bitmap bmp, @NonNull String prefix) throws Exception {
        File Img;
        String fileName = String.format(Locale.ENGLISH, "sticker_%d" + new Date().getTime() + prefix + ".png", System.currentTimeMillis());
        File myDir = new File(Environment.getExternalStorageDirectory(), "addiee/stickers");
        if (!myDir.exists())
            myDir.mkdirs();
        Img = new File(myDir.toString() + "/" + fileName);
        if (Img.exists()) {
            Img = new File(myDir.toString() + "/" + fileName);
        }
        if (bmp == null) return Img;
        FileOutputStream out = null;
        Img.createNewFile();
        out = new FileOutputStream(Img);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
        return Img;
    }

    private void setDisplayPicture() {
        final CharSequence[] items = {"Choose from Library", "Camera", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(AureikSticker.this);
        builder.setTitle("Setting Display Picture");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Choose from Library")) {
                    Intent galleryPhotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryPhotoIntent, REQUESTGALLERYCODE);
                } else if (items[item].equals("Camera")) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = saveImg(null, prefix);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //path = FileProvider.getUriForFile(AureikSticker.this, getString(R.string.file_provider_authority), photoFile);
                        homeUri = path;
                        logIt("Uri Path of the local image is : " + path, UiThread);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, path);
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                            takePictureIntent.setClipData(ClipData.newRawUri("", path));
                            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        }
                        startActivityForResult(takePictureIntent, REQUESTCAMERACODE);
                    }
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private class StickerTask extends AsyncTask<Bitmap, Integer, Void> {
        File imgFile;
        private Bitmap output;
        private String filePath;
        private Exception exception;
        private String ProgressString;
        private ProgressDialog progressDialog;
        private StickerTask reference;

        public StickerTask(Context context) {
            progressDialog = new ProgressDialog(context);
            reference = this;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Generating sticker...");
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    reference.cancel(true);
                    progressDialog.dismiss();
                }
            });
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            try {
                output = generateSticker(bitmaps[0]);
                publishProgress(new Integer(1));
                imgFile = saveImg(output, prefix);
                publishProgress(new Integer(2));
            } catch (Exception e) {
                exception = e;
                publishProgress(0);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            switch (progress[0]) {
                case 0:
                    ProgressString = "Something went wrong, please try again";
                    exception.printStackTrace();
                    break;
                case 1:
                    ProgressString = "Generated Sticker, Saving file...";
                    break;
                case 2:
                    ProgressString = "Success! Saved " + filePath;
                    break;
                default:
                    //Wont be called but .. Just in case so we can know something is off...
                    ProgressString = "Doing stuff...";
                    break;
            }
            progressDialog.setMessage(ProgressString);
        }

        @Override
        protected void onPostExecute(Void a) {
            sticker = output;
            //homeUri = FileProvider.getUriForFile(AureikSticker.this, getString(R.string.file_provider_authority), imgFile);
            imageView.setImageBitmap(output);
            {
                try {

                    // Create the timestamp.
                    Date dNow = new Date();
                    SimpleDateFormat ft = new SimpleDateFormat("EEE_MMM-dd-yyyyy_HH:mm:ss_z", Locale.ENGLISH);
                    String currentTimestamp = ft.format(dNow);

                    // Generate Random Number
                    Random rn = new Random();
                    int randomKey = rn.nextInt(90000000) + 10000000;

                    // Generate Message ID
                    String fileID = "NLSNSF" + "IN-ADDIEDEVJ" + randomKey + "#" + currentTimestamp;

//                    StickerToServerTaskParams params = new StickerToServerTaskParams(
//                            getApplicationContext(), imgFile, fileID);
//                    new StickerToServerTask().execute(params);
                    Log.d("Sending Images", "Sending...!! " ); // TODO: replace >> + params);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Internal Error, please try after sometime",
                            Toast.LENGTH_SHORT).show();
                }
            }
            progressDialog.dismiss();
        }
    }
}
