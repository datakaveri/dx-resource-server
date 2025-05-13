package org.cdpg.dx.common.response;


import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DxResponse<T> {
    private String type;     // e.g. "urn:dx:acl:success"
    private String title;    // e.g. "Success" or "Bad Request"
    private String detail;   // Optional detailed message
    private T result;        // Optional payload

    public DxResponse() {}

    public DxResponse(String type, String title, String detail, T result) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.result = result;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
