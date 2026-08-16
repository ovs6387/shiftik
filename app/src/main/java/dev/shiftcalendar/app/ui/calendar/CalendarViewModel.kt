package dev.shiftcalendar.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.shiftcalendar.app.data.AppState
import dev.shiftcalendar.app.data.ShiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarScreenState(
    val app: AppState,
    val visibleMonth: YearMonth,
    val monthPopupOpen: Boolean = false,
    val yearPopupOpen: Boolean = false,
    val selectedDay: LocalDate? = null,
)

class CalendarViewModel(
    private val repository: ShiftRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CalendarScreenState(app = repository.state.value, visibleMonth = YearMonth.now())
    )
    val state: StateFlow<CalendarScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { app ->
                _state.value = _state.value.copy(app = app)
            }
        }
    }

    fun prevMonth() = changeMonth(-1)

    fun nextMonth() = changeMonth(1)

    private fun changeMonth(delta: Long) {
        _state.value = _state.value.copy(visibleMonth = _state.value.visibleMonth.plusMonths(delta))
    }

    fun goToday() {
        _state.value = _state.value.copy(
            visibleMonth = YearMonth.now(),
            monthPopupOpen = false,
            yearPopupOpen = false,
        )
    }

    fun toggleMonthPopup() {
        val s = _state.value
        _state.value = s.copy(monthPopupOpen = !s.monthPopupOpen, yearPopupOpen = false)
    }

    fun toggleYearPopup() {
        val s = _state.value
        _state.value = s.copy(yearPopupOpen = !s.yearPopupOpen, monthPopupOpen = false)
    }

    fun closePopups() {
        _state.value = _state.value.copy(monthPopupOpen = false, yearPopupOpen = false)
    }

    fun selectMonth(month: Int) {
        val s = _state.value
        val y = s.visibleMonth.year
        _state.value = s.copy(
            visibleMonth = YearMonth.of(y, month),
            monthPopupOpen = false,
        )
    }

    fun selectYear(year: Int) {
        val s = _state.value
        _state.value = s.copy(
            visibleMonth = YearMonth.of(year, s.visibleMonth.monthValue),
            yearPopupOpen = false,
        )
    }

    fun selectDay(date: LocalDate) {
        _state.value = _state.value.copy(selectedDay = date)
    }

    fun dismissDay() {
        _state.value = _state.value.copy(selectedDay = null)
    }

    companion object {
        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CalendarViewModel(repository) as T
            }
    }
}