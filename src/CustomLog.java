import java.io.*;
import java.util.Date;

/**
 * Created by jsybrand on 7/2/15.
 */
public class CustomLog {

    public static void log(String msg, String logName)
    {
        try {
            String logFile = Constants.LOG_DIR+(new Date().toString())+logName;
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            printWriter.println(msg);
            printWriter.close();
        } catch (IOException e) {
            System.err.println("LogFailed:" + msg);
        }
    }
}

