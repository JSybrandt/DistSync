import java.net.InetAddress;
import java.nio.file.Paths;

/**
 * Created by jsybrand on 6/25/15.
 */

public class main {


    /*******************************
     *
     * This is the entry point for distsync (not "Distributed_Copy" thats a bad name.)
     * Run with java -jar distsync.jar [-w|-m]
     *
     * @param args -w specifies this is a worker and must be followed with a hostname,
     *             -m specifies this is a manager and may be followed with a list of hosts to spawn wokers on
     *             FOR -m TO WORK, EVERY HOST NEEDS EQUAL ACCESS TO THE .jar
     */


    public static void main(String[] args) {



        Manager manager = null;
        Worker worker = null;
        try {
            if (args.length < 1) System.out.println("Specify -m for manager, or -w for worker");
            else if (args[0].equals("-w")) {
                if (args.length < 2) System.out.println("Specify manager hostname.");
                else {
                    worker = new Worker(args[1]);
                    worker.start();
                    worker.join();
                }
            } else if (args[0].equals("-m")) {
                manager = new Manager();
                manager.start();

                if(args.length>=2) {

                    String managerHostname = InetAddress.getLocalHost().getHostName();
                    String pathToJar = Paths.get("").toAbsolutePath().toString()+"/"+Constants.JAR_FILE_NAME;
                    System.out.println(managerHostname);
                    System.out.println(pathToJar);
                    Runtime runtime = Runtime.getRuntime();


                    for (int i = 1; i < args.length; i++) {

                        runtime.exec("ssh " + args[i] + " java -jar " + pathToJar + " -w " + managerHostname);
                        System.out.println("Attempted to start worker on " + args[i]);

                    }
                }
                manager.join();
            }
            else if (args[0].equals("-l")){
                Runtime.getRuntime().exec("./resetExpirement");
                System.out.println("Connecting to self.");
                manager = new Manager();
                manager.start();
                Worker worker1 = new Worker("localhost");
                worker1.start();
                Worker worker2 = new Worker("localhost");
                worker2.start();
                Worker worker3 = new Worker("localhost");
                worker3.start();
                Worker worker4 = new Worker("localhost");
                worker4.start();
                Worker worker5 = new Worker("localhost");
                worker5.start();

                worker1.join();
                worker2.join();
                worker3.join();
                worker4.join();
                worker5.join();
                manager.join();
            }
            else {System.out.println("Argument " + args[0] + " not understood.");}
        } catch (Exception e) {
            System.err.println("Failed to start services. " + e);
            e.printStackTrace();
        }
    }
}
