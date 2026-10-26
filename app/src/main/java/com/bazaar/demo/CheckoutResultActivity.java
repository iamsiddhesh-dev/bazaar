package com.bazaar.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class CheckoutResultActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_result);
        setTitle(R.string.checkout_title);
        ((TextView) findViewById(R.id.tv_result)).setText(R.string.checkout_ok);
        findViewById(R.id.btn_done).setOnClickListener(v -> finish());
    }
}
