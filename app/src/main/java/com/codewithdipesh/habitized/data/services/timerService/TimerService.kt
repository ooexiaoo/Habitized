package com.codewithdipesh.habitized.data.services.timerService

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.codewithdipesh.habitized.R
import com.codewithdipesh.habitized.TIMER_NOTIFICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerService : Service() {

    private var timerJob : Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val binder = TimerBind()
    private var timerCallback : TimerCallback? =  null

    // Store the actual start time instead of relying on delays
    private var startTime: Long = 0
    private var totalDurationMs: Long = 0
    private var pausedTimeMs: Long = 0
    private var totalPausedDurationMs: Long = 0

    //store the title , id , color of the habit
    private var title : String = ""
    private var id : String = ""
    private var color : String = ""
    private var screen : String = ""

    //timerState
    private val _timerState = MutableStateFlow(TimerClassState(0, 0, 0, false,false))
    val timerState: StateFlow<TimerClassState> = _timerState.asStateFlow()

    //pendingIntent
    private lateinit var pendingIntent : PendingIntent

    interface TimerCallback {
        fun onTimerUpdate(h :Int,m:Int,s:Int)
        fun onTimerFinished()
        fun onTimerPaused()
        fun onTimerStarted()
    }
    inner class TimerBind : Binder(){
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val totalSeconds = intent?.getIntExtra("duration_seconds", 0) ?: 0
        id = intent?.getStringExtra("id") ?: ""
        title = intent?.getStringExtra("habit") ?: "Habit Timer"
        color = intent?.getStringExtra("color") ?: "yellow"
        screen = intent?.getStringExtra("screen") ?: "duration"
        startForegroundService(durationSeconds = totalSeconds,title = title,id = id, color = color, screen = screen)
        return START_STICKY
    }

    fun pauseTimer() {
        if (!_timerState.value.isPaused) {
            pausedTimeMs = System.currentTimeMillis()
            _timerState.value = _timerState.value.copy(
                isPaused = true
            )
            timerJob?.cancel()
            timerCallback?.onTimerPaused()
        }
    }
    fun finishTimer(){
        _timerState.value = _timerState.value.copy(
            isPaused = false,
            isFinished = true
        )
        stopSelf()
    }

    fun resumeTimer() {
        if (_timerState.value.isPaused) {
            val pauseDuration = System.currentTimeMillis() - pausedTimeMs
            totalDurationMs += pauseDuration
            _timerState.value = _timerState.value.copy(
                isPaused = false
            )
            resumeTimerJob()
            timerCallback?.onTimerStarted()
        } 
    }


    private fun startForegroundService(id:String,title:String,durationSeconds:Int,color:String,screen:String){
        //reinitializing when we retry
        val initialHour = durationSeconds / 3600
        val initialMinute = (durationSeconds % 3600) / 60
        val initialSecond = durationSeconds % 60
        _timerState.value = TimerClassState(initialHour, initialMinute, initialSecond, false, false)

        startTime = System.currentTimeMillis()
        totalDurationMs = durationSeconds * 1000L

        //intent for deeplinking
        val intent = Intent(
            Intent.ACTION_VIEW,
            "com.codewithdipesh.habitized://$screen/$id/$title/$durationSeconds/$color".toUri()
        )
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        startForeground(1,createNotification("Starting...",title,durationSeconds,0,pendingIntent))

        resumeTimerJob()
    }

    private fun resumeTimerJob() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val elapsedMs = currentTime - startTime - totalPausedDurationMs
                val remainingMs = totalDurationMs - elapsedMs

                if (remainingMs <= 0) {
                    notificationManager.notify(TIMER_NOTIFICATION_ID, showAlarmNotification())
                    _timerState.value = _timerState.value.copy(
                        isPaused = false,
                        isFinished = true
                    )
                    timerCallback?.onTimerFinished()
                    stopSelf()
                    break
                }

                val remainingSeconds = ((remainingMs + 999) / 1000).toInt()
                val hour = remainingSeconds / 3600
                val minute = (remainingSeconds % 3600) / 60
                val second = remainingSeconds % 60

                val timerString = String.format("%02d:%02d:%02d", hour, minute, second)
                val notification = createNotification(
                    "Time Left : $timerString",
                    title,
                    (totalDurationMs / 1000).toInt(),
                    (elapsedMs / 1000).toInt(),
                    pendingIntent
                )

                //updating in service state
                _timerState.value = _timerState.value.copy(
                    hour=hour,
                    minute = minute,
                    second=second
                )
                timerCallback?.onTimerUpdate(hour, minute, second)
                notificationManager.notify(TIMER_NOTIFICATION_ID, notification)

                val nextUpdateTime = startTime + ((elapsedMs / 1000 + 1) * 1000)
                val delayMs = nextUpdateTime - System.currentTimeMillis()
                if (delayMs > 0) {
                    delay(delayMs)
                } else {
                    delay(100)
                }
            }
        }
    }


    private fun createNotification(content : String,title : String,maxProgress:Int,currentProgress:Int?,pendingIntent : PendingIntent) : Notification {
        val notification =  NotificationCompat.Builder(this,"TIMER_CHANNEL")
            .setSmallIcon(R.drawable.ic_action_name)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_action_name))
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if(currentProgress != null){
            notification
                .setProgress(maxProgress,currentProgress,false)
        }

        return notification.build()
    }
    override fun onDestroy() {
        timerJob?.cancel()
        timerCallback = null
        super.onDestroy()
    }
    fun cancel(){
        stopSelf()
        timerJob?.cancel()
    }

    fun setTimerCallback(callback: TimerCallback?) {
        this.timerCallback = callback
    }

    private fun showAlarmNotification() : Notification {

        return NotificationCompat.Builder(this, "TIMER_CHANNEL")
            .setSmallIcon(R.drawable.ic_action_name)
            .setContentTitle("⏰ Time's Up!")
            .setContentText("Your timer finished.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)
            .build()

    }


}