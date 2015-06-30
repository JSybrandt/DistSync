import java.io.*;
import java.net.Socket;

/**
 * Created by jsybrand on 6/25/15.
 */
public class Worker extends Thread {
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
    }

    @Override
    public void finalize(){
        try {
            System.out.println("Finalize Called");
            in.close();
            out.close();
            socket.close();
        }
        catch(Exception e)
        {
            System.err.println("Worker failed to close.");
        }
    }

    @Override
    public void run()
    {
        try {
            while(true) {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                Job received = (Job)in.readObject();
                System.out.println("Client Received:" + received);
                if (received==null) {
                    return;
                }
                out.writeObject("STARTED");
                sleep(1000);
                //Random rand = new Random();
                //if(rand.nextInt(100)<20)
                //    out.println("ERROR");
                //else
                out.writeObject("FINISHED");
            }
        }
        catch(Exception e){
            System.err.println("Client Failed to listen :(" + e);
        }

    }
}
