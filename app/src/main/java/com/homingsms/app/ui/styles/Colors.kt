package com.homingsms.app.ui.styles

import android.graphics.Color

val com.homingsms.app.domain.ProcessingState.color: Int
    get() = when (this) {
        com.homingsms.app.domain.ProcessingState.Pending -> Color.parseColor("#FFBB86FC")
        com.homingsms.app.domain.ProcessingState.Cancelled -> Color.parseColor("#FF9E9E9E")
        com.homingsms.app.domain.ProcessingState.Processed -> Color.parseColor("#FF6200EE")
        com.homingsms.app.domain.ProcessingState.Sent -> Color.parseColor("#FF3700B3")
        com.homingsms.app.domain.ProcessingState.Delivered -> Color.parseColor("#FF03DAC5")
        com.homingsms.app.domain.ProcessingState.Failed -> Color.parseColor("#FF018786")
    }
