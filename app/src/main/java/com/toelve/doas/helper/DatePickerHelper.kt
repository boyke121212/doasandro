package com.toelve.doas.helper

import android.app.DatePickerDialog
import android.content.Context
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.*

object DatePickerHelper {

    private val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun show(
        context: Context,
        editText: EditText,
        defaultMillis: Long = System.currentTimeMillis(),
        minDateMillis: Long? = null,
        onSelected: (Long) -> Unit
    ) {

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = defaultMillis

        val picker = DatePickerDialog(
            context,
            { _, year, month, day ->

                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val resultMillis = cal.timeInMillis

                // tampilkan ke EditText
                editText.setText(displayFormat.format(cal.time))

                // kirim hasil
                onSelected(resultMillis)

            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // set minimal tanggal
        minDateMillis?.let {
            picker.datePicker.minDate = it
        }

        picker.show()
    }
}
