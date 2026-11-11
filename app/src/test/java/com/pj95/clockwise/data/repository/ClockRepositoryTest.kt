package com.pj95.clockwise.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.data.prefs.SavedCities
import com.pj95.clockwise.data.prefs.SavedThemeMode
import com.pj95.clockwise.data.prefs.UserPreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

/**
 * Exercises persistence against a real Proto DataStore on a temporary file, so
 * the proto round-trip and the restart path are covered for real.
 */
@RunWith(AndroidJUnit4::class)
class ClockRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private var fileCounter = 0

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun newDataStore(file: File): DataStore<SavedCities> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scopes += scope
        return DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { SavedCities.getDefaultInstance() },
            scope = scope,
            produceFile = { file },
        )
    }

    private fun newFile(): File = File(tempFolder.root, "prefs-${fileCounter++}.pb")

    // --- First launch --------------------------------------------------------

    @Test
    fun firstLaunchSeedsTheDefaultCities() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        val preferences = repository.preferences.first()

        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, preferences.activeCityId)
        assertTrue(preferences.use24Hour)
        // Nothing has been chosen yet, so the first of the five themes applies.
        assertEquals(AppTheme.Default, preferences.theme)
    }

    // --- Add / remove / select ----------------------------------------------

    @Test
    fun addingACityAppendsItAndMakesItActive() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.addCity("paris")

        val preferences = repository.preferences.first()
        assertEquals(CityDirectory.defaultSavedCityIds + "paris", preferences.savedCityIds)
        assertEquals("paris", preferences.activeCityId)
    }

    @Test
    fun duplicateCityIsIgnored() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.addCity("paris")
        repository.addCity("paris")

        val preferences = repository.preferences.first()
        assertEquals(1, preferences.savedCityIds.count { it == "paris" })
        assertEquals(CityDirectory.defaultSavedCityIds.size + 1, preferences.savedCityIds.size)
    }

    @Test
    fun removingTheActiveCityPromotesTheFirstRemainingCity() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.removeCity(CityDirectory.defaultActiveCityId)

        val preferences = repository.preferences.first()
        assertEquals(listOf("tokyo", "sydney", "new-york", "london"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)
    }

    @Test
    fun removingANonActiveCityKeepsTheActiveCity() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.removeCity("tokyo")

        val preferences = repository.preferences.first()
        assertEquals(listOf("sydney", "los-angeles", "new-york", "london"), preferences.savedCityIds)
        assertEquals("los-angeles", preferences.activeCityId)
    }

    @Test
    fun undoingARemovalPutsTheCityBackInItsOldPosition() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))
        val before = repository.preferences.first()

        repository.removeCity("tokyo")
        repository.restoreCity("tokyo", before.savedCityIds, before.activeCityId)

        val after = repository.preferences.first()
        assertEquals(before.savedCityIds, after.savedCityIds)
        assertEquals(before.activeCityId, after.activeCityId)
    }

    @Test
    fun undoingTheRemovalOfTheActiveCityMakesItActiveAgain() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))
        val before = repository.preferences.first()
        val active = requireNotNull(before.activeCityId)

        repository.removeCity(active)
        assertEquals("tokyo", repository.preferences.first().activeCityId)

        repository.restoreCity(active, before.savedCityIds, active)

        val after = repository.preferences.first()
        assertEquals(before.savedCityIds, after.savedCityIds)
        assertEquals(active, after.activeCityId)
    }

    @Test
    fun removingEveryCityLeavesTheEmptyState() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        CityDirectory.defaultSavedCityIds.forEach { repository.removeCity(it) }

        val preferences = repository.preferences.first()
        assertTrue(preferences.savedCityIds.isEmpty())
        assertNull(preferences.activeCityId)
    }

    @Test
    fun selectingACityOnlyWorksForSavedCities() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.selectCity("berlin")
        assertEquals(CityDirectory.defaultActiveCityId, repository.preferences.first().activeCityId)

        repository.selectCity("tokyo")
        assertEquals("tokyo", repository.preferences.first().activeCityId)
    }

    @Test
    fun use24HourIsPersisted() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setUse24Hour(true)
        assertTrue(repository.preferences.first().use24Hour)

        repository.setUse24Hour(false)
        assertFalse(repository.preferences.first().use24Hour)
    }

    @Test
    fun theThemeIsPersisted() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setTheme(AppTheme.COOL_SLATE)
        assertEquals(AppTheme.COOL_SLATE, repository.preferences.first().theme)

        repository.setTheme(AppTheme.WARM_SEPIA)
        assertEquals(AppTheme.WARM_SEPIA, repository.preferences.first().theme)

        repository.setTheme(AppTheme.LIGHT_MONOCHROME)
        assertEquals(AppTheme.LIGHT_MONOCHROME, repository.preferences.first().theme)
    }

    @Test
    fun settingAThemeLeavesTheCitiesAlone() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setTheme(AppTheme.COOL_SLATE)

        val preferences = repository.preferences.first()
        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, preferences.activeCityId)
    }

    @Test
    fun choosingAThemeMigratesAStoreThatOnlyHadTheLightDarkField() = runBlocking {
        val store = newDataStore(newFile())
        store.updateData {
            SavedCities.newBuilder()
                .setSeeded(true)
                .addCityIds("tokyo")
                .setThemeMode(SavedThemeMode.THEME_MODE_DARK)
                .build()
        }
        val repository = ClockRepository(store)
        // The dark mode of the old field was True Dark, which 3.1.0 retired.
        assertEquals(AppTheme.Default, repository.preferences.first().theme)

        repository.setTheme(AppTheme.COOL_SLATE)

        val preferences = repository.preferences.first()
        assertEquals(AppTheme.COOL_SLATE, preferences.theme)
        // The legacy field is reset to its zero value, so the store carries one
        // answer rather than two that can disagree.
        assertEquals(SavedThemeMode.THEME_MODE_SYSTEM, store.data.first().themeMode)
    }

    @Test
    fun theComparisonChoiceIsPersisted() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        // Off until the user opts in.
        assertFalse(repository.preferences.first().compareWithLocalTime)

        repository.setCompareWithLocalTime(true)
        assertTrue(repository.preferences.first().compareWithLocalTime)

        repository.setCompareWithLocalTime(false)
        assertFalse(repository.preferences.first().compareWithLocalTime)
    }

    @Test
    fun turningOffTheComparisonLeavesTheCitiesAlone() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setCompareWithLocalTime(false)

        val preferences = repository.preferences.first()
        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, preferences.activeCityId)
    }

    // --- Restore / prune -----------------------------------------------------

    @Test
    fun unknownStoredCityIdsArePrunedOnRestore() = runBlocking {
        val store = newDataStore(newFile())
        store.updateData {
            SavedCities.newBuilder()
                .setSeeded(true)
                .addCityIds("tokyo")
                .addCityIds("atlantis")
                .addCityIds("paris")
                .setActiveCityId("atlantis")
                .build()
        }

        val preferences = ClockRepository(store).preferences.first()

        assertEquals(listOf("tokyo", "paris"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)
    }

    @Test
    fun stateSurvivesARestart() = runBlocking {
        val file = newFile()

        val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val firstRepository = ClockRepository(
            DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler {
                    SavedCities.getDefaultInstance()
                },
                scope = firstScope,
                produceFile = { file },
            ),
        )
        firstRepository.addCity("paris")
        firstRepository.setUse24Hour(true)
        firstRepository.selectCity("tokyo")
        firstRepository.setTheme(AppTheme.COOL_SLATE)
        firstRepository.setCityLabel("london", "Head Office")
        firstRepository.setCompareWithLocalTime(false)
        val before = firstRepository.preferences.first()

        firstScope.cancel()
        firstScope.coroutineContext[Job]?.join()

        val secondRepository = ClockRepository(newDataStore(file))
        val after = secondRepository.preferences.first()

        assertEquals(before, after)
        assertEquals(true, after.use24Hour)
        assertEquals("tokyo", after.activeCityId)
        assertEquals(AppTheme.COOL_SLATE, after.theme)
        assertEquals("Head Office", after.cityLabels["london"])
        assertEquals(false, after.compareWithLocalTime)
        assertEquals(CityDirectory.defaultSavedCityIds + "paris", after.savedCityIds)
    }

    // --- Display order (Room) -------------------------------------------------
    //
    // Order is owned by Room, membership by DataStore. These tests exercise the
    // reconciliation rules directly against a fake store, so a change to either
    // half is caught without needing a database.

    @Test
    fun theFirstCollectionSeedsTheRoomOrderFromTheStoredCities() = runBlocking {
        val order = FakeCityOrderStore()
        val repository = ClockRepository(newDataStore(newFile()), order)

        val preferences = repository.preferences.first()

        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultSavedCityIds, order.getOrder())
    }

    @Test
    fun aReorderIsPersistedToBothRoomAndDataStore() = runBlocking {
        val store = newDataStore(newFile())
        val order = FakeCityOrderStore()
        val repository = ClockRepository(store, order)
        val before = repository.preferences.first()

        val requested = before.savedCityIds.reversed()
        repository.reorderCities(requested)

        assertEquals(requested, order.getOrder())
        // The proto carries the same list, so the widget's fallback and any
        // older build of the app sees the new order too.
        assertEquals(requested, store.data.first().cityIdsList)
        assertEquals(requested, repository.preferences.first().savedCityIds)
    }

    @Test
    fun theRoomOrderWinsWhenTheTwoStoresDisagree() = runBlocking {
        val order = FakeCityOrderStore(listOf("london", "tokyo", "sydney"))
        val repository = ClockRepository(newDataStore(newFile()), order)

        val preferences = repository.preferences.first()

        // Room's order first, then every saved id Room did not know about.
        assertEquals(
            listOf("london", "tokyo", "sydney", "los-angeles", "new-york"),
            preferences.savedCityIds,
        )
    }

    @Test
    fun staleRoomIdsArePrunedAndMissingOnesAreAppended() = runBlocking {
        val order = FakeCityOrderStore(listOf("tokyo", "atlantis", "london"))
        val repository = ClockRepository(newDataStore(newFile()), order)

        val preferences = repository.preferences.first()

        assertEquals(
            listOf("tokyo", "london", "sydney", "los-angeles", "new-york"),
            preferences.savedCityIds,
        )
        // The repair is written back, so the stale id is not retried forever.
        assertEquals(preferences.savedCityIds, order.getOrder())
    }

    @Test
    fun reorderIgnoresUnknownAndDuplicateIdsAndAppendsWhatWasLeftOut() = runBlocking {
        val order = FakeCityOrderStore()
        val repository = ClockRepository(newDataStore(newFile()), order)
        val current = repository.preferences.first().savedCityIds

        repository.reorderCities(
            listOf("london", "london", "atlantis", "tokyo"),
        )

        // Requested ids first, in request order and without duplicates; every
        // saved city the caller omitted keeps its previous relative order at
        // the end rather than disappearing.
        val expected = listOf("london", "tokyo", "sydney", "los-angeles", "new-york")
        assertEquals(expected, repository.preferences.first().savedCityIds)
        assertEquals(expected, order.getOrder())
        assertEquals(current.size, expected.size)
    }

    @Test
    fun aReorderThatChangesNothingDoesNotRewriteTheStores() = runBlocking {
        val store = newDataStore(newFile())
        val order = FakeCityOrderStore()
        val repository = ClockRepository(store, order)
        val current = repository.preferences.first().savedCityIds
        val writesBefore = order.replaceAllCalls

        repository.reorderCities(current)

        assertEquals(writesBefore, order.replaceAllCalls)
        assertEquals(current, store.data.first().cityIdsList)
    }

    @Test
    fun addingACityAppendsItToTheRoomOrder() = runBlocking {
        val order = FakeCityOrderStore()
        val repository = ClockRepository(newDataStore(newFile()), order)
        repository.preferences.first()

        repository.addCity("paris")

        assertEquals(CityDirectory.defaultSavedCityIds + "paris", order.getOrder())
        assertEquals(CityDirectory.defaultSavedCityIds + "paris", repository.preferences.first().savedCityIds)
    }

    @Test
    fun addingACityRepairsARoomTableThatWasNeverSeeded() = runBlocking {
        // The upgrade path: DataStore already holds cities, the v3 table is
        // new and empty, and the user adds one before opening the list.
        val order = FakeCityOrderStore()
        val repository = ClockRepository(newDataStore(newFile()), order)

        repository.addCity("paris")

        assertEquals(CityDirectory.defaultSavedCityIds + "paris", order.getOrder())
    }

    @Test
    fun removingACityRemovesItFromTheOrderAndClosesTheGap() = runBlocking {
        val order = FakeCityOrderStore()
        val repository = ClockRepository(newDataStore(newFile()), order)
        repository.preferences.first()

        repository.removeCity("sydney")

        val expected = CityDirectory.defaultSavedCityIds - "sydney"
        assertEquals(expected, order.getOrder())
        assertEquals(expected, repository.preferences.first().savedCityIds)
    }

    @Test
    fun anEmptyRoomOrderFallsBackToTheStoredList() = runBlocking {
        val order = FakeCityOrderStore()
        val repository = ClockRepository(newDataStore(newFile()), order)

        // A store whose write failed is observed through the same `order` flow.
        val preferences = repository.preferences.first()
        order.replaceAll(emptyList())

        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultSavedCityIds, repository.preferences.first().savedCityIds)
    }

    @Test
    fun aRepositoryWithoutARoomStoreKeepsTheDataStoreOrder() = runBlocking {
        // Tests and previews construct the repository this way; adding the
        // Room-backed constructor must not have changed that behaviour.
        val repository = ClockRepository(newDataStore(newFile()))

        repository.addCity("paris")

        assertEquals(
            CityDirectory.defaultSavedCityIds + "paris",
            repository.preferences.first().savedCityIds,
        )
    }

    // --- First-launch intro ---------------------------------------------------

    @Test
    fun aFreshInstallStartsWithTheIntroPending() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        assertFalse(repository.preferences.first().onboardingCompleted)
    }

    @Test
    fun completingTheIntroIsPersistedAndCanBeReplayed() = runBlocking {
        val file = newFile()
        val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val firstRepository = ClockRepository(
            DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler {
                    SavedCities.getDefaultInstance()
                },
                scope = firstScope,
                produceFile = { file },
            ),
        )
        assertFalse(firstRepository.preferences.first().onboardingCompleted)

        firstRepository.completeOnboarding()
        assertTrue(firstRepository.preferences.first().onboardingCompleted)

        firstRepository.restartOnboarding()
        assertFalse(firstRepository.preferences.first().onboardingCompleted)

        firstRepository.completeOnboarding()

        // Release the file before the second instance opens it: DataStore allows
        // only one active store per file.
        firstScope.cancel()
        firstScope.coroutineContext[Job]?.join()

        val reopened = ClockRepository(newDataStore(file))
        assertTrue(reopened.preferences.first().onboardingCompleted)
    }

    @Test
    fun aStoreWrittenBeforeTheIntroExistedIsTreatedAsCompleted() = runBlocking {
        // v2 and earlier never wrote the field, so there is no presence bit.
        // Those users already know the app and must not be shown the intro.
        val store = newDataStore(newFile())
        store.updateData {
            SavedCities.newBuilder()
                .setSeeded(true)
                .addCityIds("tokyo")
                .build()
        }

        assertTrue(ClockRepository(store).preferences.first().onboardingCompleted)
    }

    @Test
    fun aSeededStoreWithAnExplicitFalseStillShowsTheIntro() = runBlocking {
        val store = newDataStore(newFile())
        store.updateData {
            SavedCities.newBuilder()
                .setSeeded(true)
                .addCityIds("tokyo")
                .setOnboardingCompleted(false)
                .build()
        }

        assertFalse(ClockRepository(store).preferences.first().onboardingCompleted)
    }

    @Test
    fun completingTheIntroLeavesTheCitiesAndSettingsAlone() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))
        repository.setTheme(AppTheme.WARM_SEPIA)
        repository.setUse24Hour(false)

        repository.completeOnboarding()

        val preferences = repository.preferences.first()
        assertEquals(AppTheme.WARM_SEPIA, preferences.theme)
        assertFalse(preferences.use24Hour)
        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
    }

    // --- City labels ---------------------------------------------------------

    @Test
    fun aCityLabelIsPersistedTrimmed() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setCityLabel("tokyo", "  Head Office  ")

        assertEquals(mapOf("tokyo" to "Head Office"), repository.preferences.first().cityLabels)
    }

    @Test
    fun aBlankOrAbsentLabelClearsIt() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setCityLabel("tokyo", "Head Office")
        repository.setCityLabel("tokyo", "   ")
        assertTrue(repository.preferences.first().cityLabels.isEmpty())

        repository.setCityLabel("tokyo", "Head Office")
        repository.setCityLabel("tokyo", null)
        assertTrue(repository.preferences.first().cityLabels.isEmpty())
    }

    @Test
    fun labelsAreIndependentPerCity() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setCityLabel("tokyo", "Head Office")
        repository.setCityLabel("new-york", "Client calls")

        val preferences = repository.preferences.first()
        assertEquals("Head Office", preferences.cityLabels["tokyo"])
        assertEquals("Client calls", preferences.cityLabels["new-york"])

        repository.setCityLabel("new-york", null)
        val after = repository.preferences.first()
        assertEquals("Head Office", after.cityLabels["tokyo"])
        assertNull(after.cityLabels["new-york"])
    }

    @Test
    fun settingALabelLeavesTheCitiesAndTheThemeAlone() = runBlocking {
        val repository = ClockRepository(newDataStore(newFile()))

        repository.setTheme(AppTheme.COOL_SLATE)
        repository.setCityLabel("tokyo", "Head Office")

        val preferences = repository.preferences.first()
        assertEquals(CityDirectory.defaultSavedCityIds, preferences.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, preferences.activeCityId)
        assertEquals(AppTheme.COOL_SLATE, preferences.theme)
    }
}

/**
 * An in-memory [CityOrderStore] with the same semantics as the Room one.
 *
 * Counting [replaceAll] calls is what lets the no-op reorder test prove that a
 * drag that ended where it started costs no write.
 */
private class FakeCityOrderStore(
    initial: List<String> = emptyList(),
) : CityOrderStore {

    private val state = MutableStateFlow(initial)

    override val order: Flow<List<String>> = state

    var replaceAllCalls: Int = 0
        private set

    override suspend fun getOrder(): List<String> = state.value

    override suspend fun replaceAll(cityIds: List<String>) {
        replaceAllCalls++
        state.value = cityIds
    }

    override suspend fun appendIfMissing(cityId: String) {
        if (cityId !in state.value) state.value = state.value + cityId
    }

    override suspend fun remove(cityId: String) {
        state.value = state.value.filterNot { it == cityId }
    }
}
