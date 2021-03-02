package com.app.activitys;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import com.app.R;
import com.app.activitys.Bluetooth.BluetoothDeviceListActivity;

import static org.opencv.core.Core.bitwise_and;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.rectangle;

public class CamActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static com.app.activitys.SeekBarSpeed mySeekBarSpeed;

    JavaCameraView javaCameraView;

    private Mat mRGBA, hsvImg, mask;
    private Scalar scalarLow = getHsvScalar(205, 30, 5);
    private Scalar scalarHigh = getHsvScalar(290, 400, 400);
    private Scalar rectangleColor;
    private Point lo = new Point(0,0);
    private Point ru = new Point(60,60);

    private boolean togglePic = true;
    private int picWidth, picHeight;
    private int redCounter = 0;
    private int lastSeenAvgHeight = 320;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(CamActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == BaseLoaderCallback.SUCCESS) {
                javaCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    //-----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //init der OpenCV Kamera
        javaCameraView = findViewById(R.id.CameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setMaxFrameSize(1280, 720);
        javaCameraView.setCvCameraViewListener(this);

        //Lässt Roboter zu beginn drehen um Objekt zu finden
        mySeekBarSpeed.letItSpin(100);
    }

    @Override
    protected void onDestroy() {
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }

        mySeekBarSpeed.setSpeed(0);
        mySeekBarSpeed.setSteering(0);
        mySeekBarSpeed.refresh();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(width, height, CvType.CV_8UC4);
        hsvImg = new Mat(width, height, CvType.CV_8UC4);

        mask = new Mat(width, height, CvType.CV_8UC4);
    }

    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        hsvImg.release();
        mask.release();

        mySeekBarSpeed.setSpeed(0);
        mySeekBarSpeed.setSteering(0);
        mySeekBarSpeed.refresh();
    }

    //-----------------------------------------------------------------
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if (togglePic) {

            mRGBA = inputFrame.rgba();
            cvtColor(mRGBA, hsvImg, COLOR_RGB2HSV);

            Core.inRange(hsvImg, scalarLow, scalarHigh, mask);


            //"schwerpunkt" der erkannten roten flächen berechnen
            picHeight       = mask.rows();
            picWidth        = mask.width(); //mask.cols();
            int avgHeight   = 0;
            int avgWidth    = 0;  // average height and width
            int count       = 1;  // init mit 1 um divideByZeroException zu vermeiden

            for (int i = 0; i< picHeight; i += 4) {
                for (int ii = 0; ii< picWidth; ii += 4) {
                    if (0 < mask.get(i,ii)[0]) {
                        avgHeight += i;
                        avgWidth += ii;
                        ++count;
                    }
                }
            }

            avgHeight /= count;
            avgWidth /= count;

            // Ecken um den Schwerpunkt setzen
            lo.set( new double[]{ avgWidth - 30, avgHeight - 30 });
            ru.set( new double[]{ avgWidth + 30, avgHeight + 30 });

            if (50 < count) {    // neue fahrtrichtung nur setzen, wenn nennenswerte menge an punkten gefunden wurde
                drive(avgWidth, avgHeight);
                rectangleColor = new Scalar(20, 200, 50);    // für overlay-quadrat: funktioniert -> gruen
                redCounter = 0;
                lastSeenAvgHeight = avgHeight;

            } else {
                rectangleColor = new Scalar(250, 30, 40);    // funktioniert nicht -> rot
                if (35 < ++redCounter) {
//                    drive(avgWidth/2, avgHeight/2);
                    // wenn das objekt auch nach 100 frames blinden Folgens nicht gefunden wurde,
                    // dreht ORB sich auf der Stelle in richtung der letzten sichtung
                    if (lastSeenAvgHeight > picHeight/2) {
                        mySeekBarSpeed.letItSpin(100);
                    } else {
                        mySeekBarSpeed.letItSpin(-100);
                    }
                }
            }

//            bitwise_not(mask, mask);                         // zum debuggen, um *nicht* erkannte bereiche zu sehen
            bitwise_and(mRGBA, mRGBA, hsvImg, mask);           // farbbild auf filter legen
//            cvtColor(hsvImg, hsvImg, COLOR_RGB2BGR);         // zum debuggen, da die erkennung auf falschfarben (rgb <-> bgr) läuft

            rectangle(hsvImg, lo, ru, rectangleColor, 3);       // quadrat über das erzeugte bild legen
        }
        togglePic = !togglePic;
        return hsvImg;
    }

    private Scalar getHsvScalar(double H, double S, double V) {
        // konvertiert normale HSV-Scalare (360,100,100) in OpenCV-HSV-Scalare (255,255,255)
        return new Scalar(((int)(H/360.0*255.0)), ((int)(S*2.55)), ((int)(V*2.55)));
    }

    private void drive(int avgWidht, int avgHeight){
        int steer = (int)( avgHeight * 200.0  / picHeight      -100 );     // -100 um bei 0 grade aus zu fahren -> zwischen -100 und 100
        int speed = (int)( avgWidht  * 1000.0 / picWidth *(-1) +850 );     // -> zwischen -150 und 750
        // muss auf den kopf gestellt werden, da beim bild (0,0) links oben ist aber links unten benoetigt wird

        if (-50 > speed) {
            speed *= 10;
        } else if (0 > speed) {
            speed = 0;
//        } else if (400 < speed) {
//            speed *= 3;
        } else {
            speed *= 3;
        }

        System.out.println("speed: " + speed + "  steer: " + (steer) + "  height: " + picHeight + "  width: " + picWidth);

        mySeekBarSpeed.setSpeed(speed);
        mySeekBarSpeed.setSteering(-steer);
        mySeekBarSpeed.refresh();
    }

    //-----------------------------------------------------------------
    static public void setMySeekBarSpeed(com.app.activitys.SeekBarSpeed sbs){
        mySeekBarSpeed = sbs;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }
}