package com.codewithdipesh.habitized.presentation.homescreen.component

import androidx.compose.ui.text.font.FontStyle
import com.codewithdipesh.habitized.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithdipesh.habitized.domain.model.Habit
import com.codewithdipesh.habitized.domain.model.HabitType
import com.codewithdipesh.habitized.domain.model.HabitWithProgress
import com.codewithdipesh.habitized.domain.model.ReminderType
import com.codewithdipesh.habitized.domain.model.Status
import com.codewithdipesh.habitized.domain.model.SubTask
import com.codewithdipesh.habitized.presentation.util.getThemedColorFromKey
import com.codewithdipesh.habitized.presentation.util.toWord
import com.codewithdipesh.habitized.ui.theme.instrumentSerif
import com.codewithdipesh.habitized.ui.theme.regular
import java.time.LocalDate

//count based
@Composable
fun HabitCard(
    habitWithProgress: HabitWithProgress,
    modifier: Modifier = Modifier,
    onAddCounter : (HabitWithProgress) -> Unit = {},
    onDone : (HabitWithProgress) -> Unit = {},
    onSkip : (HabitWithProgress) -> Unit = {},
    onUnSkip : (HabitWithProgress) -> Unit = {},
    onStartDuration : (HabitWithProgress)-> Unit = {},
    onStartSession : (HabitWithProgress)-> Unit = {},
    onFutureTaskStateChange : () ->Unit = {},
    onSubTaskAdding : (HabitWithProgress)->Unit,
    onToggle : (SubTask)-> Unit,
    onHabitClick : (Habit) -> Unit,
    onLogForAnotherDay : (HabitWithProgress) -> Unit = {},
){
    when(habitWithProgress.habit.type){
        HabitType.Count -> {
            CountHabit(
                habitWithProgress = habitWithProgress,
                onAddCounter = {
                    onAddCounter(habitWithProgress)
                },
                onDeny = onFutureTaskStateChange,
                onClick = {
                    onHabitClick(habitWithProgress.habit)
                },
                onLogForAnotherDay = { onLogForAnotherDay(habitWithProgress) }
            )
        }
        HabitType.Duration -> {
            DurationHabit(
                habitWithProgress = habitWithProgress,
                onStart = {
                    onStartDuration(it)
                },
                onDeny = onFutureTaskStateChange,
                onClick = {
                    onHabitClick(habitWithProgress.habit)
                },
                onLogForAnotherDay = { onLogForAnotherDay(habitWithProgress) }
            )
        }
        HabitType.OneTime ->{
            SwipeContainer(
                item = habitWithProgress,
                onDone = { onDone(it)},
                onSkip = { onSkip(it)},
                onUnSkipDone = {onUnSkip(it)},
                content = {
                    OneTimeHabit(
                        habitWithProgress = habitWithProgress,
                        onClick = {
                            onHabitClick(habitWithProgress.habit)
                        },
                        onLogForAnotherDay = { onLogForAnotherDay(habitWithProgress) }
                    )
                },
                swipable = habitWithProgress.progress.date <= LocalDate.now(),
                onDeny = onFutureTaskStateChange,
                isReminder = habitWithProgress.habit.reminderType != null,
                height = 60
            )
        }
        HabitType.Session -> {
            SessionHabit(
                habitWithProgress = habitWithProgress,
                onStartSession = {
                    onStartSession(it)
                },
                onAddSubTask = {
                    onSubTaskAdding(habitWithProgress)
                },
                onToggle = {onToggle(it)},
                onDeny = onFutureTaskStateChange,
                onClick = {
                    onHabitClick(habitWithProgress.habit)
                },
                onLogForAnotherDay = { onLogForAnotherDay(habitWithProgress) }
            )
        }
    }
}

@Composable
fun OneTimeHabit(
    modifier: Modifier = Modifier,
    habitWithProgress: HabitWithProgress,
    onClick: () -> Unit,
    onLogForAnotherDay : () -> Unit = {}
) {
    HabitElement(
        color = getThemedColorFromKey(habitWithProgress.habit.colorKey),
        reminder = when(habitWithProgress.habit.reminderType){
            is ReminderType.Once -> habitWithProgress.habit.reminderType.reminderTime
            else -> null
        },
        isDone = habitWithProgress.progress.status != Status.NotStarted,
        clickable = true,
        onClick = onClick
    ){
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            //title
            Text(
                text = habitWithProgress.habit.title,
                style = TextStyle(
                    color = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.scrim,
                    fontFamily = regular,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            Icon(
                painter = painterResource(R.drawable.clock),
                contentDescription = "Log for another day",
                tint = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary.copy(0.6f)
                        else MaterialTheme.colorScheme.scrim,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onLogForAnotherDay() }
            )
        }
    }
}

@Composable
fun CountHabit(
    modifier: Modifier = Modifier,
    habitWithProgress: HabitWithProgress,
    onAddCounter : ()->Unit,
    onDeny : ()->Unit,
    onClick : () -> Unit,
    onLogForAnotherDay : () -> Unit = {}
) {
    HabitElement(
        color = getThemedColorFromKey(habitWithProgress.habit.colorKey),
        reminder = when(habitWithProgress.habit.reminderType){
            is ReminderType.Once -> habitWithProgress.habit.reminderType.reminderTime
            else -> null
        },
        isDone = habitWithProgress.progress.status != Status.NotStarted,
        clickable = true,
        onClick = onClick
    ){
        Row(modifier = Modifier
            .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //title
            Text(
                text = habitWithProgress.habit.title,
                style = TextStyle(
                    color = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.scrim,
                    fontFamily = regular,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                modifier = Modifier
                    .fillMaxWidth(.45f)
                    .wrapContentHeight()
            )

            Row(
                modifier = Modifier.wrapContentSize(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                //progress
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = "${habitWithProgress.progress.currentCount}/"+
                                "${habitWithProgress.progress.targetCount}",
                        style = TextStyle(
                            color = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.scrim,
                            fontFamily = regular,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    //param ex:- glasses
                    Text(
                        text = habitWithProgress.progress.countParam.displayName,
                        style = TextStyle(
                            color = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.scrim,
                            fontFamily = regular,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    )
                }

                //clock icon for log on another day
                Icon(
                    painter = painterResource(R.drawable.clock),
                    contentDescription = "Log for another day",
                    tint = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary.copy(0.6f)
                            else MaterialTheme.colorScheme.scrim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onLogForAnotherDay() }
                )

                //button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            color = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.inverseOnSurface
                            else MaterialTheme.colorScheme.scrim,
                            shape = CircleShape
                        )
                        .clickable {
                            if(habitWithProgress.progress.date <= LocalDate.now()){
                                onAddCounter()
                            }else{
                                onDeny()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = if(habitWithProgress.progress.status == Status.NotStarted) painterResource(R.drawable.add)
                        else painterResource(R.drawable.tick_small),
                        contentDescription = "add ${habitWithProgress.habit.title}",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }


        }
    }
}

@Composable
fun DurationHabit(
    modifier: Modifier = Modifier,
    habitWithProgress: HabitWithProgress,
    onStart : (HabitWithProgress) ->Unit,
    onDeny: () -> Unit,
    onClick: () -> Unit,
    onLogForAnotherDay : () -> Unit = {}
) {
    HabitElement(
        color = getThemedColorFromKey(habitWithProgress.habit.colorKey),
        reminder = when(habitWithProgress.habit.reminderType){
            is ReminderType.Once -> habitWithProgress.habit.reminderType.reminderTime
            else -> null
        },
        isDone = habitWithProgress.progress.status != Status.NotStarted,
        clickable = true,
        onClick = onClick
    ){
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    //start button
                    Box(
                       modifier = Modifier.size(80.dp,20.dp)
                           .clip(RoundedCornerShape(10.dp))
                           .border(
                               1.dp,
                               if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.scrim,
                               RoundedCornerShape(10.dp))
                           .clickable{
                               if(habitWithProgress.progress.date <= LocalDate.now()){
                                   onStart(habitWithProgress)
                               }else{
                                   onDeny()
                               }
                           },
                        contentAlignment = Alignment.Center
                    ){
                        Text(
                            text = if(habitWithProgress.progress.status == Status.NotStarted) "Start"
                            else if (habitWithProgress.progress.status == Status.Ongoing) "Ongoing"
                            else "Finished",
                            style = TextStyle(
                                color = when(habitWithProgress.progress.status){
                                    Status.Cancelled -> MaterialTheme.colorScheme.scrim
                                    Status.Done -> MaterialTheme.colorScheme.scrim
                                    Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                                    Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                                },
                                fontFamily = instrumentSerif,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp
                            )
                        )
                    }

                    VerticalDivider(
                        thickness = 1.dp,
                        color = when(habitWithProgress.progress.status){
                            Status.Cancelled -> MaterialTheme.colorScheme.scrim
                            Status.Done -> MaterialTheme.colorScheme.scrim
                            Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                            Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.height(18.dp)
                    )

                    //time
                    habitWithProgress.habit.duration?.let {
                        Text(
                            text = habitWithProgress.habit.duration.toWord(),
                            style = TextStyle(
                                color = when(habitWithProgress.progress.status){
                                    Status.Cancelled -> MaterialTheme.colorScheme.scrim
                                    Status.Done -> MaterialTheme.colorScheme.scrim
                                    Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                                    Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                                },
                                fontFamily = regular,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.clock),
                    contentDescription = "Log for another day",
                    tint = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary.copy(0.6f)
                            else MaterialTheme.colorScheme.scrim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onLogForAnotherDay() }
                )
            }
            Spacer(Modifier.height(16.dp))
            //title
            Text(
                text = habitWithProgress.habit.title,
                style = TextStyle(
                    color = when(habitWithProgress.progress.status){
                        Status.Cancelled -> MaterialTheme.colorScheme.scrim
                        Status.Done -> MaterialTheme.colorScheme.scrim
                        Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                        Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                    },
                    fontFamily = regular,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Composable
fun SessionHabit(
    modifier: Modifier = Modifier,
    habitWithProgress: HabitWithProgress,
    onStartSession: (HabitWithProgress) -> Unit,
    onAddSubTask : () -> Unit = {},
    onToggle : (SubTask)->Unit = {},
    onDeny: () -> Unit,
    onClick: () -> Unit,
    onLogForAnotherDay : () -> Unit = {}
) {
    HabitElement(
        color = getThemedColorFromKey(habitWithProgress.habit.colorKey),
        reminder = when(habitWithProgress.habit.reminderType){
            is ReminderType.Once -> habitWithProgress.habit.reminderType.reminderTime
            else -> null
        },
        isDone = habitWithProgress.progress.status != Status.NotStarted,
        clickable = true,
        onClick = onClick
    ){
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    //start button
                    Box(
                        modifier = Modifier.size(80.dp,20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                when(habitWithProgress.progress.status){
                                    Status.Cancelled -> MaterialTheme.colorScheme.scrim
                                    Status.Done -> MaterialTheme.colorScheme.scrim
                                    Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                                    Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .clickable{
                                if(habitWithProgress.progress.date <= LocalDate.now()){
                                    onStartSession(habitWithProgress)
                                }else{
                                    onDeny()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ){
                        Text(
                            text = if(habitWithProgress.progress.status == Status.NotStarted) "Start"
                            else if (habitWithProgress.progress.status == Status.Ongoing) "Ongoing"
                            else "Finished",
                            style = TextStyle(
                                color = when(habitWithProgress.progress.status){
                                    Status.Cancelled -> MaterialTheme.colorScheme.scrim
                                    Status.Done -> MaterialTheme.colorScheme.scrim
                                    Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                                    Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                                },
                                fontFamily = instrumentSerif,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp
                            )
                        )
                    }

                    VerticalDivider(
                        thickness = 1.dp,
                        color = when(habitWithProgress.progress.status){
                            Status.Cancelled -> MaterialTheme.colorScheme.scrim
                            Status.Done -> MaterialTheme.colorScheme.scrim
                            Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                            Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.height(18.dp)
                    )

                    //target
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(
                            text = "${habitWithProgress.progress.currentCount}/"+
                                    "${habitWithProgress.progress.targetCount}",
                            style = TextStyle(
                                color = when(habitWithProgress.progress.status){
                                    Status.Cancelled -> MaterialTheme.colorScheme.scrim
                                    Status.Done -> MaterialTheme.colorScheme.scrim
                                    Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                                    Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                                },
                                fontFamily = regular,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        //param ex:- session
                        Text(
                            text = habitWithProgress.progress.countParam.displayName,
                            style = TextStyle(
                                color = when(habitWithProgress.progress.status){
                                    Status.Cancelled -> MaterialTheme.colorScheme.scrim
                                    Status.Done -> MaterialTheme.colorScheme.scrim
                                    Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                                    Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                                },
                                fontFamily = regular,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.clock),
                    contentDescription = "Log for another day",
                    tint = if(habitWithProgress.progress.status == Status.NotStarted) MaterialTheme.colorScheme.onPrimary.copy(0.6f)
                            else MaterialTheme.colorScheme.scrim,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onLogForAnotherDay() }
                )
            }
            Spacer(Modifier.height(16.dp))
            //title
            Text(
                text = habitWithProgress.habit.title,
                style = TextStyle(
                    color = when(habitWithProgress.progress.status){
                        Status.Cancelled -> MaterialTheme.colorScheme.scrim
                        Status.Done -> MaterialTheme.colorScheme.scrim
                        Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                        Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                    },
                    fontFamily = regular,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            //subTask
            habitWithProgress.subtasks.forEach {subtask->
                SubTaskEditor(
                    subTask = subtask,
                    textSize = 12,
                    textColor = when(habitWithProgress.progress.status){
                        Status.Cancelled -> MaterialTheme.colorScheme.scrim
                        Status.Done -> MaterialTheme.colorScheme.scrim
                        Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                        Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                    },
                    enabled = false,
                    onToggleSubtask = {onToggle(subtask)},
                    modifier = modifier
                )
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "+ Add Task",
                style = TextStyle(
                    color = when(habitWithProgress.progress.status){
                        Status.Cancelled -> MaterialTheme.colorScheme.scrim
                        Status.Done -> MaterialTheme.colorScheme.scrim
                        Status.NotStarted -> MaterialTheme.colorScheme.onPrimary
                        Status.Ongoing -> MaterialTheme.colorScheme.onPrimary
                    },
                    fontFamily = regular,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                ),
                modifier = Modifier.clickable{
                    onAddSubTask()
                }
            )

        }
    }
}

