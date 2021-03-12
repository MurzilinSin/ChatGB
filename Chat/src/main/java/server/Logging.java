package server;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Logging {
    public Logging(){
        PropertyConfigurator.configure("src/main/resources/log/config/log4j.properties");
    }

    public void info(String text){
        Logger logToFile = Logger.getLogger("file");
        Logger logToConsole = Logger.getLogger("console");
        logToConsole.info(text);
        logToFile.info(text);
    }

    public void error(String text, Throwable t){
        Logger logToFile = Logger.getLogger("file");
        Logger logToConsole = Logger.getLogger("console");
        logToConsole.error(text);
        logToFile.error(text,t);
    }
}
