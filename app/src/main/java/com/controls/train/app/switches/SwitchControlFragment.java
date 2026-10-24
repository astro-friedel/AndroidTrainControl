package com.controls.train.app.switches;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.controls.train.R;
import com.controls.train.app.switches.view.SwitchPairView;
import com.controls.train.app.switches.view.SwitchSingleView;
import com.controls.train.app.switches.view.SwitchYView;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * SwitchControlFragment
 *
 * The right-panel control for a single selected switch entity.
 * Inflates fragment_switch_control.xml and:
 *   1. Sets the switch name label.
 *   2. Programmatically adds the correct diagram View into switch_diagram_container.
 *   3. Wires the toggle to fire OnSwitchThrowListener (BLE) and update the diagram.
 *
 * Created via newInstance(SwitchEntry) — the SwitchEntry supplies type, id,
 * and current thrown state so the UI is immediately consistent on appearance.
 *
 * The parent (SwitchesLevelFragment) holds the listener and calls setThrown()
 * if a BLE acknowledgement needs to correct the displayed state after the fact.
 */
public class SwitchControlFragment extends Fragment {

    // ── Listener ──────────────────────────────────────────────────────────────

    public interface OnSwitchThrowListener {
        void onSwitchThrow(int switchId, boolean thrown);
    }

    private OnSwitchThrowListener throwListener;

    public void setOnSwitchThrowListener(OnSwitchThrowListener listener) {
        this.throwListener = listener;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    private static final String ARG_SWITCH_ID    = "switch_id";
    private static final String ARG_SWITCH_TYPE  = "switch_type";
    private static final String ARG_DIVERGE_RIGHT = "diverge_right";
    private static final String ARG_THROWN        = "thrown";

    public static SwitchControlFragment newInstance(SwitchEntry entry) {
        SwitchControlFragment f = new SwitchControlFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SWITCH_ID,   entry.getId());
        args.putString(ARG_SWITCH_TYPE, entry.getType().name());
        args.putBoolean(ARG_DIVERGE_RIGHT,
                        entry.getType() == SwitchEntry.SwitchType.RIGHT);
        args.putBoolean(ARG_THROWN, entry.isThrown());
        f.setArguments(args);
        return f;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private int switchId;
    private SwitchEntry.SwitchType switchType;
    private boolean divergeRight;
    private boolean thrown;

    // Diagram view — one of the three custom View types.
    // Held as View so we can call setThrown via the interface below.
    private ThrowableView diagramView;

    private SwitchMaterial toggle;

    /** Common interface so we can call setThrown() without knowing the subtype. */
    private interface ThrowableView {
        void setThrown(boolean thrown);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_switch_control,
                                     container, false);

        // Read args
        Bundle args = getArguments();
        if (args != null) {
            switchId    = args.getInt(ARG_SWITCH_ID, 0);
            switchType  = SwitchEntry.SwitchType.valueOf(
                    args.getString(ARG_SWITCH_TYPE,
                                   SwitchEntry.SwitchType.LEFT.name()));
            divergeRight = args.getBoolean(ARG_DIVERGE_RIGHT, false);
            thrown       = args.getBoolean(ARG_THROWN, false);
        }

        // ① Name label
        TextView nameView = root.findViewById(R.id.tv_switch_name);
        nameView.setText(buildSwitchName());

        // ② Diagram view — instantiated based on type, added to container
        FrameLayout diagramContainer =
                root.findViewById(R.id.switch_diagram_container);
        addDiagramView(diagramContainer);

        // ③ Toggle — set initial state silently, then wire listener
        toggle = root.findViewById(R.id.switch_toggle);

        // setChecked without triggering the listener
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(!thrown);   // checked = straight = not thrown

        toggle.setOnCheckedChangeListener((btn, isChecked) -> {
            boolean nowThrown = !isChecked;

            // Update diagram immediately (fire-and-forget)
            diagramView.setThrown(nowThrown);
            thrown = nowThrown;

            // Notify parent → BLE
            if (throwListener != null) {
                throwListener.onSwitchThrow(switchId, nowThrown);
            }
        });

        return root;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Called by the parent if a BLE acknowledgement corrects the thrown state.
     * Not used in fire-and-forget mode.
     */
    public void setThrown(boolean thrown) {
        if (this.thrown == thrown) return;
        this.thrown = thrown;

        if (diagramView != null) diagramView.setThrown(thrown);

        if (toggle != null) {
            // Update toggle without re-firing the listener
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(!thrown);
            toggle.setOnCheckedChangeListener((btn, isChecked) -> {
                boolean nowThrown = !isChecked;
                diagramView.setThrown(nowThrown);
                this.thrown = nowThrown;
                if (throwListener != null) {
                    throwListener.onSwitchThrow(switchId, nowThrown);
                }
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addDiagramView(FrameLayout container) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);

        switch (switchType) {
            case PAIR: {
                SwitchPairView v = new SwitchPairView(requireContext());
                v.setThrown(thrown);
                container.addView(v, params);
                diagramView = v::setThrown;
                break;
            }
            case Y: {
                SwitchYView v = new SwitchYView(requireContext());
                v.setThrown(thrown);
                container.addView(v, params);
                diagramView = v::setThrown;
                break;
            }
            default: {
                // LEFT or RIGHT
                SwitchSingleView v = new SwitchSingleView(requireContext());
                v.setDivergeRight(divergeRight);
                v.setThrown(thrown);
                container.addView(v, params);
                diagramView = v::setThrown;
                break;
            }
        }
    }

    private String buildSwitchName() {
        switch (switchType) {
            case PAIR: return "Switch " + switchId + " (Crossover)";
            case Y:    return "Switch " + switchId + " (Y)";
            case RIGHT: return "Switch " + switchId + " (Right)";
            default:   return "Switch " + switchId + " (Left)";
        }
    }
}
