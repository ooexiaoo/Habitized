package com.codewithdipesh.habitized.presentation.timerscreen.durationScreen

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.codewithdipesh.habitized.data.services.timerService.TimerService
import com.codewithdipesh.habitized.data.sharedPref.HabitPreference
import com.codewithdipesh.habitized.domain.model.Frequency
import com.codewithdipesh.habitized.domain.model.Habit
import com.codewithdipesh.habitized.domain.model.HabitType
import com.codewithdipesh.habitized.domain.model.HabitWithProgress
import com.codewithdipesh.habitized.domain.model.Status
import com.codewithdipesh.habitized.domain.repository.HabitRepository
import com.codewithdipesh.habitized.presentation.timerscreen.Theme
import com.codewithdipesh.habitized.presentation.timerscreen.TimerState
import com.codewithdipesh.habitized.presentation.util.IntToWeekDayMap
import com.codewithdipesh.habitized.presentation.util.toDays
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max

@HiltViewModel
class DurationViewModel @Inject constructor(
    private val repo : HabitRepository,
    private val pref : HabitPreference
) : ViewModel(){

     private val _state = MutableStateFlow(DurationUI())
     val state = _state.asStateFlow()

    init {
        val theme = pref.getTheme(_state.value.theme.displayName)
        _state.value = _state.value.copy(
            theme = Theme.Companion.fromString(theme)
        )
    }

    suspend fun init(id : UUID){
        val response = repo.getHabitProgressById(id)
        val isStopwatch = response.habit.type == HabitType.Stopwatch
        _state.value = _state.value.copy(
            progressId = id,
            habitWithProgress = response,
            timerState = when{
                isStopwatch && response.progress.status != Status.Done -> TimerState.Resumed
                response.progress.status is Status.Done -> TimerState.Finished
                response.progress.status == Status.Ongoing -> TimerState.Resumed
                else -> TimerState.Not_Started
            },
            isStarted = isStopwatch || response.progress.status == Status.Ongoing
        )
    }

    fun isStopwatch(): Boolean {
        return _state.value.habitWithProgress?.habit?.type == HabitType.Stopwatch
    }

    fun resumeTimer(){
        _state.value = _state.value.copy(
            timerState = TimerState.Resumed
        )
    }
    fun pauseTimer(){
        _state.value = _state.value.copy(
            timerState = TimerState.Paused
        )
    }
    fun finishTimer(){
        _state.value = _state.value.copy(
            timerState = TimerState.Finished
        )
    }
    suspend fun cancelTimer(){
        repo.onNotStartedHabitProgress(_state.value.progressId!!)
        _state.value = _state.value.copy(
            timerState = TimerState.Not_Started,
            isStarted = false
        )
    }

    fun clearUi(){
        _state.value = _state.value.copy(
            progressId = null,
            habitWithProgress = null,
            timerState = TimerState.Not_Started,
            isThemesOpen=false
        )
    }

    suspend fun startTimer(context : Context, totalSeconds : Int){
        _state.value = _state.value.copy(
            isStarted = true,
            timerState = TimerState.Resumed
        )
        val isStopwatch = isStopwatch()
        val intent = Intent(context, TimerService::class.java).apply {
            putExtra("duration_seconds",totalSeconds)
            putExtra("habit",_state.value.habitWithProgress!!.habit.title)
            putExtra("id",_state.value.progressId.toString())
            putExtra("color",_state.value.habitWithProgress!!.habit.colorKey)
            putExtra("is_stopwatch", isStopwatch)
        }
        context.startForegroundService(intent)
        repo.onStartedHabitProgress(_state.value.progressId!!)
    }

    suspend fun finishHabit(){
        _state.value.progressId?.let { id ->
            if (isStopwatch()) {
                val elapsedDuration = java.time.LocalTime.of(
                    _state.value.elapsedHour,
                    _state.value.elapsedMinute,
                    _state.value.elapsedSecond
                )
                repo.updateProgressDuration(id, elapsedDuration)
            }
            repo.onDoneHabitProgress(id)
            updateStreak(_state.value.habitWithProgress!!)
        }
    }

    fun updateElapsedTime(hour: Int, minute: Int, second: Int){
        _state.value = _state.value.copy(
            elapsedHour = hour,
            elapsedMinute = minute,
            elapsedSecond = second
        )
    }

    fun chooseTheme(theme: Theme){
        _state.value = _state.value.copy(
            theme = theme
        )
        pref.updateTheme(theme.displayName)
    }
    fun openThemes(){
        _state.value = _state.value.copy(
            isThemesOpen = true
        )
    }
    fun closeThemes(){
        _state.value = _state.value.copy(
            isThemesOpen = false
        )
    }

    suspend fun updateStreak(habitWithProgress: HabitWithProgress,isSkipped : Boolean = false){
        val completedDates = repo.getAllCompletedDates(habitWithProgress.habit.habit_id!!)
        val streak = calculateCurrentStreak(habitWithProgress.habit,completedDates)
        repo.updateStreak(
            habitId = habitWithProgress.habit.habit_id,
            current = streak,
            max = max(streak,habitWithProgress.habit.maxStreak)
        )

    }
    fun calculateCurrentStreak(
        habit: Habit,
        completedDatesDesc: List<LocalDate>
    ): Int {
        var streak = 0
        var expectedDate = LocalDate.now()

        for (date in completedDatesDesc) {
            // Skip non-scheduled days
            expectedDate = getPreviousScheduledDate(habit,expectedDate)

            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }
    fun getPreviousScheduledDate(habit: Habit, fromDate: LocalDate): LocalDate {
        var date = fromDate
        while (true) {
            when (habit.frequency) {
                Frequency.Weekly -> {
                    val frequencyMap = IntToWeekDayMap(habit.days_of_week)
                    if (frequencyMap[date.dayOfWeek.toDays()] == true) return date
                }
                Frequency.Monthly -> {
                    val frequency = habit.daysOfMonth ?: emptyList()
                    if (frequency.contains(date.dayOfMonth)) return date
                }
                else -> return date
            }
            date = date.minusDays(1)
        }
    }



}