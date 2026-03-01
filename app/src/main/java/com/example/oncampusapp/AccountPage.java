package com.example.oncampusapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class AccountPage extends AppCompatActivity {

    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String email = getIntent().getStringExtra("email");
        String eventsJson = getIntent().getStringExtra("calendar_events_json");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_page);

        backButton = findViewById(R.id.btn_back);

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountPage.this, MapsActivity.class);
            startActivity(intent);
        });

        Button btnOpenCalendar = findViewById(R.id.btnOpenCalendar);
        TextView txtUserEmail = findViewById(R.id.txtUserEmail);

        GoogleSignInAccount account =
                GoogleSignIn.getLastSignedInAccount(this);

        if (account != null) {
            txtUserEmail.setText("Logged in as:\n" + account.getEmail());
        } else {
            txtUserEmail.setText("Not signed in");
        }

        btnOpenCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleViewer.class);
            intent.putExtra("calendar_events_json", eventsJson);
            startActivity(intent);
        });
    }
}