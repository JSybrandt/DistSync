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
    private static ConcurrentHashMap <String,PrintWriter> openFiles = new ConcurrentHashMap<>();

    public static void log(String msg, String logName) throws IOException
    {
        PrintWriter writer = null;
        if(openFiles.containsKey(logName))
            writer = openFiles.get(logName);
        else {
            writer = new PrintWriter(new FileOutputStream(new File(logName), true));
           // Mutex m = new Mutex();
            //Pair<PrintWriter,Mutex> pair = new Pair<>(p,m);
            openFiles.put(logName,writer);
        }
        //writePermissions.getValue().lock();
        writer.println("------" + new Date() + "--------");
        writer.println(msg);
        //writePermissions.getValue().unlock();
        System.out.println(msg);
    }

    public static void close()
    {
        for(PrintWriter p : openFiles.values())
        {
            p.close();
        }
    }

}

