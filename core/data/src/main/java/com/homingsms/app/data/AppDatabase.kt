package com.homingsms.app.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.homingsms.app.data.dao.MessagesDao
import com.homingsms.app.data.entities.Message
import com.homingsms.app.data.entities.MessageRecipient
import com.homingsms.app.data.entities.MessageState
import com.homingsms.app.data.entities.RecipientState
import com.homingsms.app.modules.incoming.db.IncomingMessage
import com.homingsms.app.modules.incoming.db.IncomingMessagesDao
import com.homingsms.app.modules.logs.db.LogEntriesDao
import com.homingsms.app.modules.logs.db.LogEntry
import com.homingsms.core.data.db.ApiInboxDao
import com.homingsms.core.data.db.ApiKeyEntity
import com.homingsms.core.data.db.ApiKeysDao
import com.homingsms.core.data.db.ApiMessagesDao
import com.homingsms.core.data.db.CloudInboxOutboxEntity
import com.homingsms.core.data.db.CloudOutboxDao
import com.homingsms.core.data.db.CloudStateReportEntity
import com.homingsms.core.data.db.MessageApiMeta
import com.homingsms.core.data.db.StatsDao
import com.homingsms.core.data.db.WebhookDeliveriesDao
import com.homingsms.core.data.db.WebhookDeliveryEntity
import com.homingsms.core.data.db.WebhookEndpointEntity
import com.homingsms.core.data.db.WebhookEndpointsDao

@Database(
    entities = [
        Message::class,
        MessageRecipient::class,
        RecipientState::class,
        MessageState::class,
        LogEntry::class,
        IncomingMessage::class,
        MessageApiMeta::class,
        ApiKeyEntity::class,
        WebhookEndpointEntity::class,
        WebhookDeliveryEntity::class,
        CloudStateReportEntity::class,
        CloudInboxOutboxEntity::class
    ],
    version = 26,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        // AutoMigration(from = 7, to = 8),  // manual migration
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        // AutoMigration(from = 13, to = 14),   // manual migration
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21),
//        AutoMigration(from = 21, to = 22), // manual migration
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messagesDao(): MessagesDao
    abstract fun logDao(): LogEntriesDao
    abstract fun incomingMessagesDao(): IncomingMessagesDao
    abstract fun apiMessagesDao(): ApiMessagesDao
    abstract fun apiInboxDao(): ApiInboxDao
    abstract fun apiKeysDao(): ApiKeysDao
    abstract fun webhookEndpointsDao(): WebhookEndpointsDao
    abstract fun webhookDeliveriesDao(): WebhookDeliveriesDao
    abstract fun cloudOutboxDao(): CloudOutboxDao
    abstract fun statsDao(): StatsDao

    companion object {
        fun getDatabase(context: android.content.Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gateway"
        )
            .addMigrations(
                MIGRATION_7_8,
                MIGRATION_13_14,
                MIGRATION_21_22,
                MIGRATION_25_26
            )
            .allowMainThreadQueries()
            .build()
    }
}
