package com.controls.train.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.controls.train.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class TrackTabFragment extends Fragment {

    private static final String ARG_SVG_FILE = "svg_file";
    private static final String ARG_TRACK_LEVEL = "track_level";

    private WebView        mWebView;
    private TrackViewModel mViewModel;
    private String         mSvgFile;
    private boolean        mWebViewReady = false;

    SvgPathClickListener pathClickListener;

    public static TrackTabFragment newInstance(String svgFileName, int lvl) {
        TrackTabFragment fragment = new TrackTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SVG_FILE, svgFileName);
        args.putInt(ARG_TRACK_LEVEL, lvl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                            Bundle saveInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_map, container, false);
        mWebView = view.findViewById(R.id.svgTrackWebView);

        assert getArguments() != null;
        mSvgFile = getArguments().getString(ARG_SVG_FILE);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false); // hides the +/- overlay buttons
        mWebView.getSettings().setUseWideViewPort(true);      // lets content be wider than screen
        mWebView.getSettings().setLoadWithOverviewMode(true); // fits content to screen on load

        // Prevent ViewPager2 from intercepting drags inside the WebView
        mWebView.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false; // false = WebView still handles the event normally
        });

        // Apply all current colors once the page finishes loading
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mWebViewReady = true;
                applyCurrentColors();
            }
        });

        // JS bridge — forwards click to ViewModel
        mWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onPathClicked(String pathId) {
                requireActivity().runOnUiThread(() -> {
                    if (mViewModel != null) mViewModel.onSegmentClicked(pathId);
                });
            }
        }, "Android");

        // Load SVG with click handlers injected
        try {
            String svgContent = loadSvgFromAssets(mSvgFile);
            String html = buildHtml(svgContent);
            mWebView.loadDataWithBaseURL(null, buildHtml(svgContent),
                                         "text/html", "UTF-8", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel is owned by the parent TrackFragment
        mViewModel = new ViewModelProvider(requireParentFragment())
                .get(TrackViewModel.class);

        // When the color map changes, update any paths that live in this SVG
        mViewModel.getColorMap().observe(getViewLifecycleOwner(), colorMap -> {
            if (mWebViewReady) applyColors(colorMap);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mWebView      = null;
        mWebViewReady = false;
    }

    // -------------------------------------------------------------------------
    // Color application
    // -------------------------------------------------------------------------

    private void applyCurrentColors() {
        if (mViewModel == null) return;
        Map<String, String> colorMap = mViewModel.getColorMap().getValue();
        if (colorMap != null) applyColors(colorMap);
    }

    private void applyColors(@NonNull Map<String, String> colorMap) {
        if (mWebView == null || mViewModel == null) return;
        for (Map.Entry<String, String> entry : colorMap.entrySet()) {
            Track track = mViewModel.getTrack(entry.getKey());
            if (track != null && track.appearsInTab(mSvgFile)) {
                setPathColor(entry.getKey(), entry.getValue());
            }
        }
    }

    public void setPathColor(String pathId, String hexColor) {
        if (mWebView == null) return;
        String js = "var el = document.getElementById('" + pathId + "');"
                    + "if (el) el.style.fill = '" + hexColor + "';";
        mWebView.evaluateJavascript(js, null);
    }

    // -------------------------------------------------------------------------
    // HTML construction
    // -------------------------------------------------------------------------

    private String loadSvgFromAssets(String fileName) throws IOException {
        InputStream is = requireContext().getAssets().open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private String buildHtml(String svgContent) {
// Highlight/restore is handled entirely by the ViewModel via setPathColor(),
        // so the JS here only needs to forward the click — no color logic needed.
        return "<!DOCTYPE html><html>"
               + "<head>"
               + "<meta name='viewport' content='width=device-width, initial-scale=1.0, "
               + "user-scalable=yes, minimum-scale=1.0, maximum-scale=5.0'>"
               + "<style>"
               + "* { margin: 0; padding: 0; }"
               + "body { background: #000; overflow: auto; }"
               + "svg { display: block; }"
               + "</style>"
               + "</head>"
               + "<body>"
               + svgContent
               + "<script>"
               + "document.querySelectorAll('path').forEach(function(path) {"
               + "  path.style.cursor = 'pointer';"
               + "  path.addEventListener('click', function() {"
               + "    Android.onPathClicked(this.id);"
               + "  });"
               + "});"
               + "</script>"
               + "</body></html>";
    }

    public interface SvgPathClickListener {
        void onPathClicked(String svgFile, String pathId, String pathName);
    }
}
