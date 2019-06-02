package com.example.sam.smartsleepzzz;

import android.app.Fragment;
import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class AlarmActivity extends AppCompatActivity implements TimeFragment.OnFragmentInteractionListener {

    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
    }

    public void changeStartTime(View view){
        TimeFragment fragment = TimeFragment.newInstance("startTime", "hello");

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.main_container, fragment).commit();
    }

    public void changeEndTime(View view) {
        TimeFragment fragment = TimeFragment.newInstance("endTime", "hello");

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.main_container, fragment).commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri){

    }

    public void setStartTime(int hour, int minute){
        startHour = hour;
        startMinute = minute;
        Log.v("time", String.valueOf(startHour));
        TextView startTime = (TextView) this.findViewById(R.id.startTime);
        startTime.setText(String.valueOf(startHour) + ":" + String.valueOf(startMinute));
    }

    public void setEndTime(int hour, int minute){
        endHour = hour;
        endMinute = minute;
        Log.v("time", String.valueOf(endHour));
        TextView endTime = (TextView) this.findViewById(R.id.endTime);
        endTime.setText(String.valueOf(endHour) + ":" + String.valueOf(endMinute));
    }

}
