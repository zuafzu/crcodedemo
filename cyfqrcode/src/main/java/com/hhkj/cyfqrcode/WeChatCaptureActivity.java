package com.hhkj.cyfqrcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import cn.bingoogolapple.qrcode.core.QRCodeView;

import static cn.bingoogolapple.qrcode.zxing.QRCodeDecoder.syncDecodeQRCode;

public class WeChatCaptureActivity extends AppCompatActivity {

    private static final String TAG = WeChatCaptureActivity.class.getSimpleName();
    private QRCodeView mQRCodeView;
    private SensorManager sm;
    private Sensor ligthSensor;
    private SensorEventListener sensorEventListener;

    private boolean isLight = true;
    private CheckBox cb_light;

    private static OnResultListener onResultListener;

    private static final int REQUEST_CODE = 0; // 请求码
    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    public static void startQR(Activity activity, OnResultListener onResultListener) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            PermissionsChecker mPermissionsChecker = new PermissionsChecker(activity);
            // 缺少权限时, 进入权限配置页面
            if (mPermissionsChecker.lacksPermissions(PERMISSIONS)) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS, REQUEST_CODE);
                return;
            }
        }
        WeChatCaptureActivity.onResultListener = onResultListener;
        Intent intent = new Intent(activity, WeChatCaptureActivity.class);
        activity.startActivity(intent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_we_chat_capture);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        ((TextView) findViewById(R.id.tv_toolsbar_title)).setText("二维码/条码");
        findViewById(R.id.tv_toolsbar_left).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_toolsbar_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        ((TextView) findViewById(R.id.tv_toolsbar_right)).setText("相册");
        findViewById(R.id.tv_toolsbar_right).setBackgroundColor(Color.TRANSPARENT);
        findViewById(R.id.tv_toolsbar_right).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_toolsbar_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 0);
            }
        });
        cb_light = findViewById(R.id.cb_light);
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int height = outMetrics.heightPixels;
        setMargins(cb_light, 0, 0, 0, height / 2 - dip2px(130));
        cb_light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mQRCodeView.openFlashlight();//开闪光灯
                } else {
                    mQRCodeView.closeFlashlight();//关闪光灯
                    if (isLight) {
                        cb_light.setVisibility(View.GONE);
                    }
                }
            }
        });

        mQRCodeView = findViewById(R.id.zbarview);
        mQRCodeView.setDelegate(delegate);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    // 用户权限申请的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {

        }
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 0) {
                try {
                    Uri imageUri = data.getData();
                    final String path = UriTool.getRealFilePath(this, imageUri);
                    new AsyncTask<String, Void, String>() {

                        @Override
                        protected String doInBackground(String... strings) {
                            String string = syncDecodeQRCode(path);
                            return string;
                        }

                        @Override
                        protected void onPostExecute(String s) {
                            super.onPostExecute(s);
                            if (onResultListener != null && s != null && !"".equals(s)) {
                                vibrate();
                                mQRCodeView.startSpot();
                                onResultListener.getData(s);
                                finish();
                            }else {
                                Toast.makeText(WeChatCaptureActivity.this,"未发现二维码",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mQRCodeView.startCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        mQRCodeView.showScanRect();
        mQRCodeView.changeToScanQRCodeStyle();
        mQRCodeView.startSpot();
        initLight();
    }

    private void initLight() {
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        ligthSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float lux = sensorEvent.values[0];//获取光线强度
                int retval = Float.compare(lux, (float) 10.0);
                if (retval > 0) {//光线强度>10.0
                    isLight = false;
                    if (!cb_light.isChecked()) {
                        cb_light.setVisibility(View.GONE);
                    }
                } else {
                    cb_light.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        sm.registerListener(sensorEventListener, ligthSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        mQRCodeView.stopCamera();
        mQRCodeView.stopSpot();
        sm.unregisterListener(sensorEventListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mQRCodeView.onDestroy();
        sm.unregisterListener(sensorEventListener);
        onResultListener = null;
        super.onDestroy();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    public interface OnResultListener {
        void getData(String data);
    }

    QRCodeView.Delegate delegate = new QRCodeView.Delegate() {

        @Override
        public void onScanQRCodeSuccess(String result) {
            // Log.i(TAG, "result:" + result);
            vibrate();
            mQRCodeView.startSpot();
            if (onResultListener != null) {
                onResultListener.getData(result);
                finish();
            }
        }


        @Override
        public void onScanQRCodeOpenCameraError() {
            Log.e(TAG, "打开相机出错");
        }
    };

}