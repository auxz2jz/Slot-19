package com.auxz2jz.videogrammetry

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Filenames reflect the app exporting a file. Older run metadata retains
 * the actual capture/analysis source version; never silently rewrite it.
 */
object ExportNames {
    private fun normalizedVersion(versionName: String): String {
        require(Regex("[0-9]+(?:\\.[0-9]+){1,3}(?:-[A-Za-z0-9.]+)?")
            .matches(versionName)) { "Invalid app version" }
        return "v" + versionName
    }
    private fun timestamp(timeUtcMs: Long): String =
        SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timeUtcMs))

    private fun checkedRunId(runId: String): String {
        require(Regex("[A-Za-z0-9_-]{1,80}").matches(runId)) {
            "Invalid run identifier"
        }
        return runId
    }

    fun sparsePly(versionName: String, runId: String,
                  timeUtcMs: Long = System.currentTimeMillis()): String =
        "Android-" + normalizedVersion(versionName) + "-sparse-two-view-" +
            checkedRunId(runId) + "-" + timestamp(timeUtcMs) + ".ply"

    fun objectFocusPly(versionName: String, runId: String,
                       timeUtcMs: Long = System.currentTimeMillis()): String =
        "Android-" + normalizedVersion(versionName) + "-object-focus-" +
            checkedRunId(runId) + "-" + timestamp(timeUtcMs) + ".ply"

    fun reconstructedPly(versionName: String, runId: String,
        timeUtcMs: Long = System.currentTimeMillis()): String =
        "Android-" + normalizedVersion(versionName) + "-object-reconstruction-" +
            checkedRunId(runId) + "-" + timestamp(timeUtcMs) + ".ply"

    fun multiviewPly(versionName: String, runId: String,
        timeUtcMs: Long = System.currentTimeMillis()): String =
        "Android-" + normalizedVersion(versionName) + "-object-multi-view-" +
            checkedRunId(runId) + "-" + timestamp(timeUtcMs) + ".ply"

    fun latestRunZip(versionName: String, runId: String,
                     timeUtcMs: Long = System.currentTimeMillis()): String =
        "Android-" + normalizedVersion(versionName) + "-last-run-diagnostics-" +
            checkedRunId(runId) + "-" + timestamp(timeUtcMs) + ".zip"

    fun allRunsZip(versionName: String,
                   timeUtcMs: Long = System.currentTimeMillis()): String =
        "Android-" + normalizedVersion(versionName) + "-ALL-run-comparison-" +
            timestamp(timeUtcMs) + ".zip"
}
