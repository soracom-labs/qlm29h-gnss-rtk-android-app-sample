package jp.co.soracom.qlm29hrtk.service

import android.content.Context
import android.content.Intent

interface ForegroundController {
    fun start()
    fun stop()
}

class AndroidForegroundController(context: Context) : ForegroundController {
    private val appContext = context.applicationContext
    override fun start() {
        appContext.startForegroundService(Intent(appContext, RtkForegroundService::class.java))
    }
    override fun stop() {
        appContext.stopService(Intent(appContext, RtkForegroundService::class.java))
    }
}
