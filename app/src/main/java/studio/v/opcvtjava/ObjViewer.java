package studio.v.opcvtjava;


import android.app.Activity;
import android.opengl.GLSurfaceView;

import android.os.Bundle;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.content.Intent;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import min3d.core.RendererActivity;
import min3d.core.Object3dContainer;
import min3d.vos.Light;


public  class ObjViewer extends RendererActivity {
    private final static String TAG = "OBJV::";

    private Uri objPath;
    private GLSurfaceView glv;
    private RelativeLayout RL;

    private Object3dContainer object;
    private int objLoaded = 0;
    private final static int objreq = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void choseModel(View view){
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"), objreq);
    }

    public void accept(View view){
        Intent inten2 = new Intent(this,ArActivity.class);
        inten2.putExtra("filepath",objPath);
        startActivity(inten2);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( (requestCode == objreq) && (resultCode == Activity.RESULT_OK) ) {
            if(objLoaded == 2){ //Remove alread loaded object if any
                scene.removeChild(object);
                object.clear();
                objLoaded = 0;
            }
            objPath = data.getData();
            if(objPath==null)
                return;
            eObjParser op;
            try{
                op = new eObjParser(getApplicationContext().getContentResolver().openInputStream(objPath));
            }
            catch (FileNotFoundException e){
                e.printStackTrace();
                Log.i(TAG," File not found, might! Uri:" + objPath.toString());
                return;
            }
            op.parse();
            object = op.getParsedObject();
            objLoaded++;
            Log.i(TAG,"Object must be loaded & rendering now " + object.toString());
        }
    }

    @Override
    protected void onCreateSetContentView()
    {
        glv = this.glSurfaceView();
        setContentView(R.layout.obj_viewer);
        RL = (RelativeLayout) findViewById(R.id.rLayout2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        RL.addView(glv);
        glv.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
    }

    @Override
    public void updateScene(){
        if(objLoaded == 1) {
            scene.lights().add(new Light());
            scene.backgroundColor().setAll(0x0);
            scene.addChild(object);
            objLoaded++;
        }
        else if(objLoaded == 2)
            object.rotation().y++;
    }

}
