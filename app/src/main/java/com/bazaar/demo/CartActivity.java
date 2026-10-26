package com.bazaar.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

public class CartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        setTitle(R.string.cart_title);

        ListView list = findViewById(R.id.list_cart);
        list.setAdapter(new CartAdapter(this));

        TextView summary = findViewById(R.id.tv_cart_summary);
        summary.setText(getString(R.string.cart_count, CartStore.count()));

        // B3 (seeded, deterministic): coupon button has an empty listener.
        findViewById(R.id.btn_apply_coupon).setOnClickListener(v -> {
            // intentionally empty
        });

        TextView error = findViewById(R.id.tv_checkout_error);
        findViewById(R.id.btn_checkout).setOnClickListener(v -> {
            error.setVisibility(android.view.View.VISIBLE);
            error.setText(R.string.checkout_pending);
            ApiClient.call("POST", "/api/checkout", "{\"cart\":" + CartStore.count() + "}", (code, body) -> {
                if (code >= 400 || code == 0) {
                    error.setText(getString(R.string.checkout_failed, code));
                } else {
                    startActivity(new Intent(this, CheckoutResultActivity.class));
                }
            });
        });
    }
}
