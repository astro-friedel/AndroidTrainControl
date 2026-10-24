package com.controls.train.app;

import java.util.ArrayList;
import java.util.List;

public class Track {

    private final String id;           // SVG path ID — unique across all tabs
    private final String name;         // display name
    private final String originalColor; // color from SVG at parse time
    private String currentColor;       // current display color (may differ when highlighted)
    private String groupColor;           // color set by the owning group; null if unassigned
    private final List<String> svgFiles; // tabs this segment appears in (1 for most, 2 for spanning)
    private int groupId = 0;            // null if unassigned

    public Track(String id, String name, String originalColor, List<String> svgFiles) {
        this.id            = id;
        this.name          = name;
        this.originalColor = originalColor;
        this.currentColor  = originalColor; // starts at its natural color
        this.svgFiles      = new ArrayList<>(svgFiles);
    }

    // Returns true if successfully linked, false if already in a group
    public boolean link(int groupId) {
        if (this.groupId == 0) {
            this.groupId = groupId;
            return true;
        }
        return false;
    }

    // Reverts color and clears group membership
    public void unlink() {
        this.groupId      = 0;
        this.groupColor   = null;
        this.currentColor = originalColor;
    }

    /** Restore from a temporary highlight without leaving the group. */
    public void restoreFromHighlight() {
        this.currentColor = (groupColor != null) ? groupColor : originalColor;
    }

    /** Called by TrackGroup.add() immediately after linking. */
    public void setGroupColor(String color) {
        this.groupColor   = color;
        this.currentColor = color;
    }

    public boolean isInGroup() {
        return groupId != 0;
    }

    /** Used during SVG parsing to handle spanning segments discovered in a second file. */
    public void addSvgFile(String svgFile) {
        if (!svgFiles.contains(svgFile)) svgFiles.add(svgFile);
    }

    public boolean appearsInTab(String svgFile) {
        return svgFiles.contains(svgFile);
    }

    // Getters
    public String getId()            { return id; }
    public String getName()          { return name; }
    public String getOriginalColor() { return originalColor; }
    public String getCurrentColor()  { return currentColor; }
    public String       getGroupColor()    { return groupColor; }
    public int getGroupId()       { return groupId; }
    public List<String> getSvgFiles(){ return svgFiles; }

    // Setter — used when highlighting, graying out, restoring
    public void setCurrentColor(String color) { this.currentColor = color; }
}