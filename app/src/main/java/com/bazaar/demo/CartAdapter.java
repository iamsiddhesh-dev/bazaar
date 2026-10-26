package com.bazaar.demo;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CartAdapter extends BaseAdapter {

    private final LayoutInflater inflater;

    public CartAdapter(Activity ctx) {
        this.inflater = LayoutInflater.from(ctx);
    }

    @Override
    public int getCount() {
        return CartStore.ITEMS.size();
    }

    @Override
    public Object getItem(int position) {
        return CartStore.ITEMS.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView != null ? convertView : inflater.inflate(R.layout.row_cart, parent, false);
        CartStore.Entry e = CartStore.ITEMS.get(position);
        ((TextView) v.findViewById(R.id.tv_cart_title)).setText(e.product.name);
        ((TextView) v.findViewById(R.id.tv_cart_price)).setText(e.product.price);
        return v;
    }
}
