package vcmsa.projects.wordleandroidclient

import java.text.SimpleDateFormat
import java.util.*

/** Returns today's date in YYYY-MM-DD format (device local time). */
fun todayIso(): String {
    val tz = TimeZone.getDefault()
    val cal = Calendar.getInstance(tz)
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
    return fmt.format(cal.time)
}
