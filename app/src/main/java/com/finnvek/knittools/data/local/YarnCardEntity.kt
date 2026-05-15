package com.finnvek.knittools.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finnvek.knittools.domain.model.YarnCardStatus

@Entity(tableName = "yarn_cards")
data class YarnCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val brand: String = "",
    val yarnName: String = "",
    val fiberContent: String = "",
    val weightGrams: String = "",
    val lengthMeters: String = "",
    val needleSize: String = "",
    val gaugeInfo: String = "",
    val colorName: String = "",
    val colorNumber: String = "",
    val dyeLot: String = "",
    val weightCategory: String = "",
    val careSymbols: Long = 0L,
    val photoUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "1")
    val quantityInStash: Int = 1,
    @ColumnInfo(defaultValue = YarnCardStatus.IN_STASH)
    val status: String = YarnCardStatus.IN_STASH,
    @ColumnInfo(defaultValue = "NULL")
    val linkedProjectId: Long? = null,
)
