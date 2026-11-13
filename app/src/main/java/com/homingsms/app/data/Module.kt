package com.homingsms.app.data

import com.homingsms.core.data.repository.ApiKeyRepository
import com.homingsms.core.data.repository.InboxRecords
import com.homingsms.core.data.repository.MessageRecords
import com.homingsms.core.data.repository.StatsRepository
import org.koin.dsl.module

val dbModule = module {
    single { AppDatabase.getDatabase(get()) }
    single { get<AppDatabase>().messagesDao() }
    single { get<AppDatabase>().incomingMessagesDao() }
    single { get<AppDatabase>().logDao() }
    single { get<AppDatabase>().apiMessagesDao() }
    single { get<AppDatabase>().apiInboxDao() }
    single { get<AppDatabase>().apiKeysDao() }
    single { get<AppDatabase>().webhookEndpointsDao() }
    single { get<AppDatabase>().webhookDeliveriesDao() }
    single { get<AppDatabase>().cloudOutboxDao() }
    single { get<AppDatabase>().statsDao() }

    single { MessageRecords(get()) }
    single { InboxRecords(get(), get()) }
    single { ApiKeyRepository(get(), get()) }
    single { StatsRepository(get()) }
}
