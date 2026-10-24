package com.controls.train.app.switches.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.controls.train.R;

/**
 * SwitchYView
 *
 * Draws a Y-switch (wye turnout) diagram with upward train travel.
 *
 * Geometry:
 *   - A single approach track enters from the bottom centre.
 *   - At the frog (lower portion of the view) the track splits
 *     symmetrically into two legs, both angling upward:
 *       • Left leg:  exits upper-left   → GREEN  (normal / straight position)
 *       • Right leg: exits upper-right  → YELLOW (diverging position)
 *   - Both legs use parallel rails (20dp perpendicular gauge preserved).
 *   - Neither leg is truly straight — both diverge from centre.
 *
 * XML attributes:
 *   app:switchThrown="false"   left leg active (green)   (default)
 *   app:switchThrown="true"    right leg active (yellow)
 *
 * Java API:
 *   setThrown(boolean thrown)
 */
public class SwitchYView extends View {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int COLOR_LEFT_ACTIVE   = 0xFF2E7D32;   // green 800
    private static final int COLOR_LEFT_DIM      = 0x662E7D32;   // green 40%
    private static final int COLOR_RIGHT_ACTIVE  = 0xFFF9A825;   // amber 800
    private static final int COLOR_RIGHT_DIM     = 0x66F9A825;   // amber 40%
    private static final int COLOR_APPROACH      = 0xFF9E9E9E;
    private static final int COLOR_FROG          = 0xFF757575;
    private static final int COLOR_SLEEPER       = 0x559E9E9E;

    // ── Paints ────────────────────────────────────────────────────────────────
    private final Paint railPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sleeperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frogPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean thrown = false;   // false = left leg (green), true = right leg (yellow)

    // ── Geometry ──────────────────────────────────────────────────────────────
    private float railStroke, sleeperStroke, frogRadius, railGauge;
    private float topY, bottomY;
    private float cx;        // horizontal centre

    // Approach rails
    private float appL, appR;   // left and right approach rail x
    private float frogY;        // y of the frog

    // Left leg: inner rail start = (appL, frogY), exits upper-left
    private float leftInnerEndX,  leftInnerEndY;
    private float leftOuterStartX, leftOuterStartY;
    private float leftOuterEndX,   leftOuterEndY;

    // Right leg: inner rail start = (appR, frogY), exits upper-right
    private float rightInnerEndX,  rightInnerEndY;
    private float rightOuterStartX, rightOuterStartY;
    private float rightOuterEndX,   rightOuterEndY;

    // ── Constructors ──────────────────────────────────────────────────────────
    public SwitchYView(Context context) {
        super(context);
        init(context, null);
    }

    public SwitchYView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SwitchYView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwitchYView);
            thrown = a.getBoolean(R.styleable.SwitchYView_switchThrown, false);
            a.recycle();
        }

        railPaint.setStyle(Paint.Style.STROKE);
        railPaint.setStrokeCap(Paint.Cap.ROUND);

        sleeperPaint.setStyle(Paint.Style.STROKE);
        sleeperPaint.setStrokeCap(Paint.Cap.ROUND);

        frogPaint.setStyle(Paint.Style.FILL);
        frogPaint.setColor(COLOR_FROG);
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

        railGauge    = w * 0.10f;
        railStroke   = railGauge * 0.35f;
        sleeperStroke = railStroke * 0.6f;
        frogRadius   = railStroke * 1.1f;

        topY    = pad;
        bottomY = h - pad;
        cx      = w / 2f;

        // Approach rails centred, frog sits 60% from top
        appL = cx - railGauge / 2f;
        appR = cx + railGauge / 2f;
        frogY = topY + (bottomY - topY) * 0.60f;

        float legPad = pad * 1.5f;

        // ── Left leg: exits upper-left ────────────────────────────────────────
        // Inner rail starts at (appL, frogY), ends at upper-left corner
        leftInnerEndX = legPad + railGauge;
        leftInnerEndY = topY;

        float ldx = leftInnerEndX - appL;
        float ldy = leftInnerEndY - frogY;
        float llen = (float) Math.sqrt(ldx * ldx + ldy * ldy);

        // Outer rail is one gauge to the RIGHT of the inner rail
        // Right of travel direction (ldx,ldy): CW perp = (ldy/llen, -ldx/llen)
        float lPerpX =  ldy / llen;
        float lPerpY = -ldx / llen;

        leftOuterStartX = appL          + lPerpX * railGauge;
        leftOuterStartY = frogY         + lPerpY * railGauge;
        leftOuterEndX   = leftInnerEndX + lPerpX * railGauge;
        leftOuterEndY   = leftInnerEndY + lPerpY * railGauge;

        // ── Right leg: exits upper-right ─────────────────────────────────────
        // Inner rail starts at (appR, frogY), ends at upper-right corner
        rightInnerEndX = w - legPad - railGauge;
        rightInnerEndY = topY;

        float rdx = rightInnerEndX - appR;
        float rdy = rightInnerEndY - frogY;
        float rlen = (float) Math.sqrt(rdx * rdx + rdy * rdy);

        // Outer rail is one gauge to the LEFT of the inner rail
        // Left of travel direction (rdx,rdy): CCW perp = (-rdy/rlen, rdx/rlen)
        float rPerpX = -rdy / rlen;
        float rPerpY =  rdx / rlen;

        rightOuterStartX = appR           + rPerpX * railGauge;
        rightOuterStartY = frogY          + rPerpY * railGauge;
        rightOuterEndX   = rightInnerEndX + rPerpX * railGauge;
        rightOuterEndY   = rightInnerEndY + rPerpY * railGauge;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        railPaint.setStrokeWidth(railStroke);
        sleeperPaint.setStrokeWidth(sleeperStroke);

        drawApproach(canvas);
        drawLeftLeg(canvas);
        drawRightLeg(canvas);
        canvas.drawCircle(cx, frogY, frogRadius, frogPaint);
    }

    private void drawApproach(Canvas canvas) {
        railPaint.setColor(COLOR_APPROACH);
        canvas.drawLine(appL, bottomY, appL, frogY, railPaint);
        canvas.drawLine(appR, bottomY, appR, frogY, railPaint);

        sleeperPaint.setColor(COLOR_SLEEPER);
        float step = (bottomY - frogY) / 4f;
        for (int i = 1; i <= 3; i++) {
            float y = bottomY - step * i;
            canvas.drawLine(appL - railStroke, y, appR + railStroke, y, sleeperPaint);
        }
    }

    private void drawLeftLeg(Canvas canvas) {
        boolean active = !thrown;
        railPaint.setColor(active ? COLOR_LEFT_ACTIVE : COLOR_LEFT_DIM);

        canvas.drawLine(appL, frogY, leftInnerEndX, leftInnerEndY, railPaint);
        canvas.drawLine(leftOuterStartX, leftOuterStartY, leftOuterEndX, leftOuterEndY, railPaint);

        sleeperPaint.setColor(active ? COLOR_LEFT_ACTIVE : COLOR_SLEEPER);
        sleeperPaint.setAlpha(active ? 100 : 50);
        drawLegSleepers(canvas, appL, frogY, leftInnerEndX, leftInnerEndY,
                        leftOuterStartX, leftOuterStartY, leftOuterEndX, leftOuterEndY);
        sleeperPaint.setAlpha(255);
    }

    private void drawRightLeg(Canvas canvas) {
        boolean active = thrown;
        railPaint.setColor(active ? COLOR_RIGHT_ACTIVE : COLOR_RIGHT_DIM);

        canvas.drawLine(appR, frogY, rightInnerEndX, rightInnerEndY, railPaint);
        canvas.drawLine(rightOuterStartX, rightOuterStartY, rightOuterEndX, rightOuterEndY, railPaint);

        sleeperPaint.setColor(active ? COLOR_RIGHT_ACTIVE : COLOR_SLEEPER);
        sleeperPaint.setAlpha(active ? 100 : 50);
        drawLegSleepers(canvas, appR, frogY, rightInnerEndX, rightInnerEndY,
                        rightOuterStartX, rightOuterStartY, rightOuterEndX, rightOuterEndY);
        sleeperPaint.setAlpha(255);
    }

    /**
     * Draws evenly-spaced sleeper ticks between inner and outer rails of one leg.
     */
    private void drawLegSleepers(Canvas canvas,
                                 float innerStartX, float innerStartY,
                                 float innerEndX,   float innerEndY,
                                 float outerStartX, float outerStartY,
                                 float outerEndX,   float outerEndY) {
        int ticks = 3;
        for (int i = 1; i <= ticks; i++) {
            float t = i / (float) (ticks + 1);
            float ix = innerStartX + t * (innerEndX - innerStartX);
            float iy = innerStartY + t * (innerEndY - innerStartY);
            float ox = outerStartX + t * (outerEndX - outerStartX);
            float oy = outerStartY + t * (outerEndY - outerStartY);
            canvas.drawLine(ix, iy, ox, oy, sleeperPaint);
        }
    }
}
