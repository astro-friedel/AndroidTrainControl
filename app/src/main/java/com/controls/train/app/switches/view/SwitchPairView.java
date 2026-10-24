package com.controls.train.app.switches.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.controls.train.R;

/**
 * SwitchPairView
 *
 * Draws a crossover pair diagram with upward train travel.
 *
 * Geometry:
 *   - Two parallel main tracks (left and right) run full height.
 *   - A crossover diagonal connects them:
 *       • Bottom frog on the right rail of the left main.
 *       • Top    frog on the left  rail of the right main.
 *       • The crossover goes left → right (bottom-left to top-right).
 *       • Both crossover rails are parallel (perpendicular gauge offset).
 *       • The crossover rails stay between the two mains.
 *
 * XML attributes:
 *   app:switchThrown="false"   mains active / crossover dim  (default)
 *   app:switchThrown="true"    crossover active / mains dim
 *
 * Java API:
 *   setThrown(boolean thrown)
 */
public class SwitchPairView extends View {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COLOR_STRAIGHT  = 0xFF2E7D32;
    private static final int COLOR_DIVERGING = 0xFFF9A825;
    private static final int COLOR_INACTIVE  = 0x662E7D32;
    private static final int COLOR_DIM_YEL   = 0x66F9A825;
    private static final int COLOR_SLEEPER   = 0x559E9E9E;

    // ── Paints ────────────────────────────────────────────────────────────────
    private final Paint railPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sleeperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frogPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean thrown = false;

    // ── Geometry (computed in onSizeChanged) ──────────────────────────────────
    private float railStroke, sleeperStroke, frogRadius, railGauge;
    private float topY, bottomY;

    // Left main rail positions
    private float lMainL, lMainR;
    // Right main rail positions
    private float rMainL, rMainR;

    // Crossover frogs
    private float frogBottomX, frogBottomY;  // bottom frog: right rail of left main
    private float frogTopX,    frogTopY;     // top frog:    left rail of right main

    // Crossover outer rail (parallel to inner, offset toward right)
    private float crossOuterBotX, crossOuterBotY;
    private float crossOuterTopX, crossOuterTopY;

    // ── Constructors ──────────────────────────────────────────────────────────
    public SwitchPairView(Context context) {
        super(context);
        init(context, null);
    }

    public SwitchPairView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SwitchPairView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwitchPairView);
            thrown = a.getBoolean(R.styleable.SwitchPairView_switchThrown, false);
            a.recycle();
        }

        railPaint.setStyle(Paint.Style.STROKE);
        railPaint.setStrokeCap(Paint.Cap.ROUND);

        sleeperPaint.setStyle(Paint.Style.STROKE);
        sleeperPaint.setStrokeCap(Paint.Cap.ROUND);

        frogPaint.setStyle(Paint.Style.FILL);
        frogPaint.setColor(0xFF757575);
        frogPaint.setAlpha(128);
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void setThrown(boolean thrown) {
        if (this.thrown != thrown) {
            this.thrown = thrown;
            invalidate();
        }
    }

    public boolean isThrown() {
        return thrown;
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        computeGeometry(w, h);
    }

    private void computeGeometry(int w, int h) {
        float pad = Math.min(w, h) * 0.08f;

        railGauge    = w * 0.09f;
        railStroke   = railGauge * 0.35f;
        sleeperStroke = railStroke * 0.6f;
        frogRadius   = railStroke * 1.1f;

        topY    = pad;
        bottomY = h - pad;

        // Centre the two mains with a gap equal to one rail gauge between them
        float totalSpan = railGauge * 2f + railGauge; // L-gauge + gap + R-gauge
        float startX = (w - totalSpan) / 2f;

        lMainL = startX;
        lMainR = startX + railGauge;
        rMainL = lMainR + railGauge;          // gap = railGauge between mains
        rMainR = rMainL + railGauge;

        // Crossover: bottom frog on right rail of left main (~65% from top)
        //            top    frog on left  rail of right main (~30% from top)
        frogBottomX = lMainR;
        frogBottomY = topY + (bottomY - topY) * 0.65f;
        frogTopX    = rMainL;
        frogTopY    = topY + (bottomY - topY) * 0.30f;

        // Crossover direction vector (inner rail: bottom-left → top-right)
        float dx = frogTopX - frogBottomX;
        float dy = frogTopY - frogBottomY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        // Perpendicular offset toward the right (CW rotation of travel dir)
        // CW perp of (dx,dy) = (dy, -dx)
        float perpX =  dy / len;
        float perpY = -dx / len;

        // Outer crossover rail offset one gauge to the right of inner
        crossOuterBotX = frogBottomX + perpX * railGauge;
        crossOuterBotY = frogBottomY + perpY * railGauge;
        crossOuterTopX = frogTopX    + perpX * railGauge;
        crossOuterTopY = frogTopY    + perpY * railGauge;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        railPaint.setStrokeWidth(railStroke);
        sleeperPaint.setStrokeWidth(sleeperStroke);

        drawMains(canvas);
        drawCrossover(canvas);
        drawFrogs(canvas);
    }

    private void drawMains(Canvas canvas) {
        railPaint.setColor(thrown ? COLOR_INACTIVE : COLOR_STRAIGHT);

        // Left main
        canvas.drawLine(lMainL, bottomY, lMainL, topY, railPaint);
        canvas.drawLine(lMainR, bottomY, lMainR, topY, railPaint);

        // Right main
        canvas.drawLine(rMainL, bottomY, rMainL, topY, railPaint);
        canvas.drawLine(rMainR, bottomY, rMainR, topY, railPaint);

        // Sleepers on both mains
        sleeperPaint.setColor(thrown ? COLOR_SLEEPER : COLOR_STRAIGHT);
        sleeperPaint.setAlpha(thrown ? 60 : 80);
        float[] sleeperYs = { 0.12f, 0.25f, 0.45f, 0.72f, 0.87f };
        for (float frac : sleeperYs) {
            float y = topY + (bottomY - topY) * frac;
            canvas.drawLine(lMainL - railStroke, y, lMainR + railStroke, y, sleeperPaint);
            canvas.drawLine(rMainL - railStroke, y, rMainR + railStroke, y, sleeperPaint);
        }
        sleeperPaint.setAlpha(255);
    }

    private void drawCrossover(Canvas canvas) {
        railPaint.setColor(thrown ? COLOR_DIVERGING : COLOR_DIM_YEL);

        // Inner crossover rail: frogBottom → frogTop
        canvas.drawLine(frogBottomX, frogBottomY, frogTopX, frogTopY, railPaint);
        // Outer crossover rail (parallel)
        canvas.drawLine(crossOuterBotX, crossOuterBotY, crossOuterTopX, crossOuterTopY, railPaint);

        // Sleeper ticks across the crossover
        sleeperPaint.setColor(thrown ? COLOR_DIVERGING : COLOR_SLEEPER);
        sleeperPaint.setAlpha(thrown ? 100 : 50);
        int ticks = 3;
        for (int i = 1; i <= ticks; i++) {
            float t = i / (float) (ticks + 1);
            float ix = frogBottomX    + t * (frogTopX    - frogBottomX);
            float iy = frogBottomY    + t * (frogTopY    - frogBottomY);
            float ox = crossOuterBotX + t * (crossOuterTopX - crossOuterBotX);
            float oy = crossOuterBotY + t * (crossOuterTopY - crossOuterBotY);
            canvas.drawLine(ix, iy, ox, oy, sleeperPaint);
        }
        sleeperPaint.setAlpha(255);
    }

    private void drawFrogs(Canvas canvas) {
        canvas.drawCircle(frogBottomX, frogBottomY, frogRadius, frogPaint);
        canvas.drawCircle(frogTopX,    frogTopY,    frogRadius, frogPaint);
    }
}
