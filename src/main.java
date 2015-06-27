/**
 * Created by jsybrand on 6/25/15.
 */
import java.util.*;
public class main {
    public static void main(String[] args) {
        Manager manager = null;
        Worker worker = null;
        try {

            if (args.length < 1) System.out.println("Specify -m for manager, or -w for worker");
            else if (args[0].equals("-w")) {
                if (args.length < 2) System.out.println("Specify manager hostname.");
                else {
                    worker = new Worker(args[2]);
                    worker.start();
                    worker.join();
                }
            } else if (args[0].equals("-m")) {
                manager = new Manager();
                manager.start();
                for (int i = 1; i < args.length; i++) {
                    Runtime.getRuntime().exec("ssh " + args[i] + " java ~/.dc/copy");
                    System.out.println("Attempted to start worker on " + args[i]);
                }
                manager.join();
            }
            else if (args[0].equals("-l")){
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
            System.err.println("Failed to start services.");
        }
        finally{
            if(manager != null )
                manager.safeClose();
            if(worker != null)
                worker.safeClose();
        }
    }
}
