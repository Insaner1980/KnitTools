package com.finnvek.knittools.pro

enum class ProActionAccess {
    Allowed,
    Trial,
    Locked,
    Unknown,
}

/** Valmius ratkaistaan ensin; sama tulos ohjaa merkkiä ja toiminnon Pro-kehotetta. */
fun proActionAccess(
    proState: ProState,
    ready: Boolean,
    feature: ProFeature,
    needsEntitlement: Boolean = true,
    alreadyUnlocked: Boolean = false,
): ProActionAccess =
    when {
        !ready -> {
            ProActionAccess.Unknown
        }

        !needsEntitlement || alreadyUnlocked -> {
            ProActionAccess.Allowed
        }

        proState.hasFeature(feature) -> {
            if (proState.status == ProStatus.TRIAL_ACTIVE) ProActionAccess.Trial else ProActionAccess.Allowed
        }

        else -> {
            ProActionAccess.Locked
        }
    }
