package com.wpmapp.flow;

import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

public class Animator {
    private final ImageView imageView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Example frame sequence: { drawableId, delayBeforeStart, durationToHold }
    private final int[][] frames = {
            {R.drawable.app_loading1, 400, 110},
            {R.drawable.app_loading2, 0, 110},
            {R.drawable.app_loading3, 0, 110},
            {R.drawable.app_loading4, 300,0},
            {R.drawable.app_loading5, 450,0},
            {R.drawable.app_loading6, 600,0}
    };

    private int currentIndex = 0;

    public Animator(ImageView imageView) {
        this.imageView = imageView;
    }

    public void start() {
        playNextFrame();
    }

    private void playNextFrame() {
        if (currentIndex >= frames.length) return;

        int[] frame = frames[currentIndex];
        int drawableId = frame[0];
        int delay = frame[1];
        int hold = frame[2];

        handler.postDelayed(() -> {
            imageView.setImageResource(drawableId);
            currentIndex++;

            handler.postDelayed(this::playNextFrame, hold);

        }, delay);
    }
}
