package studio.v.opcvt;

import android.net.Uri;

import java.io.File;

import common.ResourceUtils;
import common.UIUtils;
import min3d.core.Object3dContainer;
import min3d.core.RendererActivity;
import min3d.parser.IParser;
import min3d.parser.Parser;
import min3d.vos.Light;

/**
 * Created by nelsonin-04 on 14/03/2017.
 */

public class ObjLoader extends RendererActivity {
    private Object3dContainer objModel;
    @Override
    public void initScene() {

        scene.lights().add(new Light());

        IParser parser = null;

        try{
            parser = Parser.createParser(Parser.Type.OBJ,
                    getResources(), ResourceUtils.getGlobalResourcePackageIdentifier(this.getBaseContext())+":raw/camaro_obj", true);
        }catch(Exception ex){
            UIUtils.showSimpleErrorDialog(this, "Fatal Error", ex);
        }

        parser.parse();

        objModel = parser.getParsedObject();
        objModel.scale().x = objModel.scale().y = objModel.scale().z = .7f;
        scene.addChild(objModel);

    }

    @Override
    public void updateScene() {

    }
}



