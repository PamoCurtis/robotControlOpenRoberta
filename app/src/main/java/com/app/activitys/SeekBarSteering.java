package com.app.activitys;

import android.widget.SeekBar;

public class SeekBarSteering implements SeekBar.OnSeekBarChangeListener{

    private static SeekBarSpeed mySeekBarSpeed;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress > 205)     // falls im oberen puffer
            progress = 205;
        if (progress < 5)       // falls im unteren puffer
            progress = 5;

        progress -= 5;          // wegen seitlichen puffern

        mySeekBarSpeed.setSteering(progress-100);    // -100 um bei 0 grade aus zu fahren
        mySeekBarSpeed.refresh();
    }

    static public void setMySeekBarSpeed(SeekBarSpeed sbs){
        mySeekBarSpeed = sbs;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
