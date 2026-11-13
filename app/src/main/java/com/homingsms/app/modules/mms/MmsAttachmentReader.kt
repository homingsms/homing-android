package com.homingsms.app.modules.mms

import com.homingsms.core.data.repository.AttachmentReader
import com.homingsms.core.model.InboxAttachmentContent
import com.homingsms.core.model.InboxAttachmentInfo

class MmsAttachmentReader(private val storage: MmsAttachmentStorage) : AttachmentReader {
    override fun list(localId: String): List<InboxAttachmentInfo> = storage.list(localId)
        .sortedBy { it.partId }
        .map { InboxAttachmentInfo(it.partId.toString(), it.contentType, it.displayName, it.file.length()) }

    override fun read(localId: String, attachmentId: String): InboxAttachmentContent? {
        val partId = attachmentId.toLongOrNull() ?: return null
        val attachment = storage.find(localId, partId) ?: return null
        if (!attachment.file.isFile) return null
        return InboxAttachmentContent(attachment.contentType, attachment.displayName, attachment.file.readBytes())
    }
}
