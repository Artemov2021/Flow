package com.wpmapp.flow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.view.MotionEvent;

import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wpmapp.flow.model.TypingResult;
import com.wpmapp.flow.views.CustomEditText;

import java.util.Locale;


public class TypingActivity extends AppCompatActivity implements CustomEditText.TypingListener  {

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long timeLeftInMillis = 60_000; // 1 minute default
    private TextView timerText;
    private CustomEditText editText;

    private int typedWords;
    private int correctWords;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typing);

        editText = findViewById(R.id.editText);
        editText.setTypingListener(this);
        timerText = findViewById(R.id.timerText);

        setSwipeToShowBars();
        setBackButtonListener();
        setTextFieldFocused();
        setTextFieldMoveListener();
        setKeyboardHideListener();
        setTimerListener();
    }

    @Override
    public void onWordTyped() {
        typedWords++;  // Your logic here
    }
    @Override
    public void onCorrectWordCountUpdated(int correctWordCount) {
        this.correctWords = correctWordCount;
    }
    @Override
    public void onWrongCharTyped() {
        vibrate();
    }

    private void setSwipeToShowBars() {
        Window window = getWindow();

        // Transparent system bars for API 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        // Draw into notch (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }

        // For API 30+ (Android 11+): Use insets controller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            final WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        }
    }
    private void setBackButtonListener() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cancel timer here if running
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    isTimerRunning = false;
                }
                Intent intent = new Intent(TypingActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // finish this activity if you want to remove it from back stack
            }
        });
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setTextFieldFocused() {
        // Step 1: Request focus so the cursor is visible
        editText.requestFocus();

        // Set cursor at the beginning of the text
        editText.setSelection(0);

        // Step 2: Disable keyboard on initial focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editText.setShowSoftInputOnFocus(false);
        } else {
            try {
                java.lang.reflect.Method method = EditText.class.getMethod(
                        "setShowSoftInputOnFocus", boolean.class
                );
                method.setAccessible(true);
                method.invoke(editText, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Step 3: Re-enable keyboard on user tap
        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {

                    // Re-enable keyboard on future focuses
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        editText.setShowSoftInputOnFocus(true);
                    } else {
                        try {
                            java.lang.reflect.Method method = EditText.class.getMethod(
                                    "setShowSoftInputOnFocus", boolean.class
                            );
                            method.setAccessible(true);
                            method.invoke(editText, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Show keyboard manually
                    editText.post(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                    });

                    v.performClick();  // <-- This line added for accessibility
                }
                return false; // Let EditText handle the touch
            }
        });
    }
    private void setTextFieldMoveListener() {
        View rootView = getWindow().getDecorView().getRootView();
        int moveDistanceDp = 115;
        int moveDistancePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                moveDistanceDp,
                getResources().getDisplayMetrics()
        );

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousKeyboardHeight = 0;

            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                boolean isKeyboardNowVisible = keypadHeight > screenHeight * 0.15;

                if (isKeyboardNowVisible && previousKeyboardHeight == 0) {
                    // Keyboard just became visible - move EditText up smoothly in 100ms
                    editText.animate().translationY(-moveDistancePx).setDuration(300).start();
                    previousKeyboardHeight = keypadHeight;
                } else if (!isKeyboardNowVisible && previousKeyboardHeight != 0) {
                    // Keyboard just became hidden - move EditText down smoothly in 100ms
                    editText.animate().translationY(0).setDuration(300).start();
                    previousKeyboardHeight = 0;
                }
            }
        });
    }
    private void setKeyboardHideListener() {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // Hide keyboard when EditText loses focus
                hideKeyboard(v);
            }
        });
    }
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void setTimerListener() {
        editText.setTimerStartListener(new CustomEditText.TimerStartListener() {
            @Override
            public void onStartTimer(long durationMillis) {
                startTimer(durationMillis);
            }
        });
    }
    public void startTimer(long millis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millis, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                int secondsLeft = (int) (millisUntilFinished / 1000);
                int minutes = secondsLeft / 60;
                int seconds = secondsLeft % 60;
                timerText.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timerText.setText("0:00");
                closeKeyboard();
                openResultActivity();
            }
        }.start();

        isTimerRunning = true;
    }
    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }
    private void openResultActivity() {
        TypingResult result = new TypingResult(typedWords, correctWords);
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("typing_result", result);  // pass TypingResult in Intent
        startActivity(intent);
        finish();
    }
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(55);
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            // Do NOT set isTimerRunning = false here
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (isTimerRunning && timeLeftInMillis > 0) {
            startTimer(timeLeftInMillis);
        }
    }









}
