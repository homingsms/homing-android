package com.homingsms.app.providers

interface IPProvider {
    suspend fun getIP(): String?
}
