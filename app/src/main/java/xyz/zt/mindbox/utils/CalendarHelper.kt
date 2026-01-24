package xyz.zt.mindbox.utils

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import java.util.*

object CalendarHelper {

    private fun getPrimaryCalendarId(context: Context): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idColumnIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val primaryColumnIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumnIndex)
                val isPrimary = cursor.getInt(primaryColumnIndex)
                if (isPrimary == 1) {
                    cursor.close()
                    return id
                }
            }
            cursor.close()
        }
        return 1
    }

    fun addEventToCalendar(
        context: Context,
        title: String,
        notes: String,
        dateStr: String,
        timeStr: String
    ) {
        try {
            val dateParts = dateStr.split("/")
            val timeParts = timeStr.split(":")

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[2].toInt())
                set(Calendar.MONTH, dateParts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
            }

            val startMillis: Long = calendar.timeInMillis
            val endMillis: Long = startMillis + (30 * 60 * 1000)

            val calendarId = getPrimaryCalendarId(context)

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, notes)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                Log.d("CalendarHelper", "Evento creado con éxito: $uri")
                val eventId = uri.lastPathSegment?.toLong() ?: return

                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.MINUTES, 10)
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            } else {
                Log.e("CalendarHelper", "Error: No se pudo insertar el evento")
            }
        } catch (e: Exception) {
            Log.e("CalendarHelper", "Error al añadir al calendario: ${e.message}")
        }
    }
}