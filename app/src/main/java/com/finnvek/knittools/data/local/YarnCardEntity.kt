package com.finnvek.knittools.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)
