package com.example.shoppinglistapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ShopAdapter adapter;
    private final List<Shop> shops = new ArrayList<>();
    private static List<String> sqlLogs = new ArrayList<>();

    public static List<String> getSqlLogs() {
        return sqlLogs;
    }

    public static void setSqlLogs(List<String> sqlLogs) {
        MainActivity.sqlLogs = sqlLogs;
    }

    public native String initDatabase(String dbPath);
    public native String addShop(String name);
    public native String[] getShops();
    public native String deleteAll();
    public native String executeSql(String sql);
    public native String[] getLogs();
    public native void clearLogs();

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
        setContentView(R.layout.activity_main);

        // Логирование ресурсов для диагностики
        Log.d(TAG, "Resource recycler_shops: " + R.id.recycler_shops);
        Log.d(TAG, "Resource fab_add_shop: " + R.id.fab_add_shop);
        Log.d(TAG, "Resource background: " + R.color.background);
        Log.d(TAG, "Resource text_primary: " + R.color.text_primary);
        Log.d(TAG, "Resource card_bg: " + R.color.card_bg);
        Log.d(TAG, "Resource shop_name: " + R.id.shop_name);

        File dbFile = getDatabasePath("shopping.db");
        if (!Objects.requireNonNull(dbFile.getParentFile()).exists()) {
            boolean created = dbFile.getParentFile().mkdirs();
            Log.d(TAG, "Database directory created: " + created);
        }
        String dbPath = dbFile.getAbsolutePath();
        Log.d(TAG, "Database path: " + dbPath);

        String dbError = initDatabase(dbPath);
        if (dbError != null && !dbError.isEmpty()) {
            Log.e(TAG, "Database initialization error: " + dbError);
            Toast.makeText(this, getString(R.string.sql_error, dbError), Toast.LENGTH_LONG).show();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_shops);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShopAdapter(shops, this::openShop, this::deleteShop);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_shop);
        fab.setOnClickListener(v -> showAddShopDialog());

        loadShops();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadShops() {
        String[] shopData = getShops();
        if (shopData == null) {
            Log.e(TAG, "Failed to load shops: null returned");
            Toast.makeText(this, R.string.error_load_shops, Toast.LENGTH_SHORT).show();
            return;
        }
        shops.clear();
        for (String data : shopData) {
            String[] parts = data.split(":");
            if (parts.length == 2) {
                try {
                    shops.add(new Shop(Integer.parseInt(parts[0]), parts[1]));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing shop data: " + data, e);
                }
            } else {
                Log.e(TAG, "Invalid shop data format: " + data);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddShopDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.shop_name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_shop)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String error = addShop(name);
                    if (error != null && !error.isEmpty()) {
                        Log.e(TAG, "Error adding shop: " + error);
                        Toast.makeText(this, getString(R.string.sql_error, error), Toast.LENGTH_LONG).show();
                    } else {
                        loadShops();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openShop(Shop shop) {
        Intent intent = new Intent(this, ShopActivity.class);
        intent.putExtra("shop_id", shop.id);
        intent.putExtra("shop_name", shop.name);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening shop activity", e);
            Toast.makeText(this, "Ошибка открытия магазина", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteShop(Shop shop) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить магазин?")
                .setMessage("Все продукты в нём будут удалены.")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    String error = executeSql("DELETE FROM shops WHERE id = " + shop.id);
                    if (error != null && !error.isEmpty()) {
                        Log.e(TAG, "Error deleting shop: " + error);
                        Toast.makeText(this, getString(R.string.sql_error, error), Toast.LENGTH_LONG).show();
                    } else {
                        loadShops();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear_db) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.clear_db)
                    .setMessage("Удалить все данные?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        String error = deleteAll();
                        if (error != null && !error.isEmpty()) {
                            Log.e(TAG, "Error clearing database: " + error);
                            Toast.makeText(this, getString(R.string.sql_error, error), Toast.LENGTH_LONG).show();
                        } else {
                            loadShops();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        } else if (id == R.id.author) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.author)
                    .setMessage(R.string.author_info)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        } else if (id == R.id.logs) {
            showLogsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogsDialog() {
        String[] logsArray = getLogs();
        String logsText = "";
        if (logsArray != null) {
            logsText = String.join("\n", logsArray);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.logs)
                .setMessage(logsText.isEmpty() ? "Нет логов" : logsText)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton("Очистить", (dialog, which) -> {
                    clearLogs();
                    showLogsDialog();
                })
                .show();
    }
}