package com.example.sam.smartsleepzzz;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity implements DataFragment.OnFragmentInteractionListener, AlarmFragment.OnFragmentInteractionListener, ProfileFragment.OnFragmentInteractionListener{

    private static final String TAG = "tag";
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;

    private String name;
    private String email;
    private Uri photoUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            name = user.getDisplayName();
            email = user.getEmail();
            photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            String uid = user.getUid();
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);

        viewPager.setAdapter(new SectionPagerAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(viewPager);
    }

    public class SectionPagerAdapter extends FragmentPagerAdapter {

        public SectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new AlarmFragment();
                case 1:
                    Bundle bundle = new Bundle();
                    bundle.putString("name",name);
                    bundle.putString("email", email);
                    ProfileFragment profileFrag = new ProfileFragment();
                    profileFrag.setArguments(bundle);
                    return profileFrag;
                case 2:
                    return new DataFragment();
                default:
                    return new AlarmFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Alarm";
                case 1:
                    return "Profile";
                case 2:
                    return "Data";
                default:
                    return "Alarm";
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri){

    }

//    public void changeStartTime(View view){
//        TimeFragment fragment = TimeFragment.newInstance("startTime", "hello");
//
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//
//        transaction.replace(R.id.main_container, fragment).commit();
//    }
//
//    public void changeEndTime(View view) {
//        TimeFragment fragment = TimeFragment.newInstance("endTime", "hello");
//
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//
//        transaction.replace(R.id.main_container, fragment).commit();
//    }
//
//    public void setStartTime(int hour, int minute){
//        startHour = hour;
//        startMinute = minute;
//        Log.v("time", String.valueOf(startHour));
//        TextView startTime = (TextView) this.findViewById(R.id.startTime);
//        startTime.setText(String.valueOf(startHour) + ":" + String.valueOf(startMinute));
//    }
//
//    public void setEndTime(int hour, int minute){
//        endHour = hour;
//        endMinute = minute;
//        Log.v("time", String.valueOf(endHour));
//        TextView endTime = (TextView) this.findViewById(R.id.endTime);
//        endTime.setText(String.valueOf(endHour) + ":" + String.valueOf(endMinute));
//    }
//
//    public void onAlarmClick(View view){
//        Intent myIntent = new Intent(this, MainActivity.class);
//        myIntent.putExtra("key", "alarm"); //Optional parameters
//        startActivity(myIntent);
//    }
//
//    public void onProfileClick(View view){
//        Log.v("here","here");
//        Intent myIntent = new Intent(this, ProfileActivity.class);
//        myIntent.putExtra("key", "profile"); //Optional parameters
//        startActivity(myIntent);
//    }
//
//    public void onDataClick(View view){
//        Intent myIntent = new Intent(this, DataActivity.class);
//        myIntent.putExtra("key", "data"); //Optional parameters
//        startActivity(myIntent);
//    }

}
