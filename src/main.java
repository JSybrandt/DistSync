import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by jsybrand on 6/25/15.
 */

public class main {


    /*******************************
     *
     * This is the entry point for distsync (not "Distributed_Copy" thats a bad name.)
     * Run with java -jar distsync.jar [-f|-s]* [-w HOST_ADDRESS | -m WORKER_ADDRESS_1 WORKER_ADDRESS_2 ...]
     *
     * @param args -w specifies this is a worker and must be followed with a hostname,
     *             -m specifies this is a manager and may be followed with a list of hosts to spawn wokers on
     *             FOR -m TO WORK, EVERY HOST NEEDS EQUAL ACCESS TO THE .jar and both file systems
     *             -f specifies the fresh directory
     *             -s specifies the stale directory
     */


    public static void main(String[] args) throws IOException{

        Manager manager = null;
        Worker worker = null;



        if(!checkForJobs())
        {
            System.err.println("No job files found in " + Constants.JOB_DIR);
            createTempDirs();
            //return;
        }
        if (args.length < 1){
            System.out.println("Specify -m for manager, or -w for worker");
            return;
        }

        try {

            for(int i = 0 ; i < args.length;i++) {

                if(args[i].equals("-f")){
                    i++;
                    Job.upToDateMountPoint = new File(args[i]).getAbsolutePath();
                    if(!Job.upToDateMountPoint.endsWith("/"))Job.upToDateMountPoint+="/";
                    System.out.println("Fresh: " + Job.upToDateMountPoint);
                }
                else if(args[i].equals("-s"))
                {
                    i++;
                    Job.outOfDateMountPoint=new File(args[i]).getAbsolutePath();
                    if(!Job.outOfDateMountPoint.endsWith("/"))Job.outOfDateMountPoint+="/";
                    System.out.println("Stale: " + Job.outOfDateMountPoint);
                }

                else if (args[i].equals("-w")) {
                    if (args.length-i < 2) System.out.println("Specify manager hostname.");
                    else {
                        i++;
                        worker = new Worker(args[i]);
                        worker.start();
                        worker.join();
                    }
                } else if (args[i].equals("-m")) {
                    manager = new Manager();
                    manager.start();

                    if (args.length-i >= 2) {

                        String managerHostname = InetAddress.getLocalHost().getHostName();
                        String pathToJar = new File(main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
                        System.out.println("JAR located at : " + Constants.JAR_PATH);
                        System.out.println("tmp located at : " + Constants.TEMP_DIR);
                        Runtime runtime = Runtime.getRuntime();

                        for (i++; i < args.length; i++) {

                            String command = "java -jar " + pathToJar +
                                    " -f " + Job.upToDateMountPoint +
                                    " -s " + Job.outOfDateMountPoint +
                                    " -w " + managerHostname;
                            if(args[i].equals("localhost"))
                            {
                                runtime.exec(command);
                                System.out.println(command);
                            }
                            else
                            {
                                runtime.exec("ssh " + args[i] + " " + command);
                            }
                            System.out.println("Attempted to start worker on " + args[i]);

                        }
                    }
                    manager.join();

                } else {
                    System.out.println("Argument " + args[i] + " not understood.");
                }
            }
        }
        catch(IndexOutOfBoundsException e)
        {
            System.out.println("Invalid command line arguments. " + e);
        }
        catch (Exception e) {
            System.err.println("Failed to start services. " + e);
            e.printStackTrace();
        }
    }


    public static boolean checkForJobs(){
        String[] s = new File(Constants.JOB_DIR).list();
        if(s==null || s.length==0)
            return false;
        return true;
    }

    public static void createTempDirs(){
        new File(Constants.TEMP_DIR).mkdir();
        new File(Constants.JOB_DIR).mkdir();
        new File(Constants.LOG_DIR).mkdir();
    }
}
