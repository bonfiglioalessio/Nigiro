package com.github.damontecres.wholphin

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.imageLoader
import com.github.damontecres.wholphin.services.BackdropService
import com.github.damontecres.wholphin.ui.coilImageLoaderCreated
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.model.ServerVersion
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalComposeRuntimeApi::class)
@HiltAndroidApp
class WholphinApplication :
    Application(),
    Configuration.Provider {
    init {
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(
                object : Timber.Tree() {
                    override fun isLoggable(
                        tag: String?,
                        priority: Int,
                    ): Boolean = priority >= Log.INFO

                    override fun log(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?,
                    ) {
                        Log.println(priority, tag ?: "Wholphin", message)
                    }
                },
            )
        }

        Composer.setDiagnosticStackTraceEnabled(BuildConfig.DEBUG)
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy
                    .Builder()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDeathOnNetwork()
                    .build(),
            )
//            StrictMode.setVmPolicy(
//                StrictMode.VmPolicy
//                    .Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .build(),
//            )
        }
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            excludeMatchingSharedPreferencesKeys = listOf()
            reportContent =
                listOf(
                    ReportField.ANDROID_VERSION,
                    ReportField.APP_VERSION_CODE,
                    ReportField.APP_VERSION_NAME,
                    ReportField.BRAND,
                    // ReportField.BUILD_CONFIG,
                    // ReportField.BUILD,
                    ReportField.CUSTOM_DATA,
                    ReportField.LOGCAT,
                    ReportField.PHONE_MODEL,
                    ReportField.PRODUCT,
                    ReportField.REPORT_ID,
                    ReportField.SHARED_PREFERENCES,
                    ReportField.STACK_TRACE,
                    ReportField.USER_COMMENT,
                    ReportField.USER_CRASH_DATE,
                )
            dialog {
                text =
                    "Nigiro has crashed! Would you like to attempt to " +
                    "send a crash report to your Jellyfin server?"
                title = "Nigiro Crash Report"
                positiveButtonText = "Send"
                negativeButtonText = "Do not send"
            }
            reportSendFailureToast = "Crash report failed to send"
            reportSendSuccessToast = "Sent crash report!"
        }
        ACRA.errorReporter.putCustomData("SDK_INT", Build.VERSION.SDK_INT.toString())
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Lazy: building it at startup would also build the ApiClient before the UI needs it
    @Inject
    lateinit var backdropService: dagger.Lazy<BackdropService>

    /**
     * Gives back the image caches when the system runs low on memory or the UI is hidden:
     * on a 2 GB TV the bitmaps would otherwise stay around until the process is killed.
     */
    @Suppress("DEPRECATION") // TRIM_MEMORY_RUNNING_* are still sent on the TV's Android 11
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.i("onTrimMemory level=%s", level)
        val memoryCache = if (coilImageLoaderCreated) imageLoader.memoryCache else null
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            memoryCache?.clear()
            if (::backdropService.isInitialized) backdropService.get().clearColorCache()
        } else if (level >= TRIM_MEMORY_RUNNING_MODERATE) {
            memoryCache?.let { it.trimToSize(it.maxSize / 2) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        if (coilImageLoaderCreated) imageLoader.memoryCache?.clear()
        if (::backdropService.isInitialized) backdropService.get().clearColorCache()
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    companion object {
        lateinit var instance: WholphinApplication
            private set

        val minimumServerVersion: ServerVersion = Jellyfin.minimumVersion

        val newMinimumServerVersion: ServerVersion = ServerVersion(12, 0, 0)
    }
}
