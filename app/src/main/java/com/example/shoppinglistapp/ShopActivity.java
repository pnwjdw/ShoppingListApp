package com.example.shoppinglistapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {
    private static final String TAG = "ShopActivity";
    private int shopId;
    private String shopName;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> products = new ArrayList<>();
    private String currentSort = "name ASC";
    private String searchQuery = "";

    public native String addProduct(int shopId, String name, double price, String desc);
    public native String[] getProducts(int shopId, String sort, String search);
    public native String updateProduct(int id, String name, double price, int bought, String desc);
    public native String deleteProduct(int id);

    static {
        try {
            System.loadLibrary("native-lib");
            Log.d(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        // Логирование ресурсов для диагностики
        Log.d(TAG, "Resource recycler_products: " + R.id.recycler_products);
        Log.d(TAG, "Resource fab_add_product: " + R.id.fab_add_product);
        Log.d(TAG, "Resource background: " + R.color.background);
        Log.d(TAG, "Resource text_primary: " + R.color.text_primary);
        Log.d(TAG, "Resource card_bg: " + R.color.card_bg);
        Log.d(TAG, "Resource product_name: " + R.id.product_name);
        Log.d(TAG, "Resource product_price: " + R.id.product_price);
        Log.d(TAG, "Resource product_description: " + R.id.product_description);
        Log.d(TAG, "Resource product_bought: " + R.id.product_bought);

        shopId = getIntent().getIntExtra("shop_id", -1);
        shopName = getIntent().getStringExtra("shop_name");
        setTitle(shopName);

        recyclerView = findViewById(R.id.recycler_products);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(products, this::editProduct, this::deleteProduct, this::toggleBought);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> showAddProductDialog());

        loadProducts();
    }

    private void loadProducts() {
        products.clear();
        String[] prodData = getProducts(shopId, currentSort, searchQuery);
        if (prodData != null) {
            for (String data : prodData) {
                String[] parts = data.split(":");
                if (parts.length >= 4) {
                    try {
                        String description = parts.length > 4 ? parts[4] : "";
                        products.add(new Product(
                            Integer.parseInt(parts[0]),
                            parts[1],
                            Double.parseDouble(parts[2]),
                            Integer.parseInt(parts[3]) == 1,
                            description
                        ));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing product data: " + data, e);
                    }
                } else {
                    Log.e(TAG, "Invalid product data format: " + data);
                }
            }
        } else {
            Log.e(TAG, "Failed to load products: null returned");
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddProductDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_product, null);
        EditText nameEt = view.findViewById(R.id.et_name);
        EditText priceEt = view.findViewById(R.id.et_price);
        EditText descEt = view.findViewById(R.id.et_desc);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_product)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = nameEt.getText().toString().trim();
                    String priceStr = priceEt.getText().toString().trim();
                    String desc = descEt.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price;
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.error_price, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String error = addProduct(shopId, name, price, desc);
                    if (error != null && !error.isEmpty()) {
                        Log.e(TAG, "Error adding product: " + error);
                        Toast.makeText(this, getString(R.string.sql_error, error), Toast.LENGTH_LONG).show();
                    } else {
                        loadProducts();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void editProduct(Product product) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_product, null);
        EditText nameEt = view.findViewById(R.id.et_name);
        EditText priceEt = view.findViewById(R.id.et_price);
        EditText descEt = view.findViewById(R.id.et_desc);

        nameEt.setText(product.name);
        priceEt.setText(String.valueOf(product.price));
        descEt.setText(product.description);

        new AlertDialog.Builder(this)
                .setTitle("Редактировать продукт")
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = nameEt.getText().toString().trim();
                    String priceStr = priceEt.getText().toString().trim();
                    String desc = descEt.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price;
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.error_price, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String error = updateProduct(product.id, name, price, product.bought ? 1 : 0, desc);
                    if (error != null && !error.isEmpty()) {
                        Log.e(TAG, "Error updating product: " + error);
                        Toast.makeText(this, getString(R.string.sql_error, error), Toast.LENGTH_LONG).show();
                    } else {
                        loadProducts();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить продукт?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    String error = deleteProduct(product.id);
                    if (error != null && !error.isEmpty()) {
                        Log.e(TAG, "Error deleting product: " + error);
                        Toast.makeText(this, getString(R.string.sql_error, error), Toast.LENGTH_LONG).show();
                    } else {
                        loadProducts();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void toggleBought(Product product, boolean isChecked) {
        product.bought = isChecked;
        String error = updateProduct(product.id, product.name, product.price, isChecked ? 1 : 0, product.description);
        if (error != null && !error.isEmpty()) {
            Log.e(TAG, "Error toggling product: " + error);
            Toast.makeText(this, getString(R.string.sql_error, error), Toast.LENGTH_LONG).show();
        } else {
            int position = products.indexOf(product);
            if (position != -1) {
                adapter.notifyItemChanged(position);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shop_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.sort_alpha_asc) {
            currentSort = "name ASC";
        } else if (id == R.id.sort_alpha_desc) {
            currentSort = "name DESC";
        } else if (id == R.id.sort_price_asc) {
            currentSort = "price ASC";
        } else if (id == R.id.sort_price_desc) {
            currentSort = "price DESC";
        } else if (id == R.id.sort_bought) {
            currentSort = "bought DESC";
        } else if (id == R.id.search) {
            EditText input = new EditText(this);
            input.setHint(R.string.search);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.search)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        searchQuery = input.getText().toString().trim();
                        loadProducts();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        loadProducts();
        return super.onOptionsItemSelected(item);
    }
}