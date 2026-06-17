package com.codewithdipesh.habitized.presentation.homescreen

import android.R.attr.phoneNumber
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codewithdipesh.habitized.domain.model.Frequency
import com.codewithdipesh.habitized.domain.model.Habit
import com.codewithdipesh.habitized.domain.model.HabitProgress
import com.codewithdipesh.habitized.domain.model.HabitType
import com.codewithdipesh.habitized.domain.model.HabitWithProgress
import com.codewithdipesh.habitized.domain.model.OneTimeTask
import com.codewithdipesh.habitized.domain.model.Status
import com.codewithdipesh.habitized.domain.model.SubTask
import com.codewithdipesh.habitized.domain.repository.HabitRepository
import com.codewithdipesh.habitized.presentation.util.IntToWeekDayMap
import com.codewithdipesh.habitized.presentation.util.toDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.codewithdipesh.habitized.EMAIL_TO
import com.codewithdipesh.habitized.R
import com.codewithdipesh.habitized.presentation.navigation.Screen

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo : HabitRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(HomeScreenUI())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            selectedDate = LocalDate.now()
        )
        loadHomePage(_uiState.value.selectedDate)
    }

    fun loadHomePage(date: LocalDate){
        viewModelScope.launch(Dispatchers.IO){
            val habits = repo.getHabitsForDay(date)
            val todos = repo.getTasksForDay(date)
            //checking for ongoing duration or session habit
            val ongoingHabit = habits
                .asSequence()
                .filter { habit ->
                    (habit.habit.type == HabitType.Duration || habit.habit.type == HabitType.Session || habit.habit.type == HabitType.Stopwatch) &&
                            habit.progress.status == Status.Ongoing
                }
                .firstOrNull()
            // Update ongoing timer if habit exists
            ongoingHabit?.let { addOngoingTimer(it) }
            _uiState.value = _uiState.value.copy(
                habitWithProgressList = habits,
                todos = todos,
                ongoingHabit = ongoingHabit
            )
        }
    }

    fun onOptionSelected(option: HomeScreenOption){
        _uiState.value = _uiState.value.copy(
            selectedOption = option
        )
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date
        )
        loadHomePage(date)
    }

    fun openDatePicker(){
        _uiState.value = _uiState.value.copy(
            isShowingDatePicker = true
        )
    }
    fun closeDatePicker(){
        _uiState.value = _uiState.value.copy(
            isShowingDatePicker = false
        )
    }

    suspend fun addUpdateSubTasks(subtasks : List<SubTask>, habitProgressId : UUID){
        //add on the ui first then add on the database

        //change in local first(Ui)
        var updatedList = _uiState.value.habitWithProgressList.toMutableList()
        updatedList = updatedList.map {
            if(it.progress.progressId == habitProgressId){
                it.copy(subtasks = subtasks)
            }else{
                it
            }
        } as MutableList<HabitWithProgress>
        _uiState.value = _uiState.value.copy(
            habitWithProgressList = updatedList
        )
        //change in room
        val previous = repo.getSubtasks(habitProgerssId = habitProgressId )

        val deleted = previous.filter { !subtasks.contains(it) }
        val added = subtasks.filter { !previous.contains(it) }
        val updated = subtasks.filter { previous.contains(it) }

        viewModelScope.launch(Dispatchers.IO) {
            added.forEach {
                repo.insertSubtask(it)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            updated.forEach {
                repo.insertSubtask(it)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            deleted.forEach {
                repo.deleteSubtask(it.subtaskId)
            }
        }
    }

    fun toggleSubtask(subtask: SubTask){
        val updatedList = _uiState.value.habitWithProgressList.map {habitWithProgress->
            if(habitWithProgress.habit.type == HabitType.Session && habitWithProgress.subtasks.contains(subtask)){
                val updatedSubTasks = habitWithProgress.subtasks.map{
                    if(it.subtaskId == subtask.subtaskId) it.copy(isCompleted = !it.isCompleted) else it
                }
                habitWithProgress.copy(subtasks = updatedSubTasks)
            } else habitWithProgress
        }

        _uiState.value = _uiState.value.copy(
            habitWithProgressList = updatedList
        )
        viewModelScope.launch(Dispatchers.IO) {
            repo.toggleSubTask(subtask.subtaskId)
        }
    }

    fun onSkipHabit(habitProgress : HabitProgress){
        viewModelScope.launch(Dispatchers.IO){
            repo.onSkipHabitProgress(progressId = habitProgress.progressId)
        }
    }
    fun onDoneHabit(habitWithProgress: HabitWithProgress){
        viewModelScope.launch(Dispatchers.IO){
            repo.onDoneHabitProgress(progressId = habitWithProgress.progress.progressId)
            updateStreak(habitWithProgress)
        }
    }
    fun onUnSkipDoneHabit(habitWithProgress: HabitWithProgress){
        viewModelScope.launch(Dispatchers.IO){
            repo.onNotStartedHabitProgress(progressId = habitWithProgress.progress.progressId)
            updateStreak(habitWithProgress,true)
        }
    }

    fun onLogHabitForDate(habitWithProgress: HabitWithProgress, date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.logHabitForDate(habitWithProgress.habit.habit_id!!, date)
            updateStreak(habitWithProgress)
            loadHomePage(_uiState.value.selectedDate)
        }
    }

    fun onUpdateCounter(count : Int, habitWithProgress : HabitWithProgress){
        //local ui
        val ifDone = count >= (habitWithProgress.progress.targetCount ?: 0)
        val prevStatus = habitWithProgress.progress.status
        val updatedList = _uiState.value.habitWithProgressList.map {it->
            if(it.progress.progressId == habitWithProgress.progress.progressId){
                it.copy(
                    progress = habitWithProgress.progress.copy(
                        currentCount = count,
                        status = if(ifDone) Status.Done
                                 else Status.NotStarted
                    )
                )
            }else{
                it
            }
        }

        _uiState.value = _uiState.value.copy(
            habitWithProgressList = updatedList
        )
        viewModelScope.launch(Dispatchers.IO){
            repo.onUpdateCounterHabitProgress(count,habitWithProgress.progress.progressId)
        }
        if(ifDone) {
            onDoneHabit(habitWithProgress)
        }
        else{//update only if prev was done -> then change to not started
            if(prevStatus != Status.NotStarted){
                onUnSkipDoneHabit(habitWithProgress)
            }
        }

    }

    fun addOngoingTimer(habitWithProgress: HabitWithProgress){
        if(_uiState.value.ongoingHabit == null){
            _uiState.value = _uiState.value.copy(
                ongoingHabit = habitWithProgress
            )
        }
    }
    fun updateOngoingTimer(hour: Int, minute: Int, second: Int){
        _uiState.value = _uiState.value.copy(
            ongoingHour = hour,
            ongoingMinute = minute,
            ongoingSecond = second
        )
    }
    suspend fun finishTimer(){
        val ongoingHabit = _uiState.value.ongoingHabit

        if(ongoingHabit != null) {
            if(ongoingHabit.habit.type == HabitType.Duration) {
                repo.onDoneHabitProgress(ongoingHabit.progress.progressId)
                updateStreak(ongoingHabit)
            } else if (ongoingHabit.habit.type == HabitType.Stopwatch) {
                val elapsedDuration = LocalTime.of(
                    _uiState.value.ongoingHour,
                    _uiState.value.ongoingMinute,
                    _uiState.value.ongoingSecond
                )
                repo.updateProgressDuration(ongoingHabit.progress.progressId, elapsedDuration)
                repo.onDoneHabitProgress(ongoingHabit.progress.progressId)
                updateStreak(ongoingHabit)
            } else {
                val prevCount = ongoingHabit.progress.currentCount
                val targetCount = ongoingHabit.progress.targetCount

                if(prevCount != null) {
                    val newCount = prevCount + 1
                    //on update the counter
                    repo.onUpdateCounterHabitProgress(newCount,ongoingHabit.progress.progressId)

                    if(newCount == targetCount) {
                        repo.onDoneHabitProgress(ongoingHabit.progress.progressId)
                        updateStreak(ongoingHabit)
                    } else {
                        repo.onNotStartedHabitProgress(ongoingHabit.progress.progressId)
                    }
                } else {
                    Log.e("TimerFinish", "ERROR: prevCount is null!")
                }
            }
        } else {
            Log.w("TimerFinish", "WARNING: ongoingHabit is null!")
        }
    }

    suspend fun updateStreak(habitWithProgress: HabitWithProgress,isSkipped : Boolean = false){
        val completedDates = repo.getAllCompletedDates(habitWithProgress.habit.habit_id!!)
        val streak = calculateCurrentStreak(habitWithProgress.habit,completedDates)
        repo.updateStreak(
            habitId = habitWithProgress.habit.habit_id,
            current = streak,
            max = if (isSkipped) {
                if (habitWithProgress.habit.maxStreak == habitWithProgress.habit.currentStreak) max(
                    streak,
                    0
                )
                else max(streak, habitWithProgress.habit.maxStreak)
            } else max(streak, habitWithProgress.habit.maxStreak)
        )

    }
    fun calculateCurrentStreak(
        habit: Habit,
        completedDatesDesc: List<LocalDate>
    ): Int {

        if (completedDatesDesc.isEmpty()) return 0

        val completedSet = completedDatesDesc.toSet()

        var currentDate = LocalDate.now()
        var streak = 0
        val maxBackwardDays = 365
        var iterations = 0
        var startedCounting = false

        while (iterations < maxBackwardDays) {
            if (!isScheduledDay(habit, currentDate)) {
                currentDate = currentDate.minusDays(1)
                continue
            }
            if (completedSet.contains(currentDate)) {
                streak++
                startedCounting = true
            } else if(startedCounting) {
                break
            }else{
                //today is not done
            }
            currentDate = currentDate.minusDays(1)
            iterations++
        }

        return streak
    }
    fun isScheduledDay(habit: Habit, date: LocalDate): Boolean {
        return when (habit.frequency) {
            Frequency.Daily -> true
            Frequency.Weekly -> {
                val frequencyMap = IntToWeekDayMap(habit.days_of_week)
                frequencyMap[date.dayOfWeek.toDays()] == true
            }
            Frequency.Monthly -> habit.daysOfMonth?.contains(date.dayOfMonth) ?: false
            else -> false  // Handle other frequencies if needed
        }
    }

    fun addTodo(){
        if(_uiState.value.todos.find { it.title == "" } == null){
            val todoList = _uiState.value.todos.toMutableList()
            val newTodo = OneTimeTask(
                taskId = UUID.randomUUID(),
                title = "",
                isCompleted = false,
                date = _uiState.value.selectedDate,
                reminder_time = null
            )
            _uiState.value = _uiState.value.copy(
                todos = todoList + newTodo
            )
        }
    }
    fun updateTodo(title : String,id : UUID){
        val todoList = _uiState.value.todos.toMutableList()
        val updatedList = todoList
            .map { if(it.taskId == id) it.copy(title = title) else it }
        _uiState.value = _uiState.value.copy(
            todos = updatedList
        )
        //update repo
        viewModelScope.launch { repo.updateOneTimeTask(_uiState.value.todos.find { it.taskId == id }!!) }
    }
    fun deleteTodo(id : UUID){
        val todoList = _uiState.value.todos.toMutableList()
        val updatedList = todoList.filter { it.taskId != id }
        _uiState.value = _uiState.value.copy(
            todos = updatedList
        )
        //update repo
        viewModelScope.launch { repo.deleteOneTimeTask(id) }
    }
    fun toggleTodo(id : UUID){
        val todoList = _uiState.value.todos.toMutableList()
        val updatedList = todoList
            .map {
                if(it.taskId == id) it.copy(isCompleted = !it.isCompleted)
                else it
            }
        _uiState.value = _uiState.value.copy(
            todos = updatedList
        )
        //update repo
        viewModelScope.launch { repo.toggleOneTimeTask(id) }
    }


    //feedback ,follow and github
    fun sendFeedback(context : Context){
        try {
            val message = "Hi Dipesh,I've just used Habitized , I wanted to suggest you.."
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://wa.me/917602154121?text=${Uri.encode(message)}".toUri()
                `package` = "com.whatsapp"
            }
            if(intent.resolveActivity(context.packageManager) != null){
                //it means whatsapp is installed
                context.startActivity(intent)
            }else{
                // WhatsApp not installed, open in browser
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://wa.me/917602154121?text=${Uri.encode(message)}".toUri()
                )
                context.startActivity(browserIntent)
            }
        }catch (e: Exception){
        }
    }
    fun onFollow(context : Context){
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://x.com/diprssn".toUri()
                `package` = "com.twitter.android"
            }
            if(intent.resolveActivity(context.packageManager) != null){
                //it means x is installed
                context.startActivity(intent)
            }else{
                // x not installed, open in browser
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://x.com/diprssn".toUri()
                )
                context.startActivity(browserIntent)
            }
        }catch (e: Exception){
        }
    }
    fun onCodeBase(context : Context){
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://github.com/codewithdipesh/Habitized".toUri()
                `package` = "com.github.android"
            }
            if(intent.resolveActivity(context.packageManager) != null){
                //it means x is installed
                context.startActivity(intent)
            }else{
                // x not installed, open in browser
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/codewithdipesh/Habitized".toUri()
                )
                context.startActivity(browserIntent)
            }
        }catch (e: Exception){
        }
    }

    //report bug
    fun openMail(context: Context) {
        val subject = "Bug Report - ${context.getString(R.string.app_name)}"

        val bodyText = buildString {
            append("Report Description:\n\n")
            append("Device Info:\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android Version: ${Build.VERSION.RELEASE}\n")
            append("App Version: ${getAppVersion(context)}\n")
            append("Timestamp: ${System.currentTimeMillis()}\n\n")
            append("Describe the Bug :")
        }

        val uriString = buildString {
            append("mailto:").append(Uri.encode(EMAIL_TO))
            append("?subject=").append(Uri.encode(subject))
            append("&body=").append(Uri.encode(bodyText))   // ← add body
        }

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(uriString))
        context.startActivity(Intent.createChooser(intent, "Send bug report"))
    }
    fun getMyThoughts(navController: NavController){
        navController.navigate(Screen.MyThoughts.route)
    }
    fun addWidget(navController: NavController){
        navController.navigate(Screen.AddWidget.route)
    }
    fun shareApp(context: Context){
        val link = "https://habitized.diprssn.xyz"
        val sharedMessage = "Hey! I've built an amazing habit-building streak with Habitized! You can too! Try it out: $link"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT,sharedMessage)
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share with")
        context.startActivity(chooserIntent)

    }
    fun openPlayStoreForRating(context:Context){
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
        intent.setPackage("com.android.vending")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        }

    }
    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }


}