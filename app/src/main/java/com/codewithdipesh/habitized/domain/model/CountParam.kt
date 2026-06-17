package com.codewithdipesh.habitized.domain.model

sealed class CountParam(val displayName: String) {
    object Glasses : CountParam("glasses")
    object Reps : CountParam("reps")
    object Pages : CountParam("pages")
    object Steps : CountParam("steps")
    object Kilometers : CountParam("kilometers")
    object Meters : CountParam("meters")
    object Kilograms : CountParam("kilograms")
    object Calories : CountParam("calories")
    object Sets : CountParam("sets")
    object Minutes : CountParam("minutes")
    object Hours : CountParam("hours")
    object Seconds : CountParam("seconds")
    object Sessions : CountParam("sessions")
    object Tasks : CountParam("tasks")
    object Exercises : CountParam("exercises")
    object Liters : CountParam("liters")
    object Times : CountParam("times")
    object Chapters : CountParam("chapters")
    object Miles : CountParam("miles")
    object Lessons : CountParam("lessons")
    object Attempts : CountParam("attempts")

    override fun toString(): String = displayName

    companion object {
        fun fromString(value: String): CountParam {
            return when (value.lowercase()) {
                "glasses" -> Glasses
                "reps" -> Reps
                "pages" -> Pages
                "steps" -> Steps
                "kilometers"-> Kilometers
                "meters" -> Meters
                "kilograms" -> Kilograms
                "calories" -> Calories
                "sets" -> Sets
                "minutes", "min" -> Minutes
                "hours", "hr", "h" -> Hours
                "seconds" -> Seconds
                "sessions" -> Sessions
                "tasks" -> Tasks
                "exercises" -> Exercises
                "liters", "l" -> Liters
                "times" -> Times
                "chapters" -> Chapters
                "miles" -> Miles
                "lessons", "lessons completed" -> Lessons
                "attempts" -> Attempts
                else -> Tasks
            }
        }
        fun getParams(type: HabitType): List<CountParam> {
            return when (type) {
                HabitType.Count -> listOf(
                    Glasses,
                    Reps,
                    Pages,
                    Steps,
                    Kilometers,
                    Meters,
                    Kilograms,
                    Calories,
                    Sets,
                    Tasks,
                    Exercises,
                    Liters,
                    Times,
                    Chapters,
                    Miles,
                    Lessons,
                    Attempts
                )

                HabitType.Duration -> listOf(
                    Minutes,
                    Hours,
                    Seconds
                )

                HabitType.Session -> listOf(
                    Sessions,
                    Lessons,
                    Chapters
                )
                HabitType.Stopwatch -> emptyList()
                HabitType.OneTime -> emptyList()
            }

        }
    }
}
