import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by jsybrand on 6/25/15.
 */
public class Worker extends Thread {
    BufferedReader in;
    PrintWriter out;
    Socket socket;

    Worker(String serverAddress)
    {
        try {
            socket = new Socket(serverAddress, Constants.PORT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);

        }
        catch(Exception e)
        {
            System.err.println("Worker failed to connect to server.");
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
                String received = in.readLine();
                System.out.println("Client Received:" + received);
                if (received.equals("QUIT")) {
                    return;
                }
                out.println("STARTED");
                sleep(1000);
                //Random rand = new Random();
                //if(rand.nextInt(100)<20)
                //    out.println("ERROR");
                //else
                out.println("FINISHED");
            }
        }
        catch(Exception e){
            System.err.println("Client Failed to listen :(" + e);
        }

    }
}
