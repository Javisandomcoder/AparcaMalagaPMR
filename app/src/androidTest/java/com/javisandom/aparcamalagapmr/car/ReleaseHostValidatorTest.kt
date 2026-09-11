package com.javisandom.aparcamalagapmr.car

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class ReleaseHostValidatorTest {
    @Test fun release_trusts_android_auto_signatures_without_allowing_arbitrary_hosts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val validator = releaseHostValidator(context)
        val signatures = validator.allowedHosts["com.google.android.projection.gearhead"]
        assertFalse("Release must trust Android Auto signing certificates", signatures.isNullOrEmpty())
        assertFalse(validator.isValidHost(androidx.car.app.HostInfo("com.example.untrusted", 12345)))
    }
}
