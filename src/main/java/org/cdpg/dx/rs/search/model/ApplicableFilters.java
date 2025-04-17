package org.cdpg.dx.rs.search.model;

import io.vertx.codegen.annotations.DataObject;

import java.util.ArrayList;
import java.util.List;

@DataObject(generateConverter = true)
public class ApplicableFilters {
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
    public ApplicableFilters() {
        this.itemFilters = new ArrayList<>();
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

}
