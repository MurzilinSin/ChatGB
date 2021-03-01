package server;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import server.chat.MyServer;

import java.io.IOException;



public class ServerApp {

    private static final int DEFAULT_PORT = 8888;
    public static final Logger logToFile = Logger.getLogger("file");
    public static final Logger logToConsole = Logger.getLogger("console");

    public static void main(String[] args) {
        PropertyConfigurator.configure("src/main/resources/log/config/log4j.properties");
        int port = DEFAULT_PORT;
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        }
        try {
            new MyServer(port).start();
        } catch (IOException e) {
            logToFile.error("Сервер не может запуститься",e);
            logToConsole.error("Сервер не может запуститься",e);
            System.exit(1);
        }
    }
}


