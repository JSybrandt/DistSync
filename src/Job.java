import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Scanner;

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
        path = Constants.JOB_DIR+fileName;
        state = Constants.State.NOT_STARTED;
        type = determineType(fileName);
        weight = calculateWeight();

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
                return  Type.MODIFY_FILES;
            default:
                return Type.OTHER;
        }
    }

    public String fileName, path;
    public Constants.State state;
    public Type type;
    public double weight;
    public String upToDateMountPoint = "./datCurr/";
    public String outOfDateMountPoint = "./datOld/";

    private final double RM_FILE_WEIGHT=0.1;
    private final double CP_FILE_WEIGHT=1;
    private final double RM_DIR_WEIGHT=0.05;
    private final double CP_DIR_WEIGHT=0.15;
    private final double SYNC_FILE_WEIGHT=0.5;

    private double calculateWeight() throws IOException{
        double res = 0;
        Scanner scan = new Scanner(new File(path));
        while (scan.hasNext()) {
            //int size = scan.nextInt(); at the moment scans do not have size outputted
            scan.nextLine();
            double size = 0.0542488; //size in gb of typical file
            switch (type)
            {
                case CREATE_DIR:    res+=CP_DIR_WEIGHT*size;        break;
                case RM_DIR:        res+=RM_DIR_WEIGHT*size;        break;
                case CREATE_FILES:  res+=CP_FILE_WEIGHT*size;       break;
                case RM_FILES:      res+=RM_FILE_WEIGHT*size;       break;
                case MODIFY_FILES:  res+=SYNC_FILE_WEIGHT*size;     break;
                case OTHER:         res+=size;                      break;
            }
        }
        return weight;
    }
    public int compareTo(Object o)
    {
        return Integer.compare(getTypeVal(type),getTypeVal(((Job)o).type));
    }

    public String toString(){return fileName+"\t"+type+"\t"+weight;}
}
