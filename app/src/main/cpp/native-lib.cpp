#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "sqlite3.h"

#define LOG_TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static sqlite3 *db = nullptr;
static std::vector<std::string> logs;

static std::string executeWithLog(const std::string &sql) {
    logs.push_back(sql);
    LOGD("Executing SQL: %s", sql.c_str());
    if (!db) {
        LOGE("Database not initialized");
        return "Database not initialized";
    }
    char *errMsg = nullptr;
    int rc = sqlite3_exec(db, sql.c_str(), nullptr, nullptr, &errMsg);
    if (rc != SQLITE_OK) {
        std::string error = errMsg ? errMsg : "Unknown error";
        LOGE("SQL error: %s", error.c_str());
        sqlite3_free(errMsg);
        return error;
    }
    return "";
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_shoppinglistapp_MainActivity_initDatabase(JNIEnv *env, jobject, jstring dbPath) {
    if (db) {
        LOGD("Closing existing database");
        sqlite3_close(db);
        db = nullptr;
    }
    const char *path = env->GetStringUTFChars(dbPath, nullptr);
    LOGD("Opening database at: %s", path);
    int rc = sqlite3_open(path, &db);
    env->ReleaseStringUTFChars(dbPath, path);
    if (rc != SQLITE_OK) {
        std::string error = sqlite3_errmsg(db);
        LOGE("Database open error: %s", error.c_str());
        return env->NewStringUTF(error.c_str());
    }
    // Enable foreign keys
    std::string enableForeignKeys = "PRAGMA foreign_keys = ON;";
    std::string err = executeWithLog(enableForeignKeys);
    if (!err.empty()) return env->NewStringUTF(err.c_str());
    // Create tables
    std::string createShops = "CREATE TABLE IF NOT EXISTS shops (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL);";
    err = executeWithLog(createShops);
    if (!err.empty()) return env->NewStringUTF(err.c_str());
    std::string createProducts = "CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, shop_id INTEGER, name TEXT NOT NULL, price REAL, bought INTEGER DEFAULT 0, description TEXT, FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE);";
    err = executeWithLog(createProducts);
    if (!err.empty()) return env->NewStringUTF(err.c_str());
    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_shoppinglistapp_MainActivity_addShop(JNIEnv *env, jobject, jstring name) {
    if (!db) {
        LOGE("Database not initialized");
        return env->NewStringUTF("Database not initialized");
    }
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    std::string sql = "INSERT INTO shops (name) VALUES (?);";
    logs.push_back(sql);
    LOGD("Preparing SQL: %s", sql.c_str());
    sqlite3_stmt *stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in prepare: " + errmsg);
        LOGE("Prepare error: %s", errmsg.c_str());
        env->ReleaseStringUTFChars(name, nameStr);
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_bind_text(stmt, 1, nameStr, -1, SQLITE_TRANSIENT);
    env->ReleaseStringUTFChars(name, nameStr);
    rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in step: " + errmsg);
        LOGE("Step error: %s", errmsg.c_str());
        sqlite3_finalize(stmt);
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_finalize(stmt);
    return nullptr;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_shoppinglistapp_MainActivity_getShops(JNIEnv *env, jobject) {
    if (!db) {
        LOGE("Database not initialized");
        return nullptr;
    }
    std::string sql = "SELECT id, name FROM shops;";
    logs.push_back(sql);
    LOGD("Preparing SQL: %s", sql.c_str());
    sqlite3_stmt *stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in prepare: " + errmsg);
        LOGE("Prepare error: %s", errmsg.c_str());
        return nullptr;
    }
    std::vector<std::string> results;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        std::string row;
        int col_count = sqlite3_column_count(stmt);
        for (int i = 0; i < col_count; i++) {
            const char *val = (const char *)sqlite3_column_text(stmt, i);
            row += (val ? val : "");
            if (i < col_count - 1) row += ":";
        }
        results.push_back(row);
    }
    if (rc != SQLITE_DONE) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in step: " + errmsg);
        LOGE("Step error: %s", errmsg.c_str());
        sqlite3_finalize(stmt);
        return nullptr;
    }
    sqlite3_finalize(stmt);
    jobjectArray ret = env->NewObjectArray(results.size(), env->FindClass("java/lang/String"), nullptr);
    for (size_t i = 0; i < results.size(); ++i) {
        env->SetObjectArrayElement(ret, i, env->NewStringUTF(results[i].c_str()));
    }
    return ret;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_shoppinglistapp_MainActivity_deleteAll(JNIEnv *env, jobject) {
    if (!db) {
        LOGE("Database not initialized");
        return env->NewStringUTF("Database not initialized");
    }
    std::string err = executeWithLog("DELETE FROM products");
    if (!err.empty()) return env->NewStringUTF(err.c_str());
    err = executeWithLog("DELETE FROM shops");
    return env->NewStringUTF(err.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_shoppinglistapp_MainActivity_executeSql(JNIEnv *env, jobject, jstring sql) {
    if (!db) {
        LOGE("Database not initialized");
        return env->NewStringUTF("Database not initialized");
    }
    const char *sqlStr = env->GetStringUTFChars(sql, nullptr);
    std::string err = executeWithLog(sqlStr);
    env->ReleaseStringUTFChars(sql, sqlStr);
    return env->NewStringUTF(err.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_shoppinglistapp_ShopActivity_addProduct(JNIEnv *env, jobject, jint shopId, jstring name, jdouble price, jstring desc) {
    if (!db) {
        LOGE("Database not initialized");
        return env->NewStringUTF("Database not initialized");
    }
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const char *descStr = env->GetStringUTFChars(desc, nullptr);
    std::string sql = "INSERT INTO products (shop_id, name, price, description) VALUES (?, ?, ?, ?);";
    logs.push_back(sql);
    LOGD("Preparing SQL: %s", sql.c_str());
    sqlite3_stmt *stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in prepare: " + errmsg);
        LOGE("Prepare error: %s", errmsg.c_str());
        env->ReleaseStringUTFChars(name, nameStr);
        env->ReleaseStringUTFChars(desc, descStr);
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_bind_int(stmt, 1, shopId);
    sqlite3_bind_text(stmt, 2, nameStr, -1, SQLITE_TRANSIENT);
    sqlite3_bind_double(stmt, 3, price);
    if (descStr == nullptr || strlen(descStr) == 0) {
        sqlite3_bind_null(stmt, 4);
    } else {
        sqlite3_bind_text(stmt, 4, descStr, -1, SQLITE_TRANSIENT);
    }
    env->ReleaseStringUTFChars(name, nameStr);
    env->ReleaseStringUTFChars(desc, descStr);
    rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in step: " + errmsg);
        LOGE("Step error: %s", errmsg.c_str());
        sqlite3_finalize(stmt);
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_finalize(stmt);
    return nullptr;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_shoppinglistapp_ShopActivity_getProducts(JNIEnv *env, jobject, jint shopId, jstring sort, jstring search) {
    if (!db) {
        LOGE("Database not initialized");
        return nullptr;
    }
    const char *sortStr = env->GetStringUTFChars(sort, nullptr);
    const char *searchStr = env->GetStringUTFChars(search, nullptr);
    std::string sql = "SELECT id, name, price, bought, description FROM products WHERE shop_id = ?";
    std::string search_val = searchStr ? searchStr : "";
    if (!search_val.empty()) {
        sql += " AND name LIKE ?";
    }
    sql += " ORDER BY " + std::string(sortStr) + ";";
    logs.push_back(sql);
    LOGD("Preparing SQL: %s", sql.c_str());
    sqlite3_stmt *stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in prepare: " + errmsg);
        LOGE("Prepare error: %s", errmsg.c_str());
        env->ReleaseStringUTFChars(sort, sortStr);
        env->ReleaseStringUTFChars(search, searchStr);
        return nullptr;
    }
    sqlite3_bind_int(stmt, 1, shopId);
    int param_idx = 2;
    if (!search_val.empty()) {
        std::string like = "%" + search_val + "%";
        sqlite3_bind_text(stmt, param_idx++, like.c_str(), -1, SQLITE_TRANSIENT);
    }
    env->ReleaseStringUTFChars(sort, sortStr);
    env->ReleaseStringUTFChars(search, searchStr);
    std::vector<std::string> results;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        std::string row;
        int col_count = sqlite3_column_count(stmt);
        for (int i = 0; i < col_count; i++) {
            const char *val = (const char *)sqlite3_column_text(stmt, i);
            row += (val ? val : "");
            if (i < col_count - 1) row += ":";
        }
        results.push_back(row);
    }
    if (rc != SQLITE_DONE) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in step: " + errmsg);
        LOGE("Step error: %s", errmsg.c_str());
        sqlite3_finalize(stmt);
        return nullptr;
    }
    sqlite3_finalize(stmt);
    jobjectArray ret = env->NewObjectArray(results.size(), env->FindClass("java/lang/String"), nullptr);
    for (size_t i = 0; i < results.size(); ++i) {
        env->SetObjectArrayElement(ret, i, env->NewStringUTF(results[i].c_str()));
    }
    return ret;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_shoppinglistapp_ShopActivity_updateProduct(JNIEnv *env, jobject, jint id, jstring name, jdouble price, jint bought, jstring desc) {
    if (!db) {
        LOGE("Database not initialized");
        return env->NewStringUTF("Database not initialized");
    }
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const char *descStr = env->GetStringUTFChars(desc, nullptr);
    std::string sql = "UPDATE products SET name = ?, price = ?, bought = ?, description = ? WHERE id = ?;";
    logs.push_back(sql);
    LOGD("Preparing SQL: %s", sql.c_str());
    sqlite3_stmt *stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in prepare: " + errmsg);
        LOGE("Prepare error: %s", errmsg.c_str());
        env->ReleaseStringUTFChars(name, nameStr);
        env->ReleaseStringUTFChars(desc, descStr);
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_bind_text(stmt, 1, nameStr, -1, SQLITE_TRANSIENT);
    sqlite3_bind_double(stmt, 2, price);
    sqlite3_bind_int(stmt, 3, bought);
    if (descStr == nullptr || strlen(descStr) == 0) {
        sqlite3_bind_null(stmt, 4);
    } else {
        sqlite3_bind_text(stmt, 4, descStr, -1, SQLITE_TRANSIENT);
    }
    sqlite3_bind_int(stmt, 5, id);
    env->ReleaseStringUTFChars(name, nameStr);
    env->ReleaseStringUTFChars(desc, descStr);
    rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in step: " + errmsg);
        LOGE("Step error: %s", errmsg.c_str());
        sqlite3_finalize(stmt);
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_finalize(stmt);
    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_shoppinglistapp_ShopActivity_deleteProduct(JNIEnv *env, jobject, jint id) {
    if (!db) {
        LOGE("Database not initialized");
        return env->NewStringUTF("Database not initialized");
    }
    std::string sql = "DELETE FROM products WHERE id = ?;";
    logs.push_back(sql);
    LOGD("Preparing SQL: %s", sql.c_str());
    sqlite3_stmt *stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in prepare: " + errmsg);
        LOGE("Prepare error: %s", errmsg.c_str());
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_bind_int(stmt, 1, id);
    rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        std::string errmsg = sqlite3_errmsg(db);
        logs.push_back("ERROR in step: " + errmsg);
        LOGE("Step error: %s", errmsg.c_str());
        sqlite3_finalize(stmt);
        return env->NewStringUTF(errmsg.c_str());
    }
    sqlite3_finalize(stmt);
    return nullptr;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_shoppinglistapp_MainActivity_getLogs(JNIEnv *env, jobject) {
    LOGD("Fetching logs, size: %zu", logs.size());
    jobjectArray ret = env->NewObjectArray(logs.size(), env->FindClass("java/lang/String"), nullptr);
    for (size_t i = 0; i < logs.size(); ++i) {
        env->SetObjectArrayElement(ret, i, env->NewStringUTF(logs[i].c_str()));
    }
    return ret;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_shoppinglistapp_MainActivity_clearLogs(JNIEnv *env, jobject) {
    LOGD("Clearing logs");
    logs.clear();
}