/**
 * Created by jsybrand on 6/25/15.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class Manager extends Thread {

    ServerSocket listener;
    ArrayList<JobSender> jobs = new ArrayList<>();

    int maxConnectionID = 0;
    Manager() throws IOException
    {
            listener = new ServerSocket(NetConstants.PORT);
            listener.setSoTimeout(5000);
    }

    public void safeClose(){
        try {
            for(JobSender job : jobs) {
                job.join();
                job.safeClose();
            }
            listener.close();
        }
        catch(Exception e){
            System.err.println("Sever failed to close.");
        }
    }

    @Override
    public void run()
    {
        try {
            while (!listener.isClosed())
            {
                try {
                    System.out.println("Server is waiting...");
                    JobSender j  = new JobSender(listener.accept(), "Job #"+maxConnectionID);
                    jobs.add(j);
                    j.start();
                    System.out.println("Made Connection");
                    maxConnectionID++;
                }
                catch(SocketTimeoutException e){
                    System.err.println("Server timed out.");
                    listener.close();
                }
                finally {
                    //listener.close();
                }
            }
            for(JobSender j : jobs)
            {
                j.join();
            }

            for(JobSender j : jobs){
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
        private String job;
        BufferedReader in;
        PrintWriter out;
        public JobAgreementProtocol protocol;
        JobSender(Socket s, String j) throws IOException
        {
            job = j;
            socket = s;
            protocol = new JobAgreementProtocol(job);

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
