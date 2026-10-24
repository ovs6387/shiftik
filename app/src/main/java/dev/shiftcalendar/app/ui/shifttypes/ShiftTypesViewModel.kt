package dev.shiftcalendar.app.ui.shifttypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.data.ShiftType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShiftTypesScreenState(
    val shiftTypes: List<ShiftType> = emptyList(),
    val colors: Map<Pair<Int, Int>, Int> = emptyMap(),
)

class ShiftTypesViewModel(
    private val repository: ShiftRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShiftTypesScreenState())
    val state: StateFlow<ShiftTypesScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { app ->
                _state.value = ShiftTypesScreenState(
                    shiftTypes = app.shiftTypes,
                    colors = app.colors.colors.toMap(),
                )
            }
        }
    }

    fun rename(id: Int, name: String) {
        val t = _state.value.shiftTypes.firstOrNull { it.id == id } ?: return
        repository.updateShiftType(t.copy(name = name.take(15)))
    }

    fun toggleVisible(id: Int) {
        val t = _state.value.shiftTypes.firstOrNull { it.id == id } ?: return
        repository.updateShiftType(t.copy(visible = !t.visible))
    }

    companion object {
        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ShiftTypesViewModel(repository) as T
            }
    }
}