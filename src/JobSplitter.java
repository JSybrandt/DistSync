import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by jsybrand on 6/29/15.
 */
public class JobSplitter extends Thread{

    private final double MAX_JOB_WEIGHT = 10000;

    private final double RM_FILE_WEIGHT=0.1;
    private final double CP_FILE_WEIGHT=1;
    private final double RM_DIR_WEIGHT=0.05;
    private final double CP_DIR_WEIGHT=0.15;
    private final double SYNC_FILE_WEIGHT=0.5;

    private final double RM_FILE_LINE_WEIGHT=1;
    private final double CP_FILE_LINE_WEIGHT=1;
    private final double RM_DIR_LINE_WEIGHT=1;
    private final double CP_DIR_LINE_WEIGHT=1;
    private final double SYNC_FILE_LINE_WEIGHT=1;



    //will find all of the job files, give each one a weight, and split large jobs unto smaller ones
    JobSplitter(){}

    private Job initialJobs[];
    private ArrayList<Job> finalJobs = new ArrayList<>();

    public Job[] getResults(){
        Job [] j = finalJobs.toArray(new Job[finalJobs.size()]);
        Arrays.sort(j);
        return j;
    }

    @Override
    public void run(){
        //we'll figure out what to do later
        String[] s = new File(Constants.JOB_DIR).list();
        if(s!=null && s.length > 0) {
            initialJobs = new Job[s.length];
            for (int i = 0; i < s.length; i++) {
                try {

                    initialJobs[i] = new Job(s[i]);
                    evalAndSplitJob(initialJobs[i]); //populates finalJobs

                } catch (Exception e) {
                    System.err.println("Invalid input format");
                    e.printStackTrace();
                    System.err.println("Job:" + initialJobs[i]);
                }
            }
        }
    }

    private void evalAndSplitJob(Job j) throws IOException
    {
        //there are certain jobs we WONT split
        if(j.getType()== Job.Type.CREATE_DIR || j.getType()== Job.Type.RM_DIR || j.getType()== Job.Type.MODIFY_FILES)
        {
            finalJobs.add(j);
            return;
        }

        double currWeight = MAX_JOB_WEIGHT+100; //we will make a new job the first iteration
        int currFileID = 0;
        Scanner in = new Scanner(new File(j.path));
        PrintWriter out=null;

        while(in.hasNext())
        {
            if(currWeight > MAX_JOB_WEIGHT)
            {
                currWeight = 0;
                if(out!=null) {
                    out.close();
                    finalJobs.add(new Job(j.fileName + currFileID));
                }
                currFileID++;
                out = new PrintWriter(j.path+currFileID);
            }

            String file = in.next();
            BigInteger size = in.nextBigInteger();

            switch (j.getType()){
                case CREATE_DIR:    currWeight+=size.doubleValue()*CP_DIR_WEIGHT + CP_DIR_LINE_WEIGHT; break;
                case RM_DIR:        currWeight+=size.doubleValue()*RM_DIR_WEIGHT + RM_DIR_LINE_WEIGHT; break;
                case CREATE_FILES:  currWeight+=size.doubleValue()*CP_FILE_WEIGHT + CP_FILE_LINE_WEIGHT; break;
                case RM_FILES:     currWeight+=size.doubleValue()*RM_FILE_WEIGHT + RM_FILE_LINE_WEIGHT; break;
                case MODIFY_FILES: currWeight+=size.doubleValue()*SYNC_FILE_WEIGHT + SYNC_FILE_LINE_WEIGHT; break;
                default:            currWeight+=size.doubleValue()+1;
            }

            out.println(file + "\t" + size);

        }
        if(out!=null){
            out.close();
            finalJobs.add(new Job(j.fileName + currFileID));
        }

        in.close();
    }

}
