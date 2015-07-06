/**
 * Created by jsybrand on 6/25/15.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Manager extends Thread {

    ArrayList<JobSender> senders = new ArrayList<>();
    ArrayList<Socket> sockets = new ArrayList<>();
    ConcurrentHashMap<Socket,Boolean> connectionStatus = new ConcurrentHashMap<>();//true represents running
    Job[] jobs;


    public void setUpDirStructure() throws IOException
    {
        new File(Constants.TEMP_DIR).mkdir();
        new File(Constants.JOB_DIR).mkdir();
        new File(Constants.JOB_DIR + "C1.temp").createNewFile();
        new File(Constants.JOB_DIR + "R1.temp").createNewFile();
        new File(Constants.JOB_DIR + "A1.temp").createNewFile();
        new File(Constants.JOB_DIR + "D1.temp").createNewFile();
        new File(Constants.JOB_DIR + "M1.temp").createNewFile();
        new File(Constants.JOB_DIR + "C2.temp").createNewFile();
        new File(Constants.JOB_DIR + "C3.temp").createNewFile();
        new File(Constants.JOB_DIR + "R2.temp").createNewFile();
        new File(Constants.JOB_DIR + "A2.temp").createNewFile();
        new File(Constants.JOB_DIR + "D2.temp").createNewFile();
        new File(Constants.JOB_DIR + "M2.temp").createNewFile();
    }

    public void deletePath(File file)
    {
        if(file.isDirectory())
        {
            File children[] = file.listFiles();
            if(children != null)
                for (File f : children) {
                    deletePath(f);
                }
        }
        System.out.println("Removing: " + file.getName());
        file.delete();
    }

    @Override
    public void finalize(){
        try {
            for(Socket s : sockets)
                s.close();
        }
        catch(Exception e){
            System.err.println("Sever failed to close.");
        }
    }

    private Job[] getSortedJobFiles()
    {
        try {
            String[] s = new File(Constants.JOB_DIR).list();
            Job[] j = new Job[s.length];
            for (int i = 0; i < s.length; i++) {
                j[i] = new Job(s[i]);
            }
            Arrays.sort(j);
            return j;
        }catch(IOException e){
            System.err.println("Something went wonky in the file system.");
            return null;
        }
    }

    public void getConnections() throws IOException
    {
        ServerSocket listener = new ServerSocket(Constants.PORT);
        listener.setSoTimeout(2000);

        while(!listener.isClosed())
        {
            try {
                Socket s = listener.accept();
                sockets.add(s);
                connectionStatus.put(s, false);
                System.out.println("Made Connection with " + s.getLocalAddress().toString());
            }catch(SocketTimeoutException e){listener.close();/*who cares*/}
        }
    }

    //returns true if finished
    public boolean checkIsFinished(){
        for(Job j : jobs)
        {
            if(j.state == Constants.State.NOT_STARTED)
                return false;
        }
        for(JobSender s : senders)
        {
            if(s.protocol.state != Constants.State.FINISHED
                    && s.protocol.state != Constants.State.ERROR)
            {
                return false;
            }
        }
        return true;
    }

    //determines which job should be sent out next
    private Job selectNextJob()
    {
        boolean maySelectCreateFiles=true;
        boolean maySelectRemoveDirectories=true;
        //takes advantage of priority queue
        for(Job j : jobs)
        {
            if(j.state != Constants.State.FINISHED){
                if(j.type == Job.Type.CREATE_DIR)
                    maySelectCreateFiles = false;
                else if(j.type == Job.Type.RM_FILES)
                    maySelectRemoveDirectories=false;

                if(j.state==Constants.State.NOT_STARTED)
                {
                    if(j.type == Job.Type.CREATE_FILES) {
                        if (maySelectCreateFiles) {
                            return j;
                        }
                    }
                    else if(j.type == Job.Type.RM_DIR) {
                        if (maySelectRemoveDirectories) {
                            return j;
                        }
                    }
                    else return j;
                }
            }
        }
        return null;
    }

    @Override
    public void run()
    {
        long startTime = System.nanoTime();
        int cons = 0;
        try {
            getConnections();
            JobSplitter jobSplitter = new JobSplitter();
            jobSplitter.run();

            jobSplitter.join();
            jobs = jobSplitter.getResults();

            while (!checkIsFinished())
            {
                Job j = selectNextJob();
                if(j!=null)
                {
                    //System.out.println("Selected " + j);
                    for(Socket s : sockets)
                    {
                        if(!connectionStatus.get(s))
                        {
                            cons++;
                            JobSender sender = new JobSender(s,j,cons,this);
                            senders.add(sender);
                            j.state = Constants.State.ASSIGNED;
                            connectionStatus.put(s,true);
                            sender.start();
                            break;
                        }
                    }
                }
            }
            for(JobSender j : senders)
            {
                j.join();
            }

            for(JobSender j : senders){
                if(j.protocol.state == Constants.State.ERROR)
                {
                    System.err.println(j.job + " FAILED TO COMPLETE.");
                    for(Job job : jobs)
                    {
                        if(j.job==job) {
                            job.state = Constants.State.NOT_STARTED;
                            System.err.println("Allowing " + job.fileName + " to restart.");
                        }
                    }
                }
            }
        }
        catch(Exception e ){
            System.err.println(e);
            e.printStackTrace();
        }
        for(Socket s : sockets)
        {
            try {


                ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream i = new ObjectInputStream(s.getInputStream());
                o.writeObject(null);
                if(i.readObject()!=null)
                    System.err.println("Failed to get shutdown response from client.");
                else
                    System.out.println("Shutdown " + s.getLocalAddress());


            }
            catch(Exception e)
            {
                System.err.println("Failed to send quit to " + s.getLocalAddress());
                System.err.println("\t" + e);
            }
        }
        //deletePath(new File(Constants.TEMP_DIR));

        System.out.println("SYNC COMPLETE!");
        Long diffTime = System.nanoTime() - startTime;
        CustomLog.log(diffTime.toString(),"MASTER");
    }


}
