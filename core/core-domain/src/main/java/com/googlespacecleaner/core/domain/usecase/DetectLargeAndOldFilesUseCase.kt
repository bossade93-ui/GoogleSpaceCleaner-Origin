package com.googlespacecleaner.core.domain.usecase

import com.googlespacecleaner.core.domain.model.ItemFlag
import com.googlespacecleaner.core.domain.model.ScannedItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DetectLargeAndOldFilesUseCase @Inject constructor() {

    operator fun invoke(
        items: List<ScannedItem>,
        largeThresholdBytes: Long = DEFAULT_LARGE_THRESHOLD_BYTES,
        oldThresholdMonths: Int = DEFAULT_OLD_THRESHOLD_MONTHS,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ScannedItem> {
        val oldThresholdMillis = nowMillis - TimeUnit.DAYS.toMillis(oldThresholdMonths * 30L)

        return items.map { item ->
            var flags = item.flags
            if (item.sizeBytes >= largeThresholdBytes) {
                flags = flags + ItemFlag.LARGE_FILE
            }
            if (item.modifiedAt < oldThresholdMillis) {
                flags = flags + ItemFlag.OLD_FILE
            }
            item.copy(flags = flags)
        }
    }

    companion object {
        const val DEFAULT_LARGE_THRESHOLD_BYTES = 100L * 1024 * 1024 // 100 Mo
        const val DEFAULT_OLD_THRESHOLD_MONTHS = 12
    }
}
