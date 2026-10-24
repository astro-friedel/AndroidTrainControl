package com.controls.train.app.switches;

/**
 * SwitchEntry
 *
 * Abstract base for a single logical switch entity.
 * Subclasses cover the two structural variants:
 *   SingleSwitchEntry  — one set of SVG paths (switch_X_base/norm/div, rect_X)
 *   PairSwitchEntry    — two sets of SVG paths (switch_Xa/Xb_base/norm/div, rect_X)
 *
 * Both are controlled as a single unit: one toggle, one thrown state,
 * one diagram view, one selection rect.
 */
public abstract class SwitchEntry {

    public enum SwitchType {
        LEFT,       // single turnout, diverges upper-left
        RIGHT,      // single turnout, diverges upper-right
        Y,          // Y-switch, symmetric
        PAIR        // crossover pair, always PairSwitchEntry
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Numeric switch id — matches the X in switch_X_base / rect_X. */
    protected final int id;

    protected final SwitchType type;

    /** Current thrown state. false = normal/straight, true = diverging. */
    private boolean thrown = false;

    /**
     * Base colour string as read from the SVG at init time (e.g. "#FF4444").
     * Used to restore norm/div path colours when switching state.
     * Null until the WebView has loaded and initSwitches() has run.
     */
    private String baseColor = null;

    // ── Constructor ───────────────────────────────────────────────────────────

    protected SwitchEntry(int id, SwitchType type) {
        this.id   = id;
        this.type = type;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public int getId()           { return id; }
    public SwitchType getType()  { return type; }
    public boolean isThrown()    { return thrown; }
    public String getBaseColor() { return baseColor; }

    public void setThrown(boolean thrown)      { this.thrown = thrown; }
    public void setBaseColor(String baseColor) { this.baseColor = baseColor; }

    /** SVG path id for the selection rectangle (same for single and pair). */
    public String getRectPathId() {
        return "rect_" + id;
    }

    /**
     * All SVG path ids whose colour must be updated when thrown state changes.
     * Returned as a flat array: [norm0, div0, norm1, div1, ...] in pairs.
     * Callers iterate in steps of 2: index i = norm path, i+1 = div path.
     */
    public abstract String[] getNormDivPathIds();

    /**
     * The base path id(s) to read colour from at init time.
     * For a single switch this is one element; for a pair, two.
     * The first element is always used as the canonical baseColor.
     */
    public abstract String[] getBasePathIds();


    // ── Concrete subclasses ───────────────────────────────────────────────────

    /**
     * A single normal turnout (left-diverging, right-diverging, or Y).
     * SVG paths: switch_X_base, switch_X_norm, switch_X_div, rect_X
     */
    public static class SingleSwitchEntry extends SwitchEntry {

        public SingleSwitchEntry(int id, SwitchType type) {
            super(id, type);
            if (type == SwitchType.PAIR) {
                throw new IllegalArgumentException(
                        "Use PairSwitchEntry for crossover pairs.");
            }
        }

        @Override
        public String[] getNormDivPathIds() {
            return new String[]{
                    "switch_" + id + "_norm",
                    "switch_" + id + "_div"
            };
        }

        @Override
        public String[] getBasePathIds() {
            return new String[]{ "switch_" + id + "_base" };
        }
    }

    /**
     * A crossover pair: two physical switches controlled as one logical unit.
     * SVG paths: switch_Xa_base, switch_Xa_norm, switch_Xa_div,
     *            switch_Xb_base, switch_Xb_norm, switch_Xb_div, rect_X
     */
    public static class PairSwitchEntry extends SwitchEntry {

        public PairSwitchEntry(int id) {
            super(id, SwitchType.PAIR);
        }

        @Override
        public String[] getNormDivPathIds() {
            return new String[]{
                    "switch_" + id + "a_norm",
                    "switch_" + id + "a_div",
                    "switch_" + id + "b_norm",
                    "switch_" + id + "b_div"
            };
        }

        @Override
        public String[] getBasePathIds() {
            return new String[]{
                    "switch_" + id + "a_base",
                    "switch_" + id + "b_base"
            };
        }
    }
}
