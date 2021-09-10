package com.collapse.autowipe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.media.Image;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    UserPresent receiver;
    private static final int REQUEST_CODE = 0;
    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initialize the preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        CardView status = findViewById(R.id.status);


        //initialize the radio buttons used to set the time period
        RadioButton oneD = findViewById(R.id.oneday);
        RadioButton twoD = findViewById(R.id.twodays);
        RadioButton threeD = findViewById(R.id.threedays);
        RadioButton oneW = findViewById(R.id.oneweek);
        RadioButton twoW = findViewById(R.id.twoweeks);
        ImageView logo = findViewById(R.id.logo);
        long time_period = preferences.getLong("time_period",0);


        //register the device as an admin
        try
        {
            // Initiate DevicePolicyManager.
            mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
            // Set DeviceAdmin Receiver for active the component with different option
            mAdminName = new ComponentName(this, DeviceAdmin.class);

            if (!mDPM.isAdminActive(mAdminName)) {
                // try to become active
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allow Auto Wipe to clear phone data when prompted");
                startActivityForResult(intent, REQUEST_CODE);
            }
            else
            {
                // Already is a device administrator, can do security operations now.
                mDPM.lockNow();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        //register the receiver to get the actions performed

        receiver = new UserPresent();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(receiver, filter);


        //set the radio button to the time period selected
        if(time_period == 86400000 ){
            oneD.setChecked(true);
        }else if(time_period == 172800000 ){
            twoD.setChecked(true);
        }else if(time_period == 259200000 ){
            threeD.setChecked(true);
        }else if(time_period == 604800000 ){
            oneW.setChecked(true);
        }else if(time_period == 1209600000 ){
            twoW.setChecked(true);
        }

        //slide down animation for logo
        Animation slide_down = AnimationUtils.loadAnimation(logo.getContext(),
                R.anim.slide_down);
        logo.startAnimation(slide_down);



        if(isActiveAdmin()){
            status.setForegroundTintList(ColorStateList.valueOf(Color.GREEN));
        }else{
            status.setForegroundTintList(ColorStateList.valueOf(Color.RED));
        }

        
    }

    //change the time period to the user request
    public void changeTimePeriod(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.oneday:
                if (checked)
                    editor = preferences.edit();
                    editor.putLong("time_period",86400000);
                    editor.apply();
                    break;
            case R.id.twodays:
                if (checked)
                    editor = preferences.edit();
                    editor.putLong("time_period",172800000);
                    editor.apply();
                    break;
            case R.id.threedays:
                if (checked)
                    editor = preferences.edit();
                    editor.putLong("time_period",259200000);
                    editor.apply();
                    break;
            case R.id.oneweek:
                if (checked)
                    editor = preferences.edit();
                    editor.putLong("time_period",604800000);
                    editor.apply();
                    break;
            case R.id.twoweeks:
                if (checked)
                    editor = preferences.edit();
                    editor.putLong("time_period",1209600000);
                    editor.apply();
                    break;

        }
    }

    public void showHow(View view) {
        Toast.makeText(getApplicationContext(),"This is the amount of time before app wipes the phone",Toast.LENGTH_LONG).show();
    }


    //required receiver for admin privileges
    public static class  DeviceAdmin extends DeviceAdminReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
        }

        public void onEnabled(Context context, Intent intent) {};

        public void onDisabled(Context context, Intent intent) {};



    }


    // check if the device is registered as an admin
    public boolean isActiveAdmin() {
        return mDPM.isAdminActive(mAdminName);
    }

    // receiver to receive actions and perform requests
    public static class UserPresent extends BroadcastReceiver {

        long shutdown_stamp;
        SharedPreferences preferences;
        private DevicePolicyManager mDPM;

        @Override
        public void onReceive(Context context, Intent intent) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);

            //when the user unlocks the phone stop the clearing

            if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("clear", false);
                editor.apply();
            }

            //when phone shuts down collect the timestamp
            else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
                shutdown_stamp = System.currentTimeMillis();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("shutdown_stamp", shutdown_stamp);
                editor.putBoolean("clear",true);
                editor.apply();
            }

            /* when the phone has booted check if the
            current day is equal to or further than
                the user's requested time period for clearing after shutdown*/

            else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                long shutdown_stamp  =  preferences.getLong("shutdown_stamp",System.currentTimeMillis());
                long time_period = preferences.getLong("time_period",999999999);
                long clear_date = shutdown_stamp + time_period;
                boolean clear = preferences.getBoolean("clear",false);


                if((System.currentTimeMillis() >= clear_date) && clear) {
                    mDPM.wipeData(0);
                }

            }
        }

    }

}