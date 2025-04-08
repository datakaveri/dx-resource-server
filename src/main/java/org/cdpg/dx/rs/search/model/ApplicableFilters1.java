package org.cdpg.dx.rs.search.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

@DataObject(generateConverter = true)
public class ApplicableFilters1 {

    private List itemFilters;
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

        this.itemFilters = new ArrayList<>();
    }

    // Constructor with all fields
    public ApplicableFilters1(JsonObject jsonObject) {

        this.itemFilters = jsonObject.getJsonArray("iudxResourceAPIs").getList();
        this.groupId = jsonObject.getString("resourceGroup");
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
