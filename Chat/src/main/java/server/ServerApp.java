package server;

import server.chat.MyServer;
import java.io.IOException;

public class ServerApp {

    private static final int DEFAULT_PORT = 8888;
    private static Logging log = new Logging();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        }
        try {
            new MyServer(port).start();
        } catch (IOException e) {
            log.error("Сервер не может запуститься",e);
            System.exit(1);
        }
    }
}


