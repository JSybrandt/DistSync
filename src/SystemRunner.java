import java.io.BufferedReader;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jsybrand on 7/10/15.
 */
public class SystemRunner extends Thread {
    private String command[], logfile;
    AtomicBoolean isComplete = new AtomicBoolean(false);
    AtomicBoolean staleNFS = new AtomicBoolean(false);
    int numberOfTries = 0;
    final int MAX_TRIES=3;
    SystemRunner(String cmd[],String lFile)
    {
        command = cmd;
        command[0] = "nice --15 " + command[0];
        logfile = lFile;
    }

    @Override
    public void run(){
        try {

            Process proc = Runtime.getRuntime().exec(command);
            Scanner scan = new Scanner(proc.getErrorStream());
            numberOfTries++;
            proc.waitFor();


            if (proc.exitValue() != 0) {
                String err = "";
                while (scan.hasNextLine()) err += scan.nextLine() + "\n";
                CustomLog.log("Attempt #" + numberOfTries + "\t" + err, logfile);
                if (numberOfTries < MAX_TRIES && err.contains("Stale NFS"))
                    staleNFS.set(true);
                else
                    CustomLog.log("FAILURE:" + proc.exitValue(), logfile);
            }
        }
        catch(Exception ignore){}
        finally{
            isComplete.set(true);
        }
    }

    public String getLogfile(){return logfile;}
}
