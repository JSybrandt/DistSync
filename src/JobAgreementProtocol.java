import java.io.IOException;

/**
 * Created by jsybrand on 6/26/15.
 */
public class JobAgreementProtocol {

    public enum State{
        NOT_STARTED,
        ASSIGNED,
        MADE_CONTACT,
        RUNNING,
        FINISHED,
        ERROR
    }
    String pathToJobFile;
    State state = State.NOT_STARTED;

    JobAgreementProtocol(String path)
    {
        pathToJobFile=path;
    }

    //the server will send a job, the client will say that job has begun,
    //the server has the option to stop the job, the client will report the job is done, or the job is in error

    public String processInput(String input) throws IOException {
        if (state == State.NOT_STARTED) {
            state = State.MADE_CONTACT;
            return pathToJobFile;
            //start communication by sending the client the job file
        }
        else if (state == State.MADE_CONTACT && input.equals("STARTED")){
                state = State.RUNNING;
                return "";
        }
        else if (state == State.RUNNING && input.equals("FINISHED")){
            state = State.FINISHED;
            return "";
        }
        else if (state == State.RUNNING && input.equals("ERROR")){
            state = State.ERROR;
            return "";
        }
        else
            throw new IOException("Protocol not followed. Received " + input);
    }


}
