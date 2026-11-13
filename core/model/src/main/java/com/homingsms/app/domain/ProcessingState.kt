package com.homingsms.app.domain

enum class ProcessingState {
    Pending,
    Cancelled,
    Processed,
    Sent,
    Delivered,
    Failed
}
