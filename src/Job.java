import java.io.Serializable;

/**
 * Created by jsybrand on 6/29/15.
 */
public class Job implements Serializable {

    //Job type is determined by file name
    public enum Type{
        CREATE_DIR,     //C
        RM_DIR,         //R
        CREATE_FILES,   //A
        RM_FILES,       //D
        MODIFY_FILES,   //M
        OTHER
    }

    public Job(String s){fileName = s; state = Constants.State.NOT_STARTED; type = determineType(fileName);}

    //job type is determined by file name
    public Type determineType(String fileName)
    {
        switch (fileName.charAt(0))
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

    public String fileName;
    public Constants.State state;
    public Type type;
}
