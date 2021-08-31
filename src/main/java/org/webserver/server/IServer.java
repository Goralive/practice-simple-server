package org.webserver.server;

public interface IServer {
    void start(int port);
    void shutDown(int port);
}
