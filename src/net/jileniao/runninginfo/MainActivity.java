package net.jileniao.runninginfo;

import android.content.Intent;
import android.preference.PreferenceActivity;

import net.jileniao.runninginfo.R;
import net.jileniao.runninginfo.service.DeviceInfoWindowService;

public class MainActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_activity);

        Intent show = new Intent(this, DeviceInfoWindowService.class);
        show.putExtra(DeviceInfoWindowService.ACTION,
                DeviceInfoWindowService.SHOW_DEVICE_INFO);
        startService(show);
    }

    @Override
    public void onBackPressed() {
        Intent hide = new Intent(this, DeviceInfoWindowService.class);
        hide.putExtra(DeviceInfoWindowService.ACTION,
                DeviceInfoWindowService.HIDE_DEVICE_INFO);
        startService(hide);
        super.onBackPressed();
    }
}
