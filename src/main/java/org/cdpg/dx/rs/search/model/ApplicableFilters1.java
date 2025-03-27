package org.cdpg.dx.rs.search.model;

import io.vertx.codegen.annotations.DataObject;

import java.util.ArrayList;
import java.util.List;

@DataObject(generateConverter = true)
public class ApplicableFilters1 {
    private List<String> groupFilters;
    private List<String> itemFilters;
    private String groupId;

    public boolean isFilterValid() {
        return isFilterValid;
    }

    public void setFilterValid(boolean filterValid) {
        isFilterValid = filterValid;
    }

    private boolean isFilterValid;

    // Default constructor
    public ApplicableFilters1() {
        this.groupFilters = new ArrayList<>();
        this.itemFilters = new ArrayList<>();
    }

    // Constructor with all fields
    public ApplicableFilters1(List<String> groupFilters, List<String> itemFilters, String groupId) {
        this.groupFilters = groupFilters != null ? groupFilters : new ArrayList<>();
        this.itemFilters = itemFilters != null ? itemFilters : new ArrayList<>();
        this.groupId = groupId;
    }

    // Getters and setters
    public List<String> getGroupFilters() {
        return groupFilters;
    }

    public void setGroupFilters(List<String> groupFilters) {
        this.groupFilters = groupFilters;
    }

    public List<String> getItemFilters() {
        return itemFilters;
    }

    public void setItemFilters(List<String> itemFilters) {
        this.itemFilters = itemFilters;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    // Convenience method to get all filters combined
    public List<String> getAllFilters() {
        List<String> allFilters = new ArrayList<>();
        allFilters.addAll(groupFilters);
        allFilters.addAll(itemFilters);
        return allFilters;
    }
}
