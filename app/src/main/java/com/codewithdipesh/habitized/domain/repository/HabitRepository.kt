package com.codewithdipesh.habitized.domain.repository
import com.codewithdipesh.habitized.data.local.entity.HabitEntity
import com.codewithdipesh.habitized.domain.model.Goal
import com.codewithdipesh.habitized.domain.model.Habit
import com.codewithdipesh.habitized.domain.model.HabitProgress
import com.codewithdipesh.habitized.domain.model.HabitWithProgress
import com.codewithdipesh.habitized.domain.model.ImageProgress
import com.codewithdipesh.habitized.domain.model.OneTimeTask
import com.codewithdipesh.habitized.domain.model.SubTask
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

interface HabitRepository {

    // Habits
    suspend fun getAllHabits(): List<Habit>
    suspend fun getAllExistingHabits(goalId: UUID? = null) : List<Habit> //so include teh habits having the id as goalid
    suspend fun addOrUpdateHabit(habit: Habit)
    suspend fun deleteHabit(habitId: UUID)
    suspend fun getHabitById(habitId: UUID): Habit?
    suspend fun getHabitsByGoal(goalId: UUID): List<Habit>
    suspend fun getHabitsByGoalForDate(goalId: UUID,date: LocalDate): List<Habit>
    suspend fun getAllHabitsForDate(date: LocalDate): List<Habit>
    suspend fun getOverAllStartDate() : LocalDate
    suspend fun updateStreak(habitId: UUID,current : Int,max: Int)
    suspend fun unLinkGoalFromHabit(habitId: UUID)

    // Habit Progress
    suspend fun addHabitProgresses()
    suspend fun updateHabitProgress(progress: HabitProgress)
    suspend fun getHabitProgress(habitId: UUID, date: String): HabitProgress?
    suspend fun getHabitProgressById(habitProgressId: UUID): HabitWithProgress
    suspend fun getAllHabitProgress(habitId: UUID): List<HabitProgress>?
    suspend fun addTodayHabitProgresses(habits : List<HabitEntity>,date : LocalDate)
    suspend fun onDoneHabitProgress(progressId: UUID)
    suspend fun onSkipHabitProgress(progressId: UUID)
    suspend fun onNotStartedHabitProgress(progressId: UUID)
    suspend fun onStartedHabitProgress(progressId: UUID)
    suspend fun onUpdateCounterHabitProgress(count :Int,progressId: UUID)
    suspend fun deleteHabitProgressForHabit(habitId: UUID)
    suspend fun logHabitForDate(habitId: UUID, date: LocalDate)
    suspend fun checkHabitDoneOrNot(habitId: UUID,date: LocalDate) : Boolean

    suspend fun getAllCompletedDates(habitId: UUID) : List<LocalDate>
    suspend fun getAllCompletedProgress(habitId: UUID) : List<HabitProgress>

    // Goals
    suspend fun addGoal(goal: Goal)
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goalId: UUID)
    suspend fun getGoalById(goalId: UUID): Goal? //sending habits also with it
    suspend fun getAllGoals(): List<Goal>
    suspend fun getExistingGoals(): List<Goal>

    // One-Time Tasks
    suspend fun addOneTimeTask(task: OneTimeTask)
    suspend fun updateOneTimeTask(task: OneTimeTask)
    suspend fun deleteOneTimeTask(taskId: UUID)
    suspend fun toggleOneTimeTask(taskId: UUID)


    // Subtasks
    suspend fun insertSubtask(subtask: SubTask)
    suspend fun getSubtasks(habitProgerssId: UUID) : List<SubTask>
    suspend fun updateSubtask(subtask: SubTask)
    suspend fun deleteSubtask(subtaskId: UUID)
    suspend fun toggleSubTask(subtaskId: UUID)

    //image progress
    suspend fun insertImageProgress(imageProcess : ImageProgress)
    suspend fun getImageProgress(imageProgressId: UUID) : ImageProgress
    suspend fun getImageProgressesForHabit(habitId : UUID) : List<ImageProgress>
    suspend fun deleteImageProgress(imageProgressId: UUID)

    suspend fun getHabitsForDay(date: LocalDate): List<HabitWithProgress>
    suspend fun getTasksForDay(date: LocalDate): List<OneTimeTask>
}
