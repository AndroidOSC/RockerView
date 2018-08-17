# RockerView 自定义View 摇杆
#### RockerView
 主要功能
 * 摇杆方向监听（上下、左右、斜角等八个方位的监听）
 * 摇杆角度改变监听
 * 摇杆强度监听，强度取值[0,1] 
 * 设置监听延时周期,可以连续回调当前的方向的摇杆信息

#### 效果图
![mark](http://oymp4z5xr.bkt.clouddn.com/hexo/180731/jdlffd8B1i.jpg?imageslim)
#### 使用说明
1、 在module 添加 
```gradle
  implementation 'com.atomone.rockerview:recokerview:0.0.7'
```
2、在activity 中设置
```java
rockerView.setCallBackMode(RockerView.CallBackMode.CALL_BACK_STATE_CHANGE_CONTINUITY);
            rockerView.setContinuityCallBackDelay(400);
            rockerView.setDirectionMode(RockerView.DirectionMode.DIRECTION_4_ROTATE_0);
            rockerView.setGestureMode(RockerView.GestureMode.GESTURE_CONTINUOU);
            rockerView.setOnShakeListener(new RockerView.OnShakeListener() {
                @Override
                public void onStart() {
                    tvDirection.setText(null);
                }

                @Override
                public void direction(RockerView.Direction direction, String directionInfo) {
                    tvDirection.setText("摇动方向 : " + directionInfo);
                    Log.e("MainActivity", "time:" + System.currentTimeMillis() + "摇动方向 : " + directionInfo);
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

            rockerView.setOnStrengthChangeListener(new RockerView.onStrengthChangeListener() {
                @Override
                public void onStart() {
                    tvStrength.setText(null);
                }

                @Override
                public void strength(float strength) {
                    tvStrength.setText("摇动强度 : " + strength);
                }

                @Override
                public void onFinish() {
                    tvStrength.setText(null);
                }
            });
```
