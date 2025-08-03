package com.wpmapp.flow.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.TextView;

import com.wpmapp.flow.TypingActivity;

import java.util.HashSet;
import java.util.Set;

public class CustomEditText extends androidx.appcompat.widget.AppCompatEditText {
    private int lockedCursorPosition = 0;
    private int correctWordCount = 0;
    private boolean isProgrammaticSelection = false;
    private final Set<Integer> wrongCharPositions = new HashSet<>();
    private final Set<Integer> correctCharPositions = new HashSet<>();
    private ObjectAnimator scrollAnimator;
    private boolean timerStarted = false;


    public CustomEditText(Context context) {
        super(context);
        init();
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setCursorVisible(true);
        setLongClickable(false);
        setTextIsSelectable(false);

        // Disable text actions like copy/paste
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            public boolean onCreateActionMode(ActionMode mode, Menu menu) { return false; }
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }
            public void onDestroyActionMode(ActionMode mode) {}
        });

        setOnLongClickListener(v -> true); // Disable long click
    }

    @Override
    public boolean performLongClick() {
        return true; // Block long press insertion
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (!isProgrammaticSelection) {
            isProgrammaticSelection = true;
            setSelection(lockedCursorPosition);
            isProgrammaticSelection = false;
        } else {
            super.onSelectionChanged(selStart, selEnd);
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

        InputConnection base = super.onCreateInputConnection(outAttrs);
        return new InputConnectionWrapper(base, true) {

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                if (!timerStarted && text.length() > 0) {
                    // Start timer here - notify your activity or start directly
                    if (getContext() instanceof TypingActivity) {
                        ((TypingActivity) getContext()).startTimer();
                    }
                    timerStarted = true;
                }

                int cursorPos = getSelectionStart();
                String currentText = getText().toString();

                if (cursorPos < currentText.length() && text.length() == 1) {
                    char expectedChar = currentText.charAt(cursorPos);
                    char typedChar = text.charAt(0);

                    // Always move cursor forward
                    lockedCursorPosition = cursorPos + 1;

                    if (typedChar == expectedChar) {
                        // Correct char: remove from wrong positions if previously wrong
                        wrongCharPositions.remove(cursorPos);
                    } else if (expectedChar == ' ') {
                        return false;
                    } else {
                        // Wrong char: add position to wrong set
                        wrongCharPositions.add(cursorPos);
                    }

                    isProgrammaticSelection = true;
                    CustomEditText.this.setSelection(lockedCursorPosition);
                    isProgrammaticSelection = false;

                    updateTextColorsOptimized();
                    updateCorrectWordCount();
                    adjustScroll();
                    return true;
                }
                return false;
            }


            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                Log.d("CustomEditText", "Backspace ignored");
                return true; // Block delete/backspace
            }

            @Override
            public boolean setComposingText(CharSequence text, int newCursorPosition) {
                return true; // Block IME composing text
            }

            @Override
            public boolean setComposingRegion(int start, int end) {
                return true;
            }

            @Override
            public boolean finishComposingText() {
                return true;
            }
        };
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        return false; // Disable paste/cut/etc
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        char c = (char) event.getUnicodeChar();
        if (c != 0) {
            Log.d("CustomEditText", "KeyDown typed: " + c);
        }
        return true; // Block hardware key edits
    }
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            clearFocus(); // Allow dismissing keyboard
        }
        return super.onKeyPreIme(keyCode, event);
    }
    private void adjustScroll() {
        int cursorPos = getSelectionStart();
        Layout layout = getLayout();
        if (layout == null) return;

        int cursorX = (int) layout.getPrimaryHorizontal(cursorPos);
        int width = getWidth() - getPaddingLeft() - getPaddingRight();

        int thresholdCharIndex = 13;
        int thresholdX = (int) layout.getPrimaryHorizontal(thresholdCharIndex);

        int targetScrollX;
        if (cursorPos >= thresholdCharIndex && cursorX > thresholdX) {
            targetScrollX = cursorX - thresholdX;
        } else {
            targetScrollX = 0;
        }

        // Cancel previous animation if running
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }

        // Animate scrollX property smoothly over 200 ms
        scrollAnimator = ObjectAnimator.ofInt(this, "scrollX", getScrollX(), targetScrollX);
        scrollAnimator.setDuration(200);
        scrollAnimator.start();
    }
    private void updateTextColorsOptimized() {
        Editable editable = getText();
        if (editable == null) return;

        int whiteColor = 0xFFFFFFFF;
        int grayColor = 0xFF575757;
        int redColor = 0xFFF85858;

        int currentPos = lockedCursorPosition - 1;
        if (currentPos >= 0 && currentPos < editable.length()) {
            // Check and apply color only if needed
            if (wrongCharPositions.contains(currentPos)) {
                if (!hasColorSpan(editable, currentPos, redColor)) {
                    removeSpansAtPosition(editable, currentPos);
                    editable.setSpan(new ForegroundColorSpan(redColor), currentPos, currentPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                if (!hasColorSpan(editable, currentPos, whiteColor)) {
                    removeSpansAtPosition(editable, currentPos);
                    editable.setSpan(new ForegroundColorSpan(whiteColor), currentPos, currentPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    correctCharPositions.add(currentPos);
                }
            }
        }

        // Lazy-gray color rest (on-demand only, or occasionally)
        post(() -> {
            for (int i = lockedCursorPosition; i < editable.length(); i++) {
                if (!wrongCharPositions.contains(i) && !correctCharPositions.contains(i)) {
                    if (!hasColorSpan(editable, i, grayColor)) {
                        removeSpansAtPosition(editable, i);
                        editable.setSpan(new ForegroundColorSpan(grayColor), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        });
    }
    private boolean hasColorSpan(Editable editable, int pos, int color) {
        for (ForegroundColorSpan span : editable.getSpans(pos, pos + 1, ForegroundColorSpan.class)) {
            if (editable.getSpanStart(span) <= pos && editable.getSpanEnd(span) > pos && span.getForegroundColor() == color) {
                return true;
            }
        }
        return false;
    }
    private void removeSpansAtPosition(Editable editable, int position) {
        for (ForegroundColorSpan span : editable.getSpans(position, position + 1, ForegroundColorSpan.class)) {
            editable.removeSpan(span);
        }
    }
    private void updateCorrectWordCount() {
        String fullText = getText().toString();

        int lastSpace = -1;
        int wordCount = 0;

        for (int i = 0; i <= fullText.length(); i++) {
            boolean isEnd = (i == fullText.length());
            char c = isEnd ? ' ' : fullText.charAt(i);

            if (c == ' ' || isEnd) {
                boolean isWordCorrect = true;

                for (int j = lastSpace + 1; j < i; j++) {
                    if (!correctCharPositions.contains(j)) {
                        isWordCorrect = false;
                        break;
                    }
                }

                if (isWordCorrect && i > lastSpace + 1) {
                    wordCount++;
                }

                lastSpace = i;
            }
        }

        correctWordCount = wordCount;
        Log.d("CustomEditText", "Correct words: " + correctWordCount);
    }
}
