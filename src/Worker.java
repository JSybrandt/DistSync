import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * Created by jsybrand on 6/25/15.
 */
public class Worker extends Thread {

    private int numAvalibleProcs;

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
        numAvalibleProcs = Runtime.getRuntime().availableProcessors()/2;//slowing down
        //numAvalibleProcs = Math.max(1,numAvalibleProcs-1);//we want to make sure theres room for this worker
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
        long startTime = -1;
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

                startTime = System.nanoTime();

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
                    if(startTime > 0) {
                        Long diffTime = System.nanoTime()-startTime;
                        CustomLog.log(e.getMessage() + "\n" +diffTime.toString(), received.logFile);
                    }
                    else
                        CustomLog.log(e.getMessage(),received.logFile);
                    out.writeObject(e);
                    System.err.println(e);
                }
            }
        }
        catch(Exception e){
            System.err.println("Client Failed to listen: " + e);
            e.printStackTrace();
        }
        finally{
            CustomLog.close();
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

        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"cp","-Pp","",""};

        SystemRunner runners[] = new SystemRunner[numAvalibleProcs];
        while(scan.hasNextLine())
        {
            for(int i = 0 ; i < runners.length;i++)
            {
                if(runners[i]==null || runners[i].isComplete.get())
                {
                    String path = scan.nextLine();
                    cmd[2]=job.upToDateMountPoint+path;
                    cmd[3]=job.outOfDateMountPoint+path;
                    runners[i]= new SystemRunner(cmd,job.logFile);
                    runners[i].start();
                    break;
                }
            }
        }

        for(SystemRunner s : runners)
        {
            if(s != null)
                s.join();
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
        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"rm",""};

        SystemRunner runners[] = new SystemRunner[numAvalibleProcs];
        while(scan.hasNext())
        {
            for(int i = 0 ; i < runners.length;i++)
            {
                if(scan.hasNext() && (runners[i]==null || runners[i].isComplete.get()))
                {
                    String path = scan.nextLine();
                    cmd[1]=job.outOfDateMountPoint+path;
                    runners[i]= new SystemRunner(cmd,job.logFile);
                    runners[i].start();
                }
            }
        }

        for(SystemRunner s : runners)
        {
            if(s != null)
                s.join();
        }

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
                if(p.exitValue()!=0) {
                    containedErrors = true;
                    CustomLog.log("Failed to rm " + cmd[2],job.logFile);
                }
            } catch(InterruptedException e){System.err.println("RM Interupted. " + e);}
        }
        if(containedErrors)
            throw new IOException("RM -r Proc Error somewhere within " + job.fileName);
    }

    private void preformSyncFiles(Job job) throws IOException, InterruptedException{
        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"rsync","laSHAXd","",""};

        SystemRunner runners[] = new SystemRunner[numAvalibleProcs];
        while(scan.hasNext())
        {
            for(int i = 0 ; i < runners.length;i++)
            {
                if(scan.hasNext() && (runners[i]==null || runners[i].isComplete.get()))
                {
                    String path = scan.nextLine();
                    cmd[2]=job.upToDateMountPoint+path;
                    cmd[3]=job.outOfDateMountPoint+path;
                    runners[i]= new SystemRunner(cmd,job.logFile);
                    runners[i].start();
                }
            }
        }

        for(SystemRunner s : runners)
        {
            if(s != null)
                s.join();
        }
    }

    private void preformBuildLinks(Job job) throws IOException{
        String cmd[] = {"rsync","-laSHAXd","--files-from="+job.path,Job.upToDateMountPoint,Job.outOfDateMountPoint};

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
