package com.crdy.batterygyan

import com.crdy.batterygyan.domain.model.ChargeLimitValidator
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChargeLimitValidatorTest {
    @Test fun acceptsDefaultThresholds() {
        assertNull(ChargeLimitValidator.validate(80, 75))
    }

    @Test fun rejectsResumeAtOrAboveStop() {
        assertNotNull(ChargeLimitValidator.validate(80, 80))
        assertNotNull(ChargeLimitValidator.validate(75, 80))
    }

    @Test fun rejectsUnsafeBounds() {
        assertNotNull(ChargeLimitValidator.validate(100, 75))
        assertNotNull(ChargeLimitValidator.validate(80, 0))
    }
}
