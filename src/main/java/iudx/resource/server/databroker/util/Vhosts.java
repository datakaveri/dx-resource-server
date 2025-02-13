package iudx.resource.server.databroker.util;

public enum Vhosts {
    IUDX_PROD("prodVhost"), IUDX_INTERNAL("internalVhost"), IUDX_EXTERNAL("externalVhost");

    public String value;

    Vhosts(String value) {
        this.value = value;
    }
}
