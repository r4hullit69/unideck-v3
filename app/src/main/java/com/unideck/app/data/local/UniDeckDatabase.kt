package com.unideck.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SemesterEntity::class,
        CourseEntity::class,
        ClassScheduleEntity::class,
        AttendanceLogEntity::class,
        OneOffClassEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class UniDeckDatabase : RoomDatabase() {
    abstract fun uniDeckDao(): UniDeckDao

    companion object {
        @Volatile
        private var INSTANCE: UniDeckDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `one_off_classes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `courseId` INTEGER NOT NULL,
                        `semesterId` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `roomNumber` TEXT NOT NULL,
                        `componentType` TEXT NOT NULL,
                        `attendanceLogId` INTEGER,
                        FOREIGN KEY(`courseId`) REFERENCES `courses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`semesterId`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_one_off_classes_courseId` ON `one_off_classes` (`courseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_one_off_classes_semesterId` ON `one_off_classes` (`semesterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_one_off_classes_dateEpochDay` ON `one_off_classes` (`dateEpochDay`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `courses` ADD COLUMN `initialAttended` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `courses` ADD COLUMN `initialTotal` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `semesters` ADD COLUMN `startDateEpochDay` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `semesters` ADD COLUMN `endDateEpochDay` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `courses_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `semesterId` INTEGER NOT NULL,
                        `code` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `professorName` TEXT NOT NULL,
                        `credits` REAL NOT NULL DEFAULT 3.0,
                        `isLab` INTEGER NOT NULL,
                        `midSem` REAL NOT NULL,
                        `quiz1` REAL NOT NULL,
                        `quiz2` REAL NOT NULL,
                        `assignment` REAL NOT NULL,
                        `endSem` REAL NOT NULL,
                        `relativeGradingEnabled` INTEGER NOT NULL,
                        `customGradeOverride` TEXT,
                        `initialAttended` INTEGER NOT NULL,
                        `initialTotal` INTEGER NOT NULL,
                        FOREIGN KEY(`semesterId`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `courses_new` (
                        `id`, `semesterId`, `code`, `name`, `professorName`, `credits`,
                        `isLab`, `midSem`, `quiz1`, `quiz2`, `assignment`, `endSem`,
                        `relativeGradingEnabled`, `customGradeOverride`, `initialAttended`, `initialTotal`
                    )
                    SELECT
                        `id`, `semesterId`, `code`, `name`, `professorName`, CAST(`credits` AS REAL),
                        `isLab`, `midSem`, `quiz1`, `quiz2`, `assignment`, `endSem`,
                        `relativeGradingEnabled`, `customGradeOverride`, `initialAttended`, `initialTotal`
                    FROM `courses`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `courses`")
                db.execSQL("ALTER TABLE `courses_new` RENAME TO `courses`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_semesterId` ON `courses` (`semesterId`)")
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }

        fun getInstance(context: Context): UniDeckDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UniDeckDatabase::class.java,
                    "unideck_offline.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
