import java.io.*;
import java.util.Date;

/**
 * Created by jsybrand on 7/2/15.
 */
public class CustomLog {

    public static void log(String msg, String logName) throws IOException
    {
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(logName),true));
        printWriter.println("------"+new Date()+"--------");
        printWriter.println(msg);
        printWriter.close();

    }
}

