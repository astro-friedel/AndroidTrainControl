package com.controls.train.app.switches;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.controls.train.R;
import com.controls.train.app.switches.SwitchEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SwitchesLevelFragment
 *
 * One tab in the Switches screen. Hosts:
 *   - A WebView (left 2/3) displaying the combined SVG (track + switch paths).
 *   - A FrameLayout (right 1/3) that shows a SwitchControlFragment when a
 *     switch rect is selected, and is empty otherwise.
 *
 * Loading strategy:
 *   The SVG is read from assets and wrapped in a minimal HTML page that
 *   includes switches.js inline. This is loaded via loadDataWithBaseURL()
 *   so that file:///android_asset/ URLs remain resolvable if needed, and
 *   gives us a proper HTML document ready for JavaScript without needing
 *   a separate script-injection step after page load.
 *
 * The switch list is hard-coded per level via newInstance().
 * BLE communication is delegated upward via OnSwitchThrowListener.
 */
public class SwitchesLevelFragment extends Fragment {

    // ── Arguments ─────────────────────────────────────────────────────────────

    private static final String ARG_SVG_ASSET = "svg_asset";

    /**
     * @param svgAsset  Path inside assets/, e.g. "svg/level1_switches.svg"
     * @param switches  Hard-coded switch list for this level.
     */
    public static SwitchesLevelFragment newInstance(String svgAsset,
                                                    List<SwitchEntry> switches) {
        SwitchesLevelFragment f = new SwitchesLevelFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SVG_ASSET, svgAsset);
        f.setArguments(args);
        f.switchList = switches;
        return f;
    }

    // ── Listener interface ────────────────────────────────────────────────────

    public interface OnSwitchThrowListener {
        /**
         * Called when the user toggles a switch.
         * Fire-and-forget: no acknowledgement expected from BLE.
         *
         * @param switchId  The switch id (X from switch_X_base etc.)
         * @param thrown    true = diverging, false = straight.
         */
        void onSwitchThrow(int switchId, boolean thrown);
    }

    private OnSwitchThrowListener throwListener;

    public void setOnSwitchThrowListener(OnSwitchThrowListener listener) {
        this.throwListener = listener;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private List<SwitchEntry> switchList;
    private WebView webView;
    private FrameLayout controlPanel;
    private int selectedSwitchId = -1;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_switches_level,
                                     container, false);

        controlPanel = root.findViewById(R.id.switch_control_panel);
        webView      = root.findViewById(R.id.webview_switch_map);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        // Register the bridge before any content loads
        webView.addJavascriptInterface(new SwitchJsBridge(), "SwitchBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Page already contains switches.js inline; just call initSwitches()
                view.evaluateJavascript(
                        "initSwitches('" + buildSwitchConfigJson() + "')", null);
            }
        });

        loadSvgPage();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webView = null;
        controlPanel = null;
    }

    // ── SVG loading ───────────────────────────────────────────────────────────

    /**
     * Reads the SVG and switches.js from assets, wraps them in a minimal HTML
     * page, and loads that page via loadDataWithBaseURL so the WebView has a
     * proper document structure from the start.
     *
     * The base URL is set to file:///android_asset/ so any relative asset
     * references inside the SVG continue to resolve correctly.
     */
    private void loadSvgPage() {
        String svgAsset = getArguments() != null
                ? getArguments().getString(ARG_SVG_ASSET, "")
                : "";

        String svgContent = readAsset(svgAsset);
        String jsContent  = readAsset("js/switches.js");

        if (svgContent == null || jsContent == null) {
            // Asset missing — show a plain error message in the WebView
            webView.loadData(
                    "<html><body><p style='color:red'>SVG asset not found: "
                    + svgAsset + "</p></body></html>",
                    "text/html", "UTF-8");
            return;
        }

        String html = buildHtmlPage(svgContent, jsContent);

        webView.loadDataWithBaseURL(
                "file:///android_asset/",   // base URL
                html,                        // content
                "text/html",                 // MIME type
                "UTF-8",                     // encoding
                null);                       // history URL
    }

    /**
     * Wraps the SVG and JS into a minimal full-screen HTML page.
     * The SVG fills the viewport; the script tag runs after the DOM is ready.
     */
    private String buildHtmlPage(String svgContent, String jsContent) {
        return "<!DOCTYPE html>\n"
               + "<html>\n"
               + "<head>\n"
               + "<meta charset=\"UTF-8\">\n"
               + "<style>\n"
               + "  html, body { margin:0; padding:0; width:100%; height:100%;"
               + "               overflow:hidden; background:#000; }\n"
               + "  svg { width:100%; height:100%; display:block; }\n"
               + "</style>\n"
               + "</head>\n"
               + "<body>\n"
               + svgContent + "\n"
               + "<script>\n"
               + jsContent + "\n"
               + "</script>\n"
               + "</body>\n"
               + "</html>";
    }

    // ── JS config ─────────────────────────────────────────────────────────────

    /**
     * Builds the JSON config string passed to initSwitches() in switches.js.
     * Single-quote-escaped so it is safe to embed in a JS string literal.
     */
    private String buildSwitchConfigJson() {
        JSONArray array = new JSONArray();
        for (SwitchEntry sw : switchList) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id",   sw.getId());
                obj.put("type", sw.getType().name());

                JSONArray baseIds = new JSONArray();
                for (String s : sw.getBasePathIds()) baseIds.put(s);
                obj.put("basePathIds", baseIds);

                JSONArray normDivIds = new JSONArray();
                for (String s : sw.getNormDivPathIds()) normDivIds.put(s);
                obj.put("normDivIds", normDivIds);

                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array.toString().replace("'", "\\'");
    }

    // ── JS → Java bridge ─────────────────────────────────────────────────────

    private class SwitchJsBridge {
        /**
         * Called by JavaScript when a rect_ path is tapped.
         * Always arrives on a background thread.
         */
        @JavascriptInterface
        public void onRectClicked(int id) {
            requireActivity().runOnUiThread(() -> handleRectTap(id));
        }
    }

    // ── Selection logic ───────────────────────────────────────────────────────

    private void handleRectTap(int id) {
        if (selectedSwitchId == id) {
            collapsePanel();
        } else {
            selectSwitch(id);
        }
    }

    private void selectSwitch(int id) {
        SwitchEntry entry = findEntry(id);
        if (entry == null) return;

        // Deselect previous rect highlight if any
        if (selectedSwitchId != -1) {
            jsDeselectSwitch(selectedSwitchId);
        }

        selectedSwitchId = id;
        jsSelectSwitch(id);

        // Build control fragment and wire its listener
        SwitchControlFragment controlFragment =
                SwitchControlFragment.newInstance(entry);

        controlFragment.setOnSwitchThrowListener((switchId, thrown) -> {
            entry.setThrown(thrown);
            jsSwitchThrown(switchId, thrown);
            if (throwListener != null) {
                throwListener.onSwitchThrow(switchId, thrown);
            }
        });

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.switch_control_panel, controlFragment)
                .commit();
    }

    private void collapsePanel() {
        if (selectedSwitchId != -1) {
            jsDeselectSwitch(selectedSwitchId);
            selectedSwitchId = -1;
        }
        Fragment current = getChildFragmentManager()
                .findFragmentById(R.id.switch_control_panel);
        if (current != null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .remove(current)
                    .commit();
        }
    }

    // ── JS call helpers ───────────────────────────────────────────────────────

    private void jsSelectSwitch(int id) {
        if (webView != null)
            webView.evaluateJavascript("selectSwitch(" + id + ")", null);
    }

    private void jsDeselectSwitch(int id) {
        if (webView != null)
            webView.evaluateJavascript("deselectSwitch(" + id + ")", null);
    }

    private void jsSwitchThrown(int id, boolean thrown) {
        if (webView != null)
            webView.evaluateJavascript(
                    "setSwitchThrown(" + id + "," + thrown + ")", null);
    }

    // ── Public API (optional BLE ack path) ────────────────────────────────────

    /**
     * Call this if BLE sends an acknowledgement that corrects state.
     * Not used in fire-and-forget mode.
     */
    public void onSwitchThrowConfirmed(int switchId, boolean thrown) {
        SwitchEntry entry = findEntry(switchId);
        if (entry != null) entry.setThrown(thrown);
        jsSwitchThrown(switchId, thrown);
        if (switchId == selectedSwitchId) {
            SwitchControlFragment cf = (SwitchControlFragment)
                    getChildFragmentManager()
                            .findFragmentById(R.id.switch_control_panel);
            if (cf != null) cf.setThrown(thrown);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private SwitchEntry findEntry(int id) {
        for (SwitchEntry sw : switchList) {
            if (sw.getId() == id) return sw;
        }
        return null;
    }

    /** Reads a UTF-8 text file from assets. Returns null on error. */
    @Nullable
    private String readAsset(String path) {
        try (InputStream is = requireContext().getAssets().open(path)) {
            byte[] buffer = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}