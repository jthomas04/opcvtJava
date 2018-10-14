package studio.v.opcvtjava;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;

public class BaseRenderer extends Renderer {
        private DirectionalLight mDirectionalLight;
        private Sphere a;
        private Matrix4 mat4;

        public volatile float [] mRotationMatrix = new float[16];
        public volatile float [] mTranslationMatrix = new float[3];

        public BaseRenderer(Context context) {
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
                Log.e( "Renderer.initScene", error.toString());

            }
            a = new Sphere(1,30,30);
            a.setMaterial(material);
            a.setPosition(0,0,-20);
            a.setRotation(0,0,-90);
            getCurrentCamera().setPosition(0,0,0);
            getCurrentScene().addChild(a);

            //getCurrentScene().setBackgroundColor(1,1,1,0);


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
            //updatePositonOrientation();

            mat4 = new Matrix4(mRotationMatrix);

            //Quaternion qr = new Quaternion().fromEuler((double)eulers[2] * 180/Math.PI, (double)eulers[1] * 180/Math.PI, (double)eulers[0] * 180/ Math.PI);
            //mat4 = new Matrix4(qr);
            //a.setRotation(qr);
            //mat4.rotate(new Vector3(0,0,1), 180);
            //mat4.rotate(new Vector3(1,0,0),-45);
            getCurrentCamera().setRotation(mat4); //.inverse());

            //a.setRotation(mat4);
            //2, 1, 0 because X axis moves right positively.
            /*if(linearDirty){
                a.moveForward(mLAccelerationReading[2]);
                a.moveUp(mLAccelerationReading[1]);
                a.moveRight(mLAccelerationReading[0]);
                linearDirty = false;
            }*/

//            getCurrentCamera().setCameraYaw(eulers[0]);
//            getCurrentCamera().setCameraPitch(eulers[2]);
//            getCurrentCamera().setCameraRoll(eulers[1]);
        }
}
