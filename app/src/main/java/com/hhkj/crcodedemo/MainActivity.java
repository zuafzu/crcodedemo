package com.hhkj.crcodedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.hhkj.cyfqrcode.WeChatCaptureActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WeChatCaptureActivity.startQR(MainActivity.this, new WeChatCaptureActivity.OnResultListener() {
                    @Override
                    public void getData(String data) {
                        // Log.e("cyf", data);
                        // Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
                        ((TextView)findViewById(R.id.tv)).setText(data);
                    }
                });
            }
        });
    }
}
