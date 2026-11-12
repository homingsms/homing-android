package com.homingsms.app.modules.logs.repositories

import androidx.lifecycle.distinctUntilChanged
import com.homingsms.app.modules.logs.db.LogEntriesDao

class LogsRepository(
    private val dao: LogEntriesDao
) {
    val lastEntries = dao.selectLast().distinctUntilChanged()
}
