/**
 * Created by jsybrand on 6/25/15.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class Manager extends Thread {

    ServerSocket listener;
    ArrayList<JobSender> senders = new ArrayList<>();
    Job[] jobs;
    public final String TEMP_DIR = "./tmp/";
    public final String JOB_DIR = TEMP_DIR + "jobs/";

    int maxConnectionID = 0;


    Manager() throws IOException
    {
        listener = new ServerSocket(NetConstants.PORT);
        listener.setSoTimeout(5000);
        setUpDirStructure();
        jobs = getJobFiles();
    }


    public void setUpDirStructure() throws IOException
    {
        new File(TEMP_DIR).mkdir();
        new File(JOB_DIR).mkdir();
        new File(JOB_DIR + "cd.temp").createNewFile();
    }

    public void deletePath(File file)
    {
        if(file.isDirectory())
        {
            File children[] = file.listFiles();
            for (File f : children) {
                deletePath(f);
            }
        }

        file.delete();

    }

    public void safeClose(){
        try {

            deletePath(new File(TEMP_DIR));

            for(JobSender sender : senders) {
                sender.join();
                sender.safeClose();
            }
            listener.close();
        }
        catch(Exception e){
            System.err.println("Sever failed to close.");
        }
    }

    public Job[] getJobFiles()
    {
        String[] s = new File(JOB_DIR).list();
        Job [] j = new Job[s.length];
        for(int i = 0 ; i< s.length;i++)
        {
            j[i] = new Job(s[i]);
        }
        return j;
    }

    @Override
    public void run()
    {
        try {
            boolean newJobsRunning = true;
            while (newJobsRunning && !listener.isClosed())
            {
                newJobsRunning = false;
                for(Job j : jobs) {
                    if(j.state== JobAgreementProtocol.State.NOT_STARTED) {
                        try {
                            newJobsRunning = true;
                            System.out.println("Server is waiting...");
                            JobSender s = new JobSender(listener.accept(), j);
                            senders.add(s);
                            j.state= JobAgreementProtocol.State.ASSIGNED;
                            s.start();
                            System.out.println("Made Connection");
                            maxConnectionID++;
                        } catch (SocketTimeoutException e) {
                            System.err.println("Server timed out.");
                            listener.close();
                        } finally {
                            //listener.close();
                        }
                    }
                }
            }
            for(JobSender j : senders)
            {
                j.join();
            }

            for(JobSender j : senders){
                if(j.protocol.state == JobAgreementProtocol.State.ERROR)
                {
                    System.err.println(j.job + " FAILED TO COMPLETE.");
                }
            }
        }
        catch(Exception e ){
            //DoNothing
        }

    }

    private class JobSender extends Thread
    {
        private Socket socket;
        private Job job;
        BufferedReader in;
        PrintWriter out;
        public JobAgreementProtocol protocol;
        JobSender(Socket s, Job j) throws IOException
        {
            job = j;
            socket = s;
            protocol = new JobAgreementProtocol(job.path);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);

        }

        void safeClose(){
            try {
                in.close();
                out.close();
                socket.close();
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
                while(protocol.state!= JobAgreementProtocol.State.FINISHED && protocol.state!= JobAgreementProtocol.State.ERROR)
                {
                    String s = protocol.processInput(msg);
                    if(s!=""){
                        out.println(s);
                    }
                    System.out.println(protocol.state);
                    job.state = protocol.state;
                    msg = in.readLine();
                }
                safeClose();
            }
            catch (Exception e)
            {
                System.err.println("IO Failed " + e);
            }
            finally {
                try {
                    socket.close();
                }
                catch (Exception e)
                {
                    System.err.println("Failed to close socket.");
                }
            }
        }

    }


}
