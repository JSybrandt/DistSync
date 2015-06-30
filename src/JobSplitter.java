import java.io.*;
import java.util.Scanner;

/**
 * Created by jsybrand on 6/29/15.
 */
public class JobSplitter extends Thread{

    //will find all of the job files, give each one a weight, and split large jobs unto smaller ones
    JobSplitter(){}

    @Override
    public void run(){

    }

    public double getWeight(Job j) throws IOException
    {
        double weight = 0;

        Scanner scan = new Scanner(new File(j.fileName));
        while(scan.hasNext()) {
            int size = scan.nextInt();
            String length = scan.next();


        }
        return weight;
    }

}
