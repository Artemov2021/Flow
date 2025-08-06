package com.wpmapp.flow.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import com.wpmapp.flow.R;
import com.wpmapp.flow.TypingActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class CustomEditText extends androidx.appcompat.widget.AppCompatEditText {
    private int lockedCursorPosition;
    private boolean isProgrammaticSelection = false;
    private final Set<Integer> wrongCharPositions = new HashSet<>();
    private final Set<Integer> correctCharPositions = new HashSet<>();
    private ObjectAnimator scrollAnimator;
    private boolean timerStarted = false;

    private Queue<String> leftWords = new LinkedList<>();

    private TimerStartListener timerStartListener;

    private TypingListener typingListener;


    public interface TypingListener {
        void onWordTyped();
        void onCorrectWordCountUpdated(int correctWordCount);
        void onWrongCharTyped();
    }

    // In CustomEditText:
    public interface TimerStartListener {
        void onStartTimer(long durationMillis);
    }


    public void setTypingListener(TypingListener listener) {
        this.typingListener = listener;
    }

    public void setTimerStartListener(TimerStartListener listener) {
        this.timerStartListener = listener;
    }


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
        setRandomText();

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
                    // This means first character typed
                    if (getContext() instanceof TypingActivity) {
                        ((TypingActivity) getContext()).startTimer(60_000);  // pass 1 minute or your duration here
                    }
                    timerStarted = true;  // prevent restarting timer on subsequent keys
                }

                int cursorPos = getSelectionStart();
                String currentText = getText().toString();

                if (cursorPos < currentText.length() && text.length() == 1) {
                    char expectedChar = currentText.charAt(cursorPos);
                    char typedChar = text.charAt(0);

                    // Always move cursor forward
                    lockedCursorPosition = cursorPos + 1;

                    int remainingChars = currentText.length() - lockedCursorPosition;
                    if (remainingChars == 10) {
                        append(" "+leftWords.poll());
                    }

                    int nextPos = cursorPos + 1;
                    if (nextPos < currentText.length()) {
                        char nextChar = currentText.charAt(nextPos);
                        if (nextChar == ' ') {
                            // The next character to type after current position is a space
                            if (typingListener != null) {
                                typingListener.onWordTyped();
                            }
                        }
                    }

                    if (expectedChar == ' ' && typedChar != expectedChar) {
                        typingListener.onWrongCharTyped();
                        return false;
                    } else if (typedChar == expectedChar) {
                        // Correct char: remove from wrong positions if previously wrong
                        wrongCharPositions.remove(cursorPos);
                    } else {
                        // Wrong char: add position to wrong set
                        typingListener.onWrongCharTyped();
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

        // Get total text width
        float textWidth = layout.getLineWidth(0);
        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();

        // If text fits entirely in the visible area, don't scroll
        if (textWidth <= viewWidth) {
            return;
        }

        int cursorX = (int) layout.getPrimaryHorizontal(cursorPos);

        int thresholdCharIndex = 13;
        int thresholdX = (int) layout.getPrimaryHorizontal(thresholdCharIndex);

        int targetScrollX;
        if (cursorPos >= thresholdCharIndex && cursorX > thresholdX) {
            targetScrollX = cursorX - thresholdX;
        } else {
            targetScrollX = 0;
        }

        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }

        scrollAnimator = ObjectAnimator.ofInt(this, "scrollX", getScrollX(), targetScrollX);
        scrollAnimator.setDuration(200);
        scrollAnimator.start();
    }
    private void updateTextColorsOptimized() {
        Editable editable = getText();
        if (editable == null) return;

        final int whiteColor = 0xFFFFFFFF;
        final int grayColor = 0xFF575757;
        final int redColor = 0xFFF85858;

        int currentPos = lockedCursorPosition - 1;
        if (currentPos >= 0 && currentPos < editable.length()) {
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

        // Offload gray-coloring to a background thread and throttle
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Editable ed = getText();
            if (ed == null) return;

            for (int i = lockedCursorPosition; i < ed.length(); i++) {
                if (!wrongCharPositions.contains(i) && !correctCharPositions.contains(i)) {
                    if (!hasColorSpan(ed, i, grayColor)) {
                        removeSpansAtPosition(ed, i);
                        ed.setSpan(new ForegroundColorSpan(grayColor), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }, 50); // Slight delay to avoid blocking fast typing
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

        if (typingListener != null) {
            typingListener.onCorrectWordCountUpdated(wordCount);
        }
    }
    private void setRandomText() {
        try {
            String randomText = getRandomTextFromRaw(R.raw.texts);
            String[] words = randomText.split("\\s+"); // Split by whitespace

            int initialWordCount = 7;
            StringBuilder firstWords = new StringBuilder();

            // Add first 5 words to the EditText
            for (int i = 0; i < Math.min(initialWordCount, words.length); i++) {
                firstWords.append(words[i]).append(" ");
            }
            setText(firstWords.toString().trim());

            // Clear old words from the queue
            leftWords.clear();

            // Add remaining words to the queue
            for (int i = initialWordCount; i < words.length; i++) {
                leftWords.add(words[i]);  // add() adds to the end of the queue
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String getRandomTextFromRaw(int rawResId) throws IOException {
        InputStream is = getResources().openRawResource(rawResId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder allText = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            allText.append(line).append("\n");
        }
        reader.close();

        // Split texts by empty line(s)
        String[] texts = allText.toString().split("\\n\\s*\\n");

        Random random = new Random();
        int index = random.nextInt(texts.length);
        return texts[index].trim();
    }
}
