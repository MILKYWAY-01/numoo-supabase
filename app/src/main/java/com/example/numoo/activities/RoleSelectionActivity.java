package com.example.numoo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.numoo.R;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        try {
            Button btnAdmin = findViewById(R.id.btn_admin);
            Button btnUser = findViewById(R.id.btn_user);
            Button btnLogin = findViewById(R.id.btn_login);

            btnAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.putExtra("role", "ADMIN");
                startActivity(intent);
            });

            btnUser.setOnClickListener(v -> {
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.putExtra("role", "USER");
                startActivity(intent);
            });

            btnLogin.setOnClickListener(v -> {
                startActivity(new Intent(this, LoginActivity.class));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
