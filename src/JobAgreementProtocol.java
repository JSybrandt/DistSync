import java.io.IOException;

/**
 * Created by jsybrand on 6/26/15.
 */
public class JobAgreementProtocol {

    public enum Action{
        DO_NOTHING,
        SEND_JOB
    }

    JobAgreementProtocol(Job j){job = j;}
    private Job job;


    //jobs have state, protocols act on job state
    public Constants.State getState(){return job.state;}
    public void setState(Constants.State s){job.state=s;}

    //Constants.State state = Constants.State.NOT_STARTED;


    //the server will send a job, the client will say that job has begun,
    //the server has the option to stop the job, the client will report the job is done, or the job is in error

    public Action processInput(String input) throws IOException {
        if (getState() == Constants.State.NOT_STARTED) {
            setState(Constants.State.MADE_CONTACT);
            return Action.SEND_JOB;
            //start communication by sending the client the job file
        }
        else if (getState() == Constants.State.MADE_CONTACT && input.equals("STARTED")){
            setState(Constants.State.RUNNING);
                return Action.DO_NOTHING;
        }
        else if (getState() == Constants.State.RUNNING && input.equals("REQ_HELP")){
            return Action.DO_NOTHING;
        }
        else if (getState() == Constants.State.RUNNING && input.equals("FINISHED")){
            setState(Constants.State.FINISHED);
            return Action.DO_NOTHING;
        }
        else if (getState() == Constants.State.FINISHED && input.equals("RESTART")){
            setState(Constants.State.RUNNING);
            return Action.SEND_JOB;
        }
        else if (getState() == Constants.State.RUNNING && input.equals("ERROR")){
            setState(Constants.State.ERROR);
            return Action.DO_NOTHING;
        }
        else
            throw new IOException("Protocol not followed. On State: " +getState()+ " Received: " + input);
    }


}
