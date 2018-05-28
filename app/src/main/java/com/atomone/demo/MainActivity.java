package com.atomone.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.atomone.rockerview.RockerView;

public class MainActivity extends AppCompatActivity {


    private TextView tvDirection;
    private TextView tvAngle;

    private RockerView rockerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDirection = findViewById(R.id.tv_direction);
        tvAngle = findViewById(R.id.tv_angle);

        rockerView = findViewById(R.id.rockerView);

        if (rockerView != null) {
            rockerView.setCallBackMode(RockerView.CallBackMode.CALL_BACK_STATE_CHANGE);
            rockerView.setDirectionMode(RockerView.DirectionMode.DIRECTION_8);
            rockerView.setGestureMode(RockerView.GestureMode.GESTURE_UN_CONTINUOUS);
            rockerView.setOnShakeListener(new RockerView.OnShakeListener() {
                @Override
                public void onStart() {
                    tvDirection.setText(null);
                }

                @Override
                public void direction(RockerView.Direction direction, String directionInfo) {
                    tvDirection.setText("摇动方向 : " + directionInfo);
                }

                @Override
                public void onFinish() {
                    tvDirection.setText(null);
                }
            });

            rockerView.setOnAngleChangeListener(new RockerView.OnAngleChangeListener() {
                @Override
                public void onStart() {
                    tvAngle.setText(null);
                }

                @Override
                public void angle(double angle) {
                    tvAngle.setText("摇动角度 : " + angle);
                }

                @Override
                public void onFinish() {
                    tvAngle.setText(null);
                }
            });
        }
    }
}
