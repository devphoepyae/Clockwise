package com.pj95.clockwise.data.repository

/** Storage problems surfaced to the user as a non-blocking message. */
enum class PrefsError {
    /** Saved state could not be read; defaults are used instead. */
    ReadFailed,

    /** A change could not be written to disk. */
    WriteFailed,
}
