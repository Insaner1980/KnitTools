package com.finnvek.knittools.ui

import android.view.View
import android.view.ViewGroup

internal fun descendants(view: View): List<View> =
    listOf(view) +
        if (view is ViewGroup) {
            (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
        } else {
            emptyList()
        }
