package org.cdpg.dx.rs.search.model;

public class TemporalRelation1 {
    private String endTime;
    private String temprel;
    private String time;

    public TemporalRelation1() {}

    public TemporalRelation1(String temprel, String time, String endTime) {
        this.temprel = temprel;
        this.time = time;
        this.endTime = endTime;
    }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getTemprel() { return temprel; }
    public void setTemprel(String temprel) { this.temprel = temprel; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    @Override
    public String toString() {
        return "TemporalRelation{" +
                "endTime='" + endTime + '\'' +
                ", temprel='" + temprel + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
