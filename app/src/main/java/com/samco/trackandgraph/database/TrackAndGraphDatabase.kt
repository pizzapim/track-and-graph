/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samco.trackandgraph.R
import org.threeten.bp.Duration
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat

const val MAX_FEATURE_NAME_LENGTH = 40
const val MAX_LABEL_LENGTH = 30
const val MAX_DISCRETE_VALUES_PER_FEATURE = 10
const val MAX_GRAPH_STAT_NAME_LENGTH = 100
const val MAX_GROUP_NAME_LENGTH = 40
const val MAX_LINE_GRAPH_FEATURE_NAME_LENGTH = 20
const val MAX_LINE_GRAPH_FEATURES = 10

val databaseFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
val displayFeatureDateFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yy  HH:mm")
    .withZone(ZoneId.systemDefault())
val doubleFormatter = DecimalFormat("#.##################")


//this is a number coprime to the number of colours used to select them in a pseudo random order for greater contrast
const val dataVisColorGenerator = 7
val dataVisColorList = listOf(
    R.color.visColor1,
    R.color.visColor2,
    R.color.visColor3,
    R.color.visColor4,
    R.color.visColor5,
    R.color.visColor6,
    R.color.visColor7,
    R.color.visColor8,
    R.color.visColor9,
    R.color.visColor10
)

const val splitChars1 = "||"
const val splitChars2 = "!!"

@Database(
    entities = [TrackGroup::class, Feature::class, DataPoint::class, GraphStatGroup::class,
        GraphOrStat::class, LineGraph::class, AverageTimeBetweenStat::class, PieChart::class,
        TimeSinceLastStat::class, Reminder::class],
    version = 31
)
@TypeConverters(Converters::class)
abstract class TrackAndGraphDatabase : RoomDatabase() {
    abstract val trackAndGraphDatabaseDao: TrackAndGraphDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: TrackAndGraphDatabase? = null
        fun getInstance(context: Context): TrackAndGraphDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext, TrackAndGraphDatabase::class.java, "trackandgraph_database")
                        .addMigrations(MIGRATION_29_30)
                        .addMigrations(MIGRATION_30_31)
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminders_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `display_index` INTEGER NOT NULL, 
                `name` TEXT NOT NULL,
                `time` TEXT NOT NULL,
                `checked_days` TEXT NOT NULL
            )""".trimMargin())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_id` ON `reminders_table` (`id`)")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE line_graphs_table ADD y_range_type INTEGER NOT NULL DEFAULT  0")
        database.execSQL("ALTER TABLE line_graphs_table ADD y_from REAL NOT NULL DEFAULT  0")
        database.execSQL("ALTER TABLE line_graphs_table ADD y_to REAL NOT NULL DEFAULT  1")
    }
}

class Converters {
    @TypeConverter
    fun stringToListOfDiscreteValues(value: String): List<DiscreteValue> {
        return if (value.isEmpty()) listOf()
        else value.split(splitChars1).map { s -> DiscreteValue.fromString(s) }.toList()
    }

    @TypeConverter
    fun listOfDiscreteValuesToString(values: List<DiscreteValue>): String = values.joinToString(splitChars1) { v -> v.toString() }

    @TypeConverter
    fun intToFeatureType(i: Int): FeatureType = FeatureType.values()[i]

    @TypeConverter
    fun featureTypeToInt(featureType: FeatureType): Int = featureType.ordinal

    @TypeConverter
    fun intToGraphStatType(i: Int): GraphStatType = GraphStatType.values()[i]

    @TypeConverter
    fun graphStatTypeToInt(graphStat: GraphStatType): Int = graphStat.ordinal

    @TypeConverter
    fun stringToOffsetDateTime(value: String?): OffsetDateTime? = value?.let { odtFromString(value) }

    @TypeConverter
    fun offsetDateTimeToString(value: OffsetDateTime?): String = value?.let { stringFromOdt(it) } ?: ""

    @TypeConverter
    fun lineGraphFeaturesToString(value: List<LineGraphFeature>): String {
        return value.joinToString(splitChars1){ v ->
            listOf("${v.featureId}",
                v.name,
                "${v.colorIndex}",
                "${v.averagingMode.ordinal}",
                "${v.plottingMode.ordinal}",
                "${v.offset}",
                "${v.scale}"
            ).joinToString(splitChars2)
        }
    }

    @TypeConverter
    fun stringToLineGraphFeatures(value: String): List<LineGraphFeature> {
        return value.split(splitChars1).map {
            val strs = it.split(splitChars2)
            LineGraphFeature(
                strs[0].toLong(),
                strs[1],
                strs[2].toInt(),
                LineGraphAveraginModes.values()[strs[3].toInt()],
                LineGraphPlottingModes.values()[strs[4].toInt()],
                strs[5].toDouble(),
                strs[6].toDouble()
            )
        }
    }

    @TypeConverter
    fun durationToString(value: Duration?): String = value?.let { value.toString() } ?: ""

    @TypeConverter
    fun stringToDuration(value: String): Duration? = if (value.isEmpty()) null else Duration.parse(value)

    @TypeConverter
    fun intToGroupItemType(i: Int) = GroupItemType.values()[i]

    @TypeConverter
    fun groupItemTypeToInt(groupItemType: GroupItemType) = groupItemType.ordinal

    @TypeConverter
    fun localTimeToString(value: LocalTime) = value.toString()

    @TypeConverter
    fun localTimeFromString(value: String) = LocalTime.parse(value)

    @TypeConverter
    fun checkedDaysToString(value: CheckedDays) = value.toList().joinToString(splitChars1)

    @TypeConverter
    fun checkedDaysFromString(value: String): CheckedDays {
        return CheckedDays.fromList(
            value.split(splitChars1)
                .map { s -> s.toBoolean() }
        )
    }

    @TypeConverter
    fun yRangeTypeToInt(yRangeType: YRangeType) = yRangeType.ordinal

    @TypeConverter
    fun intToYRangeType(index: Int) = YRangeType.values()[index]
}


fun odtFromString(value: String): OffsetDateTime = databaseFormatter.parse(value, OffsetDateTime::from)
fun stringFromOdt(value: OffsetDateTime): String = databaseFormatter.format(value)

