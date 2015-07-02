import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by jsybrand on 6/30/15.
 */
public class JobSender extends Thread{
    private Socket socket;
    public Job job;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    public JobAgreementProtocol protocol;
    public int ID;
    Manager manager;

    JobSender(Socket s, Job j, int id,Manager m) throws IOException
    {
        manager = m;
        job = j;
        socket = s;
        protocol = new JobAgreementProtocol();
        ID = id;
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
            System.err.println("Server connection failed to close.");
        }
    }

    @Override
    public void run(){
        try{

            String msg = "";
            while(protocol.state!= Constants.State.FINISHED && protocol.state!= Constants.State.ERROR)
            {
                JobAgreementProtocol.Action action = protocol.processInput(msg);
                if(action== JobAgreementProtocol.Action.SEND_JOB){
                    out.writeObject(job);
                }
                //System.out.println(ID + ":" + protocol.state);
                if(protocol.state== Constants.State.ERROR)
                    System.err.println(ID+"ERR Occured.");
                job.state = protocol.state;
                if(protocol.state!= Constants.State.FINISHED && protocol.state!= Constants.State.ERROR){
                    Object obj = in.readObject();
                    if(obj instanceof IOException) {
                        System.out.println("Error Received:");
                        ((IOException) obj).printStackTrace();
                    }
                    else msg = (String) obj;
                }

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
            manager.connectionStatus.put(socket, false);
        }
    }
}
