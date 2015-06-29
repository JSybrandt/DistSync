import java.io.Serializable;

/**
 * Created by jsybrand on 6/29/15.
 */
public class Job implements Serializable {

    public enum Type{
        CREATE_DIR,     //C
        RM_DIR,         //R
        CREATE_FILES,   //A
        RM_FILES,       //D
        MODIFY_FILES,   //M
        OTHER
    }

    public Job(String s){path = s; state = JobAgreementProtocol.State.NOT_STARTED; type = determineType(path);}

    public Type determineType(String path)
    {
        switch (path.charAt(0))
        {
            case 'C':
                return Type.CREATE_DIR;
            case 'R':
                return Type.RM_DIR;
            case 'A':
                return Type.CREATE_FILES;
            case 'D':
                return Type.RM_FILES;
            case 'M':
                return  Type.MODIFY_FILES;
            default:
                return Type.OTHER;
        }
    }

    public String path;
    public JobAgreementProtocol.State state;
    public Type type;
}
