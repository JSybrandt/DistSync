import javafx.util.Pair;
import sun.awt.Mutex;

import java.io.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jsybrand on 7/2/15.
 */
public class CustomLog {

    //mapping of logfile to tool
    private static ConcurrentHashMap <String,Pair<PrintWriter,Mutex>> openFiles = new ConcurrentHashMap<>();

    public static void log(String msg, String logName) throws IOException
    {
        Pair<PrintWriter,Mutex> writePermissions = null;
        if(openFiles.containsKey(logName))
            writePermissions = openFiles.get(logName);
        else {
            PrintWriter p = new PrintWriter(new FileOutputStream(new File(logName), true));
            Mutex m = new Mutex();
            Pair<PrintWriter,Mutex> pair = new Pair<>(p,m);
            openFiles.put(logName,pair);
        }
        writePermissions.getValue().lock();
        writePermissions.getKey().println("------" + new Date() + "--------");
        writePermissions.getKey().println(msg);
        writePermissions.getValue().unlock();

    }

    public static void close()
    {
        for(Pair<PrintWriter,Mutex> pair : openFiles.values())
        {
            pair.getKey().close();
        }
    }

}

