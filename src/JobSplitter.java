import sun.security.util.BigInt;

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
public class JobSplitter extends Thread {

    private final double MAX_JOB_WEIGHT = 1e10;

    private final double RM_FILE_WEIGHT = 0.1;
    private final double CP_FILE_WEIGHT = 1;
    private final double RM_DIR_WEIGHT = 0.05;
    private final double CP_DIR_WEIGHT = 0.15;
    private final double SYNC_FILE_WEIGHT = 0.5;

    private final double RM_FILE_LINE_WEIGHT = 1;
    private final double CP_FILE_LINE_WEIGHT = 1;
    private final double RM_DIR_LINE_WEIGHT = 1;
    private final double CP_DIR_LINE_WEIGHT = 1;
    private final double SYNC_FILE_LINE_WEIGHT = 1;


    //will find all of the job files, give each one a weight, and split large jobs unto smaller ones
    JobSplitter() {
    }

    private Job initialJobs[];
    private ArrayList<Job> finalJobs = new ArrayList<>();

    public Job[] getResults() {
        Job[] j = finalJobs.toArray(new Job[finalJobs.size()]);
        Arrays.sort(j);
        return j;
    }

    @Override
    public void run() {
        //we'll figure out what to do later
        String[] s = new File(Constants.JOB_DIR).list();
        EvalJob[] evJs = new EvalJob[s.length];
        if (s != null && s.length > 0) {
            initialJobs = new Job[s.length];
            for (int i = 0; i < s.length; i++) {
                try {

                    initialJobs[i] = new Job(s[i]);
                    evJs[i] = new EvalJob(initialJobs[i]); //populates finalJobs
                    evJs[i].start();
                } catch (Exception e) {
                    System.err.println("Invalid input format");
                    e.printStackTrace();
                    System.err.println("Job:" + initialJobs[i]);
                }
            }
            for(EvalJob ej : evJs)
            {
                try {
                    System.out.println("Waiting for " + ej.job);
                    ej.join();
                    if(ej.exception != null) throw ej.exception;
                }catch(Exception e){e.printStackTrace();}
            }
        }
    }

    private class EvalJob extends Thread{

        Job job;
        Exception exception = null;
        EvalJob(Job j){job=j;}

        public void run () {
            try {
                //there are certain jobs we WONT split
                if (job.getType() == Job.Type.CREATE_DIR || job.getType() == Job.Type.RM_DIR || job.getType() == Job.Type.MODIFY_FILES) {
                    finalJobs.add(job);
                    return;
                }

                double currWeight = MAX_JOB_WEIGHT + 100; //we will make a new job the first iteration
                int currFileID = 0;
                Scanner in = new Scanner(new File(job.path));
                PrintWriter out = null;

                while (in.hasNext()) {


                    if (currWeight > MAX_JOB_WEIGHT) {
                        currWeight = 0;
                        if (out != null) {
                            out.close();
                            finalJobs.add(new Job(job.fileName + currFileID));
                        }
                        currFileID++;
                        out = new PrintWriter(job.path + currFileID);
                    }

                    String line = in.nextLine();
                    try {
                        String val[] = line.split("\\t");
                        String file = val[0];
                        BigInteger size = new BigInteger(val[1]);

                        switch (job.getType()) {
                            case CREATE_DIR:
                                currWeight += size.doubleValue() * CP_DIR_WEIGHT + CP_DIR_LINE_WEIGHT;
                                break;
                            case RM_DIR:
                                currWeight += size.doubleValue() * RM_DIR_WEIGHT + RM_DIR_LINE_WEIGHT;
                                break;
                            case CREATE_FILES:
                                currWeight += size.doubleValue() * CP_FILE_WEIGHT + CP_FILE_LINE_WEIGHT;
                                break;
                            case RM_FILES:
                                currWeight += size.doubleValue() * RM_FILE_WEIGHT + RM_FILE_LINE_WEIGHT;
                                break;
                            case MODIFY_FILES:
                                currWeight += size.doubleValue() * SYNC_FILE_WEIGHT + SYNC_FILE_LINE_WEIGHT;
                                break;
                            default:
                                currWeight += size.doubleValue() + 1;
                        }

                        out.println(file + "\t" + size);
                    } catch (Exception e) {
                        System.err.println("Had an error with \"" + line + "\"");
                    }
                }
                if (out != null) {
                    out.close();
                    finalJobs.add(new Job(job.fileName + currFileID));
                }

                in.close();
            }catch(Exception e){
                exception = e;
            }
        }
    }
}
