package com.wpmapp.flow;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;


public class MainActivity extends AppCompatActivity {

    private LinearLayout startButton;
    private FrameLayout startButtonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSwipeToShowBars();
        setStartButton();
        setTryAgainButtonClickAnimation();
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
    private void setStartButton() {
        // === Outer container that will be animated ===
        startButtonContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(53)
        );
        containerParams.gravity = Gravity.BOTTOM;
        containerParams.bottomMargin = dpToPx(55);
        containerParams.leftMargin = dpToPx(23);
        containerParams.rightMargin = dpToPx(23);
        startButtonContainer.setLayoutParams(containerParams);

        // === Actual button inside container ===
        startButton = new LinearLayout(this);
        startButton.setOrientation(LinearLayout.HORIZONTAL);
        startButton.setGravity(Gravity.CENTER);
        startButton.setPadding(dpToPx(16), 0, dpToPx(16), 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#FFFFFF"));
        background.setCornerRadius(dpToPx(22));
        startButton.setBackground(background);

        // Match container size
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        startButton.setLayoutParams(buttonParams);

        // === Text ===
        TextView tryAgainText = new TextView(this);
        tryAgainText.setText("Start");
        tryAgainText.setTextSize(21);
        tryAgainText.setTextColor(Color.parseColor("#000000"));
        tryAgainText.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_semi_bold));

        // Add text to button
        startButton.addView(tryAgainText);

        // Add button to container
        startButtonContainer.addView(startButton);

        // Add container to root
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView().getRootView();
        rootView.addView(startButtonContainer);
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setTryAgainButtonClickAnimation() {
        if (startButtonContainer == null) return;

        startButtonContainer.setClickable(true);
        startButtonContainer.setFocusable(true);

        startButtonContainer.post(() -> {
            ViewGroup parent = (ViewGroup) startButtonContainer.getParent();
            if (parent != null) {
                parent.setClipChildren(false);
                parent.setClipToPadding(false);
            }

            startButtonContainer.setPivotX(startButtonContainer.getWidth() / 2f);
            startButtonContainer.setPivotY(startButtonContainer.getHeight() / 2f);

            startButtonContainer.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startButtonContainer.animate()
                                .scaleX(0.90f)
                                .scaleY(0.90f)
                                .setDuration(100)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        startButtonContainer.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                        break;
                }
                return false;
            });

            startButtonContainer.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, TypingActivity.class);
                startActivity(intent);
            });
        });
    }
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,getResources().getDisplayMetrics()
        );
    }


}
