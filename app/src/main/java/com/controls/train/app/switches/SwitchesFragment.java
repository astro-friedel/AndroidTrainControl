package com.controls.train.app.switches;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.controls.train.R;
import com.controls.train.app.switches.SwitchEntry;
import com.controls.train.app.switches.SwitchEntry.SingleSwitchEntry;
import com.controls.train.app.switches.SwitchEntry.PairSwitchEntry;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

/**
 * SwitchesFragment
 *
 * Top-level fragment for the Switches screen.
 * Owns the TabLayout + ViewPager2 and creates one SwitchesLevelFragment
 * per track level.
 *
 * ── Hard-coded configuration ──────────────────────────────────────────────────
 * Switch lists and SVG asset paths are defined in buildLevelFragments().
 * Edit that method to add/remove levels or switches.
 *
 * ── BLE dispatch ─────────────────────────────────────────────────────────────
 * Implements SwitchesLevelFragment.OnSwitchThrowListener.
 * Each level fragment is given this fragment as its listener so BLE commands
 * funnel through one place. The actual BLE write uses the same pattern as the
 * rest of the app — obtain the characteristic from the parent and write to it.
 *
 * BLE message format (fire-and-forget, matches Arduino expectations):
 *   !SW{id}{state}
 *   where id    = switch number (integer)
 *         state = 0 (straight/normal) or 1 (diverging/thrown)
 *
 * Adjust the format string in sendSwitchCommand() to match your Arduino sketch.
 */
public class SwitchesFragment extends Fragment
        implements SwitchesLevelFragment.OnSwitchThrowListener {

    // ── Tab labels ────────────────────────────────────────────────────────────

    private static final String[] TAB_LABELS = {
            "Lower Level",
            "Middle Level",
            "Upper Level"
    };

    // ── Fields ────────────────────────────────────────────────────────────────

    private SwitchLevelPagerAdapter pagerAdapter;
    private List<SwitchesLevelFragment> levelFragments;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_switches, container, false);

        levelFragments = buildLevelFragments();

        pagerAdapter = new SwitchLevelPagerAdapter(
                getChildFragmentManager(),
                getLifecycle(),
                levelFragments);

        ViewPager2 viewPager = root.findViewById(R.id.view_pager_switches);
        viewPager.setAdapter(pagerAdapter);

        // Disable swipe — tab taps only, so a WebView drag doesn't
        // accidentally flip pages
        viewPager.setUserInputEnabled(false);

        TabLayout tabLayout = root.findViewById(R.id.tab_layout_switches);
        new TabLayoutMediator(tabLayout, viewPager,
                              (tab, position) -> tab.setText(TAB_LABELS[position])
        ).attach();

        return root;
    }

    // ── Level / switch configuration ──────────────────────────────────────────

    /**
     * Hard-coded switch lists and SVG asset paths for each level.
     *
     * SVG files live in assets/svg/ and contain both the track background
     * and the switch interaction paths in a single file.
     *
     * Switch types:
     *   SingleSwitchEntry(id, SwitchType.LEFT)   — diverges upper-left
     *   SingleSwitchEntry(id, SwitchType.RIGHT)  — diverges upper-right
     *   SingleSwitchEntry(id, SwitchType.Y)      — Y-switch
     *   PairSwitchEntry(id)                      — crossover pair
     */
    private List<SwitchesLevelFragment> buildLevelFragments() {

        // ── Level 1 ───────────────────────────────────────────────────────────
        List<SwitchEntry> level1Switches = Arrays.asList(
                new SingleSwitchEntry(1, SwitchEntry.SwitchType.LEFT),
                new SingleSwitchEntry(2, SwitchEntry.SwitchType.RIGHT),
                new PairSwitchEntry(3),
                new SingleSwitchEntry(4, SwitchEntry.SwitchType.Y)
                                                        );
        SwitchesLevelFragment level1 = SwitchesLevelFragment.newInstance(
                "svg/level1_switches.svg", level1Switches);
        level1.setOnSwitchThrowListener(this);

        // ── Level 2 ───────────────────────────────────────────────────────────
        List<SwitchEntry> level2Switches = Arrays.asList(
                new SingleSwitchEntry(5, SwitchEntry.SwitchType.LEFT),
                new SingleSwitchEntry(6, SwitchEntry.SwitchType.RIGHT)
                                                        );
        SwitchesLevelFragment level2 = SwitchesLevelFragment.newInstance(
                "svg/level2_switches.svg", level2Switches);
        level2.setOnSwitchThrowListener(this);

        // ── Level 3 ───────────────────────────────────────────────────────────
        List<SwitchEntry> level3Switches = Arrays.asList(
                new PairSwitchEntry(7),
                new SingleSwitchEntry(8, SwitchEntry.SwitchType.Y)
                                                        );
        SwitchesLevelFragment level3 = SwitchesLevelFragment.newInstance(
                "svg/level3_switches.svg", level3Switches);
        level3.setOnSwitchThrowListener(this);

        return Arrays.asList(level1, level2, level3);
    }

    // ── BLE dispatch ──────────────────────────────────────────────────────────

    /**
     * Receives throw events from any level fragment and sends the BLE command.
     * Called on the UI thread.
     */
    @Override
    public void onSwitchThrow(int switchId, boolean thrown) {
        sendSwitchCommand(switchId, thrown);
    }

    /**
     * Formats and sends the BLE UART command for a switch throw.
     * Adjust the message format to match your Arduino sketch.
     *
     * Current format:  !SW{id}{state}\n
     *   e.g. switch 3 thrown:    "!SW31\n"
     *        switch 3 straight:  "!SW30\n"
     */
    private void sendSwitchCommand(int switchId, boolean thrown) {
        String message = "!SW" + switchId + (thrown ? "1" : "0") + "\n";

        // TODO: obtain the BLE UART characteristic from your connection layer
        // and write the message bytes to it, following the same pattern used
        // in ControllerFragment / TrackFragment for group and speed commands.
        //
        // Example (adjust to match your BLE helper class):
        //   BluetoothGattCharacteristic tx = bleManager.getTxCharacteristic();
        //   if (tx != null) {
        //       tx.setValue(message.getBytes(StandardCharsets.UTF_8));
        //       bleManager.writeCharacteristic(tx);
        //   }
    }
}