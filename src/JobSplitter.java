import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by jsybrand on 6/29/15.
 */
public class JobSplitter extends Thread{

    private final double MAX_JOB_WEIGHT = 100;

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
            try {
                for (int i = 0; i < s.length; i++) {
                    initialJobs[i] = new Job(s[i]);
                    evalAndSplitJob(initialJobs[i]); //populates finalJobs
                }
            } catch (IOException e) {
                System.err.println("Invalid input format");
            }
        }
    }

    private void evalAndSplitJob(Job j) throws IOException
    {
        //there are certain jobs we WONT split
        if(j.getType()== Job.Type.CREATE_DIR || j.getType()== Job.Type.RM_DIR)
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
            int size = in.nextInt();

            switch (j.getType()){
                case CREATE_DIR:    currWeight+=size*CP_DIR_WEIGHT + CP_DIR_LINE_WEIGHT; break;
                case RM_DIR:        currWeight+=size*RM_DIR_WEIGHT + RM_DIR_LINE_WEIGHT; break;
                case CREATE_FILES:  currWeight+=size*CP_FILE_WEIGHT + CP_FILE_LINE_WEIGHT; break;
                case RM_FILES:     currWeight+=size*RM_FILE_WEIGHT + RM_FILE_LINE_WEIGHT; break;
                case MODIFY_FILES: currWeight+=size*SYNC_FILE_WEIGHT + SYNC_FILE_LINE_WEIGHT; break;
                default:            currWeight+=size+1;
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
