package studio.v.opcvtjava;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

public class PermissionHandler {

    List<String> permissionsRequired;
    List<String> messageToDisplay;
    private int reqCode = 1024;
    private Context C;

    //2 helper Combinations ! So you type less.
    public static String [] CamAndExtRead = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
    public static String [] CamAndExtWrite = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    public PermissionHandler(){
        messageToDisplay = new LinkedList<String>();
        permissionsRequired = new LinkedList<String>();
    }

    public PermissionHandler(String [] permissions, Context c, Activity act, String toastMessage){
        messageToDisplay = new LinkedList<String>();
        permissionsRequired = new LinkedList<String>();
        requestPermissions(permissions, c, act, toastMessage);
    }

    public void requestPermissions(String [] permissions, Context c, Activity act, String message){
        for(String perm : permissions){
            if(ContextCompat.checkSelfPermission(c, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsRequired.add(perm);
            }
        }
        if (permissionsRequired.size() == 0)
            return;
        String [] permissionsRequiredArr = permissionsRequired.toArray(new String[0]);
        ActivityCompat.requestPermissions(act, permissionsRequiredArr, reqCode);
        messageToDisplay.add(message);
        permissionsRequired.clear();
        C = c;
        reqCode++; //Increment request code to manage mutliple permisions
    }

    public boolean checkAllPermisions(String [] permissions, Context c){
        int l = permissions.length, i = 0;
        for(String perm : permissions){
            if(ContextCompat.checkSelfPermission(c, perm) == PackageManager.PERMISSION_GRANTED) {
                i++;
            }
        }
        return l == i;
    }

    public int processResults(int requestCode, String permissions[], int[] grantResults) {
        int r = grantResults.length;
        if (r > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           return r;
        } else {
            Toast.makeText(C, messageToDisplay.get(reqCode - 1025), Toast.LENGTH_LONG).show();
            return 0;
        }
    }
}
