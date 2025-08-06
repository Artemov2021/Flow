package com.wpmapp.flow;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.wpmapp.flow.model.TypingResult;
import com.wpmapp.flow.views.AccuracyLineView;

public class ResultActivity extends AppCompatActivity {

    private TextView wpmValue;
    private TextView record;
    private FrameLayout resultDashboard;
    private TextView totalTyped;
    private TextView correctPercentage;
    private LinearLayout tryAgainButton;
    private FrameLayout tryAgainButtonContainer;
    private ImageView tryAgainIcon;
    private TextView tryAgainText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        wpmValue = findViewById(R.id.wpmValue);
        resultDashboard = findViewById(R.id.resultDashboard);
        totalTyped = findViewById(R.id.totalTyped);
        correctPercentage = findViewById(R.id.correctPercentage);
        record = findViewById(R.id.record);

        // Retrieve TypingResult from Intent
        @SuppressLint("UnsafeIntentLaunch")
        TypingResult result = (TypingResult) getIntent().getSerializableExtra("typing_result");
        if (result == null) {
            // fallback or error handling
            result = new TypingResult(0, 0);
        }

        setResult(result);  // Now call your own method to update UI
        setAndShowRecord(result.correctWords);

        setSwipeToShowBars();
        setBackButtonListener();

    }
    private void setAndShowRecord(int correctWords) {
        int previousRecord = getRecord();
        if (correctWords > previousRecord) {
            SharedPreferences prefs = getSharedPreferences("TypingAppPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("wpmRecord", correctWords);  // Save your record
            editor.apply(); // Apply changes
            record.setText(String.format("New record: %s",correctWords));
        } else {
            record.setText(String.format("Record: %s",previousRecord));
        }
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
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
    }


    public void setResult(TypingResult result) {
        setWPMAnimation(result.correctWords);
        setResultDashboardValues(result);
        setResultDashboardAnimation();
        setTryAgainButton();
        setTryAgainButtonAnimation();
    }
    private void setWPMAnimation(int wpm) {
        ValueAnimator animator = ValueAnimator.ofInt(0, wpm);
        animator.setDuration(1000); // 1 second (1000 ms)

        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            wpmValue.setText(String.valueOf(animatedValue));
        });

        animator.start();
    }
    private void setResultDashboardValues(TypingResult result) {
        totalTyped.setText(String.format("Total typed words: %s",result.typedWords));
        int percentage = (int) ((result.correctWords / (double) result.typedWords) * 100);
        setAccuracyLine(percentage);
        correctPercentage.setText(percentage + "%");
    }
    private void setAccuracyLine(int percentage) {
        // 1. Reference the FrameLayout (must exist in your XML layout)
        FrameLayout resultDashboard = findViewById(R.id.resultDashboard);

        // 2. Create the AccuracyLineView dynamically
        AccuracyLineView accuracyLine = new AccuracyLineView(this);

        // 3. Define layout parameters with margins
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(10)
        );

        // 4. Set margins: start=30dp, top=70dp
        params.setMargins(dpToPx(30), dpToPx(66), dpToPx(30), 0);

        // 5. Add the custom view to the FrameLayout
        resultDashboard.addView(accuracyLine, params);

        // 6. Set the percentage value
        accuracyLine.setPercentage(percentage); // Example
    }
    private void setResultDashboardAnimation() {
        // Start fully transparent and shifted 100px to the right
        resultDashboard.setAlpha(0f);
        resultDashboard.setTranslationX(100f);
        resultDashboard.setVisibility(View.VISIBLE); // Ensure it's in the layout

        // Animate both alpha and translationX together after 1 second
        resultDashboard.animate()
                .alpha(1f)                // Fade in
                .translationX(0f)         // Slide to original position
                .setDuration(700)         // Animation duration (smooth fade+slide)
                .setStartDelay(1000)      // Delay before starting (1 second)
                .start();
    }
    private void setTryAgainButton() {
        // === Outer container that will be animated ===
        tryAgainButtonContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(62)
        );
        containerParams.gravity = Gravity.BOTTOM;
        containerParams.bottomMargin = dpToPx(55);
        containerParams.leftMargin = dpToPx(23);
        containerParams.rightMargin = dpToPx(23);
        tryAgainButtonContainer.setLayoutParams(containerParams);

        // === Actual button inside container ===
        tryAgainButton = new LinearLayout(this);
        tryAgainButton.setOrientation(LinearLayout.HORIZONTAL);
        tryAgainButton.setGravity(Gravity.CENTER);
        tryAgainButton.setPadding(dpToPx(16), 0, dpToPx(16), 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#171717"));
        background.setCornerRadius(dpToPx(24));
        tryAgainButton.setBackground(background);

        // Match container size
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        tryAgainButton.setLayoutParams(buttonParams);

        // === Icon ===
        tryAgainIcon = new ImageView(this);
        tryAgainIcon.setImageResource(R.drawable.try_again_symbol);
        tryAgainIcon.setColorFilter(Color.parseColor("#CECECE"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(19), dpToPx(19));
        iconParams.setMarginEnd(dpToPx(5));
        tryAgainIcon.setLayoutParams(iconParams);

        // === Text ===
        tryAgainText = new TextView(this);
        tryAgainText.setText("Try again");
        tryAgainText.setTextSize(21);
        tryAgainText.setTextColor(Color.parseColor("#CECECE"));
        tryAgainText.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_semi_bold));

        // Add icon + text to button
        tryAgainButton.addView(tryAgainIcon);
        tryAgainButton.addView(tryAgainText);

        // Add button to container
        tryAgainButtonContainer.addView(tryAgainButton);

        // Add container to root
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView().getRootView();
        rootView.addView(tryAgainButtonContainer);
    }
    private void setTryAgainButtonAnimation() {
        tryAgainButton.setAlpha(0f);
        tryAgainButton.setTranslationX(100f);
        tryAgainButton.setVisibility(View.VISIBLE);

        tryAgainButton.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(700)
                .setStartDelay(1000)
                .withEndAction(this::setTryAgainButtonClickAnimation) // Setup scaling click after entrance
                .start();
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setTryAgainButtonClickAnimation() {
        if (tryAgainButtonContainer == null) return;

        tryAgainButtonContainer.setClickable(true);
        tryAgainButtonContainer.setFocusable(true);

        tryAgainButtonContainer.post(() -> {
            ViewGroup parent = (ViewGroup) tryAgainButtonContainer.getParent();
            if (parent != null) {
                parent.setClipChildren(false);
                parent.setClipToPadding(false);
            }

            tryAgainButtonContainer.setPivotX(tryAgainButtonContainer.getWidth() / 2f);
            tryAgainButtonContainer.setPivotY(tryAgainButtonContainer.getHeight() / 2f);

            tryAgainButtonContainer.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        tryAgainButtonContainer.animate()
                                .scaleX(0.90f)
                                .scaleY(0.90f)
                                .setDuration(100)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        tryAgainButtonContainer.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                        break;
                }
                return false;
            });

            tryAgainButtonContainer.setOnClickListener(v -> {
                Intent intent = new Intent(ResultActivity.this, TypingActivity.class);
                startActivity(intent);
            });
        });
    }
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,getResources().getDisplayMetrics()
        );
    }
    private int getRecord() {
        SharedPreferences prefs = getSharedPreferences("TypingAppPrefs", MODE_PRIVATE);
        return prefs.getInt("wpmRecord", 0);  // 0 is default if no record is saved yet
    }
}
