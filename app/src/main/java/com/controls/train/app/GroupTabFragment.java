package com.controls.train.app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.controls.train.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class GroupTabFragment extends Fragment {

    private TrackViewModel mViewModel;
    private FrameLayout    mContainer;
    private View           mCurrentStateView;
    private TrackViewModel.GroupTabState mCurrentRenderedState;

    public static GroupTabFragment newInstance() {
        return new GroupTabFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mContainer = new FrameLayout(requireContext());
        mContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return mContainer;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireParentFragment()).get(TrackViewModel.class);

        // Primary driver: full view swap on state change
        mViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            mCurrentRenderedState = state;
            renderState(state);
        });

        // Secondary: in-place refresh when group members change while already in GROUP_EDIT,
        // or when groups are added/removed while SEGMENT_SELECTED
        mViewModel.getGroupList().observe(getViewLifecycleOwner(), groups -> {
            if (mCurrentRenderedState == TrackViewModel.GroupTabState.GROUP_EDIT) {
                refreshSegmentList();
            } else if (mCurrentRenderedState == TrackViewModel.GroupTabState.SEGMENT_SELECTED) {
                renderState(TrackViewModel.GroupTabState.SEGMENT_SELECTED);
            }
        });
    }

    // -------------------------------------------------------------------------
    // State rendering
    // -------------------------------------------------------------------------

    private void renderState(@NonNull TrackViewModel.GroupTabState state) {
        mContainer.removeAllViews();
        mCurrentStateView = null;
        switch (state) {
            case EMPTY:            showEmpty();           break;
            case GROUP_LIST:       showGroupList();       break;
            case SEGMENT_SELECTED: showSegmentSelected(); break;
            case GROUP_EDIT:       showGroupEdit();       break;
        }
    }

    // --- State 1: EMPTY ---
    private void showEmpty() {
        View v = inflate(R.layout.fragment_group_empty);
        mCurrentStateView = v;

        TextInputEditText nameInput = v.findViewById(R.id.group_name_input);
        Button createBtn = v.findViewById(R.id.btn_create_group);

        nameInput.addTextChangedListener(new AfterTextWatcher(s ->
                                                                      createBtn.setEnabled(!s.trim().isEmpty())));

        createBtn.setOnClickListener(btn -> {
            String name = textOf(nameInput);
            if (!name.isEmpty()) mViewModel.createGroup(name);
        });
    }

    // --- State 2: GROUP_LIST ---
    private void showGroupList() {
        View v = inflate(R.layout.fragment_group_list);
        mCurrentStateView = v;
        populateGroupList(v.findViewById(R.id.group_list_container), false);
    }

    // --- State 3: SEGMENT_SELECTED ---
    private void showSegmentSelected() {
        View v = inflate(R.layout.fragment_group_segment_selected);
        mCurrentStateView = v;

        Track selected = mViewModel.getSelectedTrack().getValue();
        TextView segName = v.findViewById(R.id.selected_segment_name);
        if (selected != null) segName.setText(selected.getName());

        TextInputEditText nameInput = v.findViewById(R.id.group_name_input);
        Button createBtn = v.findViewById(R.id.btn_create_group);
        nameInput.addTextChangedListener(new AfterTextWatcher(s ->
                                                                      createBtn.setEnabled(!s.trim().isEmpty())));
        createBtn.setOnClickListener(btn -> {
            String name = textOf(nameInput);
            if (!name.isEmpty()) mViewModel.createGroup(name);
        });

        // Hide the "Add to Existing Group" section when no groups exist yet
        View addToGroupSection = v.findViewById(R.id.add_to_group_section);
        List<TrackGroup> groups = mViewModel.getGroups();
        if (groups.isEmpty()) {
            if (addToGroupSection != null) addToGroupSection.setVisibility(View.GONE);
        } else {
            populateGroupList(v.findViewById(R.id.group_list_container), true);
        }
    }

    // --- State 4: GROUP_EDIT ---
    private void showGroupEdit() {
        View v = inflate(R.layout.fragment_group_edit);
        mCurrentStateView = v;

        TrackGroup editing = mViewModel.getEditingGroup().getValue();
        if (editing == null) return;

        ((TextView) v.findViewById(R.id.edit_group_name)).setText(editing.getName());
        populateSegmentList(v.findViewById(R.id.segment_list_container), editing);

        // Back
        v.findViewById(R.id.btn_back).setOnClickListener(btn ->
                                                                 mViewModel.finishEditing());

        // Add / Done toggle
        Button addBtn = v.findViewById(R.id.btn_add_segment);
        addBtn.setText(mViewModel.isAddMode() ? "Done" : "Add");
        addBtn.setOnClickListener(btn -> {
            boolean entering = !mViewModel.isAddMode();
            mViewModel.setAddMode(entering);
            addBtn.setText(entering ? "Done" : "Add");
        });

        // Delete with confirmation
        TrackGroup finalEditing = editing;
        v.findViewById(R.id.btn_delete_group).setOnClickListener(btn ->
                                                                         new AlertDialog.Builder(requireContext())
                                                                                 .setTitle("Delete Group")
                                                                                 .setMessage("Delete \"" + finalEditing.getName() + "\"?")
                                                                                 .setPositiveButton("Delete", (d, w) ->
                                                                                         mViewModel.deleteGroup(finalEditing.getId()))
                                                                                 .setNegativeButton("Cancel", null)
                                                                                 .show());
    }

    // -------------------------------------------------------------------------
    // List population
    // -------------------------------------------------------------------------

    private void populateGroupList(@Nullable LinearLayout container, boolean showAsAdd) {
        if (container == null) return;
        container.removeAllViews();
        for (TrackGroup group : mViewModel.getGroups()) {
            View row = LayoutInflater.from(requireContext())
                                     .inflate(R.layout.item_group, container, false);

            ((TextView) row.findViewById(R.id.group_item_name)).setText(group.getName());
            row.findViewById(R.id.btn_select).setOnClickListener(v ->
                                                                         mViewModel.selectGroup(group.getId()));

            Button editOrAdd = row.findViewById(R.id.btn_edit_or_add);
            if (showAsAdd) {
                editOrAdd.setText("Add");
                editOrAdd.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#2980B9")));
                editOrAdd.setOnClickListener(v -> {
                    Track selected = mViewModel.getSelectedTrack().getValue();
                    if (selected != null) {
                        group.add(selected);
                        mViewModel.startEditing(group.getId());
                    }
                });
            } else {
                editOrAdd.setText("Edit");
                editOrAdd.setOnClickListener(v -> mViewModel.startEditing(group.getId()));
            }

            container.addView(row);
        }
    }

    private void populateSegmentList(@Nullable LinearLayout container,
                                     @NonNull TrackGroup group) {
        if (container == null) return;
        container.removeAllViews();
        for (Track track : group.getMembers()) {
            View row = LayoutInflater.from(requireContext())
                                     .inflate(R.layout.item_group_segment, container, false);
            ((TextView) row.findViewById(R.id.segment_item_name)).setText(track.getName());
            row.findViewById(R.id.btn_remove_segment).setOnClickListener(v ->
                                                                                 mViewModel.removeFromGroup(group.getId(), track.getId()));
            container.addView(row);
        }
    }

    /** Called when group members change while already rendering GROUP_EDIT. */
    private void refreshSegmentList() {
        if (mCurrentStateView == null) return;
        TrackGroup editing = mViewModel.getEditingGroup().getValue();
        LinearLayout container = mCurrentStateView.findViewById(R.id.segment_list_container);
        if (editing != null && container != null) {
            populateSegmentList(container, editing);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private View inflate(int layoutResId) {
        View v = LayoutInflater.from(requireContext())
                               .inflate(layoutResId, mContainer, false);
        mContainer.addView(v);
        return v;
    }

    private String textOf(@NonNull TextInputEditText field) {
        Editable text = field.getText();
        return text != null ? text.toString().trim() : "";
    }

    /** TextWatcher that only requires afterTextChanged, reducing boilerplate. */
    private static class AfterTextWatcher implements TextWatcher {
        interface Callback { void onChanged(String text); }
        private final Callback mCallback;
        AfterTextWatcher(Callback cb) { this.mCallback = cb; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { mCallback.onChanged(s.toString()); }
    }
}