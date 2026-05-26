package com.finnvek.knittools.ui.screens.yarncard

data class ManualYarnCardInput(
    val yarnName: String,
    val brand: String = "",
    val quantity: Int = 1,
    val weightCategory: String = "",
    val colorName: String = "",
    val colorNumber: String = "",
    val dyeLot: String = "",
)
