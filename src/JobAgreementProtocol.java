import java.io.IOException;

/**
 * Created by jsybrand on 6/26/15.
 */
public class JobAgreementProtocol {


    String pathToJobFile;
    Constants.State state = Constants.State.NOT_STARTED;

    JobAgreementProtocol(String path)
    {
        pathToJobFile=path;
    }

    //the server will send a job, the client will say that job has begun,
    //the server has the option to stop the job, the client will report the job is done, or the job is in error

    public String processInput(String input) throws IOException {
        if (state == Constants.State.NOT_STARTED) {
            state = Constants.State.MADE_CONTACT;
            return pathToJobFile;
            //start communication by sending the client the job file
        }
        else if (state == Constants.State.MADE_CONTACT && input.equals("STARTED")){
                state = Constants.State.RUNNING;
                return "";
        }
        else if (state == Constants.State.RUNNING && input.equals("REQ_HELP")){
            return "TBD";
        }
        else if (state == Constants.State.RUNNING && input.equals("FINISHED")){
            state = Constants.State.FINISHED;
            return "";
        }
        else if (state == Constants.State.FINISHED && input.equals("RESTART")){
            state = Constants.State.RUNNING;
            return pathToJobFile;
        }
        else if (state == Constants.State.RUNNING && input.equals("ERROR")){
            state = Constants.State.ERROR;
            return "";
        }
        else
            throw new IOException("Protocol not followed. On State: " +state+ " Received: " + input);
    }


}
