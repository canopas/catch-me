package com.canopas.yourspace.ui.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.canopas.yourspace.R

@OptIn(ExperimentalMaterial3Api::class)
object PastOrPresentSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= System.currentTimeMillis()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePicker(
    selectedTimestamp: Long? = System.currentTimeMillis(),
    confirmButtonClick: (Long) -> Unit,
    dismissButtonClick: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp ?: System.currentTimeMillis(),
        selectableDates = PastOrPresentSelectableDates
    )
    DatePickerDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                confirmButtonClick(
                    datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                )
            }) {
                Text(text = stringResource(id = R.string.common_btn_select))
            }
        },
        dismissButton = {
            TextButton(onClick = dismissButtonClick) {
                Text(text = stringResource(id = R.string.common_btn_cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}
