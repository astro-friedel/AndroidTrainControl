package com.controls.train.app.switches.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.controls.train.R;

/**
 * SwitchSingleView
 *
 * Draws a single turnout diagram with upward train travel.
 *
 * Geometry:
 *   - A single approach track enters from the bottom centre.
 *   - At the frog (mid-height) the track splits into two routes
 *     both exiting upward:
 *       • Straight: the same vertical pair continues to the top.   (GREEN)
 *       • Diverging: exits upper-left or upper-right at an angle.  (YELLOW)
 *   - Both rails of the diverging branch are parallel (gauge preserved
 *     via perpendicular offset).
 *
 * XML attributes (declare in res/values/attrs.xml):
 *   app:switchThrown="false"   straight active  (default)
 *   app:switchThrown="true"    diverging active
 *   app:divergeRight="false"   diverges upper-left  (default)
 *   app:divergeRight="true"    diverges upper-right
 *
 * Java API:
 *   setThrown(boolean thrown)       — update state and redraw
 *   setDivergeRight(boolean right)  — change direction and redraw
 */
public class SwitchSingleView extends View {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int COLOR_STRAIGHT  = 0xFF2E7D32;   // Material green 800
    private static final int COLOR_DIVERGING = 0xFFF9A825;   // Material amber 800
    private static final int COLOR_INACTIVE  = 0x662E7D32;   // green @ 40% alpha
    private static final int COLOR_DIM_YEL   = 0x66F9A825;   // amber @ 40% alpha
    private static final int COLOR_APPROACH  = 0xFF9E9E9E;   // grey 500
    private static final int COLOR_FROG      = 0xFF757575;   // grey 600
    private static final int COLOR_SLEEPER   = 0x559E9E9E;   // grey, subtle

    // ── Paints ───────────────────────────────────────────────────────────────
    private final Paint railPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sleeperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frogPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean thrown      = false;  // false = straight, true = diverging
    private boolean divergeRight = false; // false = upper-left, true = upper-right

    // ── Geometry (computed in onSizeChanged) ──────────────────────────────────
    private float railGauge;      // pixel distance between rail centres
    private float railStroke;     // rail line width
    private float sleeperStroke;
    private float frogRadius;

    // Track centreline x positions
    private float railL, railR;   // left and right rail of the main/straight track
    private float frogY;          // y coordinate of the frog (split point)
    private float topY, bottomY;  // drawing bounds with padding

    // Diverging outer rail start offset (perpendicular to diverge direction)
    // Recomputed in onSizeChanged.
    private float divOuterStartX, divOuterStartY;
    private float divOuterEndX,   divOuterEndY;
    private float divInnerEndX,   divInnerEndY;

    // ── Constructors ──────────────────────────────────────────────────────────
    public SwitchSingleView(Context context) {
        super(context);
        init(context, null);
    }

    public SwitchSingleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SwitchSingleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwitchSingleView);
            thrown       = a.getBoolean(R.styleable.SwitchSingleView_switchThrown, false);
            divergeRight = a.getBoolean(R.styleable.SwitchSingleView_divergeRight, false);
            a.recycle();
        }

        sleeperPaint.setStyle(Paint.Style.STROKE);
        sleeperPaint.setStrokeCap(Paint.Cap.ROUND);

        railPaint.setStyle(Paint.Style.STROKE);
        railPaint.setStrokeCap(Paint.Cap.ROUND);

        frogPaint.setStyle(Paint.Style.FILL);
        frogPaint.setColor(COLOR_FROG);
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

    public void setDivergeRight(boolean right) {
        if (this.divergeRight != right) {
            this.divergeRight = right;
            computeGeometry();
            invalidate();
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        computeGeometry();
    }

    private void computeGeometry() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float pad = Math.min(w, h) * 0.08f;

        railGauge    = w * 0.10f;          // ~10% of width between rail centres
        railStroke   = railGauge * 0.35f;
        sleeperStroke = railStroke * 0.6f;
        frogRadius   = railStroke * 1.1f;

        topY    = pad;
        bottomY = h - pad;
        frogY   = topY + (bottomY - topY) * 0.55f;  // frog sits ~55% from top

        // Straight track centred horizontally
        float cx = w / 2f;
        railL = cx - railGauge / 2f;
        railR = cx + railGauge / 2f;

        // Diverging direction vector (inner rail: from frog to top corner)
        float divEndX, divEndY;
        if (!divergeRight) {
            // upper-left exit
            divEndX = pad + railGauge;
            divEndY = topY;
        } else {
            // upper-right exit
            divEndX = w - pad - railGauge;
            divEndY = topY;
        }

        // Inner diverging rail starts at the near straight rail at frog
        float innerStartX = divergeRight ? railL : railR;
        float innerStartY = frogY;
        divInnerEndX = divEndX;
        divInnerEndY = divEndY;

        // Direction vector of diverging branch
        float dx = divEndX - innerStartX;
        float dy = divEndY - innerStartY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        // Perpendicular unit vector (rotated 90° CCW from travel direction)
        // For left-diverge: outer rail is to the right of inner (toward straight track)
        // For right-diverge: outer rail is to the left of inner (toward straight track)
        float perpX =  dy / len;   // CCW perp of (dx,dy) is (-dy,dx); CW is (dy,-dx)
        float perpY = -dx / len;

        // We want outer rail on the side toward the straight track
        // For left-diverge (dx<0): CW perp points right → toward straight ✓
        // For right-diverge (dx>0): CCW perp points left → toward straight ✓
        // Both cases: CW perp = (dy/len, -dx/len) works for left, need negation for right.
        if (divergeRight) {
            perpX = -perpX;
            perpY = -perpY;
        }

        divOuterStartX = innerStartX + perpX * railGauge;
        divOuterStartY = innerStartY + perpY * railGauge;
        divOuterEndX   = divEndX     + perpX * railGauge;
        divOuterEndY   = divEndY     + perpY * railGauge;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        railPaint.setStrokeWidth(railStroke);
        sleeperPaint.setStrokeWidth(sleeperStroke);

        // 1. Approach track (bottom → frog) — always grey
        drawApproach(canvas);

        // 2. Straight route (frog → top) — green (dim if thrown)
        drawStraight(canvas);

        // 3. Diverging route (frog → top corner) — yellow (dim if not thrown)
        drawDiverging(canvas);

        // 4. Frog dot
        frogPaint.setAlpha(128);
        canvas.drawCircle(divergeRight ? railL : railR, frogY, frogRadius, frogPaint);
    }

    private void drawApproach(Canvas canvas) {
        railPaint.setColor(COLOR_APPROACH);
        canvas.drawLine(railL, bottomY, railL, frogY, railPaint);
        canvas.drawLine(railR, bottomY, railR, frogY, railPaint);

        sleeperPaint.setColor(COLOR_SLEEPER);
        float step = (bottomY - frogY) / 4f;
        for (int i = 1; i <= 3; i++) {
            float y = bottomY - step * i;
            canvas.drawLine(railL - railStroke, y, railR + railStroke, y, sleeperPaint);
        }
    }

    private void drawStraight(Canvas canvas) {
        boolean active = !thrown;
        railPaint.setColor(active ? COLOR_STRAIGHT : COLOR_INACTIVE);
        canvas.drawLine(railL, frogY, railL, topY, railPaint);
        canvas.drawLine(railR, frogY, railR, topY, railPaint);

        sleeperPaint.setColor(active ? COLOR_STRAIGHT : COLOR_SLEEPER);
        sleeperPaint.setAlpha(active ? 100 : 60);
        float step = (frogY - topY) / 5f;
        for (int i = 1; i <= 4; i++) {
            float y = frogY - step * i;
            canvas.drawLine(railL - railStroke, y, railR + railStroke, y, sleeperPaint);
        }
        sleeperPaint.setAlpha(255);
    }

    private void drawDiverging(Canvas canvas) {
        boolean active = thrown;
        railPaint.setColor(active ? COLOR_DIVERGING : COLOR_DIM_YEL);

        float innerStartX = divergeRight ? railL : railR;

        // Inner diverging rail
        canvas.drawLine(innerStartX,    frogY,          divInnerEndX, divInnerEndY, railPaint);
        // Outer diverging rail (parallel)
        canvas.drawLine(divOuterStartX, divOuterStartY, divOuterEndX, divOuterEndY, railPaint);

        // A couple of sleeper ticks along the diverging branch
        float steps = 3f;
        for (int i = 1; i <= (int) steps; i++) {
            float t = i / (steps + 1f);
            float mx = innerStartX    + t * (divInnerEndX - innerStartX);
            float my = frogY          + t * (divInnerEndY - frogY);
            float ox = divOuterStartX + t * (divOuterEndX - divOuterStartX);
            float oy = divOuterStartY + t * (divOuterEndY - divOuterStartY);
            sleeperPaint.setColor(active ? COLOR_DIVERGING : COLOR_SLEEPER);
            sleeperPaint.setAlpha(active ? 100 : 50);
            canvas.drawLine(mx, my, ox, oy, sleeperPaint);
        }
        sleeperPaint.setAlpha(255);
    }
}
