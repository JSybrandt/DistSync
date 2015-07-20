/**
 * Created by jsybrand on 6/25/15.
 */

import javafx.util.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Manager extends Thread {

    ArrayList<JobSender> senders = new ArrayList<>();
    ArrayList<Socket> sockets = new ArrayList<>();
    ConcurrentHashMap<Socket,Constants.State> connectionStatus = new ConcurrentHashMap<>();
    Job[] jobs;

    //each connected device has a list of start,end times
    ConcurrentHashMap<Socket,ArrayList<JobTiming>> startEndMapping = new ConcurrentHashMap<>();

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

    public void getConnections() throws IOException
    {
        ServerSocket listener = new ServerSocket(Constants.PORT);
        listener.setSoTimeout(2000);

        while(!listener.isClosed())
        {
            try {
                Socket s = listener.accept();
                sockets.add(s);
                connectionStatus.put(s, Constants.State.NOT_STARTED);
                System.out.println("Made Connection with " + s.getLocalAddress().toString());
            }catch(SocketTimeoutException e){listener.close();/*who cares*/}
        }

        for(Socket s : sockets)
            startEndMapping.put(s,new ArrayList<JobTiming>());
    }

    //returns true if finished
    public boolean checkIsFinished(){
        boolean hasAliveSockets=false;
        for(Socket s : sockets)
        {
            if(connectionStatus.get(s)!= Constants.State.ERROR) {
                hasAliveSockets = true;
                break;
            }
        }
        if(hasAliveSockets) {
            for (Job j : jobs) {
                if (j.state == Constants.State.NOT_STARTED)
                    return false;
            }
            for (JobSender s : senders) {
                if (s.job.state != Constants.State.FINISHED
                        && s.job.state != Constants.State.ERROR) {
                    return false;
                }
            }
        }
        else {
            System.err.println("All connections dead.");
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
                if(j.getType() == Job.Type.CREATE_DIR)
                    maySelectCreateFiles = false;
                else if(j.getType() == Job.Type.RM_FILES)
                    maySelectRemoveDirectories=false;

                if(j.state==Constants.State.NOT_STARTED)
                {
                    if(j.getType() == Job.Type.CREATE_FILES) {
                        if (maySelectCreateFiles) {
                            return j;
                        }
                    }
                    else if(j.getType() == Job.Type.RM_DIR) {
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
        try {
            System.out.println("Making Connections:");
            getConnections();
            //System.out.println("Reading and Splitting Jobs.");

            //jobSplitter.join();
            //System.out.println("Getting Results:");
            //jobs = jobSplitter.getResults();
            //GET JOB FILES FROM JOBS FOLDER

            String[] strs = new File(Constants.JOB_DIR).list();
            jobs = new Job[strs.length];
            for(int i = 0; i<strs.length;i++){
                jobs[i] = new Job(strs[i]);
            }
            Arrays.sort(jobs);

            while (!checkIsFinished())
            {
                Job j = selectNextJob();
                if(j!=null) {
                    //System.out.println("Selected " + j);
                    for (Socket s : sockets) {
                        if (connectionStatus.get(s) == Constants.State.NOT_STARTED
                                || connectionStatus.get(s) == Constants.State.FINISHED) {
                            JobSender sender = new JobSender(s, j, this);
                            senders.add(sender);
                            j.state = Constants.State.ASSIGNED;
                            connectionStatus.put(s, Constants.State.ASSIGNED);
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
        Long endTime = System.nanoTime();
        try {

            String logResults="MASTER,"+startTime+","+endTime+"\n";
            for(Socket s  : sockets)
            {
                for(JobTiming t : startEndMapping.get(s))
                {
                    logResults += s.getInetAddress().getCanonicalHostName()+","+t.startTime + "," + t.endTime +","+ t.jobName +"\n";
                }
            }


            CustomLog.log(logResults, Constants.LOG_DIR + "MASTER.log");
        }catch(IOException e){System.err.println("Log failed");}

        CustomLog.close();
    }

}
