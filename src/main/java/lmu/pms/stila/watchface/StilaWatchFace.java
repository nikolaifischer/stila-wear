package lmu.pms.stila.watchface;

import android.hardware.SensorManager;

public interface StilaWatchFace {

    public void setHRListenerRegistered(boolean flag);
    public boolean getHRListenerRegistered();
    public void registerHRReceiver();
    public SensorManager getSensorManager();
    public void startHRMonitoring();
    public void cancelHRMonitoring();
}
