import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Stack;

/**
 * Created by jsybrand on 6/25/15.
 */
public class Worker extends Thread {

    final int NUM_AVAILABLE_PROCS = 8;//keep it low

    ObjectInputStream in;
    ObjectOutputStream out;
    Socket socket;

    Worker(String serverAddress)
    {
        try {
            socket = new Socket(serverAddress, Constants.PORT);

        }
        catch(Exception e)
        {
            System.err.println("Worker failed to connect to server. " + e);
        }
    }

    @Override
    public void finalize(){
        try {
            System.out.println("Finalize Called");
            in.close();
            out.close();
            socket.close();
        }
        catch(Exception e)
        {
            System.err.println("Worker failed to close.");
        }
    }

    @Override
    public void run()
    {
        try {
            while(true) {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                Job received = (Job)in.readObject();
                System.out.println("Client Received:" + received);
                if (received==null) {
                    return;
                }
                out.writeObject("STARTED");

                try {
                    switch(received.type) {
                        case CREATE_DIR:
                            preformCreateDir(received);
                            break;
                        case RM_DIR:
                            preformRemoveDirectory(received);
                            break;
                        case CREATE_FILES:
                            preformCopy(received);
                            break;
                        case RM_FILES:
                            preformRemoveFiles(received);
                            break;
                        case MODIFY_FILES:
                            preformSyncFiles(received);
                            break;
                        case OTHER:break;
                    }

                    out.writeObject("FINISHED");
                }
                catch(IOException e)
                {
                    out.writeObject("ERROR");
                    System.err.println(e);
                }
            }
        }
        catch(Exception e){
            System.err.println("Client Failed to listen :(" + e);
        }

    }

    private boolean isRunning(Process p)
    {
        try{
            p.exitValue();
            return false;
        }catch(IllegalThreadStateException e){
            return true;
        }
    }

    private void preformCopy(Job job) throws IOException{
        Runtime r = Runtime.getRuntime();
        //for right now we are just going to use cp, because its the dumb answer

        Process procs[] = new Process[NUM_AVAILABLE_PROCS];

        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"cp","",""};

        while(scan.hasNext())
        {
            for(Process p : procs) {
                if(scan.hasNext() && (p==null || !isRunning(p))) {
                    String path = scan.next();
                    cmd[1]=job.upToDateMountPoint+path;
                    cmd[2]=job.outOfDateMountPoint+path;
                    p = r.exec(cmd);
                }
            }
        }

        for(Process p : procs)
        {
            try {
                if(p != null)
                p.waitFor();
            }
            catch(InterruptedException e) {
                System.err.println("CP Interupted. " + e);
            }
        }
    }

    //this relies on the job file listing dirs in order
    private void preformCreateDir(Job job)throws IOException{
        Scanner scan = new Scanner(new File(job.path));
        while(scan.hasNext())
        {
            String path = job.outOfDateMountPoint+scan.next();
            new File(path).mkdir();
        }
    }

    private void preformRemoveFiles(Job job) throws IOException{
        Runtime r = Runtime.getRuntime();
        //for right now we are just going to use cp, because its the dumb answer

        Process procs[] = new Process[NUM_AVAILABLE_PROCS];

        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"rm",""};

        while(scan.hasNext())
        {
            for(Process p : procs) {
                if(scan.hasNext() && (p==null || !isRunning(p))) {
                    String path = scan.next();
                    cmd[1]=job.outOfDateMountPoint+path;
                    p = r.exec(cmd);
                }
            }
        }

        for(Process p : procs)
        {
            try {
                if(p != null) {
                    p.waitFor();
                    if(p.exitValue()!=0)//err
                        throw new IOException("CP Proc Error");
                }
            }
            catch(InterruptedException e) {
                System.err.println("RM Interupted. " + e);
            }
        }
    }

    private void preformRemoveDirectory(Job job) throws IOException{
        Runtime r = Runtime.getRuntime();
        Scanner scan = new Scanner(new File(job.path));
        Stack<String> stack = new Stack<>();
        while(scan.hasNext()){
            String path = job.outOfDateMountPoint+scan.next();
            stack.push(path);
        }
        String cmd[] = {"rm","-r",""};
        while(!stack.empty()){
            cmd[2]=stack.pop();
            try {
                Process p = r.exec(cmd);
                p.waitFor();
                if(p.exitValue()!=0)
                    throw new IOException("RM -r Proc Error while attempting to remove " + cmd[2]);
            } catch(InterruptedException e){System.err.println("RM Interupted. " + e);}
        }
    }

    private void preformSyncFiles(Job job) throws IOException{
        Runtime r = Runtime.getRuntime();
        //for right now we are just going to use cp, because its the dumb answer

        Process procs[] = new Process[NUM_AVAILABLE_PROCS];

        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"rsync","",""};

        while(scan.hasNext())
        {
            for(Process p : procs) {
                if(scan.hasNext() && (p==null || !isRunning(p))) {
                    String path = scan.next();
                    cmd[1]=job.upToDateMountPoint+path;
                    cmd[2]=job.outOfDateMountPoint+path;
                    p = r.exec(cmd);
                }
            }
        }

        for(Process p : procs)
        {
            try {
                if(p != null) {
                    p.waitFor();
                    if(p.exitValue()!=0)//err
                        throw new IOException("RSYNC Proc Error");
                }
            }
            catch(InterruptedException e) {
                System.err.println("RSYNC Interupted. " + e);
            }
        }
    }
}
