package com.idscanner.idscanner;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.hardware.Camera;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.CodaBarReader;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private FrameLayout surfaceCamera;
    private Button scanBtn;
    private ImageView previewImage;

    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PreviewCallback cameraPreviewCallback;

    private Timer cameraTimer;
    private TimerTask cameraTimerTask;

    final static int width = 480;
    final static int height = 320;
    int dstLeft, dstTop, dstWidth, dstHeight;


    private static Hashtable<DecodeHintType, Object> hints;
    static {
        hints = new Hashtable<DecodeHintType, Object>(1);
        hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanBtn = (Button) findViewById(R.id.btn_scan);
        surfaceCamera = (FrameLayout) findViewById(R.id.surface_camera);
        previewImage = (ImageView) findViewById(R.id.img_preview);

        scanBtn.setOnClickListener(btnclick);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, 50);
        }else{
            mCamera = Camera.open();
        }

        cameraTimer = new Timer();


        cameraPreviewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                // access to the specified range of frames of data

                final PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, width, height, dstLeft, dstTop, dstWidth,
                        dstHeight, false);
                // output a preview image of the picture taken by the camera

                final BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
//
//                Bitmap previewBitmap  = renderCroppedGreyscaleBitmap(data,dstLeft,dstTop);
//                previewImage.setImageBitmap(previewBitmap);

                // set this one as the source to decode

                new DecodeImageTask().execute(binaryBitmap);
            }
        };

        mPreview = new CameraPreview(this, mCamera, cameraPreviewCallback);
        surfaceCamera.addView(mPreview);
        cameraTimerTask = timerTask;

        cameraTimer.schedule(cameraTimerTask, 0 ,1000);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraTimer != null) {
            cameraTimer.cancel();
        }
    }

    private View.OnClickListener btnclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            if (dstLeft == 0) {
//                double ratio = surfaceCamera.getWidth() / width;
//                dstLeft = (int) (surfaceCamera.getLeft() / (ratio+1));
//                dstTop = (int) (surfaceCamera.getTop() / (ratio+1));
//                dstWidth = surfaceCamera.getWidth() - dstLeft *2;
//                dstHeight = surfaceCamera.getWidth() - dstLeft*2;
                dstLeft = surfaceCamera.getLeft() * width
                        / getWindowManager().getDefaultDisplay().getWidth();
                dstTop = surfaceCamera.getTop() * height
                        / getWindowManager().getDefaultDisplay().getHeight();
                dstWidth = (surfaceCamera.getRight() - surfaceCamera.getLeft())
                        * width
                        / getWindowManager().getDefaultDisplay().getWidth();
                dstHeight = (surfaceCamera.getBottom() - surfaceCamera.getTop())
                        * height
                        / getWindowManager().getDefaultDisplay().getHeight();
            }
            mPreview.autoFocusAndPreviewCallback();
        }
    };

    private class DecodeImageTask extends AsyncTask<BinaryBitmap, String, String> {

        @Override
        protected String doInBackground(BinaryBitmap... bitmap) {
            String decodedText = null;
            final Reader reader = new MultiFormatReader();

            try {
                final Result result = reader.decode(bitmap[0], hints);
                decodedText = result.getText();
                cameraTimer.cancel();

            } catch (Exception e) {
                decodedText = e.toString();
                Log.e("decode", decodedText);
//                    .show();
                // txtScanResult.setText(e.toString());
            }
            return decodedText;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
//            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT)
//                    .show();
//            txtScanResult.setText(result);

        }

    }

    public Bitmap renderCroppedGreyscaleBitmap(byte[] yuvData, int left, int top) {
        int width = 320;
        int height = 480;
        int[] pixels = new int[width * height];
        byte[] yuv = yuvData;
        int inputOffset = top * this.width + left;

        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                int grey = yuv[inputOffset + x] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += this.width;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
