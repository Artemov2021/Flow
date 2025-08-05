package com.wpmapp.flow.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class AccuracyLineView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private int percentage = 0;

    public AccuracyLineView(Context context) {
        super(context);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(0xFF333333); // Background line color (dark gray)
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(Color.parseColor("#4DFF6E"));
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setAntiAlias(true);
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
        invalidate(); // Redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float height = 12;
        float width = getWidth();

        float radius = height / 2;

        // Background line
        RectF bgRect = new RectF(0, 0, width, height);
        canvas.drawRoundRect(bgRect, radius, radius, backgroundPaint);

        // Progress line
        float progressWidth = (percentage / 100f) * width;
        RectF progressRect = new RectF(0, 0, progressWidth, height);
        canvas.drawRoundRect(progressRect, radius, radius, progressPaint);
    }
}
