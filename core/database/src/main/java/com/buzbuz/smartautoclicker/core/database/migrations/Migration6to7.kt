/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.database.migrations

import androidx.room.ForeignKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteColumn
import com.buzbuz.smartautoclicker.core.base.sqlite.SQLiteTable
import com.buzbuz.smartautoclicker.core.base.sqlite.getTable

/**
 * Migration from database v6 to v7.
 *
 * Changes:
 * * creates end condition table
 * * add detection quality to scenario
 * * add end condition operator to scenario
 */
object Migration6to7 : Migration(6, 7) {

    private val scenarioIdForeignKey = SQLiteColumn.ForeignKey(
        name = "scenario_id", type = Long::class,
        referencedTable = "scenario_table", referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )

    private val eventIdForeignKey = SQLiteColumn.ForeignKey(
        name = "event_id", type = Long::class,
        referencedTable = "event_table", referencedColumn = "id", deleteAction = ForeignKey.CASCADE,
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        db.getTable("end_condition_table").apply {
            createEndConditionTable()
            createIndex(scenarioIdForeignKey)
            createIndex(eventIdForeignKey)

            initializeEndConditions()
        }

        db.getTable("scenario_table")
            .addScenarioNewColumns()
    }

    /** Create the table for the end conditions. */
    private fun SQLiteTable.createEndConditionTable(): Unit = createTable(
        primaryKey = SQLiteColumn.PrimaryKey(),
        columns = setOf(
            scenarioIdForeignKey,
            eventIdForeignKey,
            SQLiteColumn.Default("executions", Int::class),
        )
    )
}

/** Insert a new end condition for each event with a stop after. */
private fun SQLiteTable.initializeEndConditions() = insertIntoSelect(
    fromTableName = "event_table",
    extraClause = "WHERE `stop_after` IS NOT NULL",
    columnsToFromColumns = arrayOf(
        "scenario_id" to "scenario_id",
        "event_id" to "id",
        "executions" to "stop_after",
    )
)

/**
 * Add the detection quality & operator column to the scenario table.
 * Quality default value is the new algorithm default value, operator default value is OR.
 */
private fun SQLiteTable.addScenarioNewColumns() = apply {
    alterTableAddColumn(SQLiteColumn.Default("detection_quality", Int::class, defaultValue = "600"))
    alterTableAddColumn(SQLiteColumn.Default("end_condition_operator", Int::class, defaultValue = "2"))
}