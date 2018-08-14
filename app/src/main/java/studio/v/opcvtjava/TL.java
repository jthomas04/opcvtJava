package studio.v.opcvt;

/**
 * Created by JJ on 03/06/2017.
 */

import android.util.Log;

import java.lang.System;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;

//Timing Logger Class used to Log timing performance
//Comes with timeMT for threaded execution & flush to flush logs on Main Thread(requires Class on MainThread to provide flushing functionality).
//Register with Object & flusher function that's to use it before using timeItMT(preferably).
//Set Logs boolean to false for turning logs off
public class TL {
    public boolean Logs = true;


    private double now, last, diff;
    private Method flusher;
    private Object attachedObject;
    private Stack<String> logs;

    public TL(){
        now = last = diff = 0;
    }

    public TL(Object caller, Method flushMethod){
        now = last = diff = 0;
        attachedObject = caller;
        flusher = flushMethod;
    }

    public void timeIt(String TAG, String message){
        if(Logs){
            last = now;
            now = System.currentTimeMillis();
            diff = now - last;
            Log.w(TAG," Time Taken for '" + message + "' = " + diff + "ms");
        }
    }

    public void timeItMT(String TAG, String message){
        if(Logs){
            last = now;
            now = System.currentTimeMillis();
            diff = now - last;
            logs.push("<" + TAG + ">" + message);
        }
    }

    public void flush(){
        if (!Logs) return;
        if(flusher != null){
            try {
                flusher.invoke(attachedObject, null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        else{
            while(!logs.empty())
                Log.w("TL", logs.pop());
        }
    }

    public void start(){
        now = System.currentTimeMillis();
    }

}
