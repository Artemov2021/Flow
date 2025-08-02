package com.wpmapp.flow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
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
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;


public class TypingActivity extends AppCompatActivity  {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typing);

        setSwipeToShowBars();
        setBackButtonListener();
        setTextFieldFocused();
        setTextFieldMoveListener();

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
                Intent intent = new Intent(TypingActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setTextFieldFocused() {
        EditText editText = findViewById(R.id.editText);

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
        EditText editText = findViewById(R.id.editText);
        View rootView = getWindow().getDecorView().getRootView();
        int moveDistanceDp = 145;
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




    private void setColorfulText() {
        EditText editText = findViewById(R.id.editText);

        String text = "Hello colorful world!";

        SpannableStringBuilder spannable = new SpannableStringBuilder(text);

        spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.BLUE), 6, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.GREEN), 15, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannable);
    }
    private void test() {
        EditText editText = findViewById(R.id.editText);

        final String defaultText = "Hello world";
        final int grayColor = Color.parseColor("#575757");
        final int whiteColor = Color.parseColor("#FFFFFF");

        final SpannableStringBuilder spannable = new SpannableStringBuilder(defaultText);
        spannable.setSpan(new ForegroundColorSpan(grayColor), 0, defaultText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannable);
        editText.setSelection(0);

        editText.addTextChangedListener(new TextWatcher() {
            int lastLength = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // no-op
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Remove listener temporarily to avoid infinite loop
                editText.removeTextChangedListener(this);

                int cursorPos = editText.getSelectionStart();

                // Make sure cursor is within bounds
                if (cursorPos < 0) cursorPos = 0;
                if (cursorPos > defaultText.length()) cursorPos = defaultText.length();

                // We compare user input char-by-char with defaultText
                // Only advance coloring if typed char matches the expected char
                int coloredLength = 0;

                CharSequence input = s.toString();
                for (int i = 0; i < input.length() && i < defaultText.length(); i++) {
                    if (input.charAt(i) == defaultText.charAt(i)) {
                        coloredLength = i + 1;
                    } else {
                        break; // Stop coloring on first mismatch
                    }
                }

                // Create new SpannableStringBuilder and apply colors
                SpannableStringBuilder newSpan = new SpannableStringBuilder(defaultText);
                // Gray for all
                newSpan.setSpan(new ForegroundColorSpan(grayColor), 0, defaultText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // White for correctly typed chars
                if (coloredLength > 0) {
                    newSpan.setSpan(new ForegroundColorSpan(whiteColor), 0, coloredLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Set text and move cursor after last correctly typed char
                editText.setText(newSpan);
                editText.setSelection(coloredLength);

                editText.addTextChangedListener(this);
            }
        });
    }

}
