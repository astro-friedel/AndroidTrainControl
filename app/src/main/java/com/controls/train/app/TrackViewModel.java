package com.controls.train.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrackViewModel extends ViewModel {

    // -------------------------------------------------------------------------
    // State enum
    // -------------------------------------------------------------------------
    public enum GroupTabState {
        EMPTY,            // no groups, no segment selected
        GROUP_LIST,       // groups exist, nothing selected or being edited
        SEGMENT_SELECTED, // a standalone segment is selected (groups may exist)
        GROUP_EDIT        // a group is being edited
    }

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------
    private final Map<String, Track>       mAllTracks = new LinkedHashMap<>();
    private final Map<Integer, TrackGroup> mGroups    = new LinkedHashMap<>();
    private int     mNextGroupId = 1;
    private boolean mIsAddMode   = false;

    // -------------------------------------------------------------------------
    // LiveData
    // -------------------------------------------------------------------------
    private final MutableLiveData<GroupTabState>       mState         = new MutableLiveData<>(GroupTabState.EMPTY);
    private final MutableLiveData<List<TrackGroup>>    mGroupList     = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<TrackGroup>          mActiveGroup   = new MutableLiveData<>(null);
    private final MutableLiveData<TrackGroup>          mEditingGroup  = new MutableLiveData<>(null);
    private final MutableLiveData<Track>               mSelectedTrack = new MutableLiveData<>(null);
    private final MutableLiveData<Map<String, String>> mColorMap      = new MutableLiveData<>(new HashMap<>());

    private TrackGroupChangeListener mGroupChangeListener;

    public LiveData<GroupTabState>       getState()        { return mState; }
    public LiveData<List<TrackGroup>>    getGroupList()    { return mGroupList; }
    public LiveData<TrackGroup>          getActiveGroup()  { return mActiveGroup; }
    public LiveData<TrackGroup>          getEditingGroup() { return mEditingGroup; }
    public LiveData<Track>               getSelectedTrack(){ return mSelectedTrack; }
    public LiveData<Map<String, String>> getColorMap()     { return mColorMap; }
    public boolean                       isAddMode()       { return mIsAddMode; }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------
    public void initializeTracks(@NonNull Context context, @NonNull List<String> svgFiles) {
        if (!mAllTracks.isEmpty()) return; // ViewModel survives rotation; only parse once
        for (String svgFile : svgFiles) {
            List<SvgParser.TrackPath> paths = SvgParser.parse(context, svgFile);
            for (SvgParser.TrackPath path : paths) {
                if (mAllTracks.containsKey(path.id)) {
                    mAllTracks.get(path.id).addSvgFile(svgFile); // spanning segment
                } else {
                    List<String> files = new ArrayList<>();
                    files.add(svgFile);
                    mAllTracks.put(path.id,
                                   new Track(path.id, path.name, path.fillColor, files));
                }
            }
        }
        publishColors();
    }

    // -------------------------------------------------------------------------
    // Segment click — entry point from TrackTabFragment
    // -------------------------------------------------------------------------
    public void onSegmentClicked(@NonNull String pathId) {
        Track track = mAllTracks.get(pathId);
        if (track == null) return;

        if (mIsAddMode) {
            handleAddModeClick(track);
            return;
        }

        if (track.isInGroup()) {
            handleGroupMemberClick(track);
        } else {
            handleStandaloneClick(track);
        }
    }

    private void handleAddModeClick(@NonNull Track track) {
        if (track.isInGroup()) return; // grayed out — not selectable
        TrackGroup editing = mEditingGroup.getValue();
        if (editing == null) return;
        editing.add(track);
        notifyGroupChanged(editing);
        updateState();
        publishColors();
        publishGroupList();
    }

    private void handleGroupMemberClick(@NonNull Track track) {
        TrackGroup group = mGroups.get(track.getGroupId());
        if (group == null || group.getMembers().isEmpty()) return;

        boolean isHighlighted = "#FFFFFF".equals(
                group.getMembers().get(0).getCurrentColor());

        if (isHighlighted) {
            for (Track t : group.getMembers()) t.restoreFromHighlight();
        } else {
            clearSelectedTrack();
            for (Track t : group.getMembers()) t.setCurrentColor("#FFFFFF");
            mActiveGroup.setValue(group);
            mEditingGroup.setValue(group);
        }
        updateState();
        publishColors();
    }

    private void handleStandaloneClick(@NonNull Track track) {
        Track current = mSelectedTrack.getValue();
        if (track.equals(current)) {
            track.restoreFromHighlight();
            mSelectedTrack.setValue(null);
        } else {
            if (current != null) current.restoreFromHighlight();
            track.setCurrentColor("#FFFFFF");
            mSelectedTrack.setValue(track);
        }
        updateState();
        publishColors();
    }

    private void clearSelectedTrack() {
        Track current = mSelectedTrack.getValue();
        if (current != null) {
            current.restoreFromHighlight();
            mSelectedTrack.setValue(null);
        }
    }

    // -------------------------------------------------------------------------
    // Group creation
    // -------------------------------------------------------------------------
    public void createGroup(@NonNull String name) {
        List<Track> initial = new ArrayList<>();
        Track selected = mSelectedTrack.getValue();
        if (selected != null) {
            initial.add(selected);
            mSelectedTrack.setValue(null);
        }
        int id = mNextGroupId++;
        TrackGroup group = new TrackGroup(id, name, initial);
        mGroups.put(id, group);
        notifyGroupChanged(group);
        mActiveGroup.setValue(group);
        mEditingGroup.setValue(group);
        // updateState before publishGroupList to avoid a redundant GROUP_EDIT re-render
        updateState();
        publishColors();
        publishGroupList();
    }

    // -------------------------------------------------------------------------
    // Group selection (Drive tab activation)
    // -------------------------------------------------------------------------
    public void selectGroup(int groupId) {
        TrackGroup group = mGroups.get(groupId);
        if (group != null) mActiveGroup.setValue(group);
    }

    // -------------------------------------------------------------------------
    // Editing
    // -------------------------------------------------------------------------
    public void startEditing(int groupId) {
        TrackGroup group = mGroups.get(groupId);
        if (group == null) return;
        mEditingGroup.setValue(group);
        mActiveGroup.setValue(group);
        updateState();
    }

    public void finishEditing() {
        setAddMode(false);
        mEditingGroup.setValue(null);
        updateState();
    }

    public void setAddMode(boolean addMode) {
        mIsAddMode = addMode;
        TrackGroup editing = mEditingGroup.getValue();
        for (Track t : mAllTracks.values()) {
            if (addMode) {
                // Gray out segments that belong to other groups
                if (t.isInGroup() &&
                    (editing == null || t.getGroupId() != editing.getId())) {
                    t.setCurrentColor("#555555");
                }
            } else {
                // Restore all grayed segments to their group color
                if (t.isInGroup()) t.restoreFromHighlight();
            }
        }
        publishColors();
    }

    public void removeFromGroup(int groupId, @NonNull String trackId) {
        TrackGroup group = mGroups.get(groupId);
        if (group != null) {
            group.remove(trackId);
            notifyGroupChanged(group);
            publishColors();
            publishGroupList();
        }
    }

    public void deleteGroup(int groupId) {
        TrackGroup group = mGroups.get(groupId);
        if (group == null) return;
        for (Track t : new ArrayList<>(group.getMembers())) {
            group.remove(t.getId()); // unlinks each member and restores its color
        }
        mGroups.remove(groupId);
        notifyGroupDeleted(groupId);
        if (group.equals(mActiveGroup.getValue()))  mActiveGroup.setValue(null);
        if (group.equals(mEditingGroup.getValue())) mEditingGroup.setValue(null);
        setAddMode(false);
        updateState();
        publishColors();
        publishGroupList();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------
    private void updateState() {
        if (mEditingGroup.getValue() != null) {
            mState.setValue(GroupTabState.GROUP_EDIT);
        } else if (mSelectedTrack.getValue() != null) {
            mState.setValue(GroupTabState.SEGMENT_SELECTED);
        } else if (!mGroups.isEmpty()) {
            mState.setValue(GroupTabState.GROUP_LIST);
        } else {
            mState.setValue(GroupTabState.EMPTY);
        }
    }

    private void publishColors() {
        Map<String, String> map = new HashMap<>();
        for (Track t : mAllTracks.values()) {
            map.put(t.getId(), t.getCurrentColor());
        }
        mColorMap.setValue(map);
    }

    private void publishGroupList() {
        mGroupList.setValue(new ArrayList<>(mGroups.values()));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------
    @Nullable
    public Track getTrack(@NonNull String id) {
        return mAllTracks.get(id);
    }

    public List<TrackGroup> getGroups() {
        return new ArrayList<>(mGroups.values());
    }

    public void setGroupChangeListener(TrackGroupChangeListener listener) {
        mGroupChangeListener = listener;
    }

    private static int segmentNumber(String trackId) {
        // trackId is "track_XX" — parse the XX after the underscore
        try {
            return Integer.parseInt(trackId.substring(trackId.lastIndexOf('_') + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void notifyGroupChanged(TrackGroup group) {
        if (mGroupChangeListener == null) return;
        List<Integer> members = new ArrayList<>();
        for (Track t : group.getMembers()) {
            int num = segmentNumber(t.getId());
            if (num >= 0) members.add(num);
        }
        mGroupChangeListener.onGroupChanged(group.getId(), members);
    }

    private void notifyGroupDeleted(int groupId) {
        if (mGroupChangeListener != null) {
            mGroupChangeListener.onGroupDeleted(groupId);
        }
    }
}