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
        MODIFY_DIRS,    //Y
        BUILD_LINKS,    //L
        OTHER
    }



    public Job(String s) throws IOException{

        fileName = s;
        path = Constants.JOB_DIR+fileName;
        logFile = Constants.LOG_DIR+fileName+".log";
        state = Constants.State.NOT_STARTED;
        type = determineType(fileName);

    }

    //job type is determined by file name
    private static Type determineType(String fileName)
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
                return Type.MODIFY_FILES;
            case 'L':
                return  Type.BUILD_LINKS;
            case 'Y':
                    return Type.MODIFY_DIRS;
            default:
                return Type.OTHER;
        }
    }

    public String fileName, path, logFile;
    public Constants.State state;
    private Type type;
    public Type getType(){return type;}


    //these are filled in using command line arguments from main
    public static String upToDateMountPoint;
    public static String outOfDateMountPoint;
    public static String upToDateDevice;
    public static String outOfDateDevice;



    public int compareTo(Object o)
    {
        return Integer.compare(getTypeVal(type),getTypeVal(((Job)o).type));
    }

    public String toString(){return fileName;}

    private int getTypeVal(Type t)
    {
        switch (t)
        {
            case CREATE_DIR:    return 0;
            case RM_DIR:        return 2;
            case CREATE_FILES:  return 2;
            case RM_FILES:      return 1;
            case MODIFY_FILES:  return 1;
            case MODIFY_DIRS:   return 1;
            case BUILD_LINKS:   return 0;
            case OTHER:         return 3;
            default:            return 3;
        }
    }
}
