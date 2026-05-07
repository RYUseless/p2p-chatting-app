package ryu.masters_thesis.data.vault.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VaultEntry::class, RoomMetadata::class],
    version  = 4,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun roomMetadataDao(): RoomMetadataDao
}