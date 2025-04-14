/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.BitcodeKotlinLibraryLayout
import org.jetbrains.kotlin.konan.library.BitcodeWriter
import org.jetbrains.kotlin.konan.library.TargetedKotlinLibraryLayout
import org.jetbrains.kotlin.library.KotlinAbiVersion

open class TargetedWriterImpl(val targetLayout: TargetedKotlinLibraryLayout) {
    init {
        targetLayout.targetDir.mkdirs()
        targetLayout.includedDir.mkdirs()
    }

    fun addIncludedBinary(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(targetLayout.includedDir, basename))
    }
}

class BitcodeWriterImpl(
    libraryLayout: BitcodeKotlinLibraryLayout,
    abiVersion: KotlinAbiVersion?,
) : BitcodeWriter, TargetedWriterImpl(libraryLayout) {

    val bitcodeLayout = libraryLayout

    init {
        bitcodeLayout.nativeDir.mkdirs()
        if (abiVersion != null && abiVersion.major == 1 && abiVersion.minor == 201) {
            bitcodeLayout.kotlinDir.mkdirs()
        }
    }

    override fun addNativeBitcode(library: String) {
        val basename = File(library).name
        File(library).copyTo(File(bitcodeLayout.nativeDir, basename))
    }
}