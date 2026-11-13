package com.homingsms.app.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.homingsms.core.model.Ids
import com.homingsms.core.model.WebhookEventType

internal object Migration25To26 {
    private val createStatements = listOf(
        "CREATE TABLE IF NOT EXISTS `message_api_meta` (`messageId` TEXT NOT NULL, `idempotencyKey` TEXT, `requestHash` TEXT, " +
            "`metadata` TEXT, PRIMARY KEY(`messageId`), FOREIGN KEY(`messageId`) REFERENCES `Message`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_message_api_meta_idempotencyKey` ON `message_api_meta` (`idempotencyKey`)",
        "CREATE TABLE IF NOT EXISTS `api_keys` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `prefix` TEXT NOT NULL, `hash` TEXT NOT NULL, " +
            "`scopes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `lastUsedAt` INTEGER, `revokedAt` INTEGER, PRIMARY KEY(`id`))",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_api_keys_hash` ON `api_keys` (`hash`)",
        "CREATE TABLE IF NOT EXISTS `webhook_endpoints` (`id` TEXT NOT NULL, `url` TEXT NOT NULL, `description` TEXT, " +
            "`events` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `webhook_deliveries` (`id` TEXT NOT NULL, `endpointId` TEXT NOT NULL, `eventId` TEXT NOT NULL, " +
            "`eventType` TEXT NOT NULL, `payload` TEXT NOT NULL, `status` TEXT NOT NULL, `attempts` INTEGER NOT NULL, " +
            "`lastStatusCode` INTEGER, `lastResponseBody` TEXT, `lastError` TEXT, `nextAttemptAt` INTEGER, `deliveredAt` INTEGER, " +
            "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`endpointId`) " +
            "REFERENCES `webhook_endpoints`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_webhook_deliveries_endpointId_createdAt` ON `webhook_deliveries` (`endpointId`, `createdAt`)",
        "CREATE INDEX IF NOT EXISTS `index_webhook_deliveries_status_nextAttemptAt` ON `webhook_deliveries` (`status`, `nextAttemptAt`)",
        "CREATE TABLE IF NOT EXISTS `cloud_state_outbox` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `messageId` TEXT NOT NULL, " +
            "`state` TEXT NOT NULL, `at` INTEGER NOT NULL, `simSlot` INTEGER, `partsCount` INTEGER, `errorCode` TEXT, " +
            "`errorMessage` TEXT, `attempts` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_cloud_state_outbox_messageId` ON `cloud_state_outbox` (`messageId`)",
        "CREATE TABLE IF NOT EXISTS `cloud_inbox_outbox` (`localId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
            "`attempts` INTEGER NOT NULL, `lastError` TEXT, PRIMARY KEY(`localId`))"
    )

    private val incomingColumns = listOf(
        "ALTER TABLE `incoming_messages` ADD COLUMN `publicId` TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE `incoming_messages` ADD COLUMN `text` TEXT",
        "ALTER TABLE `incoming_messages` ADD COLUMN `data` TEXT",
        "ALTER TABLE `incoming_messages` ADD COLUMN `port` INTEGER",
        "ALTER TABLE `incoming_messages` ADD COLUMN `subject` TEXT",
        "ALTER TABLE `incoming_messages` ADD COLUMN `insertedAt` INTEGER NOT NULL DEFAULT 0"
    )

    private val legacyEvents = mapOf(
        "sms:received" to WebhookEventType.InboxReceived,
        "sms:data-received" to WebhookEventType.InboxReceived,
        "mms:received" to WebhookEventType.InboxReceived,
        "mms:downloaded" to WebhookEventType.InboxReceived,
        "sms:sent" to WebhookEventType.MessageSent,
        "sms:delivered" to WebhookEventType.MessageDelivered,
        "sms:failed" to WebhookEventType.MessageFailed
    )

    private val droppedTables = listOf("webhook_queue", "gateway__inbox_upload", "tokens", "device_keys", "WebHook")

    fun migrate(database: SupportSQLiteDatabase) {
        createStatements.forEach(database::execSQL)
        incomingColumns.forEach(database::execSQL)
        database.execSQL(
            "UPDATE `incoming_messages` SET `insertedAt` = `createdAt`, " +
                "`text` = CASE WHEN `type` = 'SMS' THEN `contentPreview` ELSE NULL END"
        )
        backfillPublicIds(database)
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_incoming_messages_publicId` ON `incoming_messages` (`publicId`)")
        migrateWebhooks(database)
        droppedTables.forEach { database.execSQL("DROP TABLE IF EXISTS `$it`") }
    }

    private fun backfillPublicIds(database: SupportSQLiteDatabase) {
        val ids = mutableListOf<String>()
        database.query("SELECT `id` FROM `incoming_messages`").use { cursor ->
            while (cursor.moveToNext()) ids += cursor.getString(0)
        }
        ids.forEach { id ->
            database.execSQL("UPDATE `incoming_messages` SET `publicId` = ? WHERE `id` = ?", arrayOf<Any>(Ids.inbox(), id))
        }
    }

    private fun migrateWebhooks(database: SupportSQLiteDatabase) {
        val byUrl = linkedMapOf<String, MutableSet<WebhookEventType>>()
        database.query("SELECT `url`, `event` FROM `WebHook` WHERE `source` = 'Local'").use { cursor ->
            while (cursor.moveToNext()) {
                val event = legacyEvents[cursor.getString(1)] ?: continue
                byUrl.getOrPut(cursor.getString(0)) { linkedSetOf() } += event
            }
        }
        val now = System.currentTimeMillis()
        byUrl.forEach { (url, events) ->
            database.execSQL(
                "INSERT INTO `webhook_endpoints` (`id`, `url`, `description`, `events`, `enabled`, `createdAt`, `updatedAt`) " +
                    "VALUES (?, ?, ?, ?, 1, ?, ?)",
                arrayOf<Any?>(Ids.webhook(), url, "Migrated", events.sortedBy { it.ordinal }.joinToString(",") { it.value }, now, now)
            )
        }
    }
}
