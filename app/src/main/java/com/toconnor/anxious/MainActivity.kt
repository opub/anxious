package com.toconnor.anxious

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import java.math.RoundingMode
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private val tag = MainActivity::class.java.simpleName

    //controls
    private lateinit var mainButton: Button
    private lateinit var clockTextView: TextView
    private lateinit var runCountTextView: TextView
    private lateinit var bestScoreTextView: TextView
    private lateinit var avgScoreTextView: TextView
    private lateinit var timer: CountDownTimer
    private lateinit var timerFormat: DecimalFormat
    private val startTime: Long = 5000
    private val timeInterval: Long = 10
    private val pauseTime: Long = 2000

    //state
    private var running = false
    private var timeLeft = 0L
    private var bestScore = 0L
    private var totalScore = 0L
    private var runCount = 0
    private var goodCount = 0
    private var streak = 0

    //saved state keys
    companion object {
        private const val RUNNING_KEY = "RUNNING_KEY"
        private const val TIME_LEFT_KEY = "TIME_LEFT_KEY"
        private const val BEST_SCORE_KEY = "BEST_SCORE_KEY"
        private const val TOTAL_SCORE_KEY = "TOTAL_SCORE_KEY"
        private const val RUN_COUNT_KEY = "RUN_COUNT_KEY"
        private const val GOOD_COUNT_KEY = "GOOD_COUNT_KEY"
        private const val STREAK_KEY = "STREAK_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //get references to all of our controls
        mainButton = findViewById(R.id.main_button)
        clockTextView = findViewById(R.id.clock_text_view)
        runCountTextView = findViewById(R.id.run_count_text_view)
        bestScoreTextView = findViewById(R.id.best_score_text_view)
        avgScoreTextView = findViewById(R.id.avg_score_text_view)

        //format of timer
        timerFormat = DecimalFormat("0.000")
        timerFormat.roundingMode = RoundingMode.CEILING

        mainButton.setOnClickListener {view ->
            val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce_small)
            view.startAnimation(bounce)
            if (running) stopRun() else startRun()
        }

        //restore saved state
        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean(RUNNING_KEY)
            timeLeft = savedInstanceState.getLong(TIME_LEFT_KEY)
            bestScore = savedInstanceState.getLong(BEST_SCORE_KEY)
            totalScore = savedInstanceState.getLong(TOTAL_SCORE_KEY)
            runCount = savedInstanceState.getInt(RUN_COUNT_KEY)
            goodCount = savedInstanceState.getInt(GOOD_COUNT_KEY)
            streak = savedInstanceState.getInt(STREAK_KEY)
            if (running) startRun()
        }

        Log.d(tag, "onCreate - running: $running, totalScore: $totalScore")

        updateSummary()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(RUNNING_KEY, running)
        outState.putLong(TIME_LEFT_KEY, timeLeft)
        outState.putLong(BEST_SCORE_KEY, bestScore)
        outState.putLong(TOTAL_SCORE_KEY, totalScore)
        outState.putInt(RUN_COUNT_KEY, runCount)
        outState.putInt(GOOD_COUNT_KEY, goodCount)
        Log.d(tag, "onSaveInstanceState - running: $running, totalScore: $totalScore")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(tag, "onDestroy")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            showInfo()
        }
        return true
    }

    private fun showInfo() {
        val title = getString(R.string.about_title, BuildConfig.VERSION_NAME)
        val message = getString(R.string.about_message, runCount, goodCount, streak)
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.create().show()
    }

    private fun startRun() {
        val initialTime = if (running) {
            //this will happen when restoring from a saved state
            timeLeft
        } else {
            running = true
            startTime
        }
        mainButton.text = getString(R.string.stop)

        //set clock initial state then update with our CountDownTimer
        clockTextView.text = format(initialTime)
        timer = object: CountDownTimer(initialTime, timeInterval) {
            override fun onTick(left: Long) {
                timeLeft = left
                clockTextView.text = format(left)
            }

            override fun onFinish() {
                timeLeft = 0
                clockTextView.text = format(0)
                stopRun()
            }
        }
        timer.start()
    }

    private fun stopRun() {
        running = false
        runCount += 1

        val score = timeLeft
        totalScore += score

        if (score > 0) {
            //stop was tapped before the timer expired
            timer.cancel()
            goodCount += 1
            streak += 1

            if (score < bestScore || bestScore == 0L) {
                bestScore = score
                bestState()
            } else {
                resetState()
            }
        } else {
            //timer had already run out
            streak = 0
            failState()
        }

        updateSummary()
    }

    private fun updateSummary() {
        runCountTextView.text = getString(R.string.run_count, runCount)
        bestScoreTextView.text = getString(R.string.best_score, format(bestScore))
        val avgScore = if (goodCount == 0) 0 else (totalScore / goodCount)
        avgScoreTextView.text = getString(R.string.avg_score, format(avgScore))
    }

    private fun resetState() {
        mainButton.text = getString(R.string.start)
        mainButton.isEnabled = true
        clockTextView.setTextColor(ResourcesCompat.getColor(resources, R.color.colorPrimaryDark, null))
    }

    private fun bestState() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.bounce)
        bestScoreTextView.startAnimation(anim)
        mainButton.text = getString(R.string.new_best)
        mainButton.isEnabled = false
        clockTextView.setTextColor(ResourcesCompat.getColor(resources, R.color.colorAccent, null))
        Handler().postDelayed({resetState()}, pauseTime)
    }

    private fun failState() {
        mainButton.text = getString(R.string.failed)
        mainButton.isEnabled = false
        clockTextView.setTextColor(ResourcesCompat.getColor(resources, R.color.colorAlert, null))
        Handler().postDelayed({resetState()}, pauseTime)
    }

    private fun format(millis: Long): String {
        return timerFormat.format(millis / 1000.0f)
    }
}
