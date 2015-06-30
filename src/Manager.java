/**
 * Created by jsybrand on 6/25/15.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Manager extends Thread {

    ArrayList<JobSender> senders = new ArrayList<>();
    ArrayList<Socket> sockets = new ArrayList<>();
    ConcurrentHashMap<Socket,Boolean> connectionStatus = new ConcurrentHashMap<>();//true represents running
    ArrayList<String> workerURLs; // to be used to spawn on run
    Job[] jobs;

    Manager(ArrayList<String> workerURLs) throws IOException
    {
        setUpDirStructure();
        jobs = getJobFiles();
        this.workerURLs = workerURLs;
    }

    public void setUpDirStructure() throws IOException
    {
        new File(Constants.TEMP_DIR).mkdir();
        new File(Constants.JOB_DIR).mkdir();
        new File(Constants.JOB_DIR + "Cd1.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd2.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd3.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd4.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd5.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd6.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd7.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd8.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd9.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd10.temp").createNewFile();
        new File(Constants.JOB_DIR + "Cd11.temp").createNewFile();
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
        System.out.println("Removing: "+file.getName());
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

    public Job[] getJobFiles()
    {
        String[] s = new File(Constants.JOB_DIR).list();
        Job [] j = new Job[s.length];
        for(int i = 0 ; i< s.length;i++)
        {
            j[i] = new Job(s[i]);
        }
        return j;
    }

    public void getConnections() throws IOException
    {
        ServerSocket listener = new ServerSocket(Constants.PORT);
        listener.setSoTimeout(2000);

        if(workerURLs != null)
        {
            //spawn workers using ssh to start main.java on remotes
        }
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

    @Override
    public void run()
    {
        int cons = 0;
        try {
            getConnections();

            while (!checkIsFinished())
            {
                for(Job j : jobs) {
                    if(j.state== Constants.State.NOT_STARTED) {
                        for(Socket s : sockets)
                        {
                            if(!connectionStatus.get(s))
                            {
                                cons++;
                                JobSender sender = new JobSender(s,j,cons);
                                senders.add(sender);
                                j.state = Constants.State.ASSIGNED;
                                connectionStatus.put(s,true);
                                sender.start();
                                break;
                            }
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
                }
            }
        }
        catch(Exception e ){
            //DoNothing
        }
        for(Socket s : sockets)
        {
            try {
                new PrintWriter(s.getOutputStream(), true).println("QUIT");
            }
            catch(IOException e)
            {System.out.println("Failed to send quit to " + s.getLocalAddress());}
        }
        deletePath(new File(Constants.TEMP_DIR));
    }

    private class JobSender extends Thread
    {
        private Socket socket;
        private Job job;
        BufferedReader in;
        PrintWriter out;
        public JobAgreementProtocol protocol;
        public int ID;
        JobSender(Socket s, Job j, int id) throws IOException
        {
            job = j;
            socket = s;
            protocol = new JobAgreementProtocol(job.fileName);
            ID = id;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);

        }

        @Override
        public void finalize(){
            try {
                in.close();
                out.close();
            }
            catch(Exception e)
            {
                System.err.println("Server connection failed to close.");
            }
        }

        @Override
        public void run(){
            try{
                String msg = "";
                while(protocol.state!= Constants.State.FINISHED && protocol.state!= Constants.State.ERROR)
                {
                    String s = protocol.processInput(msg);
                    if(s!=""){
                        out.println(s);
                    }
                    System.out.println(ID + ":" + protocol.state);
                    job.state = protocol.state;
                    if(protocol.state!= Constants.State.FINISHED)
                        msg = in.readLine();
                }
            }
            catch (Exception e)
            {
                System.err.println("IO Failed " + e);
            }
            finally {
                //try {
                    //socket.close();
                //}
                //catch (Exception e)
                //{
                    //System.err.println("Failed to close socket.");
                //}
                connectionStatus.put(socket,false);
            }
        }

    }


}
