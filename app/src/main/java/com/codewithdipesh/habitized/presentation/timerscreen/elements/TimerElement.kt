package com.codewithdipesh.habitized.presentation.timerscreen.elements

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithdipesh.habitized.R
import com.codewithdipesh.habitized.data.services.timerService.TimerServiceManager
import com.codewithdipesh.habitized.ui.theme.ndot
import com.codewithdipesh.habitized.ui.theme.regular
import java.time.LocalTime

@Composable
fun TimerElement(
    duration : LocalTime,
    manager : TimerServiceManager,
    start : Boolean = false,
    finished : Boolean = false,
    isStopwatch : Boolean = false,
    onPrimary : Color,
    onStart : (Int)->Unit = {},
    onPause : () ->Unit= {},
    onResume : () -> Unit = {},
    onCancel : () ->Unit = {},
    onFinish : () ->Unit = {},
    onTimerFinished : () -> Unit = {},
    modifier: Modifier = Modifier
){
    val timerState by manager.timerState.collectAsState()

    var count by remember { mutableStateOf(0) }

    val secondTimes = (duration.hour * 3600) + (duration.minute *60) + duration.second
    var resumed by remember { mutableStateOf(false) }

    var second by remember{ mutableStateOf(duration.second) }
    var minute by remember{ mutableStateOf(duration.minute) }
    var hour by remember{ mutableStateOf(duration.hour) }

    DisposableEffect(Unit) {
        manager.bindService()

        onDispose {
            manager.unbindService()
        }
    }
    LaunchedEffect(start) {
        if (start && !finished) {
            second = duration.second
            minute = duration.minute
            hour = duration.hour
            count = 0
        }
    }
    LaunchedEffect(timerState.minute,timerState.hour,timerState.second){
        if (start && !finished) {
            second = timerState.second
            minute = timerState.minute
            hour = timerState.hour
            count = secondTimes - (timerState.hour * 3600) - (timerState.minute * 60) - timerState.second
        }
        else if (finished && timerState.hour == 0 && timerState.minute == 0 && timerState.second == 0){
            onTimerFinished()
        }
    }
    LaunchedEffect(start && timerState.isPaused) {
        if(timerState.isPaused){
            resumed = false
        }else{
            resumed = true
        }
    }
    LaunchedEffect(start && timerState.isFinished) {
        if(timerState.isFinished){
            onTimerFinished()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        //timer with label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ){
            if(duration.hour > 0){
                Column(verticalArrangement = Arrangement.Center){
                    Text(
                        text = hour.toString().padStart(2, '0') + " : ",
                        style = TextStyle(
                            color = onPrimary,
                            fontFamily = ndot,
                            fontSize = 64.sp
                        )
                    )
                    Text(
                        text = "hours",
                        style = TextStyle(
                            color = onPrimary,
                            fontFamily = regular,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    )
                }
            }
            if(duration.hour >0 || duration.minute > 0){
                Column(verticalArrangement = Arrangement.Center){
                    Text(
                        text = minute.toString().padStart(2, '0') + " : ",
                        style = TextStyle(
                            color = onPrimary,
                            fontFamily = ndot,
                            fontSize = 64.sp
                        )
                    )
                    Text(
                        text = "minutes",
                        style = TextStyle(
                            color = onPrimary,
                            fontFamily = regular,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    )
                }
            }
            Column(verticalArrangement = Arrangement.Center){
                Text(
                    text = second.toString().padStart(2, '0'),
                    style = TextStyle(
                        color = onPrimary,
                        fontFamily = ndot,
                        fontSize = 64.sp
                    )
                )
                Text(
                    text = "seconds",
                    style = TextStyle(
                        color = onPrimary,
                        fontFamily = regular,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    )
                )
            }

        }
        //progress
        if (!isStopwatch) {
            Spacer(Modifier.height(24.dp))
            TimerProgressBar(
                progress = count,
                total = secondTimes,
                progressColor = onPrimary,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
        //pause retry
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ){
            //retry
            AnimatedVisibility(
                visible = start && !finished,
                enter = slideInHorizontally(
                    initialOffsetX = {it -> it}
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = {it->it}
                ) + fadeOut()
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable{
                            onStart(secondTimes)
                        },
                    contentAlignment = Alignment.Center

                ){
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "retry",
                        tint = onPrimary
                    )
                }
            }
            //pause/resume/start/finish
            Box(
                modifier = Modifier
                    .size(126.dp,40.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable{
                        if(!start && !finished){
                            onStart(secondTimes)
                        }else{
                            if(start && !finished){
                                if(resumed){
                                    manager.pause()
                                    onPause()
                                }else{
                                    manager.resume()
                                    onResume()
                                }
                            }
                            if(finished){
                                onFinish()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ){
                Row(verticalAlignment = Alignment.CenterVertically){
                    //icon
                    if(start && !finished){
                        if(resumed){
                            Icon(
                                painter = painterResource(R.drawable.resumed_icon),
                                contentDescription = "pause",
                                tint = onPrimary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }else{
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "resume",
                                tint = onPrimary
                            )
                        }
                    }
                    //text
                    Text(
                        text = if(!start && !finished) "start"
                        else {
                            if(finished) "Finish"
                            else if(resumed) "pause"
                            else "resume"
                        }
                        ,
                        style = TextStyle(
                            color = onPrimary,
                            fontFamily = regular,
                            fontSize = 16.sp
                        )
                    )
                }
            }
            //cancel
            AnimatedVisibility(
                visible = start && !finished,
                enter = slideInHorizontally(
                    initialOffsetX = {it -> -it}
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { it -> -it}
                ) + fadeOut()
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable{
                            manager.cancel()
                            onCancel()

                            hour = duration.hour
                            minute = duration.minute
                            second = duration.second

                            count = 0
                        },
                    contentAlignment = Alignment.Center
                ){
                    Icon(
                        painter = painterResource(R.drawable.cancel_timer),
                        contentDescription = "cancel",
                        tint = onPrimary
                    )
                }
            }

        }

    }
}