import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * Created by jsybrand on 6/25/15.
 */
public class Worker extends Thread {

    final int NUM_AVAILABLE_PROCS = 16;//keep it low

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
    public void finalize() throws Throwable{
        try {
            //System.out.println("Finalize Called");
            in.close();
            out.close();
            socket.close();
            super.finalize();
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
                if(socket.isClosed())
                    break;
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                Job received = (Job)in.readObject();
                System.out.println("Client Received:" + received);
                if (received==null) {
                    out.writeObject(null);
                    break;
                }

                out.writeObject("STARTED");

                long startTime = System.nanoTime();

                try {
                    switch(received.getType()) {
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
                        case BUILD_LINKS:
                            preformBuildLinks(received);
                            break;
                        case OTHER:break;
                    }

                    Long diffTime = System.nanoTime()-startTime;
                    CustomLog.log(diffTime.toString(),received.logFile);

                    out.writeObject("FINISHED");
                }
                catch(IOException e)
                {
                    out.writeObject(e);
                    System.err.println(e);
                }
            }
        }
        catch(Exception e){
            System.err.println("Client Failed to listen: " + e);
            e.printStackTrace();
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

    private void preformCopy(Job job) throws IOException, InterruptedException{
        Runtime r = Runtime.getRuntime();
        //for right now we are just going to use cp, because its the dumb answer

        Process procs[] = new Process[NUM_AVAILABLE_PROCS];

        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"mcp","-P","--preserve=all","--sparse=never","",""};

        while(scan.hasNext())
        {
            for(int i = 0;i < procs.length; i++) {
                if(scan.hasNext() && (procs[i]==null || !isRunning(procs[i]))) {
                    String path = scan.nextLine();
                    cmd[4]=job.upToDateMountPoint+path;
                    cmd[5]=job.outOfDateMountPoint+path;
                    procs[i] = r.exec(cmd);
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
            String path = job.outOfDateMountPoint+scan.nextLine();
            new File(path).mkdir();
        }
    }

    private void preformRemoveFiles(Job job) throws IOException, InterruptedException{
        Runtime r = Runtime.getRuntime();
        //for right now we are just going to use cp, because its the dumb answer


        final Process p = r.exec("xargs rm <" + job.path);

        Thread t = new Thread(){
            @Override
            public void run() {
                super.run();
                try{sleep(10000);}catch(Exception e){}
                if(isRunning(p)) {
                    p.destroy();
                }

            }
        };
        t.start();

        p.waitFor();

        if(p.exitValue()!=0)
            throw new IOException("RM proc had an error somewhere within " + job.fileName);

    }

    private void preformRemoveDirectory(Job job) throws IOException{
        boolean containedErrors = false;
        Runtime r = Runtime.getRuntime();
        Scanner scan = new Scanner(new File(job.path));
        Stack<String> stack = new Stack<>();
        while(scan.hasNext()){
            String path = job.outOfDateMountPoint+scan.nextLine();
            stack.push(path);
        }
        String cmd[] = {"rm","-r",""};
        while(!stack.empty()){
            cmd[2]=stack.pop();
            try {
                Process p = r.exec(cmd);
                p.waitFor();
                if(p.exitValue()!=0)
                    containedErrors=true;
            } catch(InterruptedException e){System.err.println("RM Interupted. " + e);}
        }
        if(containedErrors)
            throw new IOException("RM -r Proc Error somewhere within " + job.fileName);
    }

    private void preformSyncFiles(Job job) throws IOException, InterruptedException{
        Runtime r = Runtime.getRuntime();

        Scanner scan = new Scanner(new File(job.path));

        Process shiftc = r.exec("shiftc --hosts=16 --sync-fast --host-list=localhost --wait -P");
        PrintWriter writer = new PrintWriter(shiftc.getOutputStream());

        while(scan.hasNext())
        {
            String path = scan.nextLine();
            String line = job.upToDateMountPoint+path + "\t" + job.outOfDateMountPoint+path;
            writer.println(line);
            System.out.println(line);

        }

        writer.close();

        shiftc.waitFor();

        if(shiftc.exitValue()!=0)
            throw new IOException("SHIFTC --sync had an error somewhere in " + job.fileName);
    }

    private void preformBuildLinks(Job job) throws IOException{

        String cmd[] = {"rsync","-laSHAX","--files-from="+job.path,Job.upToDateMountPoint,Job.outOfDateMountPoint};

        Process p = Runtime.getRuntime().exec(cmd);
        try {
            p.waitFor();
                int val = p.exitValue();
                if(val!=0)//err
                    throw new IOException("RSYNC failed to process links");

        }
        catch(InterruptedException e) {
            System.err.println("RSYNC Interupted. " + e);
        }

    }
}
