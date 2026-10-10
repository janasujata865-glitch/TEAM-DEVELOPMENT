package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BirdSkin
import com.example.model.DesertTimeOfDay
import com.example.model.GameStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("desert_bird_prefs", Context.MODE_PRIVATE)

    private val _stats = MutableStateFlow(loadStats())
    val stats: StateFlow<GameStats> = _stats.asStateFlow()

    private val _selectedSkin = MutableStateFlow(loadSelectedSkin())
    val selectedSkin: StateFlow<BirdSkin> = _selectedSkin.asStateFlow()

    private val _timeOfDay = MutableStateFlow(loadTimeOfDay())
    val timeOfDay: StateFlow<DesertTimeOfDay> = _timeOfDay.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _isVibrateEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE, true))
    val isVibrateEnabled: StateFlow<Boolean> = _isVibrateEnabled.asStateFlow()

    private fun loadStats(): GameStats {
        return GameStats(
            bestScore = prefs.getInt(KEY_BEST_SCORE, 0),
            totalGamesPlayed = prefs.getInt(KEY_TOTAL_GAMES, 0),
            totalCactiPassed = prefs.getInt(KEY_TOTAL_CACTI, 0),
            totalCoinsCollected = prefs.getInt(KEY_TOTAL_COINS, 0)
        )
    }

    private fun loadSelectedSkin(): BirdSkin {
        val id = prefs.getString(KEY_SKIN, BirdSkin.SCARLET_FINCH.id)
        return BirdSkin.entries.find { it.id == id } ?: BirdSkin.SCARLET_FINCH
    }

    private fun loadTimeOfDay(): DesertTimeOfDay {
        val name = prefs.getString(KEY_TIME_OF_DAY, DesertTimeOfDay.DAY.name)
        return try {
            DesertTimeOfDay.valueOf(name ?: DesertTimeOfDay.DAY.name)
        } catch (_: Exception) {
            DesertTimeOfDay.DAY
        }
    }

    fun saveBestScoreLive(score: Int): Boolean {
        val currentBest = prefs.getInt(KEY_BEST_SCORE, 0)
        if (score > currentBest) {
            prefs.edit().putInt(KEY_BEST_SCORE, score).commit()
            _stats.value = _stats.value.copy(bestScore = score)
            return true
        }
        return false
    }

    fun recordGameEnded(finalScore: Int, cactiPassedInGame: Int, coinsInGame: Int) {
        val currentBest = prefs.getInt(KEY_BEST_SCORE, 0)
        val newBest = maxOf(currentBest, finalScore)
        val newTotalGames = prefs.getInt(KEY_TOTAL_GAMES, 0) + 1
        val newTotalCacti = prefs.getInt(KEY_TOTAL_CACTI, 0) + cactiPassedInGame
        val newTotalCoins = prefs.getInt(KEY_TOTAL_COINS, 0) + coinsInGame

        // Synchronously commit to persistent flash storage so it can never be lost
        prefs.edit()
            .putInt(KEY_BEST_SCORE, newBest)
            .putInt(KEY_TOTAL_GAMES, newTotalGames)
            .putInt(KEY_TOTAL_CACTI, newTotalCacti)
            .putInt(KEY_TOTAL_COINS, newTotalCoins)
            .commit()

        _stats.value = GameStats(
            bestScore = newBest,
            totalGamesPlayed = newTotalGames,
            totalCactiPassed = newTotalCacti,
            totalCoinsCollected = newTotalCoins
        )
    }

    fun setSelectedSkin(skin: BirdSkin) {
        prefs.edit().putString(KEY_SKIN, skin.id).apply()
        _selectedSkin.value = skin
    }

    fun setTimeOfDay(time: DesertTimeOfDay) {
        prefs.edit().putString(KEY_TIME_OF_DAY, time.name).apply()
        _timeOfDay.value = time
    }

    fun toggleSound(): Boolean {
        val next = !_isSoundEnabled.value
        prefs.edit().putBoolean(KEY_SOUND, next).apply()
        _isSoundEnabled.value = next
        return next
    }

    fun toggleVibrate(): Boolean {
        val next = !_isVibrateEnabled.value
        prefs.edit().putBoolean(KEY_VIBRATE, next).apply()
        _isVibrateEnabled.value = next
        return next
    }

    companion object {
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_TOTAL_GAMES = "total_games"
        private const val KEY_TOTAL_CACTI = "total_cacti"
        private const val KEY_TOTAL_COINS = "total_coins"
        private const val KEY_SKIN = "bird_skin"
        private const val KEY_TIME_OF_DAY = "time_of_day"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATE = "vibrate_enabled"
    }
}
