package com.example.helloworld.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloworld.data.BibleGamePlayerStats
import com.example.helloworld.data.BibleGameRepository
import kotlinx.coroutines.launch

class BibleGameProgressViewModel(
    private val repository: BibleGameRepository = BibleGameRepository()
) : ViewModel() {

    var stats by mutableStateOf<BibleGamePlayerStats?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            stats = repository.loadPlayerStats()
        }
    }

    fun recordQuizResult(score: Int, total: Int) {
        viewModelScope.launch {
            repository.recordQuizResult(score, total)?.let { stats = it }
        }
    }

    fun recordMemoryVerseCompleted() {
        viewModelScope.launch {
            repository.recordMemoryVerseCompleted()?.let { stats = it }
        }
    }
}
