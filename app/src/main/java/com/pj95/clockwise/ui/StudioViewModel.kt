package com.pj95.clockwise.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pj95.clockwise.billing.BillingUiState
import com.pj95.clockwise.billing.StudioBilling
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.data.repository.WidgetConfig
import com.pj95.clockwise.data.repository.WidgetConfigRepository
import com.pj95.clockwise.data.repository.WidgetLicenseRepository
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.widget.LicenseState
import com.pj95.clockwise.model.widget.WidgetLicense
import com.pj95.clockwise.model.widget.WidgetStyleId
import com.pj95.clockwise.widget.ChronosWidget
import com.pj95.clockwise.widget.WidgetPinManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/** One style as the gallery shows it. */
data class StudioStyleItem(
    val style: WidgetStyleId,
    val license: LicenseState,
    val canStartTrial: Boolean,
    val pinnedCount: Int,
)

/** One widget already on a home screen, with the cities it is showing. */
data class StudioPinnedWidget(
    val appWidgetId: Int,
    val style: WidgetStyleId,
    val cityIds: List<String>,
    val cityNames: List<String>,
)

/** Everything the gallery renders, in one frozen snapshot. */
data class StudioUiState(
    val now: Instant,
    val use24Hour: Boolean,
    val styles: List<StudioStyleItem>,
    val pinned: List<StudioPinnedWidget>,
    val billing: BillingUiState,
)

/** A one-shot result the screen turns into a snackbar or a sheet. */
enum class StudioMessage {
    TRIAL_STARTED,
    TRIAL_ALREADY_USED,
    PIN_REQUESTED,
    PIN_UNAVAILABLE,
    CITIES_SAVED,
    PURCHASE_COMPLETE,
    RESTORED,
    NOTHING_TO_RESTORE,
    PURCHASE_CANCELLED,
    PURCHASE_FAILED,
    BILLING_UNAVAILABLE,
}

/**
 * Drives the Chronos Studio gallery.
 *
 * Licensing is read from Room, never from Play: the billing client only writes
 * the purchase down. That is what lets a user who bought the unlock see every
 * premium style on a plane, and what keeps a trial honest across a reinstall
 * (Room is restored from the app's own backup, not from the network).
 *
 * The one-second ticker is deliberate: the dial preview and the card previews
 * carry live times, and the trial countdown is displayed in hours and minutes.
 *
 * The context is the application context (see [Factory]), which lives for the
 * whole process, so holding it here cannot leak an Activity.
 */
@SuppressLint("StaticFieldLeak")
class StudioViewModel(
    private val context: Context,
    private val licenseRepository: WidgetLicenseRepository,
    private val configRepository: WidgetConfigRepository,
    private val clockRepository: ClockRepository,
    private val billing: StudioBilling,
    private val clock: Clock,
    private val ticker: Flow<Instant>,
) : ViewModel() {

    private val message = kotlinx.coroutines.flow.MutableStateFlow<StudioMessage?>(null)
    val messages: StateFlow<StudioMessage?> = message.asStateFlow()

    val uiState: StateFlow<StudioUiState> = combine(
        clockRepository.preferences,
        licenseRepository.licenses,
        configRepository.configs,
        billing.state,
        ticker,
    ) { preferences, licenses, configs, billingState, now ->
        StudioUiState(
            now = now,
            use24Hour = preferences.use24Hour,
            styles = licenses.toGallery(now.toEpochMilli(), configs),
            pinned = configs.map { it.toPinned() },
            billing = billingState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StudioUiState(
            now = clock.instant(),
            use24Hour = false,
            styles = WidgetStyleId.entries.map { style ->
                StudioStyleItem(
                    style = style,
                    license = if (style.isPremium) LicenseState.Locked else LicenseState.Free,
                    canStartTrial = style.isPremium,
                    pinnedCount = 0,
                )
            },
            pinned = emptyList(),
            billing = BillingUiState(),
        ),
    )

    init {
        // Connecting here — rather than on every recomposition — means opening
        // the gallery is the only thing that ever talks to Play.
        billing.connect()
    }

    fun startTrial(style: WidgetStyleId) {
        viewModelScope.launch {
            val started = licenseRepository.startTrial(style)
            message.value = if (started) {
                StudioMessage.TRIAL_STARTED
            } else {
                StudioMessage.TRIAL_ALREADY_USED
            }
        }
    }

    fun purchase(activity: Activity) {
        billing.purchase(activity)
    }

    fun restore() {
        billing.restore()
    }

    fun pin(style: WidgetStyleId) {
        viewModelScope.launch {
            val requested = runCatching { WidgetPinManager.pin(context, style) }
                .getOrDefault(false)
            message.value = if (requested) {
                StudioMessage.PIN_REQUESTED
            } else {
                StudioMessage.PIN_UNAVAILABLE
            }
        }
    }

    /** Rewrites the three cities of an existing widget and redraws it. */
    fun updatePinnedCities(appWidgetId: Int, style: WidgetStyleId, cityIds: List<String>) {
        viewModelScope.launch {
            configRepository.save(
                WidgetConfig(
                    appWidgetId = appWidgetId,
                    style = style,
                    cityIds = cityIds,
                    isPremium = style.isPremium,
                ),
            )
            runCatching {
                ChronosWidget(style).update(
                    context,
                    GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId),
                )
            }
            message.value = StudioMessage.CITIES_SAVED
        }
    }

    /** Turns a billing notice into the gallery's own vocabulary. */
    fun noteBillingNotice(notice: com.pj95.clockwise.billing.BillingNotice) {
        message.value = when (notice) {
            com.pj95.clockwise.billing.BillingNotice.PURCHASE_COMPLETE ->
                StudioMessage.PURCHASE_COMPLETE
            com.pj95.clockwise.billing.BillingNotice.RESTORED -> StudioMessage.RESTORED
            com.pj95.clockwise.billing.BillingNotice.NOTHING_TO_RESTORE ->
                StudioMessage.NOTHING_TO_RESTORE
            com.pj95.clockwise.billing.BillingNotice.CANCELLED ->
                StudioMessage.PURCHASE_CANCELLED
            com.pj95.clockwise.billing.BillingNotice.UNAVAILABLE ->
                StudioMessage.BILLING_UNAVAILABLE
            com.pj95.clockwise.billing.BillingNotice.FAILED -> StudioMessage.PURCHASE_FAILED
        }
        billing.consumeNotice()
    }

    fun clearMessage() {
        message.value = null
    }

    override fun onCleared() {
        billing.close()
        super.onCleared()
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("Application is not available")
                StudioViewModel(
                    context = app.applicationContext,
                    licenseRepository = app.appContainer.widgetLicenseRepository,
                    configRepository = app.appContainer.widgetConfigRepository,
                    clockRepository = app.appContainer.clockRepository,
                    billing = app.appContainer.studioBilling,
                    clock = app.appContainer.clock,
                    ticker = app.appContainer.secondTicker.ticker(),
                )
            }
        }
    }
}

private fun List<WidgetLicense>.toGallery(
    nowMillis: Long,
    configs: List<WidgetConfig>,
): List<StudioStyleItem> = map { license ->
    StudioStyleItem(
        style = license.style,
        license = license.state(nowMillis),
        canStartTrial = license.canStartTrial,
        pinnedCount = configs.count { it.style == license.style },
    )
}

private fun WidgetConfig.toPinned(): StudioPinnedWidget = StudioPinnedWidget(
    appWidgetId = appWidgetId,
    style = style,
    cityIds = cityIds,
    cityNames = cityIds.map { id -> CityDirectory.find(id)?.name ?: id },
)
