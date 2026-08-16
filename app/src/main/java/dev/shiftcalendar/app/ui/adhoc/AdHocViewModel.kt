package dev.shiftcalendar.app.ui.adhoc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.shiftcalendar.app.data.AdHoc
import dev.shiftcalendar.app.data.AppState
import dev.shiftcalendar.app.data.ShiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AdHocScreenState(
    val app: AppState,
    val dialogOpen: Boolean = false,
    val editing: AdHoc? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val shift: Int = 1,
    val type: Int = 0,
    val showDatePicker: Boolean = false,
    val confirmDelete: Boolean = false,
)

class AdHocViewModel(
    private val repository: ShiftRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AdHocScreenState(app = repository.state.value)
    )
    val state: StateFlow<AdHocScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { app ->
                _state.value = _state.value.copy(app = app)
            }
        }
    }

    fun openAdd() {
        _state.value = _state.value.copy(
            dialogOpen = true,
            editing = null,
            start = null,
            end = null,
            shift = 1,
            type = 0,
        )
    }

    fun openEdit(a: AdHoc) {
        _state.value = _state.value.copy(
            dialogOpen = true,
            editing = a,
            start = a.startDate,
            end = a.endDate,
            shift = a.shift,
            type = a.shiftTypeId,
        )
    }

    fun dismiss() {
        _state.value = _state.value.copy(
            dialogOpen = false,
            showDatePicker = false,
            confirmDelete = false,
        )
    }

    /** Первый тап — начало периода, второй — конец; ещё один — начать выбор заново. */
    fun tapDate(date: LocalDate) {
        val s = _state.value
        val (ns, ne) = when {
            s.start == null -> date to null
            s.end == null -> {
                if (date.isBefore(s.start)) date to s.start else s.start to date
            }
            else -> date to null
        }
        _state.value = s.copy(start = ns, end = ne)
    }

    fun setShift(shift: Int) {
        _state.value = _state.value.copy(shift = shift)
    }

    fun setType(type: Int) {
        _state.value = _state.value.copy(type = type)
    }

    fun requestDatePicker() {
        _state.value = _state.value.copy(showDatePicker = true)
    }

    fun dismissDatePicker() {
        _state.value = _state.value.copy(showDatePicker = false)
    }

    fun requestDelete() {
        _state.value = _state.value.copy(confirmDelete = true)
    }

    fun save() {
        val s = _state.value
        val start = s.start ?: return dismiss()
        val end = s.end ?: return dismiss()
        val editing = s.editing
        if (editing != null) repository.deleteAdHoc(editing)
        val existing = repository.state.value.adHocs.filter {
            it.shift == s.shift && it.startDate <= end && it.endDate >= start
        }
        existing.forEach { repository.deleteAdHoc(it) }
        repository.addAdHoc(s.shift, start, end, s.type)
        dismiss()
    }

    fun delete() {
        val editing = _state.value.editing
        if (editing != null) repository.deleteAdHoc(editing)
        dismiss()
    }

    companion object {
        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdHocViewModel(repository) as T
            }
    }
}