package org.webserver.endpoints;

public enum ENDPOINT {
    APP_TXTS("/app/txts");


    private final String url;

    private ENDPOINT(String url) {
        this.url = url;
    }

    public String getURL() {
        return url;
    }
}

