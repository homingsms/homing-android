package com.homingsms.app.helpers

import com.homingsms.app.BuildConfig

object BuildHelper {
    val isInsecureVersion =
        BuildConfig.BUILD_TYPE == "insecure" || BuildConfig.BUILD_TYPE == "debugInsecure"
}
