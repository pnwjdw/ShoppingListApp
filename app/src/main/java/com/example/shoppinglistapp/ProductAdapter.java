package com.example.shoppinglistapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private static final String TAG = "ProductAdapter";
    private final List<Product> products;
    private final OnProductEditListener editListener;
    private final OnProductDeleteListener deleteListener;
    private final OnProductToggleListener toggleListener;

    public interface OnProductEditListener {
        void onEdit(Product product);
    }

    public interface OnProductDeleteListener {
        void onDelete(Product product);
    }

    public interface OnProductToggleListener {
        void onToggle(Product product, boolean isChecked);
    }

    public ProductAdapter(List<Product> products, OnProductEditListener editListener,
                          OnProductDeleteListener deleteListener, OnProductToggleListener toggleListener) {
        this.products = products;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
        this.toggleListener = toggleListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Product product = products.get(position);
        Log.d(TAG, "Binding product: " + product.name);
        holder.name.setText(product.name);
        holder.price.setText(String.format("%.2f", product.price));
        holder.bought.setChecked(product.bought);
        holder.description.setText(product.description != null ? product.description : "");
        holder.itemView.setOnClickListener(v -> editListener.onEdit(product));
        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onDelete(product);
            return true;
        });
        holder.bought.setOnCheckedChangeListener(null); // Отключаем слушатель для предотвращения рекурсии
        holder.bought.setChecked(product.bought);
        holder.bought.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleListener.onToggle(product, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, price, description;
        CheckBox bought;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            price = itemView.findViewById(R.id.product_price);
            description = itemView.findViewById(R.id.product_description);
            bought = itemView.findViewById(R.id.product_bought);
        }
    }
}