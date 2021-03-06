import sun.rmi.runtime.Log;

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
        numAvalibleProcs = Math.max(1,Runtime.getRuntime().availableProcessors()-1);//we want to make sure theres room for this worker
    }

    @Override
    public void finalize() throws Throwable{
        try {
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
                        case MODIFY_DIRS:
                            preformSyncDirs(received);
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

                    Exception newE = new Exception(e.getMessage() + "\nLog file should be:" + received.logFile+"\nWas given " + received.path);
                    //out.writeObject(newE);
                    out.writeObject("FINISHED");
                    System.err.println(newE);
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

    private boolean checkAndRepairStaleNSF(SystemRunner [] runners) throws IOException, InterruptedException
    {
        boolean ret = false;
        //pre check for stale nfs (if the whole file system drops out, this is a fixable issue
        for(int i = 0 ; i < runners.length;i++)
        {
            //if any runner has a stale nsf handle
            if(runners[i]!=null && runners[i].staleNFS.get())
            {
                ret = true;
                CustomLog.log("Attempting to reset NFS Handle: Up:" + Job.upToDateDevice + " Out:" + Job.outOfDateDevice, runners[i].getLogfile());

                //attempt to toggle the mounts of
                Runtime.getRuntime().exec("umount " + Job.upToDateDevice).waitFor();
                Runtime.getRuntime().exec("mount " + Job.upToDateDevice).waitFor();
                Runtime.getRuntime().exec("umount " + Job.outOfDateDevice).waitFor();
                Runtime.getRuntime().exec("mount " + Job.outOfDateDevice).waitFor();

                sleep(10000);

                //loop through all runners and restart any that have failed.
                for(int j = 0 ; j < runners.length;j++)
                {
                    if(runners[j]!=null && runners[j].staleNFS.get())
                    {
                        runners[j].staleNFS.set(false);
                        runners[j].isComplete.set(false);
                        runners[j].start();
                    }
                }

                break;
            }
        }
        return ret;
    }

    private void preformCopy(Job job) throws IOException, InterruptedException{

        System.out.println("COPY STARTED");

        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"cp","-Pp","",""};

        SystemRunner runners[] = new SystemRunner[numAvalibleProcs];
        while(scan.hasNextLine())
        {
            checkAndRepairStaleNSF(runners);
            for(int i = 0 ; i < runners.length;i++)
            {
                if(runners[i]==null || runners[i].isComplete.get())
                {
                    String path = scan.nextLine();
                    cmd[2]=job.upToDateMountPoint+path;
                    cmd[3]=job.outOfDateMountPoint+path;
                    runners[i]= new SystemRunner(cmd.clone(),job.logFile);
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
    private void preformCreateDir(Job job)throws IOException, InterruptedException{
        Scanner scan = new Scanner(new File(job.path));

        String cmd1[] = {"chown","",""};
        String cmd2[] = {"chmod","",""};

        SystemRunner runners[][] = new SystemRunner[Math.max(1,numAvalibleProcs/2)][2];
        while(scan.hasNextLine())
        {
            for(int i = 0 ; i < runners.length;i++)
            {
                if((runners[i][0]==null && runners[i][1]==null)
                        || (runners[i][0].isComplete.get()&&runners[i][1].isComplete.get()))
                {
                    String path = scan.nextLine();
                    new File(job.outOfDateMountPoint+path).mkdir();
                    //System.out.println("Making : " + job.outOfDateMountPoint+path);
                    cmd1[1] = cmd2[1]="--reference="+job.upToDateMountPoint+path;
                    cmd1[2] = cmd2[2]=job.outOfDateMountPoint+path;
                    runners[i][0]= new SystemRunner(cmd1.clone(),job.logFile);
                    runners[i][1]= new SystemRunner(cmd2.clone(),job.logFile);
                    runners[i][0].start();
                    runners[i][1].start();
                    break;
                }
            }
        }

        for(SystemRunner s[] : runners)
        {
            for(SystemRunner r : s)
                if(r != null)
                    r.join();
        }
    }

    private void preformRemoveFiles(Job job) throws IOException, InterruptedException{
        Scanner scan = new Scanner(new File(job.path));

        String cmd[] = {"rm","-f",""};

        SystemRunner runners[] = new SystemRunner[numAvalibleProcs];
        while(scan.hasNextLine())
        {
            checkAndRepairStaleNSF(runners);
            for(int i = 0 ; i < runners.length;i++)
            {
                if(runners[i]==null || runners[i].isComplete.get())
                {
                    String path = scan.nextLine();
                    cmd[2]=job.outOfDateMountPoint+path;
                    runners[i]= new SystemRunner(cmd.clone(),job.logFile);
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

    private void preformRemoveDirectory(Job job) throws IOException{
        boolean containedErrors = false;
        Runtime r = Runtime.getRuntime();
        Scanner scan = new Scanner(new File(job.path));
        Stack<String> stack = new Stack<>();
        while(scan.hasNext()){
            String path = job.outOfDateMountPoint+scan.nextLine();
            stack.push(path);
        }
        String cmd[] = {"rm","-rf",""};
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

        String cmd[] = {"rsync","-aSHAXd","",""};

        SystemRunner runners[] = new SystemRunner[numAvalibleProcs];
        while(scan.hasNextLine())
        {
            checkAndRepairStaleNSF(runners);
            for(int i = 0 ; i < runners.length;i++)
            {
                if(runners[i]==null || runners[i].isComplete.get())
                {
                    String path = scan.nextLine();
                    cmd[2]=job.upToDateMountPoint+path;
                    cmd[3]=job.outOfDateMountPoint+path;
                    runners[i]= new SystemRunner(cmd.clone(),job.logFile);
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

    private void preformSyncDirs(Job job) throws IOException, InterruptedException{
        Scanner scan = new Scanner(new File(job.path));

        String cmd1[] = {"chown","",""};
        String cmd2[] = {"chmod","",""};

        SystemRunner runners[][] = new SystemRunner[numAvalibleProcs/2][2];
        while(scan.hasNextLine())
        {
            for(int i = 0 ; i < runners.length;i++)
            {
                if((runners[i][0]==null && runners[i][1]==null)
                        || (runners[i][0].isComplete.get()&&runners[i][1].isComplete.get()))
                {
                    String path = scan.nextLine();
                    cmd1[1] = cmd2[1]="--reference="+job.upToDateMountPoint+path;
                    cmd1[2] = cmd2[2]=job.outOfDateMountPoint+path;
                    runners[i][0]= new SystemRunner(cmd1.clone(),job.logFile);
                    runners[i][1]= new SystemRunner(cmd2.clone(),job.logFile);
                    runners[i][0].start();
                    runners[i][1].start();
                    break;
                }
            }
        }

        for(SystemRunner s[] : runners)
        {
            for(SystemRunner r : s)
                if(r != null)
                     r.join();
        }
    }

    private void preformBuildLinks(Job job) throws IOException{
        String cmd[] = {"rsync","-aSHAXd","--files-from="+job.path,Job.upToDateMountPoint,Job.outOfDateMountPoint};

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
