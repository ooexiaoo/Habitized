package com.codewithdipesh.habitized.presentation.timerscreen.durationScreen

import com.codewithdipesh.habitized.domain.model.HabitWithProgress
import com.codewithdipesh.habitized.presentation.timerscreen.Theme
import com.codewithdipesh.habitized.presentation.timerscreen.TimerState
import java.util.UUID

data class DurationUI(
    val  progressId: UUID? =  null,
    val habitWithProgress: HabitWithProgress? = null,
    val timerState : TimerState = TimerState.Not_Started,
    val theme : Theme = Theme.Normal,
    val isStarted : Boolean = false,
    val isThemesOpen : Boolean = false,
    val elapsedHour: Int = 0,
    val elapsedMinute: Int = 0,
    val elapsedSecond: Int = 0
)
