package com.surce555.shuju.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surce555.shuju.data.models.DataSource
import com.surce555.shuju.data.models.MarketSnapshot
import com.surce555.shuju.data.repository.MarketRepository
import com.surce555.shuju.utils.AppLogger
import com.surce555.shuju.utils.TextFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val snapshot: MarketSnapshot? = null,
    val formattedText: String? = null,
    val selectedSource: DataSource = DataSource.BINANCE_VISION,
    val error: String? = null
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun setDataSource(source: DataSource) {
        _uiState.value = _uiState.value.copy(selectedSource = source)
        AppLogger.log("[VM] DataSource changed to ${source.displayName}")
    }

    fun fetchData() {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            AppLogger.log("[VM] Start fetch, source=${_uiState.value.selectedSource.displayName}")
            try {
                val snapshot = MarketRepository.fetchMarketSnapshot(_uiState.value.selectedSource)
                val text = TextFormatter.format(snapshot)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    snapshot = snapshot,
                    formattedText = text,
                    error = null
                )
                AppLogger.log("[VM] Fetch complete")
            } catch (e: Exception) {
                val errMsg = "采集失败: ${e.message}"
                AppLogger.log("[VM] ERROR: $errMsg")
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = errMsg
                )
            }
        }
    }
}
