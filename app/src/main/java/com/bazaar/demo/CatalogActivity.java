package com.bazaar.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

public class CatalogActivity extends Activity {

    private final List<Product> items = Product.catalog();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);
        setTitle(R.string.catalog_title);

        ListView list = findViewById(R.id.list_products);
        list.setAdapter(new ProductAdapter(this, items));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(this, ProductActivity.class);
            i.putExtra("product_id", items.get(position).id);
            startActivity(i);
        });

        findViewById(R.id.btn_open_cart).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));

        // B5 (seeded, flaky): express checkout dereferences a session token that is
        // only provisioned for ~1 in 3 sessions.
        findViewById(R.id.btn_express).setOnClickListener(v -> {
            if (new Random().nextInt(3) == 0) {
                throw new NullPointerException("express checkout: session token missing");
            }
            startActivity(new Intent(this, CartActivity.class));
        });

        ApiClient.call("GET", "/api/products", null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView count = findViewById(R.id.tv_cart_count);
        count.setText(getString(R.string.cart_count, CartStore.count()));
    }
}
