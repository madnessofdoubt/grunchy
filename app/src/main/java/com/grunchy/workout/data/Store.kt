package com.grunchy.workout.data

import android.content.Context
import com.grunchy.workout.model.AppData
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Whole-state JSON persistence.
 *
 * The dataset is tiny (years of training is a few hundred kB) and writes only happen on a
 * tap, so one atomic file write beats a database here: no migration surface, and a crash
 * during a write can never leave a half-written history behind because we write a temp file
 * and rename it over the real one.
 */
object Store {

    private const val FILE_NAME = "grunchy.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Never throws: an unreadable or corrupt file degrades to an empty history. */
    fun load(context: Context): AppData {
        val f = file(context)
        if (!f.exists()) return AppData()
        return runCatching { json.decodeFromString<AppData>(f.readText()) }.getOrElse { AppData() }
    }

    fun save(context: Context, data: AppData): Boolean = runCatching {
        val f = file(context)
        val tmp = File(f.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(json.encodeToString(data))
        if (f.exists()) f.delete()
        tmp.renameTo(f)
    }.getOrDefault(false)

    fun encode(data: AppData): String = json.encodeToString(data)

    fun decode(text: String): AppData = json.decodeFromString(text)
}
