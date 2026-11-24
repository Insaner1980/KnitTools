package com.finnvek.knittools.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.knittools.data.datastore.editPreferencesSafely
import com.finnvek.knittools.data.datastore.readPreferencesOrNull
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Viimeisin Playn vahvistama omistustila. Pitää ostetun Pron käytettävissä, kun Play-kysely
 * epäonnistuu käynnistyksessä; seuraava onnistunut kysely korvaa arvon (myös hyvityksen jälkeen).
 */
interface PurchaseOwnershipCache {
    suspend fun read(): Boolean

    suspend fun write(owned: Boolean)

    object None : PurchaseOwnershipCache {
        override suspend fun read(): Boolean = false

        override suspend fun write(owned: Boolean) = Unit
    }
}

@Singleton
class DataStorePurchaseOwnershipCache
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PurchaseOwnershipCache {
        override suspend fun read(): Boolean = context.ownershipDataStore.readPreferencesOrNull()?.get(KEY_PRO_OWNED) == true

        override suspend fun write(owned: Boolean) {
            context.ownershipDataStore.editPreferencesSafely { preferences -> preferences[KEY_PRO_OWNED] = owned }
        }

        private companion object {
            val KEY_PRO_OWNED = booleanPreferencesKey("pro_owned")
            val Context.ownershipDataStore by preferencesDataStore(name = "billing_ownership")
        }
    }

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PurchaseOwnershipCacheModule {
    @Binds
    abstract fun bindPurchaseOwnershipCache(cache: DataStorePurchaseOwnershipCache): PurchaseOwnershipCache
}
