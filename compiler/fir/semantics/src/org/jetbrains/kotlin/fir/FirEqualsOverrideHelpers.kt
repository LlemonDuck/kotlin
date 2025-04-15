/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.collectSymbolsForType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * The entries are ordered from least trustworthy to most trustworthy.
 * `A` is more trustworthy than `B` iff we can rely on `A` in more cases compared to `B`.
 */
enum class EqualsOverrideContract {
    UNKNOWN,
    TRUSTED_FOR_EXHAUSTIVENESS,

    /**
     * You can think of it as:
     * ```
     * contract {
     *     returns(true) implies (other is SelfType)
     *     returns(false) implies (other !is SelfLiteralType)
     * }
     * ```
     */
    SAFE_FOR_SMART_CAST,
}

fun computeEqualsOverrideContract(
    type: ConeKotlinType,
    session: FirSession,
    scopeSession: ScopeSession,
): EqualsOverrideContract {
    return computeEqualsOverrideContract(
        symbolsForType = collectSymbolsForType(type, session),
        session = session,
        scopeSession = scopeSession,
        visitedSymbols = mutableSetOf(),
    )
}

fun computeEqualsOverrideContract(
    symbolsForType: List<FirClassSymbol<*>>,
    session: FirSession,
    scopeSession: ScopeSession,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
): EqualsOverrideContract {
    val subtypesContract = symbolsForType
        .maxOfOrNull { it.computeEqualsOverrideContract(session, scopeSession, visitedSymbols) }
        ?: EqualsOverrideContract.SAFE_FOR_SMART_CAST

    if (subtypesContract == EqualsOverrideContract.UNKNOWN) {
        return EqualsOverrideContract.UNKNOWN
    }

    val superTypes = lookupSuperTypes(
        symbolsForType,
        lookupInterfaces = false,
        deep = true,
        session,
        substituteTypes = false,
        visitedSymbols = visitedSymbols,
    )
    val superClassSymbols = superTypes.mapNotNull { it.fullyExpandedType(session).toRegularClassSymbol(session) }

    val supertypesContract = when (superClassSymbols.any { it.hasEqualsOverrideContract(session, scopeSession) }) {
        true -> EqualsOverrideContract.UNKNOWN
        false -> EqualsOverrideContract.SAFE_FOR_SMART_CAST
    }

    return minOf(subtypesContract, supertypesContract)
}

private fun FirClassSymbol<*>.computeEqualsOverrideContract(
    session: FirSession,
    scopeSession: ScopeSession,
    visitedSymbols: MutableSet<FirClassifierSymbol<*>>,
): EqualsOverrideContract {
    fun FirClassSymbol<*>.computeInheritorsContract(): EqualsOverrideContract {
        if (this !is FirRegularClassSymbol) return EqualsOverrideContract.UNKNOWN

        val inheritors = fir.getSealedClassInheritors(session).map {
            it.toSymbol(session) as? FirClassSymbol<*> ?: return EqualsOverrideContract.UNKNOWN
        }

        // Note that `sealed class` variants may have additional supertypes
        return computeEqualsOverrideContract(inheritors, session, scopeSession, visitedSymbols)
    }

    return when {
        isFinal -> when {
            !hasEqualsOverrideContract(session, scopeSession) -> EqualsOverrideContract.SAFE_FOR_SMART_CAST
            isData || isInlineOrValue || classKind == ClassKind.OBJECT -> EqualsOverrideContract.TRUSTED_FOR_EXHAUSTIVENESS
            else -> EqualsOverrideContract.UNKNOWN
        }
        isSealed && !hasEqualsOverrideContract(session, scopeSession) -> minOf(
            EqualsOverrideContract.TRUSTED_FOR_EXHAUSTIVENESS,
            computeInheritorsContract(),
        )
        else -> EqualsOverrideContract.UNKNOWN
    }
}

private fun FirClassSymbol<*>.hasEqualsOverrideContract(session: FirSession, scopeSession: ScopeSession): Boolean {
    if (resolvedStatus.isExpect) return true
    if (isSmartcastPrimitive(classId)) return false
    when (classId) {
        StandardClassIds.Any -> return false
        // Float and Double effectively had non-trivial `equals` semantics while they don't have explicit overrides (see KT-50535)
        StandardClassIds.Float, StandardClassIds.Double -> return true
        // kotlin.Enum has `equals()`, but we know it's reasonable
        StandardClassIds.Enum -> return false
    }

    // When the class belongs to a different module, "equals" contract might be changed without re-compilation
    // But since we had such behavior in FE1.0, it might be too strict to prohibit it now, especially once there's a lot of cases
    // when different modules belong to a single project, so they're totally safe (see KT-50534)
    // if (moduleData != session.moduleData) {
    //     return true
    // }

    val ownerTag = this.toLookupTag()
    return this.unsubstitutedScope(
        session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = FirResolvePhase.STATUS
    ).getFunctions(OperatorNameConventions.EQUALS).any {
        !it.isSubstitutionOrIntersectionOverride && it.isEquals(session) && ownerTag.isRealOwnerOf(it)
    }
}

/**
 * Determines if type smart-casting to the specified [org.jetbrains.kotlin.name.ClassId] can be performed when values are
 * compared via equality. Because this is determined using the ClassId, only standard built-in
 * types are considered.
 */
fun isSmartcastPrimitive(classId: ClassId?): Boolean {
    return when (classId) {
        // Support other primitives as well: KT-62246.
        StandardClassIds.String,
            -> true

        else -> false
    }
}
