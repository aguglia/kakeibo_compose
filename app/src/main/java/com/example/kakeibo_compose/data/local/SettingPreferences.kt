package com.example.kakeibo_compose.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 💡 Contextの拡張プロパティとしてDataStoreを定義（シングルトンとして安全に扱うため）
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingPreferences(private val context: Context) {

    companion object {
        // 💡 最低維持資産のキー（Int型）を定義。初期値用に使うキーです
        private val MINIMUM_ASSET_KEY = intPreferencesKey("minimum_asset")
        const val DEFAULT_MINIMUM_ASSET = 0 // 💡 ここに共通の初期値を定義しておく！
    }

    // 💡 最低維持資産を取得するFlow（未設定なら初期値として 0 円を返す）
    val minimumAsset: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[MINIMUM_ASSET_KEY] ?: DEFAULT_MINIMUM_ASSET
    }

    // 💡 最低維持資産を上書き保存する関数
    suspend fun saveMinimumAsset(amount: Int) {
        context.dataStore.edit { preferences ->
            preferences[MINIMUM_ASSET_KEY] = amount
        }
    }
}