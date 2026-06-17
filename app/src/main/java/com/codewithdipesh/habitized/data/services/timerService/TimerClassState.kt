package com.codewithdipesh.habitized.data.services.timerService

data class TimerClassState(
    val hour: Int,
    val minute: Int,
    val second: Int,
    val isPaused : Boolean,
    val isFinished: Boolean,
    val isStopwatch: Boolean = false
)