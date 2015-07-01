import java.io.*;
import java.util.ArrayList;
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
    ArrayList<Job> finalJobs = new ArrayList<>();
    @Override
    public void run(){
        //we'll figure out what to do later
        String[] s = new File(Constants.JOB_DIR).list();
        initialJobs = new Job[s.length];
        try {
            for (Job j : initialJobs)
                evalAndSplitJob(j); //populates finalJobs
        }catch (IOException e){
            System.err.println("Invalid input format");
        }
    }

    private void evalAndSplitJob(Job j) throws IOException
    {
        double currWeight =9999; //we will make a new job the first iteration
        int currFileID = 0;
        Scanner in = new Scanner(new File(j.path));
        PrintWriter out;

        while(in.hasNext())
        {
            if(currWeight > MAX_JOB_WEIGHT)
            {
                currWeight = 0;
                if(out!=null) {
                    out.close();
                    finalJobs.add(new Job(j.path+currFileID));
                }
                currFileID++;
                out = new PrintWriter(j.path+currFileID);
            }

            String file = in.next();
            int size = in.nextInt();

            switch (j.type){
                case CREATE_DIR:    currWeight+=size*CP_DIR_WEIGHT + CP_DIR_LINE_WEIGHT; break;
                case RM_DIR:        currWeight+=size*RM_DIR_WEIGHT + RM_DIR_LINE_WEIGHT; break;
                case CREATE_FILES:  currWeight+=size*CP_DIR_WEIGHT + CP_DIR_LINE_WEIGHT; break;
                case RM_FILES:     currWeight+=size*RM_FILE_WEIGHT + RM_FILE_LINE_WEIGHT; break;
                case MODIFY_FILES: currWeight+=size*SYNC_FILE_WEIGHT + SYNC_FILE_LINE_WEIGHT; break;
                default:            currWeight+=size+1;
            }

            out.println(file);

        }
    }

}
