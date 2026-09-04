package ir.tivan.controller.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema upgrades. These must stay additive so installing a new version keeps
 * the user's paired devices, custom names and message history — dropping the
 * database on upgrade would silently lose all of it.
 */
object Migrations {

    /**
     * v1 → v2: per-device configuration mirrored from the controller, plus the
     * cached `device_status` snapshot the UI reads on cold start.
     *
     * `devices` is rebuilt rather than patched with `ALTER TABLE ... ADD COLUMN`.
     * Room validates the migrated schema against the one it generates from the
     * entities, and an added column carries a DEFAULT clause that the generated
     * schema has no counterpart for. Recreating the table with Room's own DDL
     * (copied verbatim from schemas/2.json) removes any chance of a mismatch
     * crashing the app on first launch after an update.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `devices_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`phoneNumber` TEXT NOT NULL, " +
                    "`icon` TEXT NOT NULL, " +
                    "`outputNames` TEXT NOT NULL, " +
                    "`outputIcons` TEXT NOT NULL, " +
                    "`inputIcons` TEXT NOT NULL, " +
                    "`inputMessages` TEXT NOT NULL, " +
                    "`inputModes` TEXT NOT NULL, " +
                    "`inputResponses` TEXT NOT NULL, " +
                    "`securityZones` INTEGER NOT NULL, " +
                    "`autoReportMode` INTEGER NOT NULL, " +
                    "`outputMemory` INTEGER NOT NULL, " +
                    "`buzzer` INTEGER NOT NULL, " +
                    "`remoteLatch` INTEGER NOT NULL, " +
                    "`remoteSecurityMode` INTEGER NOT NULL, " +
                    "`isSelected` INTEGER NOT NULL)"
            )

            // Carry v1 rows across, filling the new columns with the same values
            // the Kotlin data class defaults to. Converters join List<String>
            // with "§" and List<Int> with ",".
            db.execSQL(
                """
                INSERT INTO devices_new (
                    id, name, phoneNumber, icon, outputNames,
                    outputIcons, inputIcons, inputMessages, inputModes, inputResponses,
                    securityZones, autoReportMode, outputMemory, buzzer,
                    remoteLatch, remoteSecurityMode, isSelected
                )
                SELECT
                    id, name, phoneNumber, icon, outputNames,
                    '💡§🔌§💧§🚪',
                    '🚪§👁§💨§📥',
                    'In1 Triggered§In2 Triggered§In3 Triggered§In4 Triggered',
                    '1,1,1,1',
                    '0,0,0,0',
                    2, 1, 0, 1, 1, 0,
                    isSelected
                FROM devices
                """.trimIndent()
            )

            db.execSQL("DROP TABLE devices")
            db.execSQL("ALTER TABLE devices_new RENAME TO devices")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `device_status` (" +
                    "`deviceId` INTEGER NOT NULL, " +
                    "`outputStates` TEXT NOT NULL, " +
                    "`outputsAt` INTEGER NOT NULL, " +
                    "`inputStates` TEXT NOT NULL, " +
                    "`inputsAt` INTEGER NOT NULL, " +
                    "`securityArmed` INTEGER, " +
                    "`securityAt` INTEGER NOT NULL, " +
                    "`antenna` TEXT, " +
                    "`antennaAt` INTEGER NOT NULL, " +
                    "`temperature` TEXT, " +
                    "`temperatureAt` INTEGER NOT NULL, " +
                    "`adminNumbers` TEXT NOT NULL, " +
                    "`adminsAt` INTEGER NOT NULL, " +
                    "`lastReport` TEXT, " +
                    "`lastReportAt` INTEGER NOT NULL, " +
                    "`lastContactAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`deviceId`))"
            )
        }
    }

    /**
     * v2 → v3: per-input trigger timestamps.
     *
     * v2 recorded a trigger by flipping the input's live-state character, which
     * had no way back to idle — an input that fired once stayed "triggered"
     * forever and shadowed what REPORT said. Trigger times now live in their own
     * column so the two never collide.
     *
     * Here a plain ADD COLUMN is safe: the entity declares a default for this
     * field, so the generated schema carries one too.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE device_status ADD COLUMN inputTriggeredAt " +
                    "TEXT NOT NULL DEFAULT '0,0,0,0'"
            )
            // Any input marked triggered under v2 has an unknown trigger time and
            // an unreliable live state; clear the state so it reads as "unknown"
            // rather than asserting something stale.
            db.execSQL("UPDATE device_status SET inputStates = '????'")
        }
    }

    /**
     * v3 → v4: how many channels a controller has (2, 4 or 8).
     *
     * A plain ADD COLUMN is safe here — Room's schema validation compares
     * column properties (name, type, not-null, default), not the physical
     * order ALTER TABLE appends in, and the default matches the Kotlin default
     * exactly. Existing devices default to 4, which is what every device
     * added before this version actually was.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE devices ADD COLUMN channelCount INTEGER NOT NULL DEFAULT 4")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
