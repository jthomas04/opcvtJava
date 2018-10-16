package studio.v.opcvtjava.auriek;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLES20;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.ALoader;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.async.IAsyncLoaderCallback;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.renderer.Renderer;

import studio.v.opcvtjava.R;

import static android.view.MotionEvent.INVALID_POINTER_ID;
import static studio.v.opcvtjava.auriek.AureikLogger.AsyncParserThread;
import static studio.v.opcvtjava.auriek.AureikLogger.RendererThread;
import static studio.v.opcvtjava.auriek.AureikLogger.logIt;


public class BRenderer extends Renderer implements IAsyncLoaderCallback {
    private static Vector3 anglecontainer = new Vector3();
    private final int DEFAULTMODEL = 0, STICKER = 1, SDCARDMODEL = 2;
    private final int backgroundId = R.drawable.renderer_bg;
    private final float[] indexPrev = new float[2], otherPrev = new float[2], midPrev = new float[2];
    private final float[] touchVals = new float[4];
    //    String fileOnSD;
    Context context;
    Material defaultMaterial;
    Object3D centered;
    Bitmap stickerImage = null;
    int mode;
    View view;
    boolean AR;
    DirectionalLight mDirectionalLight;
    Vector3 vec, campos;
    private Quaternion quaternion;
    private Matrix4 mRotationMatrix;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector gDetector;
    private AureikGestureListener mGestureListener;
    private float scaleFactor = 1f;
    private float cameraRadius = 10f;
    private Uri objfile;

    BRenderer(Context context, int mode) {
        super(context);
        this.context = context;
        this.mode = mode;
        logIt("Initialising render thread in " + mode + "mode", RendererThread);
        quaternion = new Quaternion();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureListener = new AureikGestureListener();
        gDetector = new GestureDetector(context, new AureikGestureListener());
    }

    BRenderer(Context context, int mode, Uri objfile) {
        super(context);
        this.context = context;
        this.mode = mode;
        this.objfile = objfile;
        logIt("Initialising render thread in " + mode + "mode", RendererThread);
        quaternion = new Quaternion();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureListener = new AureikGestureListener();
        gDetector = new GestureDetector(context, new AureikGestureListener());
    }

    BRenderer(Context context, int mode, @Nullable Bitmap stickerImage) {
        super(context);
        this.context = context;
        this.mode = mode;
        this.stickerImage = stickerImage;
        logIt("Initialising render thread in " + mode + "mode", RendererThread);
        quaternion = new Quaternion();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureListener = new AureikGestureListener();
        gDetector = new GestureDetector(context, new AureikGestureListener());
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setAR(boolean AR) {
        this.AR = AR;
    }

    private void initSceneForMode(int mode) {
        switch (mode) {
            case DEFAULTMODEL: {
                logIt("Starting the loader for default object", RendererThread);
                final LoaderOBJ loaderOBJ = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.addiee);
                logIt("loading for default object", AsyncParserThread);
                loadModel(loaderOBJ, this, R.raw.addiee);
            }
            break;
            case STICKER: {
                Plane mPlane = new Plane((float) 0.02 * stickerImage.getWidth(), (float) 0.02 * stickerImage.getHeight(), 3, 3);
                Material material001 = new Material();
                material001.setDiffuseMethod(new DiffuseMethod.Lambert());
                material001.setColorInfluence(0);
                try {
                    material001.addTexture(new Texture("diffuse_texture", stickerImage));
                } catch (ATexture.TextureException e) {
                    logIt(e.getMessage(), RendererThread);
                }
                mPlane.setMaterial(material001);
                mPlane.setDoubleSided(true);
                mPlane.setBlendingEnabled(true);
                mPlane.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                mPlane.setPosition(Vector3.ZERO);
                centered = mPlane;
//                if (!AR)
                getCurrentScene().addChild(centered);
//                else
//                    ARinits(centered);
            }
            break;
            case SDCARDMODEL: {
            }
            break;
        }
    }

//    private Object3D ARinits(Object3D centered) {
//        final Object3D origin = new Object3D();
//        origin.addChild(centered);
////        Handler handler = new Handler();
////        handler.postDelayed(new Runnable() {
////            public void run() {
////                // Actions to do after 10 seconds
////                origin.setRotation(quaternion);
////        centered.rotate(0, 0, 1, 90);
//        centered.setPosition(4, 0, 0);
////                centered.rotate(0,1,90);
////                origin.rotate(1,0,0,-90);
//        getCurrentScene().addChild(origin);
////            }
////        }, 1000);
////        centered.setPosition(5,0,0);
////        centered.setRotation(0,0,1,90);
//////        centered.setRotation(1,0,0,180);
////        origin.setRotation(Vector3.Axis.Z,quaternion.getRotationZ());
//        return origin;
//    }

    @Override
    protected void initScene() {
        //        MyArcballCamera arcballCamera = null;
        if (!AR) {
            //            arcballCamera = new MyArcballCamera(context, view);
//            arcballCamera.setPosition(0, 0, 10);
//            getCurrentScene().addAndSwitchCamera(arcballCamera);
            try {
                getCurrentScene().setSkybox(backgroundId, backgroundId, backgroundId, backgroundId, backgroundId, backgroundId);
            } catch (Exception e) {
                logIt(e.getMessage(), RendererThread);
            }
        }
        mDirectionalLight = new DirectionalLight(1f, .2f, -1.0f);
        mDirectionalLight.setColor(1.0f, 1.0f, 0.7f);
        mDirectionalLight.setPower(2);
        mDirectionalLight.setScale(5);
        getCurrentScene().addLight(mDirectionalLight);
        logIt("Basic inits done loading models", RendererThread);
        initSceneForMode(mode);
        if (!AR)
            getCurrentCamera().setPosition(0, 0, cameraRadius);
        campos = getCurrentCamera().getPosition();
        vec = campos.add(0, 0, 10);
    }

    @Override
    public void onModelLoadComplete(ALoader loader) {
        if (mode == 0) {
            final LoaderOBJ obj = (LoaderOBJ) loader;
            logIt("Successfully loaded default object", AsyncParserThread);
            final Object3D parsedObject = obj.getParsedObject();
            parsedObject.setPosition(Vector3.ZERO);
            centered = parsedObject;
            centered.setDoubleSided(true);
//            centered.getMaterial().enableLighting(false);
            centered.getMaterial().setDiffuseMethod(new DiffuseMethod.Lambert());
            centered.getMaterial().setSpecularMethod(new SpecularMethod.Phong());
            centered.getMaterial().setColorInfluence(0.5f);
            centered.setColor(0xe33448);
            logIt("Successfully added default object", RendererThread);
//            if (!AR) {
            centered.rotate(1, 0, 0, -90);
            getCurrentScene().addChild(centered);
//            }
//            else ARinits(centered);
        }
    }

    @Override
    public void onModelLoadFailed(ALoader loader) {
        if (mode == 0) logIt("Couldn't default object", RendererThread);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent ev) {
        if (centered == null) return;
        mScaleDetector.onTouchEvent(ev);
        gDetector.onTouchEvent(ev);
        MotionEvent.PointerCoords index;
        MotionEvent.PointerCoords others;
        final int action = ev.getAction();
        index = new MotionEvent.PointerCoords();
        others = new MotionEvent.PointerCoords();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: {
//                final float x = ev.getX();
//                final float y = ev.getY();
//                mLastTouchX = x;
//                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
                ev.getPointerCoords(0, index);
                indexPrev[0] = index.x;
                indexPrev[1] = index.y;
//                logIt("getting Pointer Coords index "+indexPrev[0]+" "+indexPrev[1],RendererThread);
                if (ev.getPointerCount() > 1) {
                    ev.getPointerCoords(1, others);
                    otherPrev[0] = others.x;
                    otherPrev[1] = others.y;
                    midPrev[0] = (indexPrev[0] + otherPrev[0]) / 2;
                    midPrev[1] = (indexPrev[1] + otherPrev[1]) / 2;
//                    logIt("getting Pointer Coords others "+otherPrev[0]" "+otherPrev[1],RendererThread);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                ev.getPointerCoords(0, index);
                logIt("getting Pointer Coords index " + index.x + " " + index.y, RendererThread);
                if (ev.getPointerCount() > 1) {
                    ev.getPointerCoords(1, others);
                    float midx, midy;
                    midx = (index.x + others.x) / 2;
                    midy = (index.y + others.y) / 2;
                    double y = centered.getY();
                    if (!AR)
                        centered.setPosition(new Vector3((midx - midPrev[0]) * 0.01, (midy - midPrev[1]) * -0.01, y));
//                    else
//                        centered.setPosition(new Vector3((midx - midPrev[0]) * -0.01, (midy - midPrev[1]) * 0.01, y));
                    logIt("getting mid Point coords to move object " + midx + " " + midy, RendererThread);
                } else if (ev.getPointerCount() == 1) {
                    double si1 = centered.getRotY();
                    double si2 = centered.getRotX();
                    if (AR)
//                        centered.rotate(eulerstoRMat(centered.getRotZ(), si1 - (index.x - indexPrev[0]), si2 - (index.y - indexPrev[1])));
                        centered.setRotation(centered.getRotZ(), si1 - (index.x - indexPrev[0]), si2 - (index.y - indexPrev[1]));
//                        centered.setRotation(centered.getRotX(),  si2 - (index.x - indexPrev[0]),centered.getRotZ());
                    else {
//                        double ix,iy,iz;
//                        ix= cameraRadius*Math.sin((index.y-indexPrev[1])*0.05)*Math.cos((index.x-indexPrev[0])*0.05);
//                        iy= cameraRadius*Math.sin((index.y-indexPrev[1])*0.05)*Math.sin((index.x-indexPrev[0])*0.05);
//                        iz= cameraRadius*Math.cos((index.y-indexPrev[1])*0.05);
//                        getCurrentCamera().setPosition(ix,iy,iz);
//                        getCurrentCamera().setLookAt(Vector3.ZERO);
//                        centered.setRotation(centered.getRotZ(), si1 - (index.x - indexPrev[0]), si2 - (index.y - indexPrev[1]));
//                        centered.setRotation(eulerstoRMat(Math.toDegrees(centered.getRotX()), si1 - (index.x - indexPrev[0]), si2 - (index.y - indexPrev[1])));

                        centered.setRotY(si1 - (index.x - indexPrev[0]));
                        centered.setRotZ(si2 - (index.y - indexPrev[1]));
                    }
                }
                logIt("touched by " + ev.getPointerCount() + " fingers", RendererThread);
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                break;
            }
        }
    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
        if (AR) {
            mRotationMatrix = new Matrix4(quaternion);
            getCurrentCamera().setRotation(mRotationMatrix);
        }
    }

    public void addChild(Object3D result) {
        centered = result;
        getCurrentScene().addChild(centered);
    }

    private Matrix4 eulerstoRMat(double x, double y, double z) {
        double x1 = x * 0.01, y1 = y * 0.01, z1 = z * 0.01;
        double[] Rx = new double[9], Ry = new double[9], Rz = new double[9], R = new double[16];
        Rx[0] = 1;
        Rx[4] = Math.cos(x1);
        Rx[5] = -Math.sin(x1);
        Rx[7] = Math.sin(x1);
        Rx[8] = Math.cos(x1);
        Ry[0] = Math.cos(y1);
        Ry[2] = Math.sin(y1);
        Ry[4] = 1;
        Ry[6] = -Math.sin(y1);
        Ry[8] = Math.cos(y1);
        Rz[0] = Math.cos(z1);
        Rz[1] = -Math.sin(z1);
        Rz[3] = Math.sin(z1);
        Rz[4] = Math.sin(z1);
        Rz[8] = 1;
        double[] Rxyz = dotProduct(dotProduct(Rx, Ry), Rz);
        double[] Rvals = new double[16];
        Rvals[0] = Rxyz[0];
        Rvals[1] = Rxyz[1];
        Rvals[2] = Rxyz[2];
        Rvals[4] = Rxyz[3];
        Rvals[5] = Rxyz[4];
        Rvals[6] = Rxyz[5];
        Rvals[8] = Rxyz[6];
        Rvals[9] = Rxyz[7];
        Rvals[10] = Rxyz[8];
        Rvals[15] = 1;
        return new Matrix4(Rvals);
    }

    private double[] dotProduct(double[] a, double[] b) {
        double[] c = new double[9];
        for (int i = 0; i < 9; i++) {
            c[i] = a[i] * b[i];
        }
        return c;
    }

    public void setQuaternion(Quaternion quaternion) {
        this.quaternion = quaternion;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            // Don't let the object get too small or too large.
            scaleFactor = Math.max(0.01f, Math.min(scaleFactor, 5.0f));
            touchVals[3] = scaleFactor;
            centered.setScale(scaleFactor);
            return true;
        }
    }

    private class AureikGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Toast t = Toast.makeText(context, "Placing Object at selected point", Toast.LENGTH_LONG);
            t.show();

            Vector3 result = unProject((double)e.getX(), (double)e.getY(), getCurrentCamera().getNearPlane());
            result.x/=2;
            result.y/=2;
            result.z/=2;
            Log.w("long PRESS", "Position = {" + result.toString() + "} ");
            if(centered == null) return true;
            else{
                centered.setPosition(result);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

//        @Override
//        public boolean onScale(ScaleGestureDetector detector) {
//            scaleFactor *= detector.getScaleFactor();
//            // Don't let the object get too small or too large.
//            scaleFactor = Math.max(0.01f, Math.min(scaleFactor, 5.0f));
//            touchVals[3] = scaleFactor;
//            centered.setScale(scaleFactor);
//            return true;
//        }
//
//        @Override
//        public boolean onScaleBegin(ScaleGestureDetector detector) {
//            return true;
//        }
//
//        @Override
//        public void onScaleEnd(ScaleGestureDetector detector) {
//
//        }
    }
}
