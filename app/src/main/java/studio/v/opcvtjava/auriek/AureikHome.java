package studio.v.opcvtjava.auriek;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import org.rajawali3d.view.SurfaceView;

import studio.v.opcvtjava.R;

import static studio.v.opcvtjava.auriek.AureikLogger.KEY_AR_state;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_bitmap_sticker;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_file_string;
import static studio.v.opcvtjava.auriek.AureikLogger.KEY_mode_picked;
import static studio.v.opcvtjava.auriek.AureikLogger.UiThread;
import static studio.v.opcvtjava.auriek.AureikLogger.logIt;

//TODO FILE ACCESS FIX gotta fix that, Making the stickerGenerator activity Aureik-ish , Drag Sensing, Using the sensors RotationVector if available or Gyro if now also WHY is there not a rotation vector sensor in some phones are they dumb? do they think that by not including one SOFTWARE BASED SENSOR they can reduce the phone price? meh Its surprising You're still reading this get to work!
public class AureikHome extends Activity {
    static final int PICKFILE_REQUEST_CODE = 1512435;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 024513;
    private static AlertDialog alertDialog;
    private static int modePicked = 0;
    private static Uri uriSticker, uriModel;
    private boolean permitted = false;
    private SurfaceView surfaceView;
    private BRenderer bRenderer;
    private Intent filePickIntent;
    private boolean ARstate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_aureik_home);
        logIt("started Activity loaded layout", UiThread);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        surfaceView = findViewById(R.id.aureikGLsurface);
        ImageButton imageButton = findViewById(R.id.aureikButton);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                try {
                    ARstate = extras.getBoolean(KEY_AR_state);
                    if (ARstate)
                        logIt("AR STATE IS TRUE when its supposed to be false make sure it doesn't happen", UiThread);
                    modePicked = extras.getInt(KEY_mode_picked);
                    String file = extras.getString(KEY_file_string);
                    String sticker = extras.getString(KEY_bitmap_sticker);
                    uriSticker = Uri.parse(sticker);
                    uriModel = Uri.parse(file);
                } catch (Exception e) {
                    logIt(e.getMessage(), UiThread);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_STORAGE);
            else
                permitted = true;
        }
        SurfaceViewConfig:
        {
            surfaceView.setFrameRate(60.0);
            surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            surfaceView.setEGLContextClientVersion(2);
            surfaceView.setZOrderOnTop(false);
            logIt("default Surface view properties set", UiThread);
            switch (modePicked) {
                case 0: {
                    bRenderer = new BRenderer(this, 0);
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
                    bRenderer = new BRenderer(this, 1, stickerImage);
                }
                break;
                case 2: {
                    if (permitted) {
                        bRenderer = new BRenderer(this, 2, uriModel);
                        try {
                            parseUriModel(uriModel);
                        } catch (Exception e) {
                            logIt(e.getMessage(), UiThread);
                        }
                    }
                }
                break;
                default:
                    bRenderer = new BRenderer(this, 0);
                    break;
            }
            bRenderer.setView(surfaceView);
            bRenderer.setAR(false);
            surfaceView.setSurfaceRenderer(bRenderer);
        }
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inflateADialog();
            }
        });
    }

    private void inflateADialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_aureik_options, null, false);
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(AureikHome.this);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKFILE_REQUEST_CODE && (resultCode == RESULT_OK && data != null)) {
            uriModel = data.getData();
            logIt("picked file is " + uriModel.toString(), UiThread);
            this.recreate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permitted = true;
                } else {
                    Toast.makeText(this, "Please grant storage permissions to access the file", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void dialog_onclick(View view) {
        logIt("awaiting User InPut", UiThread);
        switch (view.getId()) {
            case R.id.aureikMode0Button: {
                modePicked = 0;
                this.recreate();
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
                AureikHome.this.finish();
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
                Intent arMode = new Intent(AureikHome.this, AureikARHome.class);
                arMode.putExtra(KEY_mode_picked, modePicked);
                if (uriSticker != null) arMode.putExtra(KEY_bitmap_sticker, uriSticker.toString());
                if (uriModel != null) arMode.putExtra(KEY_file_string, uriModel.toString());
                arMode.putExtra(KEY_AR_state, true);
                startActivity(arMode);
                if (alertDialog != null && alertDialog.isShowing())
                    alertDialog.dismiss();
                AureikHome.this.finish();

                return;
        }
//        alertDialog.dismiss();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean val = super.onTouchEvent(event);
        bRenderer.onTouchEvent(event);
        return val;
    }

    private void parseUriModel(Uri uriModel) {
        AsyncObjLoader asyncObjLoader = new AsyncObjLoader(bRenderer, uriModel);
        try {
            asyncObjLoader.asyncParse(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
