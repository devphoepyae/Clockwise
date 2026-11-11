package com.pj95.clockwise.data.prefs

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/** Reads and writes [SavedCities] as length-delimited protobuf-lite. */
object UserPreferencesSerializer : Serializer<SavedCities> {

    override val defaultValue: SavedCities = SavedCities.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SavedCities =
        try {
            SavedCities.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            // Surfaces to DataStore as corruption so it can fall back to
            // [defaultValue] via the configured corruption handler.
            throw CorruptionException("Cannot read saved cities.", exception)
        }

    override suspend fun writeTo(t: SavedCities, output: OutputStream) {
        t.writeTo(output)
    }
}
