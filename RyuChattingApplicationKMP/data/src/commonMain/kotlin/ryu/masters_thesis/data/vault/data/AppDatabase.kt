package ryu.masters_thesis.data.vault.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VaultEntry::class, RoomMetadata::class, RoomConfigEntity::class],
    version  = 6,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao():      MessageDao
    abstract fun roomMetadataDao(): RoomMetadataDao
    abstract fun roomConfigDao():   RoomConfigDao
}