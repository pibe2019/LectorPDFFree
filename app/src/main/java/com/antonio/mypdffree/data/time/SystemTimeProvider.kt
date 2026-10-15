package com.antonio.mypdffree.data.time

import com.antonio.mypdffree.domain.util.TimeProvider
import javax.inject.Inject

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
