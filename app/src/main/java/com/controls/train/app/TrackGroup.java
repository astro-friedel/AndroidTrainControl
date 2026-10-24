package com.controls.train.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackGroup {

    private final int id;           // unique group identifier
    private final String name;         // user-entered name
    private final String groupColor;   // taken from first segment's originalColor at creation
    private final List<Track> members;

    private int speed = 0;
    private boolean directionForward = true;

    public TrackGroup(int id, String name, List<Track> initialMembers) {
        this.id      = id;
        this.name    = name;
        this.members = new ArrayList<>();

        // Group color is determined by the first segment
        this.groupColor = (initialMembers != null && !initialMembers.isEmpty())
                ? initialMembers.get(0).getOriginalColor()
                : "#888888";

        if (initialMembers != null) {
            for (Track t : initialMembers) add(t);
        }
    }

    // Returns true if added, false if the track already belongs to a group
    public boolean add(Track track) {
        if (track.link(this.id)) {
            track.setCurrentColor(this.groupColor);
            members.add(track);
            return true;
        }
        return false;
    }

    // Removes by track ID, unlinks the track, and reverts its color
    public boolean remove(String trackId) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getId().equals(trackId)) {
                members.get(i).unlink(); // unlink restores originalColor internally
                members.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean contains(String trackId) {
        for (Track t : members) {
            if (t.getId().equals(trackId)) return true;
        }
        return false;
    }

    // Getters
    public int getId()         { return id; }
    public String getName()       { return name; }
    public String getGroupColor() { return groupColor; }
    public int getMemberCount()   { return members.size(); }

    public List<Track> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public int getSpeed() { return speed; }

    public void setSpeed(int speed) { this.speed = speed; }

    public boolean isDirectionForward() { return directionForward; }

    public void setDirectionForward(boolean forward) { this.directionForward = forward; }
}