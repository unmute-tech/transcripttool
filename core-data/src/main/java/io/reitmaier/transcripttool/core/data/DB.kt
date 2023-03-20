package io.reitmaier.transcripttool.core.data

import com.squareup.sqldelight.android.AndroidSqliteDriver
import io.reitmaier.transcripttool.data.TranscriptToolDb
import logcat.LogPriority
import logcat.logcat

private const val VERSION_PRAGMA = "user_version"

fun migrateIfNeeded(driver: AndroidSqliteDriver) {
  val oldVersion =
    driver.executeQuery(null, "PRAGMA $VERSION_PRAGMA", 0).use { cursor ->
      if (cursor.next()) {
        cursor.getLong(0)?.toInt()
      } else {
        null
      }
    } ?: 0

  val newVersion = TranscriptToolDb.Schema.version

  if (oldVersion == 0) {
    logcat("DB", LogPriority.INFO) { "Creating DB version $newVersion!" }
    TranscriptToolDb.Schema.create(driver)
    driver.execute(null, "PRAGMA $VERSION_PRAGMA=$newVersion", 0)
  } else if (oldVersion < newVersion) {
    logcat("DB", LogPriority.DEBUG) { "Migrating DB from version $oldVersion to $newVersion!" }
    TranscriptToolDb.Schema.migrate(driver, oldVersion, newVersion)
    driver.execute(null, "PRAGMA $VERSION_PRAGMA=$newVersion", 0)
  }
}
