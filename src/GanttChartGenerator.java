import javafx.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import p79068.bmpio.*;

/**
 * Created by jsybrand on 7/20/15.
 */
public class GanttChartGenerator {

    static final int backGroundColor = 0xFFFFFF;
    static final int borderColor = 0;
    static final int CColor = 0x5DA5DA;//blue
    static final int RColor = 0xFAA43A;//orange
    static final int AColor = 0x60BD68;//green
    static final int DColor = 0xF17CB0;//pink
    static final int MColor = 0xB2912F;//brown
    static final int YColor = 0xB276B2;//purple
    static final int LColor = 0xDECF3F;//yellow
    static final int MasterColor = 0xF15854; //red
    static final int TimeSliceColor = 0xC0C0C0; //grey
    //colors from http://www.mulinblog.com/a-color-palette-optimized-for-data-visualization/


    public static void printImage(ArrayList<JobTiming> timings, String filename) throws IOException
    {

        //System.out.println("Printing Image...");

        File file = new File(filename);

        HashMap<String,Integer>devices = new HashMap<>();
        long largestTiming = 0;

        for(JobTiming t : timings)
        {
            if(!devices.containsKey(t.deviceName))
                devices.put(t.deviceName,devices.keySet().size());
            largestTiming = Math.max(largestTiming,t.endTime);
        }


        int width = (int) (largestTiming * 1e-9  * 75);
        if(width > 10000) width=10000;
        int height = devices.keySet().size()*50;

        System.out.println("W:"+width+" Height:"+height);
        Rgb888ImageArray image = new Rgb888ImageArray(width,height,backGroundColor);

        for(JobTiming t : timings)
        {
            //System.out.println("Printing:"+t.jobName);
            int color = getColorFromJobName(t.jobName);
            Rectangle rectangle = new Rectangle();

            rectangle.top = (int)(devices.get(t.deviceName)/(double)devices.keySet().size()*height);
            rectangle.bottom = (int)((devices.get(t.deviceName)+1)/(double)devices.keySet().size()*height);
            rectangle.left = (int)(t.startTime/(double)largestTiming * width);
            rectangle.right = (int)(t.endTime/(double)largestTiming * width);
            try{
                //System.out.println("Printing:"+t.jobName + " " +rectangle);
            fillRectangle(rectangle, color, image);
            }catch (Exception e){System.err.println(t.jobName + " " + rectangle);throw e;}
        }

        //make lines to show every 5 sec
        for(long l = 0; l < largestTiming;l+=5e9)
        {
            Rectangle rectangle = new Rectangle();
            rectangle.top = 0;
            rectangle.bottom = height;
            rectangle.left = rectangle.right = (int)(l/(double)largestTiming * width);
            rectangle.right++;
            fillRectangle(rectangle,TimeSliceColor,image);
        }

        BmpImage bmp = new BmpImage();
        bmp.image = image;
        FileOutputStream out = new FileOutputStream(file);
        try {
            BmpWriter.write(out, bmp);
        } finally {
            out.close();
        }
    }

    private static int getColorFromJobName(String jobName)
    {
        switch(jobName.charAt(0)){
            case 'C':return CColor;
            case 'R':return RColor;
            case 'A':return AColor;
            case 'D':return DColor;
            case 'M':return MColor;
            case 'Y':return YColor;
            case 'L':return LColor;
            default:return MasterColor;
        }
    }
    private static void fillRectangle(Rectangle rec, int color, Rgb888ImageArray image)
    {
        for(int i = rec.left; i < rec.right; i++)
        {
            for(int j = rec.top; j<rec.bottom;j++)
            {
                if(i==rec.left || i==rec.right-1 || j==rec.top || j==rec.bottom-1)
                    image.setRgb888Pixel(i,j,borderColor);
                else
                    image.setRgb888Pixel(i,j,color);
            }
        }
    }


    private static class Rectangle{
        public int top,bottom,left,right;
        public Rectangle(int t,int b,int l, int r){top=t;bottom=b;left=l;right=r;}
        public Rectangle(){top=bottom=left=right=0;}

        public String toString(){return "T:"+top + " B:"+bottom + " L:"+left + " R:"+right;}

    }
}
