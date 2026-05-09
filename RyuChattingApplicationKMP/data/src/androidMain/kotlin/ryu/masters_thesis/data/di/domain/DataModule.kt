package ryu.masters_thesis.data.di.domain

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import ryu.masters_thesis.data.vault.data.AppDatabase
import ryu.masters_thesis.data.vault.domain.MessageVault
import ryu.masters_thesis.data.vault.domain.RoomConfigVault
import ryu.masters_thesis.data.vault.implementation.MessageVaultImpl
import ryu.masters_thesis.data.vault.implementation.RoomConfigVaultImpl
import ryu.masters_thesis.data.vault.implementation.VaultCipher

//rename domain to implementation? -- TODO
actual fun dataPlatformModule(): Module = module {
    single<VaultCipher> {
        VaultCipher()
    }
    single<AppDatabase> {
        Room.databaseBuilder<AppDatabase>(
            context = get<Context>(),
            name    = get<Context>().getDatabasePath("vault.db").absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single<MessageVault> {
        MessageVaultImpl(db = get(), cipher = get())
    }
    single<RoomConfigVault> {
        RoomConfigVaultImpl(db = get(), cipher = get())
    }
}