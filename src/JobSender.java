import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by jsybrand on 6/30/15.
 */
public class JobSender extends Thread{
    public Socket socket;
    public Job job;
    public String ID;



    private Manager manager;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private JobAgreementProtocol protocol;
    private Long startTime,endTime;

    JobSender(Socket s, Job j, Manager m) throws IOException
    {
        manager = m;
        job = j;
        socket = s;
        protocol = new JobAgreementProtocol(j);
        ID = socket.getInetAddress().getCanonicalHostName();
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void finalize() throws Throwable{
        try {
            in.close();
            out.close();
            super.finalize();
        }
        catch(Exception e)
        {
            System.err.println(ID+": Server connection failed to close.");
        }
    }

    @Override
    public void run(){
        try{
            startTime = System.nanoTime();
            String msg = "";
            while(job.state!= Constants.State.FINISHED && job.state!= Constants.State.ERROR)
            {
                JobAgreementProtocol.Action action = protocol.processInput(msg);
                if(action== JobAgreementProtocol.Action.SEND_JOB){
                    out.writeObject(job);
                    System.out.println("Sent " + ID + " " + job.fileName);
                }
                if(job.state!= Constants.State.FINISHED && job.state!= Constants.State.ERROR){
                    Object obj = in.readObject();
                    if(obj instanceof IOException) {
                        System.out.println("Error Received:");
                        ((IOException) obj).printStackTrace();
                        msg = "ERROR";
                        job.state = Constants.State.ERROR;
                    }
                    else msg = (String) obj;
                }

            }
        }
        catch (Exception e)
        {
            System.err.println(ID+": IO Failed " + e);
            job.state = Constants.State.ERROR;
        }
        finally {
                //manager.connectionStatus.put(socket, job.state);
            //DIABLED ERROR
            manager.connectionStatus.put(socket, Constants.State.FINISHED);
            endTime = System.nanoTime();
            manager.startEndMapping.get(socket).add(new JobTiming(ID,job.fileName,startTime,endTime));
        }
    }
}
