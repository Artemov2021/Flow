package com.wpmapp.flow;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wpmapp.flow.model.TypingResult;

public class ResultActivity extends AppCompatActivity {

    private TextView wpmValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        wpmValue = findViewById(R.id.wpmValue);


        // Retrieve TypingResult from Intent
        @SuppressLint("UnsafeIntentLaunch")
        TypingResult result = (TypingResult) getIntent().getSerializableExtra("typing_result");
        if (result == null) {
            // fallback or error handling
            result = new TypingResult(0, 0);
        }

        setResult(result);  // Now call your own method to update UI

        setSwipeToShowBars();
        setBackButtonListener();

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
        String wpm = String.valueOf(result.correctWords);

        setWMPMargin(wpm);
        setWPMAnimation(result.correctWords);

        Log.d("CustomEditText","Result: typed words: "+result.typedWords);
    }
    private void setWMPMargin(String wpm) {
        int startMargin = 0;
        if (wpm.length() == 1) {
            startMargin = 120;
        } else if (wpm.length() == 2) {
            startMargin = 90;
        } else if (wpm.length() == 3) {
            startMargin = 75;
        }

        int marginStartPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                startMargin,
                getResources().getDisplayMetrics()
        );

        // Get current layout params
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) wpmValue.getLayoutParams();
        params.setMarginStart(marginStartPx);  // Only affects start (left in LTR)
        wpmValue.setLayoutParams(params);
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
}
