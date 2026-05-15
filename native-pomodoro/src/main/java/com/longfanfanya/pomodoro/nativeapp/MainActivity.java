package com.longfanfanya.pomodoro.nativeapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int WORK_SECONDS = 25 * 60;
    private static final int SHORT_BREAK_SECONDS = 5 * 60;
    private static final int LONG_BREAK_SECONDS = 15 * 60;
    private static final int LONG_BREAK_EVERY = 4;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }

            if (remainingSeconds > 0) {
                remainingSeconds--;
                render();
                handler.postDelayed(this, 1000);
            } else {
                finishCurrentSession();
            }
        }
    };

    private TextView modeText;
    private TextView timerText;
    private TextView roundText;
    private ProgressBar progressBar;
    private Button startButton;
    private Button workButton;
    private Button shortBreakButton;
    private Button longBreakButton;

    private Mode mode = Mode.WORK;
    private int remainingSeconds = WORK_SECONDS;
    private int completedWorkSessions = 0;
    private boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildLayout());
        render();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(255, 248, 240));

        TextView title = new TextView(this);
        title.setText("番茄时钟");
        title.setTextColor(Color.rgb(35, 35, 35));
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        modeText = new TextView(this);
        modeText.setTextSize(18);
        modeText.setTextColor(Color.rgb(99, 68, 58));
        modeText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(-1, -2);
        modeParams.setMargins(0, dp(14), 0, 0);
        root.addView(modeText, modeParams);

        timerText = new TextView(this);
        timerText.setTextSize(68);
        timerText.setTextColor(Color.rgb(242, 78, 61));
        timerText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        timerText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(-1, -2);
        timerParams.setMargins(0, dp(34), 0, dp(8));
        root.addView(timerText, timerParams);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(WORK_SECONDS);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(10));
        progressParams.setMargins(0, 0, 0, dp(24));
        root.addView(progressBar, progressParams);

        startButton = makePrimaryButton("开始");
        startButton.setOnClickListener(v -> toggleRunning());
        root.addView(startButton, new LinearLayout.LayoutParams(-1, dp(54)));

        Button resetButton = makeSecondaryButton("重置");
        resetButton.setOnClickListener(v -> resetCurrentMode());
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-1, dp(48));
        resetParams.setMargins(0, dp(12), 0, dp(22));
        root.addView(resetButton, resetParams);

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER);
        workButton = makeModeButton("工作");
        shortBreakButton = makeModeButton("短休");
        longBreakButton = makeModeButton("长休");
        workButton.setOnClickListener(v -> switchMode(Mode.WORK));
        shortBreakButton.setOnClickListener(v -> switchMode(Mode.SHORT_BREAK));
        longBreakButton.setOnClickListener(v -> switchMode(Mode.LONG_BREAK));
        modeRow.addView(workButton, weightedButtonParams(0));
        modeRow.addView(shortBreakButton, weightedButtonParams(dp(8)));
        modeRow.addView(longBreakButton, weightedButtonParams(dp(8)));
        root.addView(modeRow, new LinearLayout.LayoutParams(-1, dp(46)));

        roundText = new TextView(this);
        roundText.setTextSize(15);
        roundText.setTextColor(Color.rgb(99, 68, 58));
        roundText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams roundParams = new LinearLayout.LayoutParams(-1, -2);
        roundParams.setMargins(0, dp(18), 0, 0);
        root.addView(roundText, roundParams);

        return root;
    }

    private void toggleRunning() {
        running = !running;
        if (running) {
            handler.post(ticker);
        } else {
            handler.removeCallbacks(ticker);
        }
        render();
    }

    private void resetCurrentMode() {
        running = false;
        handler.removeCallbacks(ticker);
        remainingSeconds = mode.durationSeconds;
        render();
    }

    private void switchMode(Mode newMode) {
        mode = newMode;
        resetCurrentMode();
    }

    private void finishCurrentSession() {
        running = false;
        handler.removeCallbacks(ticker);
        notifyDone();

        if (mode == Mode.WORK) {
            completedWorkSessions++;
            mode = completedWorkSessions % LONG_BREAK_EVERY == 0 ? Mode.LONG_BREAK : Mode.SHORT_BREAK;
        } else {
            mode = Mode.WORK;
        }
        remainingSeconds = mode.durationSeconds;
        render();
    }

    private void notifyDone() {
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 450);
        } catch (RuntimeException ignored) {
            // Some devices disallow tone generation in restricted audio states.
        }

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(250);
        }
    }

    private void render() {
        modeText.setText(mode.label);
        timerText.setText(formatTime(remainingSeconds));
        progressBar.setMax(mode.durationSeconds);
        progressBar.setProgress(mode.durationSeconds - remainingSeconds);
        startButton.setText(running ? "暂停" : "开始");
        roundText.setText(String.format(Locale.CHINA, "已完成 %d 个专注番茄，长休每 %d 个触发", completedWorkSessions, LONG_BREAK_EVERY));
        updateModeButton(workButton, mode == Mode.WORK);
        updateModeButton(shortBreakButton, mode == Mode.SHORT_BREAK);
        updateModeButton(longBreakButton, mode == Mode.LONG_BREAK);
    }

    private String formatTime(int seconds) {
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private Button makePrimaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackgroundColor(Color.rgb(242, 78, 61));
        return button;
    }

    private Button makeSecondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(Color.rgb(242, 78, 61));
        button.setBackgroundColor(Color.rgb(255, 232, 224));
        return button;
    }

    private Button makeModeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(leftMargin, 0, 0, 0);
        return params;
    }

    private void updateModeButton(Button button, boolean selected) {
        button.setTextColor(selected ? Color.WHITE : Color.rgb(88, 66, 58));
        button.setBackgroundColor(selected ? Color.rgb(47, 158, 68) : Color.rgb(255, 239, 229));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private enum Mode {
        WORK("专注 25 分钟", WORK_SECONDS),
        SHORT_BREAK("短休 5 分钟", SHORT_BREAK_SECONDS),
        LONG_BREAK("长休 15 分钟", LONG_BREAK_SECONDS);

        final String label;
        final int durationSeconds;

        Mode(String label, int durationSeconds) {
            this.label = label;
            this.durationSeconds = durationSeconds;
        }
    }
}
