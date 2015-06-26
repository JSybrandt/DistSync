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
            socket = new Socket(serverAddress, NetConstants.PORT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);

        }
        catch(Exception e)
        {
            System.err.println("Failed to connect to server.");
        }
        finally
        {
            try {
                socket.close();
            }
            catch(Exception e) {
                System.err.println("Failed to close socket.");
            }
        }
    }

    @Override
    public void run()
    {
        String jobFile = in.readLine();
    }
}
