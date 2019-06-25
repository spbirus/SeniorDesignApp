package com.example.sam.smartsleepzzz;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String email = intent.getStringExtra("email");

        TextView textName = (TextView) findViewById(R.id.profileName);
        TextView textEmail = (TextView) findViewById(R.id.profileEmail);
        textName.setText(String.valueOf(name));
        textEmail.setText(String.valueOf(email));

        Button btn = (Button) findViewById(R.id.nameChangeButton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //make your toast here
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                EditText textName = (EditText) findViewById(R.id.nameChange);
                String mName = String.valueOf(textName.getText());
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(mName).build();
                user.updateProfile(profileUpdates);
            }
        });
    }
}
