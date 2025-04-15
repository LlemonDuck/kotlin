/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.konan.target.HostManager

internal val CrossCompilationDiagnosticsSetupAction = KotlinProjectSetupAction {
    // Skip the entire process if klibs cross compilation is disabled
    if (kotlinPropertiesProvider.disableKlibsCrossCompilation)
        return@KotlinProjectSetupAction

    multiplatformExtension
        .targets
        .withType(KotlinNativeTarget::class.java)
        // Find targets that are incompatible with the current host
        .matching { !HostManager().isEnabled(it.konanTarget) }
        // For each incompatible target, report diagnostics for all its cinterops
        .all { target ->
            target.cinterops.all { interop ->
                reportDiagnostic(
                    KotlinToolingDiagnostics.CrossCompilationWithCinterops(
                        target.targetName,
                        interop.name,
                        HostManager.hostName
                    )
                )
            }
        }
}

private val KotlinNativeTarget.cinterops get() = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops