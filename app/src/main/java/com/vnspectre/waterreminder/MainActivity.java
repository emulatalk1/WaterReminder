package com.vnspectre.waterreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vnspectre.waterreminder.sync.ReminderTasks;
import com.vnspectre.waterreminder.sync.ReminderUtilities;
import com.vnspectre.waterreminder.sync.WaterReminderIntentService;
import com.vnspectre.waterreminder.utilities.NotificationUtils;
import com.vnspectre.waterreminder.utilities.PreferenceUtilities;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    private TextView mWaterCountDisplay;
    private TextView mChargingCountDisplay;
    private ImageView mChargingImageView;

    private Toast mToast;

    ChargingBroadCastReceiver mChargingReceiver;
    IntentFilter mChargingFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the views.
        mWaterCountDisplay = findViewById(R.id.tv_water_count);
        mChargingCountDisplay = findViewById(R.id.tv_charging_reminder_count);
        mChargingImageView = findViewById(R.id.iv_power_increment);

        // Set the original values in the UI.
        updateWaterCount();
        updateChargingReminderCount();

        // Schedule the charging reminder.
        ReminderUtilities.scheduleChargingReminder(this);

        // Setup the shared preference listener.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);


        // Setup and register the broadcast receiver.
        mChargingFilter = new IntentFilter();
        mChargingReceiver = new ChargingBroadCastReceiver();

        // This sets up an intent filter which will trigger when the charging state changes.
        mChargingFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        mChargingFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            showCharging(batteryManager.isCharging());
        } else {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent currentBatteryStatusIntent = registerReceiver(null, iFilter);

            int batteryStatus = currentBatteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatus == BatteryManager.BATTERY_STATUS_FULL;
            showCharging(isCharging);
        }
        registerReceiver(mChargingReceiver, mChargingFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mChargingReceiver);
    }

    // Updates the TextView to display the new water count from SharedPreferences.
    private void updateChargingReminderCount() {
        int chargingReminders = PreferenceUtilities.getChargingReminderCount(this);
        String formattedChargingReminders = getResources().getQuantityString(R.plurals.charge_notification_count, chargingReminders, chargingReminders);
        mChargingCountDisplay.setText(formattedChargingReminders);
    }

    // Updates the TextView to display the new charging reminder count from SharedPreferences.
    private void updateWaterCount() {
        int waterCount = PreferenceUtilities.getWaterCount(this);
        mWaterCountDisplay.setText(String.valueOf(waterCount));
    }

    // Adds one to the water count and shows a Toast.
    public void incrementWater(View view) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, R.string.water_chug_toast, Toast.LENGTH_SHORT);
        mToast.show();

        Intent incrementWaterCountIntent = new Intent(this, WaterReminderIntentService.class);
        incrementWaterCountIntent.setAction(ReminderTasks.ACTION_INCREMENT_WATER_COUNT);
        startService(incrementWaterCountIntent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferenceUtilities.KEY_WATER_COUNT.equals(key)) {
            updateWaterCount();
        } else if (PreferenceUtilities.KEY_CHARGING_REMINDER_COUNT.equals(key)) {
            updateChargingReminderCount();
        }
    }

    // Change color power from black to pink when phone is plugged.
    private void showCharging(boolean isCharging){
        if (isCharging) {
            // True is pink
            mChargingImageView.setImageResource(R.drawable.ic_power_pink_80px);
        } else {
            mChargingImageView.setImageResource(R.drawable.ic_power_grey_80px);
        }
    }

    // BroadCast Receiver
    private class ChargingBroadCastReceiver extends BroadcastReceiver {
        // Intent.ACTION_POWER_CONNECTED. If It matches, it's charging. If it doesn't match, it's not charging.
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean isCharging = (action.equals(Intent.ACTION_POWER_CONNECTED));

            // Update to UI.
            showCharging(isCharging);
        }
    }

    public void testNotification(View view) {

        NotificationUtils.remindUserBecauseCharging(this);
    }
}
