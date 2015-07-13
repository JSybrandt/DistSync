import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jsybrand on 7/10/15.
 */
public class SystemRunner extends Thread {
    private String command[], logfile;
    boolean isComplete = false;
    SystemRunner(String cmd[],String lFile)
    {
        command = cmd;
        logfile = lFile;
    }

    @Override
    public void run(){
        try {
            Process proc = Runtime.getRuntime().exec(command);

            proc.waitFor();

            if(proc.exitValue()!=0)
                throw new Exception(command[0] + " Failed to run properly.");

        }catch(Exception e){
            try {
                CustomLog.log(e.getClass().toString() + ":" + e.getMessage(), logfile);
            }catch(Exception ignore) {}
        }
        finally{
            isComplete=true;
        }
    }
}
