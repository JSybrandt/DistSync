/**
 * Created by jsybrand on 6/25/15.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Manager {

    final String IDLE = "IDLE";
    final String DONE = "DONE";
    final String WORKING = "WORKING";
    final String ERR = "ERR";

    ServerSocket listener;
    ConcurrentHashMap<Integer,String> statuses = new ConcurrentHashMap<>();
    int maxConnectionID = 0;
    Manager() throws IOException
    {
        listener = new ServerSocket(NetConstants.PORT);
    }

    public void start() throws IOException
    {
        while(true)
        {
            for(int id = 0 ; id < maxConnectionID; id++) {
                if(statuses.get(id).equals(ERR))
                {
                    //return job to the pool
                }
            }

            try {
                new JobSender(listener.accept(), "TMEP",maxConnectionID).start();
                maxConnectionID++;
            }
            finally {
                listener.close();
            }
        }
    }

    private class JobSender extends Thread
    {
        private Socket socket;
        private String job;
        private int connectionID;
        JobSender(Socket s, String j, int id)
        {
            connectionID = id;
            job = j;
            socket = s;
            System.out.println("Made Connection.");
            statuses.put(connectionID, IDLE);
        }

        @Override
        public void run(){
            try{
                statuses.put(connectionID,WORKING);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

                out.println(job);

                while(DONE.equals(in.readLine())){}
            }
            catch (Exception e)
            {
                System.err.println("IO Failed");
                statuses.put(connectionID, ERR);
            }
            finally {
                try {
                    socket.close();
                }
                catch (Exception e)
                {
                    System.err.println("Failed to close socket.");
                    statuses.put(connectionID, ERR);
                }
            }
            statuses.put(connectionID,DONE);
        }
    }


}
