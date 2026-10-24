package com.controls.train.app;

import java.util.List;

public interface TrackGroupChangeListener {
    /**
     * Called when a group is created, or a segment is added or removed.
     * @param groupId  the BLE group id
     * @param members  current list of segment numbers (the XX from track_XX)
     */
    void onGroupChanged(int groupId, List<Integer> members);

    /**
     * Called when a group is deleted.
     * @param groupId  the BLE group id
     */
    void onGroupDeleted(int groupId);
}