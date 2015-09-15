package net.jileniao.runninginfo.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.jileniao.runninginfo.R;
import net.jileniao.runninginfo.util.BatteryUtil;
import net.jileniao.runninginfo.util.CpuUtil;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class DeviceInfoWindowService extends Service {

    private static final int REFRESH_TIME = 1000;
    private static final String LF = "\n";
    public static final String ACTION = "action";
    public static final int SHOW_DEVICE_INFO = 100;
    public static final int HIDE_DEVICE_INFO = 101;

    private static final int HANDLE_CHECK_ACTIVITY = 200;

    private boolean isAdded = false;
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private TextView mFloatView;
    private StringBuffer mCPUHeaderText;
    private StringBuffer mPowerHeaderText;
    private int mCPUCoreNum;
    private String eachCpuInfo = "CPU%d\t%s\t%s\t%s" + LF;
    private boolean mIsCpuDisp;
    private boolean mIsPowerDisp;
    private SharedPreferences mSP;

    List<String> cpuMaxList = new ArrayList<String>();
    List<String> cpuMinList = new ArrayList<String>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSP = PreferenceManager.getDefaultSharedPreferences(this);

        // initial CPU header text
        mCPUHeaderText = new StringBuffer();
        mCPUHeaderText.append("----------" + LF);
        mCPUHeaderText.append("CPU INFO" + LF);
        mCPUCoreNum = CpuUtil.getNumCores();
        mCPUHeaderText.append("CPU Number:" + mCPUCoreNum + LF);
        mCPUHeaderText.append("(KHz)\tMax\tMin\tCurrent" + LF);
        for (int i = 0; i < mCPUCoreNum; i++) {
            cpuMaxList.add(getKHz(CpuUtil.getMaxCpuFreq(i)));
            cpuMinList.add(getKHz(CpuUtil.getMinCpuFreq(i)));
        }

        // initial Power header text
        mPowerHeaderText = new StringBuffer();
        mPowerHeaderText.append("----------" + LF);
        mPowerHeaderText.append("Battery INFO" + LF);

        createFloatView();

        // register a BroadcastReceiver to listen battery change event
        // we are not allowed to register this in AndroidManifest.xml
        registerReceiver(mBatInfoReceiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBatInfoReceiver);
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        int operation = intent.getIntExtra(ACTION, SHOW_DEVICE_INFO);
        switch (operation) {
        case SHOW_DEVICE_INFO:
            mHandler.removeMessages(HANDLE_CHECK_ACTIVITY);
            mHandler.sendEmptyMessage(HANDLE_CHECK_ACTIVITY);
            break;
        case HIDE_DEVICE_INFO:
            while (mHandler.hasMessages(HANDLE_CHECK_ACTIVITY)) {
                mHandler.removeMessages(HANDLE_CHECK_ACTIVITY);
            }
            wm.removeView(mFloatView);
            stopSelf();
            break;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case HANDLE_CHECK_ACTIVITY:
                mIsCpuDisp = mSP.getBoolean(getString(R.string.cpu_key), true);
                mIsPowerDisp = mSP.getBoolean(getString(R.string.battery_key),
                        false);
                StringBuffer sb = new StringBuffer();

                if (mIsCpuDisp) {
                    sb.append(mCPUHeaderText.toString());
                    for (int i = 0; i < mCPUCoreNum; i++) {
                        sb.append(String.format(eachCpuInfo, i,
                                cpuMaxList.get(i), cpuMinList.get(i),
                                getKHz(CpuUtil.getCurCpuFreq(i))));
                    }
                }

                if (mIsPowerDisp) {
                    sb.append(mPowerHeaderText);
                    sb.append("Current    :" + BatteryUtil.getCurrent() + "mA"
                            + LF);
                    sb.append("Voltage    :" + BatteryV + "mV" + LF);
                    sb.append("Temperature:" + BatteryT * 0.1 + "℃" + LF);
                    sb.append("Quantity   :" + BatteryN + "%" + LF);
                }
                mFloatView.setText(sb.toString());

                if (!isAdded) {
                    wm.addView(mFloatView, params);
                    isAdded = true;
                }
                mHandler.sendEmptyMessageDelayed(HANDLE_CHECK_ACTIVITY,
                        REFRESH_TIME);
                break;
            }
        }
    };

    private String getKHz(String hzStr) {
        try {
            int hz = Integer.parseInt(hzStr);
            DecimalFormat df = new DecimalFormat("###.0");
            return df.format((double) hz / 1000);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * 创建悬浮窗
     */
    private void createFloatView() {
        mFloatView = new TextView(getApplicationContext());
        mFloatView.setText("CPU Info");
        mFloatView.setBackgroundColor(0x00ffffff);

        wm = (WindowManager) getApplicationContext().getSystemService(
                Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();

        // 设置window type
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        /*
         * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
         * 即拉下通知栏不可见
         */
        params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明

        // 设置Window flag
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        /*
         * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
         * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
         * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
         */

        // 设置悬浮窗的长得宽
        params.width = LayoutParams.WRAP_CONTENT;
        params.height = LayoutParams.WRAP_CONTENT;

        // 设置悬浮窗的Touch监听
        mFloatView.setOnTouchListener(new OnTouchListener() {
            int lastX, lastY;
            int paramX, paramY;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    paramX = params.x;
                    paramY = params.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) event.getRawX() - lastX;
                    int dy = (int) event.getRawY() - lastY;
                    params.x = paramX + dx;
                    params.y = paramY + dy;
                    // 更新悬浮窗位置
                    wm.updateViewLayout(mFloatView, params);
                    break;
                }
                return true;
            }
        });

        wm.addView(mFloatView, params);
        isAdded = true;
    }

    private int BatteryN; // 目前电量
    private int BatteryV; // 电池电压
    private double BatteryT; // 电池温度

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            /*
             * 如果捕捉到的action是ACTION_BATTERY_CHANGED， 就运行onBatteryInfoReceiver()
             */
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                BatteryN = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0); // 目前电量
                BatteryV = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0); // 电池电压
                BatteryT = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,
                        0); // 电池温度
            }
        }
    };
}
