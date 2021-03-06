package com.atomone.rockerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author atomOne
 */
public class RockerView extends View {

    //默认大小
    private static final int DEFAULT_SIZE          = 400;
    private static final int DEFAULT_ROCKER_RADIUS = DEFAULT_SIZE / 8;
    private static final int DEFAULT_DELAY         = 100;

    //画布实际大小
    private int measuredWidth  = DEFAULT_SIZE;
    private int measuredHeight = DEFAULT_SIZE;

    //画笔
    private Paint mAreaBackgroundPaint;
    private Paint mRockerPaint;

    //控件中心点
    private Point mCenterPoint;
    //摇杆中心点
    private Point mRockerPosition;
    //触摸中心点
    private Point mTouchPoint;

    //连续触发事件(适用于方位状态改变的回调模式)
    private ScheduledExecutorService mScheduledExecutorService;
    private long mDelay = DEFAULT_DELAY;

    //半径
    private int mAreaRadius;
    private int mRockerRadius;

    //监听
    private OnAngleChangeListener    mOnAngleChangeListener;
    private OnShakeListener          mOnShakeListener;
    private onStrengthChangeListener mOnStrengthChangeListener;

    //回调模式
    private CallBackMode   mCallBackMode   = CallBackMode.CALL_BACK_MOVE;
    private DirectionMode  mDirectionMode  = DirectionMode.DIRECTION_8;
    private GestureMode    mGestureMode    = GestureMode.GESTURE_CENTER_CONTINUOU;
    private Direction      tempDirection   = Direction.DIRECTION_CENTER;
    private RockerBackMode mRockerBackMode = RockerBackMode.BACK_CENTER_ORIGIN;

    // 角度
    private static final double ANGLE_0                   = 0;
    private static final double ANGLE_360                 = 360;
    // 360°水平方向平分2份的边缘角度
    private static final double ANGLE_HORIZONTAL_2D_OF_0P = 90;
    private static final double ANGLE_HORIZONTAL_2D_OF_1P = 270;
    // 360°垂直方向平分2份的边缘角度
    private static final double ANGLE_VERTICAL_2D_OF_0P   = 0;
    private static final double ANGLE_VERTICAL_2D_OF_1P   = 180;
    // 360°平分4份的边缘角度
    private static final double ANGLE_4D_OF_0P            = 0;
    private static final double ANGLE_4D_OF_1P            = 90;
    private static final double ANGLE_4D_OF_2P            = 180;
    private static final double ANGLE_4D_OF_3P            = 270;
    // 360°平分4份的边缘角度(旋转45度)
    private static final double ANGLE_ROTATE45_4D_OF_0P   = 45;
    private static final double ANGLE_ROTATE45_4D_OF_1P   = 135;
    private static final double ANGLE_ROTATE45_4D_OF_2P   = 225;
    private static final double ANGLE_ROTATE45_4D_OF_3P   = 315;

    // 360°平分8份的边缘角度
    private static final double ANGLE_8D_OF_0P = 22.5;
    private static final double ANGLE_8D_OF_1P = 67.5;
    private static final double ANGLE_8D_OF_2P = 112.5;
    private static final double ANGLE_8D_OF_3P = 157.5;
    private static final double ANGLE_8D_OF_4P = 202.5;
    private static final double ANGLE_8D_OF_5P = 247.5;
    private static final double ANGLE_8D_OF_6P = 292.5;
    private static final double ANGLE_8D_OF_7P = 337.5;

    // 摇杆可移动区域背景
    private static final int AREA_BACKGROUND_MODE_PIC     = 0;
    private static final int AREA_BACKGROUND_MODE_COLOR   = 1;
    private static final int AREA_BACKGROUND_MODE_XML     = 2;
    private static final int AREA_BACKGROUND_MODE_DEFAULT = 3;
    private              int mAreaBackgroundMode          = AREA_BACKGROUND_MODE_DEFAULT;
    private Bitmap mAreaBitmap;
    private int    mAreaColor;
    // 摇杆背景
    private static final int ROCKER_BACKGROUND_MODE_PIC     = 4;
    private static final int ROCKER_BACKGROUND_MODE_COLOR   = 5;
    private static final int ROCKER_BACKGROUND_MODE_XML     = 6;
    private static final int ROCKER_BACKGROUND_MODE_DEFAULT = 7;
    private              int mRockerBackgroundMode          = ROCKER_BACKGROUND_MODE_DEFAULT;
    private Bitmap mRockerBitmap;
    private int    mRockerColor;

    //是否开始监听
    private boolean mIsStartCallBack = false;

    public RockerView(Context context) {
        this(context, null);
    }

    public RockerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RockerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr);

        if (isEnabled()) {
            RockerLog.i("RockerView: isInEditMode");
        }
    }

    /**
     * 初始化
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        if (null != attrs) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RockerView, defStyleAttr, 0);

            // 摇杆半径
            mRockerRadius = typedArray.getDimensionPixelOffset(R.styleable.RockerView_rockerRadius, DEFAULT_ROCKER_RADIUS);
            //可移动区域的背景
            Drawable areaBackground = typedArray.getDrawable(R.styleable.RockerView_areaBackground);
            if (null != areaBackground) {
                if (areaBackground instanceof BitmapDrawable) {
                    //图片
                    mAreaBitmap = ((BitmapDrawable) areaBackground).getBitmap();
                    mAreaBackgroundMode = AREA_BACKGROUND_MODE_PIC;
                } else if (areaBackground instanceof ShapeDrawable) {
                    // XML-shape
                    mAreaBitmap = drawable2Bitmap(areaBackground, mRockerRadius);
                    mRockerBackgroundMode = AREA_BACKGROUND_MODE_XML;
                } else if (areaBackground instanceof LayerDrawable) {
                    // XML-layer
                    mAreaBitmap = drawable2Bitmap(areaBackground, mRockerRadius);
                    mRockerBackgroundMode = AREA_BACKGROUND_MODE_XML;
                } else if (areaBackground instanceof GradientDrawable) {
                    // XML-gradient
                    mAreaBitmap = drawable2Bitmap(areaBackground, mRockerRadius);
                    mAreaBackgroundMode = AREA_BACKGROUND_MODE_XML;
                } else if (areaBackground instanceof ColorDrawable) {
                    //色值
                    mAreaColor = ((ColorDrawable) areaBackground).getColor();
                    mAreaBackgroundMode = AREA_BACKGROUND_MODE_COLOR;
                } else {
                    //other
                    mAreaBackgroundMode = AREA_BACKGROUND_MODE_DEFAULT;
                }
            } else {
                //默认背景
                mAreaBackgroundMode = AREA_BACKGROUND_MODE_DEFAULT;
            }
            // 摇杆背景
            Drawable rockerBackground = typedArray.getDrawable(R.styleable.RockerView_rockerBackground);
            if (null != rockerBackground) {
                // 设置了摇杆背景
                if (rockerBackground instanceof BitmapDrawable) {
                    // 图片
                    mRockerBitmap = ((BitmapDrawable) rockerBackground).getBitmap();
                    mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_XML;
                } else if (areaBackground instanceof ShapeDrawable) {
                    // XML-shape
                    mRockerBitmap = drawable2Bitmap(rockerBackground, mRockerRadius);
                    mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_XML;
                } else if (rockerBackground instanceof LayerDrawable) {
                    // XML-layer
                    mRockerBitmap = drawable2Bitmap(rockerBackground, mRockerRadius);
                    mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_XML;
                } else if (rockerBackground instanceof GradientDrawable) {
                    // XML-gradient
                    mRockerBitmap = drawable2Bitmap(rockerBackground, mRockerRadius);
                    mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_XML;
                } else if (rockerBackground instanceof ColorDrawable) {
                    // 色值
                    mRockerColor = ((ColorDrawable) rockerBackground).getColor();
                    mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_COLOR;
                } else {
                    // 其他形式
                    mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_DEFAULT;
                }
            } else {
                // 没有设置摇杆背景
                mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_DEFAULT;
            }
            typedArray.recycle();
        } else {
            mAreaBackgroundMode = AREA_BACKGROUND_MODE_DEFAULT;
            mRockerBackgroundMode = ROCKER_BACKGROUND_MODE_DEFAULT;
            mRockerRadius = DEFAULT_ROCKER_RADIUS;
        }

        // 移动区域画笔
        mAreaBackgroundPaint = new Paint();
        // 抗锯齿
        mAreaBackgroundPaint.setAntiAlias(true);
        mAreaBackgroundPaint.setFilterBitmap(true);
        // 摇杆画笔
        mRockerPaint = new Paint();
        // 抗锯齿
        mRockerPaint.setAntiAlias(true);
        mAreaBackgroundPaint.setFilterBitmap(true);
        // 中心点
        mCenterPoint = new Point(0, 0);
        // 摇杆位置
        mRockerPosition = new Point(0, 0);
        // 触摸点
        mTouchPoint = new Point(0, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = View.getDefaultSize(DEFAULT_SIZE, widthMeasureSpec);
        int measureHeight = View.getDefaultSize(DEFAULT_SIZE, heightMeasureSpec);
        setMeasuredDimension(measureWidth, measureHeight);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        measuredWidth = w;
        measuredHeight = h;
        int mCenterX = measuredWidth / 2;
        int mCenterY = measuredHeight / 2;
        // 中心点
        mCenterPoint.set(mCenterX, mCenterY);
        mRockerPosition.set(mCenterX, mCenterY);
        // 可移动区域的半径
        mAreaRadius = (measuredWidth <= measuredHeight) ? mCenterX : mCenterY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画摇杆区域
        if (AREA_BACKGROUND_MODE_PIC == mAreaBackgroundMode || AREA_BACKGROUND_MODE_XML == mAreaBackgroundMode) {
            Rect src = new Rect(0, 0, mAreaBitmap.getWidth(), mAreaBitmap.getHeight());
            Rect dst = new Rect(mCenterPoint.x - mAreaRadius, mCenterPoint.y - mAreaRadius, mCenterPoint.x + mAreaRadius, mCenterPoint.y + mAreaRadius);
            canvas.drawBitmap(mAreaBitmap, src, dst, mAreaBackgroundPaint);
        } else if (AREA_BACKGROUND_MODE_COLOR == mAreaBackgroundMode) {
            // 色值
            mAreaBackgroundPaint.setColor(mAreaColor);
            canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mAreaRadius, mAreaBackgroundPaint);
        } else {
            // 其他或者未设置
            mAreaBackgroundPaint.setColor(Color.GRAY);
            canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mAreaRadius, mAreaBackgroundPaint);
        }

        // 画摇杆
        if (ROCKER_BACKGROUND_MODE_PIC == mRockerBackgroundMode || ROCKER_BACKGROUND_MODE_XML == mRockerBackgroundMode) {
            // 图片
            Rect src = new Rect(0, 0, mRockerBitmap.getWidth(), mRockerBitmap.getHeight());
            Rect dst = new Rect(mRockerPosition.x - mRockerRadius, mRockerPosition.y - mRockerRadius, mRockerPosition.x + mRockerRadius, mRockerPosition.y + mRockerRadius);
            canvas.drawBitmap(mRockerBitmap, src, dst, mRockerPaint);
        } else if (ROCKER_BACKGROUND_MODE_COLOR == mRockerBackgroundMode) {
            // 色值
            mRockerPaint.setColor(mRockerColor);
            canvas.drawCircle(mRockerPosition.x, mRockerPosition.y, mRockerRadius, mRockerPaint);
        } else {
            // 其他或者未设置
            mRockerPaint.setColor(Color.RED);
            canvas.drawCircle(mRockerPosition.x, mRockerPosition.y, mRockerRadius, mRockerPaint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:// 按下
                // 回调 开始
                if (mGestureMode == GestureMode.GESTURE_CENTER_CONTINUOU) {
                    float downX = event.getX();
                    float downY = event.getY();
                    mTouchPoint.set((int) downX, (int) downY);
                    if (isInRockerCenterZone(mCenterPoint, mTouchPoint)) {
                        callBackStart();
                    }
                } else {
                    callBackStart();
                }
            case MotionEvent.ACTION_MOVE:// 移动
                float moveX = event.getX();
                float moveY = event.getY();
                mTouchPoint.set((int) moveX, (int) moveY);
                if (mGestureMode == GestureMode.GESTURE_CENTER_CONTINUOU) {
                    if (mIsStartCallBack) {
                        callBackMove(mCenterPoint, mTouchPoint);
                    } else {
                        if (isInRockerCenterZone(mRockerPosition, mTouchPoint)) {
                            callBackStart();
                            callBackMove(mCenterPoint, mTouchPoint);
                        }
                    }
                } else {
                    callBackMove(mCenterPoint, mTouchPoint);
                }
                break;
            case MotionEvent.ACTION_UP:// 抬起
            case MotionEvent.ACTION_CANCEL:// 取消
                // 回调 结束
                if (mIsStartCallBack) {
                    float upX = event.getX();
                    float upY = event.getY();
                    mTouchPoint.set((int) upX, (int) upY);
                    rockerBack(mCenterPoint, mTouchPoint);
                    callBackFinish();
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 摇杆回退
     *
     * @param centerPoint
     * @param touchPoint
     */
    private void rockerBack(Point centerPoint, Point touchPoint) {
        if (mRockerBackMode == RockerBackMode.BACK_NONE) {
            Point point = ComputingRockerPositionPoint(centerPoint, touchPoint, mAreaRadius, mRockerRadius);
            moveRocker(point.x, point.y);
        } else if (mRockerBackMode == RockerBackMode.BACK_CENTER_ORIGIN) {
            moveRocker(centerPoint.x, centerPoint.y);
        } else if (mRockerBackMode == RockerBackMode.BACK_X_AXIS) {
            Point point = ComputingRockerPositionPoint(centerPoint, touchPoint, mAreaRadius, mRockerRadius);
            moveRocker(point.x, centerPoint.y);
        } else if (mRockerBackMode == RockerBackMode.BACK_Y_AXIS) {
            Point point = ComputingRockerPositionPoint(centerPoint, touchPoint, mAreaRadius, mRockerRadius);
            moveRocker(centerPoint.x, point.y);
        }
    }


    /**
     * 监听移动
     *
     * @param centerPoint 中心点
     * @param touchPoint  触摸点
     */
    private void callBackMove(Point centerPoint, Point touchPoint) {
        Point point = ComputingRockerPositionPoint(centerPoint, touchPoint, mAreaRadius, mRockerRadius);
        moveRocker(point.x, point.y);
    }

    /**
     * 获取摇杆实际要显示的位置（点）
     *
     * @param centerPoint  中心点
     * @param touchPoint   触摸点
     * @param regionRadius 摇杆可活动区域半径
     * @param rockerRadius 摇杆半径
     * @return 摇杆实际显示的位置（点）
     */
    private Point ComputingRockerPositionPoint(Point centerPoint, Point touchPoint, float regionRadius, float rockerRadius) {
        // 两点在X轴的距离
        float lenX = (float) (touchPoint.x - centerPoint.x);
        // 两点在Y轴距离
        float lenY = (float) (touchPoint.y - centerPoint.y);
        // 两点距离
        float lenXY = (float) Math.sqrt((double) (lenX * lenX + lenY * lenY));
        RockerLog.i("ComputingRockerPositionPoint: lenXY :" + lenXY);

        // 摇杆有效活动距离
        float strangthLen = regionRadius - (rockerRadius * 2);
        RockerLog.i("ComputingRockerPositionPoint: strangthLen :" + strangthLen);

        // 计算弧度
        double radian = Math.acos(lenX / lenXY) * (touchPoint.y < centerPoint.y ? -1 : 1);
        RockerLog.i("ComputingRockerPositionPoint: lenXY :" + radian);

        //防止误触
        if (lenXY >= rockerRadius) {
            // 计算角度
            double angle = radian2Angle(radian);
            RockerLog.i("ComputingRockerPositionPoint: 角度 :" + angle);
            // 回调 返回参数
            callBackShake(angle);
            float calcStrength = (lenXY - rockerRadius) / strangthLen;
            if (calcStrength > 1) {
                callBackStrength(1F);
            } else {
                callBackStrength(calcStrength);
            }
        } else {
            callBackStrength(0F);
        }


        // 触摸位置在可活动范围内
        if (lenXY + rockerRadius <= regionRadius) {
            return touchPoint;
        } else { // 触摸位置在可活动范围以外
            // 计算要显示的位置
            int showPointX = (int) (centerPoint.x + (regionRadius - rockerRadius) * Math.cos(radian));
            int showPointY = (int) (centerPoint.y + (regionRadius - rockerRadius) * Math.sin(radian));
            return new Point(showPointX, showPointY);
        }
    }

    /**
     * 移动摇杆到指定位置
     *
     * @param x x坐标
     * @param y y坐标
     */
    private void moveRocker(float x, float y) {
        mRockerPosition.set((int) x, (int) y);
        RockerLog.i("onTouchEvent: 移动位置 : x = " + mRockerPosition.x + " y = " + mRockerPosition.y);
        invalidate();
    }

    /**
     * 弧度转角度
     *
     * @param radian 弧度
     * @return 角度[0, 360)
     */
    private double radian2Angle(double radian) {
        double tmp = Math.round(radian / Math.PI * 180);
        return tmp >= 0 ? tmp : 360 + tmp;
    }

    /**
     * 开始回调
     */
    private void callBackStart() {
        mIsStartCallBack = true;
        tempDirection = Direction.DIRECTION_CENTER;
        if (null != mOnAngleChangeListener) {
            mOnAngleChangeListener.onStart();
        }
        if (null != mOnShakeListener) {
            mOnShakeListener.onStart();
        }
        if (null != mOnStrengthChangeListener) {
            mOnStrengthChangeListener.onStart();
        }
    }


    /**
     * 强度回调
     *
     * @param strength 摇动强度 [0,1]
     */
    private void callBackStrength(float strength) {
        if (null != mOnStrengthChangeListener) {
            mOnStrengthChangeListener.strength(strength);
        }
    }

    /**
     * 方位回调
     *
     * @param angle 摇动角度
     */
    private void callBackShake(double angle) {
        if (null != mOnAngleChangeListener) {
            mOnAngleChangeListener.angle(angle);
        }
        if (null != mOnShakeListener) {
            if (CallBackMode.CALL_BACK_MOVE == mCallBackMode) {
                switch (mDirectionMode) {
                    case DIRECTION_2_HORIZONTAL:// 左右方向
                        if (ANGLE_0 <= angle && ANGLE_HORIZONTAL_2D_OF_0P > angle || ANGLE_HORIZONTAL_2D_OF_1P <= angle && ANGLE_360 > angle) {
                            // 右
                            mOnShakeListener.direction(Direction.DIRECTION_RIGHT, getDirectionInfo(Direction.DIRECTION_RIGHT));
                        } else if (ANGLE_HORIZONTAL_2D_OF_0P <= angle && ANGLE_HORIZONTAL_2D_OF_1P > angle) {
                            // 左
                            mOnShakeListener.direction(Direction.DIRECTION_LEFT, getDirectionInfo(Direction.DIRECTION_LEFT));
                        }
                        break;
                    case DIRECTION_2_VERTICAL:// 上下方向
                        if (ANGLE_VERTICAL_2D_OF_0P <= angle && ANGLE_VERTICAL_2D_OF_1P > angle) {
                            // 下
                            mOnShakeListener.direction(Direction.DIRECTION_DOWN, getDirectionInfo(Direction.DIRECTION_DOWN));
                        } else if (ANGLE_VERTICAL_2D_OF_1P <= angle && ANGLE_360 > angle) {
                            // 上
                            mOnShakeListener.direction(Direction.DIRECTION_UP, getDirectionInfo(Direction.DIRECTION_UP));
                        }
                        break;
                    case DIRECTION_4_ROTATE_0:// 四个方向
                        if (ANGLE_0 <= angle && ANGLE_ROTATE45_4D_OF_0P > angle || ANGLE_ROTATE45_4D_OF_3P <= angle && ANGLE_360 > angle) {
                            // 右
                            mOnShakeListener.direction(Direction.DIRECTION_RIGHT, getDirectionInfo(Direction.DIRECTION_RIGHT));
                        } else if (ANGLE_ROTATE45_4D_OF_0P <= angle && ANGLE_ROTATE45_4D_OF_1P > angle) {
                            // 下
                            mOnShakeListener.direction(Direction.DIRECTION_DOWN, getDirectionInfo(Direction.DIRECTION_DOWN));
                        } else if (ANGLE_ROTATE45_4D_OF_1P <= angle && ANGLE_ROTATE45_4D_OF_2P > angle) {
                            // 左
                            mOnShakeListener.direction(Direction.DIRECTION_LEFT, getDirectionInfo(Direction.DIRECTION_LEFT));
                        } else if (ANGLE_ROTATE45_4D_OF_2P <= angle && ANGLE_ROTATE45_4D_OF_3P > angle) {
                            // 上
                            mOnShakeListener.direction(Direction.DIRECTION_UP, getDirectionInfo(Direction.DIRECTION_UP));
                        }
                        break;
                    case DIRECTION_4_ROTATE_45:// 四个方向 旋转45度

                        if (ANGLE_4D_OF_0P <= angle && ANGLE_4D_OF_1P > angle) {
                            // 右下
                            mOnShakeListener.direction(Direction.DIRECTION_DOWN_RIGHT, getDirectionInfo(Direction.DIRECTION_DOWN_RIGHT));
                        } else if (ANGLE_4D_OF_1P <= angle && ANGLE_4D_OF_2P > angle) {
                            // 左下
                            mOnShakeListener.direction(Direction.DIRECTION_DOWN_LEFT, getDirectionInfo(Direction.DIRECTION_DOWN_LEFT));
                        } else if (ANGLE_4D_OF_2P <= angle && ANGLE_4D_OF_3P > angle) {
                            // 左上
                            mOnShakeListener.direction(Direction.DIRECTION_UP_LEFT, getDirectionInfo(Direction.DIRECTION_UP_LEFT));
                        } else if (ANGLE_4D_OF_3P <= angle && ANGLE_360 > angle) {
                            // 右上
                            mOnShakeListener.direction(Direction.DIRECTION_UP_RIGHT, getDirectionInfo(Direction.DIRECTION_UP_RIGHT));
                        }
                        break;
                    case DIRECTION_8:// 八个方向
                        if (ANGLE_0 <= angle && ANGLE_8D_OF_0P > angle || ANGLE_8D_OF_7P <= angle && ANGLE_360 > angle) {
                            // 右
                            mOnShakeListener.direction(Direction.DIRECTION_RIGHT, getDirectionInfo(Direction.DIRECTION_RIGHT));
                        } else if (ANGLE_8D_OF_0P <= angle && ANGLE_8D_OF_1P > angle) {
                            // 右下
                            mOnShakeListener.direction(Direction.DIRECTION_DOWN_RIGHT, getDirectionInfo(Direction.DIRECTION_DOWN_RIGHT));
                        } else if (ANGLE_8D_OF_1P <= angle && ANGLE_8D_OF_2P > angle) {
                            // 下
                            mOnShakeListener.direction(Direction.DIRECTION_DOWN, getDirectionInfo(Direction.DIRECTION_DOWN));
                        } else if (ANGLE_8D_OF_2P <= angle && ANGLE_8D_OF_3P > angle) {
                            // 左下
                            mOnShakeListener.direction(Direction.DIRECTION_DOWN_LEFT, getDirectionInfo(Direction.DIRECTION_DOWN_LEFT));
                        } else if (ANGLE_8D_OF_3P <= angle && ANGLE_8D_OF_4P > angle) {
                            // 左
                            mOnShakeListener.direction(Direction.DIRECTION_LEFT, getDirectionInfo(Direction.DIRECTION_LEFT));
                        } else if (ANGLE_8D_OF_4P <= angle && ANGLE_8D_OF_5P > angle) {
                            // 左上
                            mOnShakeListener.direction(Direction.DIRECTION_UP_LEFT, getDirectionInfo(Direction.DIRECTION_UP_LEFT));
                        } else if (ANGLE_8D_OF_5P <= angle && ANGLE_8D_OF_6P > angle) {
                            // 上
                            mOnShakeListener.direction(Direction.DIRECTION_UP, getDirectionInfo(Direction.DIRECTION_UP));
                        } else if (ANGLE_8D_OF_6P <= angle && ANGLE_8D_OF_7P > angle) {
                            // 右上
                            mOnShakeListener.direction(Direction.DIRECTION_UP_RIGHT, getDirectionInfo(Direction.DIRECTION_UP_RIGHT));
                        }
                        break;
                    default:
                        break;
                }
            } else if (CallBackMode.CALL_BACK_STATE_CHANGE == mCallBackMode) {
                switch (mDirectionMode) {
                    case DIRECTION_2_HORIZONTAL:// 左右方向
                        if ((ANGLE_0 <= angle && ANGLE_HORIZONTAL_2D_OF_0P > angle || ANGLE_HORIZONTAL_2D_OF_1P <= angle && ANGLE_360 > angle) && tempDirection != Direction.DIRECTION_RIGHT) {
                            // 右
                            tempDirection = Direction.DIRECTION_RIGHT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_HORIZONTAL_2D_OF_0P <= angle && ANGLE_HORIZONTAL_2D_OF_1P > angle && tempDirection != Direction.DIRECTION_LEFT) {
                            // 左
                            tempDirection = Direction.DIRECTION_LEFT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_2_VERTICAL:// 上下方向
                        if (ANGLE_VERTICAL_2D_OF_0P <= angle && ANGLE_VERTICAL_2D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN) {
                            // 下
                            tempDirection = Direction.DIRECTION_DOWN;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_VERTICAL_2D_OF_1P <= angle && ANGLE_360 > angle && tempDirection != Direction.DIRECTION_UP) {
                            // 上
                            tempDirection = Direction.DIRECTION_UP;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_4_ROTATE_0:// 四个方向
                        if ((ANGLE_0 <= angle && ANGLE_ROTATE45_4D_OF_0P > angle || ANGLE_ROTATE45_4D_OF_3P <= angle && ANGLE_360 > angle) && tempDirection != Direction.DIRECTION_RIGHT) {
                            // 右
                            tempDirection = Direction.DIRECTION_RIGHT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_ROTATE45_4D_OF_0P <= angle && ANGLE_ROTATE45_4D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN) {
                            // 下
                            tempDirection = Direction.DIRECTION_DOWN;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_ROTATE45_4D_OF_1P <= angle && ANGLE_ROTATE45_4D_OF_2P > angle && tempDirection != Direction.DIRECTION_LEFT) {
                            // 左
                            tempDirection = Direction.DIRECTION_LEFT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_ROTATE45_4D_OF_2P <= angle && ANGLE_ROTATE45_4D_OF_3P > angle && tempDirection != Direction.DIRECTION_UP) {
                            // 上
                            tempDirection = Direction.DIRECTION_UP;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_4_ROTATE_45:// 四个方向 旋转45度
                        if (ANGLE_4D_OF_0P <= angle && ANGLE_4D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN_RIGHT) {
                            // 右下
                            tempDirection = Direction.DIRECTION_DOWN_RIGHT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_4D_OF_1P <= angle && ANGLE_4D_OF_2P > angle && tempDirection != Direction.DIRECTION_DOWN_LEFT) {
                            // 左下
                            tempDirection = Direction.DIRECTION_DOWN_LEFT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_4D_OF_2P <= angle && ANGLE_4D_OF_3P > angle && tempDirection != Direction.DIRECTION_UP_LEFT) {
                            // 左上
                            tempDirection = Direction.DIRECTION_UP_LEFT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_4D_OF_3P <= angle && ANGLE_360 > angle && tempDirection != Direction.DIRECTION_UP_RIGHT) {
                            // 右上
                            tempDirection = Direction.DIRECTION_UP_RIGHT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_8:// 八个方向
                        if ((ANGLE_0 <= angle && ANGLE_8D_OF_0P > angle || ANGLE_8D_OF_7P <= angle && ANGLE_360 > angle) && tempDirection != Direction.DIRECTION_RIGHT) {
                            // 右
                            tempDirection = Direction.DIRECTION_RIGHT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_0P <= angle && ANGLE_8D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN_RIGHT) {
                            // 右下
                            tempDirection = Direction.DIRECTION_DOWN_RIGHT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_1P <= angle && ANGLE_8D_OF_2P > angle && tempDirection != Direction.DIRECTION_DOWN) {
                            // 下
                            tempDirection = Direction.DIRECTION_DOWN;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_2P <= angle && ANGLE_8D_OF_3P > angle && tempDirection != Direction.DIRECTION_DOWN_LEFT) {
                            // 左下
                            tempDirection = Direction.DIRECTION_DOWN_LEFT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_3P <= angle && ANGLE_8D_OF_4P > angle && tempDirection != Direction.DIRECTION_LEFT) {
                            // 左
                            tempDirection = Direction.DIRECTION_LEFT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_4P <= angle && ANGLE_8D_OF_5P > angle && tempDirection != Direction.DIRECTION_UP_LEFT) {
                            // 左上
                            tempDirection = Direction.DIRECTION_UP_LEFT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_5P <= angle && ANGLE_8D_OF_6P > angle && tempDirection != Direction.DIRECTION_UP) {
                            // 上
                            tempDirection = Direction.DIRECTION_UP;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_6P <= angle && ANGLE_8D_OF_7P > angle && tempDirection != Direction.DIRECTION_UP_RIGHT) {
                            // 右上
                            tempDirection = Direction.DIRECTION_UP_RIGHT;
                            mOnShakeListener.direction(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    default:
                        break;
                }
            } else if (CallBackMode.CALL_BACK_STATE_CHANGE_CONTINUITY == mCallBackMode) {
                switch (mDirectionMode) {
                    case DIRECTION_2_HORIZONTAL:// 左右方向
                        if ((ANGLE_0 <= angle && ANGLE_HORIZONTAL_2D_OF_0P > angle || ANGLE_HORIZONTAL_2D_OF_1P <= angle && ANGLE_360 > angle) && tempDirection != Direction.DIRECTION_RIGHT) {
                            // 右
                            tempDirection = Direction.DIRECTION_RIGHT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_HORIZONTAL_2D_OF_0P <= angle && ANGLE_HORIZONTAL_2D_OF_1P > angle && tempDirection != Direction.DIRECTION_LEFT) {
                            // 左
                            tempDirection = Direction.DIRECTION_LEFT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_2_VERTICAL:// 上下方向
                        if (ANGLE_VERTICAL_2D_OF_0P <= angle && ANGLE_VERTICAL_2D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN) {
                            // 下
                            tempDirection = Direction.DIRECTION_DOWN;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_VERTICAL_2D_OF_1P <= angle && ANGLE_360 > angle && tempDirection != Direction.DIRECTION_UP) {
                            // 上
                            tempDirection = Direction.DIRECTION_UP;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_4_ROTATE_0:// 四个方向
                        if ((ANGLE_0 <= angle && ANGLE_ROTATE45_4D_OF_0P > angle || ANGLE_ROTATE45_4D_OF_3P <= angle && ANGLE_360 > angle) && tempDirection != Direction.DIRECTION_RIGHT) {
                            // 右
                            tempDirection = Direction.DIRECTION_RIGHT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_ROTATE45_4D_OF_0P <= angle && ANGLE_ROTATE45_4D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN) {
                            // 下
                            tempDirection = Direction.DIRECTION_DOWN;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_ROTATE45_4D_OF_1P <= angle && ANGLE_ROTATE45_4D_OF_2P > angle && tempDirection != Direction.DIRECTION_LEFT) {
                            // 左
                            tempDirection = Direction.DIRECTION_LEFT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_ROTATE45_4D_OF_2P <= angle && ANGLE_ROTATE45_4D_OF_3P > angle && tempDirection != Direction.DIRECTION_UP) {
                            // 上
                            tempDirection = Direction.DIRECTION_UP;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_4_ROTATE_45:// 四个方向 旋转45度
                        if (ANGLE_4D_OF_0P <= angle && ANGLE_4D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN_RIGHT) {
                            // 右下
                            tempDirection = Direction.DIRECTION_DOWN_RIGHT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_4D_OF_1P <= angle && ANGLE_4D_OF_2P > angle && tempDirection != Direction.DIRECTION_DOWN_LEFT) {
                            // 左下
                            tempDirection = Direction.DIRECTION_DOWN_LEFT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_4D_OF_2P <= angle && ANGLE_4D_OF_3P > angle && tempDirection != Direction.DIRECTION_UP_LEFT) {
                            // 左上
                            tempDirection = Direction.DIRECTION_UP_LEFT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_4D_OF_3P <= angle && ANGLE_360 > angle && tempDirection != Direction.DIRECTION_UP_RIGHT) {
                            // 右上
                            tempDirection = Direction.DIRECTION_UP_RIGHT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    case DIRECTION_8:// 八个方向
                        if ((ANGLE_0 <= angle && ANGLE_8D_OF_0P > angle || ANGLE_8D_OF_7P <= angle && ANGLE_360 > angle) && tempDirection != Direction.DIRECTION_RIGHT) {
                            // 右
                            tempDirection = Direction.DIRECTION_RIGHT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_0P <= angle && ANGLE_8D_OF_1P > angle && tempDirection != Direction.DIRECTION_DOWN_RIGHT) {
                            // 右下
                            tempDirection = Direction.DIRECTION_DOWN_RIGHT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_1P <= angle && ANGLE_8D_OF_2P > angle && tempDirection != Direction.DIRECTION_DOWN) {
                            // 下
                            tempDirection = Direction.DIRECTION_DOWN;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_2P <= angle && ANGLE_8D_OF_3P > angle && tempDirection != Direction.DIRECTION_DOWN_LEFT) {
                            // 左下
                            tempDirection = Direction.DIRECTION_DOWN_LEFT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_3P <= angle && ANGLE_8D_OF_4P > angle && tempDirection != Direction.DIRECTION_LEFT) {
                            // 左
                            tempDirection = Direction.DIRECTION_LEFT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_4P <= angle && ANGLE_8D_OF_5P > angle && tempDirection != Direction.DIRECTION_UP_LEFT) {
                            // 左上
                            tempDirection = Direction.DIRECTION_UP_LEFT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_5P <= angle && ANGLE_8D_OF_6P > angle && tempDirection != Direction.DIRECTION_UP) {
                            // 上
                            tempDirection = Direction.DIRECTION_UP;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        } else if (ANGLE_8D_OF_6P <= angle && ANGLE_8D_OF_7P > angle && tempDirection != Direction.DIRECTION_UP_RIGHT) {
                            // 右上
                            tempDirection = Direction.DIRECTION_UP_RIGHT;
                            updateAddOrSubtract(tempDirection, getDirectionInfo(tempDirection));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 回调结束
     */
    private void callBackFinish() {
        if (mIsStartCallBack) {
            mIsStartCallBack = false;
            tempDirection = Direction.DIRECTION_CENTER;
            if (CallBackMode.CALL_BACK_STATE_CHANGE_CONTINUITY == mCallBackMode) {
                stopAddOrSubtract();
            }
            if (null != mOnAngleChangeListener) {
                mOnAngleChangeListener.onFinish();
            }
            if (null != mOnShakeListener) {
                mOnShakeListener.onFinish();
            }
            if (null != mOnStrengthChangeListener) {
                mOnStrengthChangeListener.onFinish();
            }
        }
    }

    /**
     * 判断点在摇杆中心区域内
     */
    private boolean isInRockerCenterZone(Point rockerPosition, Point touchPoint) {
        // 两点在X轴的距离
        float lenX = (float) (touchPoint.x - rockerPosition.x);
        // 两点在Y轴距离
        float lenY = (float) (touchPoint.y - rockerPosition.y);
        // 两点距离
        float lenXY = (float) Math.sqrt((double) (lenX * lenX + lenY * lenY));
        RockerLog.i("lenX  => " + lenX + " lenY => " + lenY + " mRockerRadius => " + mRockerRadius + " lenXY-Center = > " + lenXY);
        if (lenXY < mRockerRadius / 3 * 2) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 手势模式
     */
    public enum GestureMode {
        // 必须从中心开始连续响应
        GESTURE_CENTER_CONTINUOU,
        // 无需从中心开始连续响应,到达响应边界开始响应
        GESTURE_UN_CENTER_CONTINUOUS
    }

    /**
     * 摇杆松开返回模式
     */
    public enum RockerBackMode {
        // 不回退
        BACK_NONE,
        // 回到中心原点
        BACK_CENTER_ORIGIN,
        // 回到X轴
        BACK_X_AXIS,
        // 回到Y轴
        BACK_Y_AXIS
    }

    /**
     * 回调模式
     */
    public enum CallBackMode {
        // 有移动就立刻回调
        CALL_BACK_MOVE,
        // 只有状态变化的时候才回调
        CALL_BACK_STATE_CHANGE,
        // 只有状态变化的时候连续回调
        CALL_BACK_STATE_CHANGE_CONTINUITY
    }


    /**
     * 摇杆支持几个方向
     */
    public enum DirectionMode {
        // 横向 左右两个方向
        DIRECTION_2_HORIZONTAL,
        // 纵向 上下两个方向
        DIRECTION_2_VERTICAL,
        // 四个方向
        DIRECTION_4_ROTATE_0,
        // 四个方向 倾斜45度
        DIRECTION_4_ROTATE_45,
        // 八个方向
        DIRECTION_8
    }


    /**
     * 方向
     */
    public enum Direction {
        DIRECTION_LEFT, // 左
        DIRECTION_RIGHT, // 右
        DIRECTION_UP, // 上
        DIRECTION_DOWN, // 下
        DIRECTION_UP_LEFT, // 左上
        DIRECTION_UP_RIGHT, // 右上
        DIRECTION_DOWN_LEFT, // 左下
        DIRECTION_DOWN_RIGHT, // 右下
        DIRECTION_CENTER // 中间
    }

    /**
     * 摇动强度的监听接口
     */
    public interface onStrengthChangeListener {
        /**
         * 开始
         */
        void onStart();

        /**
         * 摇杆角度变化
         *
         * @param strength 强度 [0-1]
         */
        void strength(float strength);

        /**
         * 结束
         */
        void onFinish();
    }


    /**
     * 摇动角度的监听接口
     */
    public interface OnAngleChangeListener {
        /**
         * 开始
         */
        void onStart();

        /**
         * 摇杆角度变化
         *
         * @param angle 角度[0,360)
         */
        void angle(double angle);

        /**
         * 结束
         */
        void onFinish();
    }


    /**
     * 摇动方向监听接口
     */
    public interface OnShakeListener {
        /**
         * 开始
         */
        void onStart();

        /**
         * 摇动方向
         *
         * @param direction     方向
         * @param directionInfo 方向描述
         */
        void direction(Direction direction, String directionInfo);

        /**
         * 结束
         */
        void onFinish();
    }


    /**
     * Drawable 转 Bitmap
     *
     * @param drawable 要转化bitmap的drawable
     * @return
     */
    private Bitmap drawable2Bitmap(Drawable drawable, int size) {
        //        //获取 drawable 长宽
        //        int width = drawable.getIntrinsicWidth();
        //        int height = drawable.getIntrinsicHeight();
        //获取 drawable 颜色模式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        //创建bitmap
        Bitmap bitmap = Bitmap.createBitmap(size, size, config);
        //创建对应的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        // 把drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 触发连续消息发送
     *
     * @param tempDirection
     * @param directionInfo
     */
    private void updateAddOrSubtract(final Direction tempDirection, final String directionInfo) {
        stopAddOrSubtract();
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        mScheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                RockerView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mOnShakeListener != null && mIsStartCallBack) {
                            mOnShakeListener.direction(tempDirection, directionInfo);
                        }
                    }
                });
            }
        }, 0, mDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * 结束消息发送
     */
    private void stopAddOrSubtract() {
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService.shutdownNow();
            mScheduledExecutorService = null;
        }
    }

    /**
     * 方向的描述内容
     *
     * @param direction 方向
     * @return
     */
    private String getDirectionInfo(RockerView.Direction direction) {
        String message = null;
        switch (direction) {
            case DIRECTION_CENTER:
                message = getResources().getString(R.string.rockerview_center);
                break;
            case DIRECTION_LEFT:
                message = getResources().getString(R.string.rockerview_left);
                break;
            case DIRECTION_RIGHT:
                message = getResources().getString(R.string.rockerview_right);
                break;
            case DIRECTION_UP:
                message = getResources().getString(R.string.rockerview_up);
                break;
            case DIRECTION_DOWN:
                message = getResources().getString(R.string.rockerview_down);
                break;
            case DIRECTION_UP_LEFT:
                message = getResources().getString(R.string.rockerview_left_up);
                break;
            case DIRECTION_UP_RIGHT:
                message = getResources().getString(R.string.rockerview_right_up);
                break;
            case DIRECTION_DOWN_LEFT:
                message = getResources().getString(R.string.rockerview_left_down);
                break;
            case DIRECTION_DOWN_RIGHT:
                message = getResources().getString(R.string.rockerview_right_down);
                break;
            default:
                break;
        }
        return message;
    }
    /************************************** public ********************************************/


    /**
     * 设置手势模式
     *
     * @param mode 回调模式
     */
    public void setGestureMode(GestureMode mode) {
        if (null != mode) {
            mGestureMode = mode;
        }
    }

    /**
     * 设置回调模式
     *
     * @param mode 回调模式
     */
    public void setCallBackMode(CallBackMode mode) {
        if (null != mode) {
            mCallBackMode = mode;
        }
    }

    /**
     * 设置支持监听模式
     *
     * @param mode 回调模式
     */
    public void setDirectionMode(DirectionMode mode) {
        if (null != mode) {
            this.mDirectionMode = mode;
        }
    }

    /**
     * 添加摇动的监听
     *
     * @param listener 回调
     */
    public void setOnShakeListener(OnShakeListener listener) {
        if (null != listener) {
            mOnShakeListener = listener;
        }
    }

    /**
     * 设置松开摇杆后摇杆回退模式
     *
     * @param mode
     */
    public void setRockerBackMode(RockerBackMode mode) {
        if (null != mode) {
            this.mRockerBackMode = mode;
        }
    }

    /**
     * 添加摇杆摇动角度的监听
     *
     * @param listener 回调接口
     */
    public void setOnAngleChangeListener(OnAngleChangeListener listener) {
        if (null != listener) {
            mOnAngleChangeListener = listener;
        }
    }

    /**
     * 添加摇杆摇动强度的监听
     *
     * @param listener 回调接口
     */
    public void setOnStrengthChangeListener(onStrengthChangeListener listener) {
        if (null != listener) {
            mOnStrengthChangeListener = listener;
        }
    }

    /**
     * 设置连续监听延时时间
     *
     * @param time 毫秒
     */
    public void setContinuityCallBackDelay(long time) {
        if (time < DEFAULT_DELAY) {
            time = DEFAULT_DELAY;
        }
        mDelay = time;
    }
}
