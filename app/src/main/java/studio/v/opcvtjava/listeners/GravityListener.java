package studio.v.opcvtjava.listeners;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;

public class GravityListener implements SensorEventListener2 {

    private float [] gValues = new float[3];

    public GravityListener(){
        gValues[0] = 0;
        gValues[1] = 0;
        gValues[2] = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        System.arraycopy(event, 0, gValues, 0, gValues.length);
    }

    public float [] getGravityValues(){
      return gValues;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i){

    }

    public void onFlushCompleted(Sensor sensor){

    }

}
