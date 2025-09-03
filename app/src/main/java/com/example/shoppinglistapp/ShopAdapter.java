package com.example.shoppinglistapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {
    private static final String TAG = "ShopAdapter";
    private final List<Shop> shops;
    private final OnShopClickListener clickListener;
    private final OnShopDeleteListener deleteListener;

    public interface OnShopClickListener {
        void onShopClick(Shop shop);
    }

    public interface OnShopDeleteListener {
        void onShopDelete(Shop shop);
    }

    public ShopAdapter(List<Shop> shops, OnShopClickListener clickListener, OnShopDeleteListener deleteListener) {
        this.shops = shops;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Shop shop = shops.get(position);
        Log.d(TAG, "Binding shop: " + shop.name);
        holder.name.setText(shop.name);
        holder.itemView.setOnClickListener(v -> clickListener.onShopClick(shop));
        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onShopDelete(shop);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return shops.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.shop_name);
        }
    }
}