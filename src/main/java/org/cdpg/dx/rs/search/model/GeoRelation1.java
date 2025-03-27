package org.cdpg.dx.rs.search.model;

public class GeoRelation1 {
    private String relation;
    private Double maxDistance;
    private Double minDistance;

    public GeoRelation1() {}

    public GeoRelation1(String relation, Double maxDistance, Double minDistance) {
        this.relation = relation;
        this.maxDistance = maxDistance;
        this.minDistance = minDistance;
    }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public Double getMaxDistance() { return maxDistance; }
    public void setMaxDistance(Double maxDistance) { this.maxDistance = maxDistance; }

    public Double getMinDistance() { return minDistance; }
    public void setMinDistance(Double minDistance) { this.minDistance = minDistance; }

    @Override
    public String toString() {
        return "GeoRelation{" +
                "relation='" + relation + '\'' +
                ", maxDistance=" + maxDistance +
                ", minDistance=" + minDistance +
                '}';
    }
}
