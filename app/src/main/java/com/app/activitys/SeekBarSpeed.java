package com.app.activitys;

import android.widget.SeekBar;
import android.widget.TextView;

import com.app.activitys.ORB.ORB;

//Diese Klasse wandelt den einkommenden SeekBar Wert in einen entsprechenden Wert für den ORB um
public class SeekBarSpeed implements SeekBar.OnSeekBarChangeListener{

    private final ORB orb;
    static private int steer;
    static private int speed;
    static TextView info;

    SeekBarSpeed(ORB orb){
        this.orb = orb;
        SeekBarSteering.setMySeekBarSpeed(this);
        CamActivity.setMySeekBarSpeed(this);
    }

    //wird bei Veränderung der Seekbar aufgerufen
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        speed = progress - 1000;
        speed *= 4;
        refresh();
    }

    //berechnet und setzt die Geschwindkeit des ORBs
    public void refresh() {
        double steering = steer / 100.0;
        int leftSpeed, rightSpeed;

        if (0 == steer) {
            leftSpeed =  (int) (speed * 0.9);
            rightSpeed = (int) (speed * 0.9);

        } else if (steer < 0) {
            leftSpeed =  (int) (speed * (0.9 + (steering *9 / 10)) );
            rightSpeed = (int) (speed * (0.9 - (steering    / 10)) );

        } else {
            leftSpeed =  (int) (speed * (0.9 + (steering    / 10)) );
            rightSpeed = (int) (speed * (0.9 - (steering *9 / 10)) );
        }

        orb.setMotor( 0, ORB.Mode.SPEED, - leftSpeed,  0);
        orb.setMotor( 1, ORB.Mode.SPEED,   rightSpeed, 0);
    }

    static public void setSteering(int steerNew){
        steer = steerNew;
    }

    static public void setSpeed(int speedNew){
        speed = speedNew;
    }

    public void letItSpin(int speed){
        orb.setMotor( 0, ORB.Mode.SPEED, speed, 0);
        orb.setMotor( 1, ORB.Mode.SPEED, speed, 0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
