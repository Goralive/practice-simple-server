import org.webserver.server.HttpServerImp;

public class Main {
    public static void main(String[] args) {
        HttpServerImp httpServer = new HttpServerImp();
        httpServer.start(Integer.parseInt(System.getProperty("port","8080")));
    }
}
