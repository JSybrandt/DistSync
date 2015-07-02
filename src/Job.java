import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;

/**
 * Created by jsybrand on 6/29/15.
 */
public class Job implements Serializable, Comparable {

    //Job type is determined by file name
    public enum Type{
        CREATE_DIR,     //C
        RM_DIR,         //R
        CREATE_FILES,   //A
        RM_FILES,       //D
        MODIFY_FILES,   //M
        OTHER
    }

    public int getTypeVal(Type t)
    {
        switch (t)
        {
            case CREATE_DIR:    return 1;
            case RM_DIR:        return 2;
            case CREATE_FILES:  return 2;
            case RM_FILES:      return 1;
            case MODIFY_FILES:  return 1;
            case OTHER:         return 3;
            default:            return 3;
        }
    }

    public Job(String s) throws IOException{

        fileName = s;
        path = Paths.get("").toAbsolutePath().toString() + "/" + Constants.JOB_DIR+fileName;
        state = Constants.State.NOT_STARTED;
        type = determineType(fileName);

    }

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
            case 'L'://we are going to work on the assumption that the links can be run the same way
                return  Type.MODIFY_FILES;
            default:
                return Type.OTHER;
        }
    }

    public String fileName, path;
    public Constants.State state;
    public Type type;

    public static String upToDateMountPoint = "datCurr/";
    public static String outOfDateMountPoint = "datOld/";


    public int compareTo(Object o)
    {
        return Integer.compare(getTypeVal(type),getTypeVal(((Job)o).type));
    }

    public String toString(){return fileName+"\t"+type;}
}
