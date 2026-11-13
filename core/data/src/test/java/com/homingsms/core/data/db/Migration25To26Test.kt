package com.homingsms.core.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homingsms.app.data.AppDatabase
import com.homingsms.app.data.MIGRATION_25_26
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration25To26Test {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun createVersion25(name: String): File {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        val schema = Json.parseToJsonElement(File(schemaDir(), "25.json").readText()).jsonObject["database"]!!.jsonObject
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema["entities"]!!.jsonArray.forEach { entity ->
                val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
                db.execSQL(entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                entity.jsonObject["indices"]?.jsonArray?.forEach { index ->
                    db.execSQL(index.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL(
                "INSERT INTO incoming_messages (id, type, sender, recipient, simNumber, subscriptionId, contentPreview, createdAt) " +
                    "VALUES ('text:1', 'SMS', '+15550001', NULL, 1, 3, 'Hello there', 1000), " +
                    "('data:2', 'DATA_SMS', '+15550002', NULL, NULL, NULL, '0102', 2000)"
            )
            db.execSQL(
                "INSERT INTO WebHook (id, url, event, source) VALUES " +
                    "('w1', 'https://hooks.example.com/a', 'sms:received', 'Local'), " +
                    "('w2', 'https://hooks.example.com/a', 'sms:delivered', 'Local'), " +
                    "('w3', 'https://hooks.example.com/b', 'system:ping', 'Local'), " +
                    "('w4', 'https://cloud.example.com/c', 'sms:sent', 'Cloud')"
            )
            db.execSQL(
                "INSERT INTO Message (id, withDeliveryReport, simNumber, validUntil, scheduleAt, isEncrypted, skipPhoneValidation, " +
                    "priority, source, type, content, state, partsCount, createdAt, processedAt) VALUES " +
                    "('m1', 1, NULL, NULL, NULL, 0, 0, 0, 'Local', 'Text', '{\"text\":\"hi\"}', 'Sent', 1, 500, 600)"
            )
            db.execSQL("INSERT INTO tokens (id, expiresAt, tokenUse) VALUES ('t1', 1, 'access')")
            db.version = VERSION_25
        }
        return file
    }

    @Test
    fun migratesData() {
        createVersion25(NAME)
        val database = Room.databaseBuilder(context, AppDatabase::class.java, NAME)
            .addMigrations(MIGRATION_25_26)
            .allowMainThreadQueries()
            .build()
        val sqlite = database.openHelper.writableDatabase
        assertEquals(26, sqlite.version)

        runBlocking {
            val sms = database.apiInboxDao().getByLocalIds(listOf("text:1")).single()
            assertTrue(sms.publicId.startsWith("inb_"))
            assertEquals("Hello there", sms.text)
            assertEquals(1000, sms.insertedAt)
            val data = database.apiInboxDao().getByLocalIds(listOf("data:2")).single()
            assertNull(data.text)
            assertFalse(data.publicId == sms.publicId)

            val endpoints = database.webhookEndpointsDao().all()
            assertEquals(1, endpoints.size)
            assertEquals("https://hooks.example.com/a", endpoints.single().url)
            assertEquals("message.delivered,inbox.received", endpoints.single().events)
            assertTrue(endpoints.single().enabled)

            assertEquals("m1", database.apiMessagesDao().get("m1")?.message?.id)
        }
        listOf("tokens", "device_keys", "webhook_queue", "gateway__inbox_upload", "WebHook").forEach { table ->
            sqlite.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'").use { assertEquals(0, it.count) }
        }
        database.close()
    }

    @Test
    fun freshDatabaseOpensAtLatestVersion() {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        assertEquals(26, database.openHelper.writableDatabase.version)
        database.close()
    }

    private fun schemaDir(): File = File(System.getProperty("room.schemaDir"), "com.homingsms.app.data.AppDatabase")

    private companion object {
        const val NAME = "migration-test"
        const val VERSION_25 = 25
    }
}
