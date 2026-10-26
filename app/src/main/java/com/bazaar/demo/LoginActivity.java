package com.bazaar.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.login_title);

        EditText email = findViewById(R.id.et_email);
        EditText pass = findViewById(R.id.et_password);
        TextView error = findViewById(R.id.tv_login_error);

        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String p = pass.getText().toString().trim();
            if (e.isEmpty() || p.isEmpty()) {
                error.setVisibility(View.VISIBLE);
                error.setText(R.string.login_error);
                return;
            }
            error.setVisibility(View.GONE);
            ApiClient.call("POST", "/api/login", "{\"email\":\"" + e + "\"}", null);
            startActivity(new Intent(this, CatalogActivity.class));
            finish();
        });
    }
}
