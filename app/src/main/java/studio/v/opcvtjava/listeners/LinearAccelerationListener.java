package studio.v.opcvtjava.listeners;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;

public class LinearAccelerationListener implements SensorEventListener2 {
    private float [] laValues = new float[3];
    public boolean ready = false;

    public LinearAccelerationListener(){
        laValues[0] = 0;
        laValues[1] = 0;
        laValues[2] = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        System.arraycopy(event.values, 0, laValues, 0, laValues.length);
        ready = true;
    }

    public void getValues(float [] var){
        System.arraycopy(laValues,0, var, 0, laValues.length);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i){

    }

    public void onFlushCompleted(Sensor sensor){

    }
}
