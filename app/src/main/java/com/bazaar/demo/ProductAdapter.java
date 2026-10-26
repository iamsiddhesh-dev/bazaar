package com.bazaar.demo;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ProductAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<Product> items;

    public ProductAdapter(Activity ctx, List<Product> items) {
        this.inflater = LayoutInflater.from(ctx);
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView != null ? convertView : inflater.inflate(R.layout.row_product, parent, false);
        Product p = items.get(position);
        ((TextView) v.findViewById(R.id.tv_row_title)).setText(p.name);
        ((TextView) v.findViewById(R.id.tv_row_price)).setText(p.price);
        return v;
    }
}
