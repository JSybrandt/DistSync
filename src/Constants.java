/**
 * Created by jsybrand on 6/25/15.
 */
public abstract class Constants {
    public static final int PORT = 31415;
    public static final String TEMP_DIR = "tmp/";
    public static final String JOB_DIR = TEMP_DIR + "jobs/";
    public static final String LOG_DIR = TEMP_DIR + "logs/";
    public enum State{
        NOT_STARTED,
        ASSIGNED,
        MADE_CONTACT,
        RUNNING,
        FINISHED,
        ERROR
    }

    public static final String JAR_FILE_NAME = "distsync.jar";


}
