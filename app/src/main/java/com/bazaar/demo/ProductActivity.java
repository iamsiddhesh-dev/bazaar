package com.bazaar.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class ProductActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        String id = getIntent().getStringExtra("product_id");
        Product p = Product.byId(id);
        if (p == null) {
            finish();
            return;
        }
        setTitle(p.name);

        ((TextView) findViewById(R.id.tv_product_title)).setText(p.name);
        ((TextView) findViewById(R.id.tv_product_price)).setText(p.price);
        ((TextView) findViewById(R.id.tv_product_desc)).setText(p.desc);

        findViewById(R.id.btn_add_cart).setOnClickListener(v -> {
            CartStore.add(p);
            Toast.makeText(this, R.string.added_toast, Toast.LENGTH_SHORT).show();
        });
    }
}
