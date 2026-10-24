/**
 * switches.js
 *
 * Loaded into the WebView alongside the SVG track map.
 * Handles:
 *   1. Reading base colours from SVG paths at init time.
 *   2. Applying initial norm/div colours for all switches.
 *   3. Forwarding rect taps to Java via SwitchBridge.onRectClicked(id).
 *   4. Updating norm/div path colours when Java calls setSwitchThrown().
 *   5. Highlighting/clearing the selection rect border.
 *
 * Contract with Java (SwitchBridge @JavascriptInterface):
 *   Java calls  → JS:   initSwitches(jsonConfig)
 *                       setSwitchThrown(id, thrown)
 *                       selectSwitch(id)
 *                       deselectSwitch(id)
 *   JS calls    → Java: SwitchBridge.onRectClicked(id)
 *
 * jsonConfig format (produced by SwitchesLevelFragment):
 * [
 *   { "id": 1, "type": "LEFT",  "basePathIds": ["switch_1_base"],
 *                                "normDivIds":  ["switch_1_norm","switch_1_div"] },
 *   { "id": 3, "type": "PAIR",  "basePathIds": ["switch_3a_base","switch_3b_base"],
 *                                "normDivIds":  ["switch_3a_norm","switch_3a_div",
 *                                               "switch_3b_norm","switch_3b_div"] },
 *   ...
 * ]
 */

// ── Module-level state ────────────────────────────────────────────────────────

/**
 * Map of switch id (int) → { baseColor, normDivIds, thrown }
 * Populated by initSwitches().
 */
var switchState = {};

/** Currently selected switch id, or -1 if none. */
var selectedId = -1;

const WHITE        = "#FFFFFF";
const SEL_COLOR    = "#4CAF50";   // green rect outline when selected
const RECT_STROKE  = "3";         // px, selection rect stroke width

// ── Called by Java after WebView finishes loading ─────────────────────────────

/**
 * Initialise switch state from the Java-supplied config JSON string.
 * Reads base colours from the SVG, sets initial path colours, wires rect taps.
 *
 * @param {string} jsonConfig  JSON array of switch descriptors (see above).
 */
function initSwitches(jsonConfig) {
    var config = JSON.parse(jsonConfig);

    config.forEach(function(sw) {
        // Read base colour from the first base path
        var basePath = document.getElementById(sw.basePathIds[0]);
        var baseColor = basePath ? getPathColor(basePath) : "#888888";

        // Store state
        switchState[sw.id] = {
            baseColor:   baseColor,
            normDivIds:  sw.normDivIds,
            thrown:      false
        };

        // Apply initial colours (not thrown: norm=base, div=white)
        applyColors(sw.id, false);

        // Wire the rect tap
        var rect = document.getElementById("rect_" + sw.id);
        if (rect) {
            rect.style.cursor = "pointer";
            rect.addEventListener("click", (function(id) {
                return function(e) {
                    e.stopPropagation();
                    SwitchBridge.onRectClicked(id);
                };
            })(sw.id));
        }
    });
}

// ── Called by Java to update visual state ─────────────────────────────────────

/**
 * Update norm/div path colours for the given switch.
 * Called by Java after BLE fire-and-forget so the SVG reflects the new state.
 *
 * @param {number}  id      Switch id.
 * @param {boolean} thrown  true = diverging active, false = straight active.
 */
function setSwitchThrown(id, thrown) {
    var sw = switchState[id];
    if (!sw) return;
    sw.thrown = thrown;
    applyColors(id, thrown);
}

/**
 * Highlight the selection rect for the given switch.
 * Called by Java after it has confirmed the tap and swapped in the control panel.
 *
 * @param {number} id  Switch id.
 */
function selectSwitch(id) {
    // Clear previous selection
    if (selectedId !== -1 && selectedId !== id) {
        clearRectHighlight(selectedId);
    }
    selectedId = id;
    var rect = document.getElementById("rect_" + id);
    if (rect) {
        rect.style.stroke        = SEL_COLOR;
        rect.style.strokeWidth   = RECT_STROKE;
        rect.style.strokeOpacity = "1";
    }
}

/**
 * Remove selection highlight from the given switch rect.
 * Called by Java when the panel is collapsed.
 *
 * @param {number} id  Switch id.
 */
function deselectSwitch(id) {
    clearRectHighlight(id);
    selectedId = -1;
}

// ── Internal helpers ──────────────────────────────────────────────────────────

/**
 * Apply norm/div fill colours based on thrown state.
 * normDivIds is a flat array in pairs: [norm0, div0, norm1, div1, ...]
 */
function applyColors(id, thrown) {
    var sw = switchState[id];
    if (!sw) return;

    var ids = sw.normDivIds;
    for (var i = 0; i < ids.length; i += 2) {
        var normPath = document.getElementById(ids[i]);
        var divPath  = document.getElementById(ids[i + 1]);
        if (normPath) setPathColor(normPath, thrown ? WHITE           : sw.baseColor);
        if (divPath)  setPathColor(divPath,  thrown ? sw.baseColor   : WHITE);
    }
}

function clearRectHighlight(id) {
    var rect = document.getElementById("rect_" + id);
    if (rect) {
        rect.style.stroke        = "";
        rect.style.strokeWidth   = "";
        rect.style.strokeOpacity = "";
    }
}

/**
 * Read the effective fill colour of an SVG path element.
 * Checks inline style first, then the fill attribute.
 */
function getPathColor(el) {
    if (el.style && el.style.fill && el.style.fill !== "") {
        return el.style.fill;
    }
    var attr = el.getAttribute("fill");
    return attr ? attr : "#888888";
}

/** Set the fill colour of an SVG path element. */
function setPathColor(el, color) {
    el.style.fill = color;
}
