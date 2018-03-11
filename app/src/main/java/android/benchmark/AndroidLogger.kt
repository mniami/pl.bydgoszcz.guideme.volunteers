package android.benchmark

import android.benchmark.log.LogLevel
import android.benchmark.log.Logger
import android.util.Log

class AndroidLogger : Logger {
    override fun log(logLevel: LogLevel, category: String?, message: String) {
        when(logLevel){
            LogLevel.DEBUG -> Log.d(category, message)
            LogLevel.INFO -> Log.i(category, message)
            LogLevel.WARNING-> Log.w(category, message)
        }
    }
}
