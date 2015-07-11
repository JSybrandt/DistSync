import java.io.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jsybrand on 7/2/15.
 */
public class CustomLog {

    //mapping of logfile to tool
    private static ConcurrentHashMap <String,PrintWriter> openFiles = new ConcurrentHashMap<>();

    public static void log(String msg, String logName) throws IOException
    {
        PrintWriter printWriter = null;
        if(openFiles.containsKey(logName))
            printWriter = openFiles.get(logName);
        else {
            printWriter = new PrintWriter(new FileOutputStream(new File(logName), true));
            openFiles.put(logName,printWriter);
        }
        printWriter.println("------"+new Date()+"--------");
        printWriter.println(msg);
        printWriter.close();

    }

    public static void close()
    {
        for(PrintWriter p : openFiles.values())
        {
            p.close();
        }
    }

}

