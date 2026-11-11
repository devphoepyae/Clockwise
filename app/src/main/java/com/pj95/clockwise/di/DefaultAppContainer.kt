package com.pj95.clockwise.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.pj95.clockwise.alarm.AndroidAlarmScheduler
import com.pj95.clockwise.alarm.AndroidTimerAlerting
import com.pj95.clockwise.alarm.AndroidTimerScheduler
import com.pj95.clockwise.billing.PlayBillingClient
import com.pj95.clockwise.billing.StudioBilling
import com.pj95.clockwise.data.local.ClockwiseDatabase
import com.pj95.clockwise.data.prefs.SavedCities
import com.pj95.clockwise.data.prefs.UserPreferencesSerializer
import com.pj95.clockwise.data.repository.AlarmRepository
import com.pj95.clockwise.data.repository.CityRepository
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.data.repository.RoomAlarmRepository
import com.pj95.clockwise.data.repository.RoomCityOrderStore
import com.pj95.clockwise.data.repository.RoomTimerRepository
import com.pj95.clockwise.data.repository.SolarRepository
import com.pj95.clockwise.data.repository.TimerRepository
import com.pj95.clockwise.data.repository.WidgetConfigRepository
import com.pj95.clockwise.data.repository.WidgetLicenseRepository
import com.pj95.clockwise.common.time.SecondTicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock

/**
 * Default [AppContainer] used at runtime.
 *
 * Lives in `:app` because it is the composition root: it is the only place that
 * knows how the database, DataStore and billing client are constructed.
 */
class DefaultAppContainer(
    override val applicationContext: Context,
) : AppContainer {

    private val userPreferencesDataStore: DataStore<SavedCities> by lazy {
        DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { SavedCities.getDefaultInstance() },
            produceFile = { applicationContext.dataStoreFile(DATA_STORE_FILE_NAME) },
        )
    }

    override val clockRepository: ClockRepository by lazy {
        ClockRepository(
            dataStore = userPreferencesDataStore,
            cityOrder = RoomCityOrderStore(database.savedCityDao()),
        )
    }

    override val clock: Clock = Clock.systemUTC()

    /** Opened lazily; the first screen never waits for database creation. */
    private val database: ClockwiseDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ClockwiseDatabase::class.java,
            ClockwiseDatabase.NAME,
        )
            .addMigrations(
                ClockwiseDatabase.MIGRATION_1_2,
                ClockwiseDatabase.MIGRATION_2_3,
                ClockwiseDatabase.MIGRATION_3_4,
                ClockwiseDatabase.MIGRATION_4_5,
                ClockwiseDatabase.MIGRATION_5_6,
                ClockwiseDatabase.MIGRATION_6_7,
                ClockwiseDatabase.MIGRATION_7_8,
                ClockwiseDatabase.MIGRATION_8_9,
                ClockwiseDatabase.MIGRATION_9_10,
            )
            .build()
    }

    override val cityRepository: CityRepository by lazy {
        CityRepository(database.cityDao())
    }

    override val solarRepository: SolarRepository by lazy {
        SolarRepository(database.solarDao(), clock)
    }

    override val alarmRepository: AlarmRepository by lazy {
        RoomAlarmRepository(
            dao = database.alarmDao(),
            scheduling = AndroidAlarmScheduler(applicationContext, clock),
            clock = clock,
        )
    }

    override val timerRepository: TimerRepository by lazy {
        RoomTimerRepository(
            dao = database.timerDao(),
            scheduling = AndroidTimerScheduler(applicationContext),
            alerting = AndroidTimerAlerting(applicationContext),
            clock = clock,
        )
    }

    override val widgetLicenseRepository: WidgetLicenseRepository by lazy {
        WidgetLicenseRepository(database.widgetDao(), clock)
    }

    override val widgetConfigRepository: WidgetConfigRepository by lazy {
        WidgetConfigRepository(database.widgetDao())
    }

    private val billingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val studioBilling: StudioBilling by lazy {
        PlayBillingClient(
            context = applicationContext,
            scope = billingScope,
            onEntitlement = { widgetLicenseRepository.unlockAllPremium() },
        )
    }

    override val secondTicker: SecondTicker by lazy { SecondTicker(clock) }

    private companion object {
        const val DATA_STORE_FILE_NAME = "user_preferences.pb"
    }
}
