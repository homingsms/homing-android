package com.homingsms.app.modules.mms

import com.homingsms.core.data.repository.AttachmentReader
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mmsModule = module {
    singleOf(::MmsAttachmentStorage)
    singleOf(::MmsSender)
    single<AttachmentReader> { MmsAttachmentReader(get()) }
}

internal const val MODULE_NAME = "mms"
