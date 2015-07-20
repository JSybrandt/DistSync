/**
 * Created by jsybrand on 7/20/15.
 */
public class JobTiming {
    public String jobName,deviceName;
    public Long startTime,endTime;
    JobTiming(String d, String n, Long s, Long e)
    {
        deviceName = d;
        jobName=n;
        startTime=s;
        endTime=e;
    }
    public void SetZeroTime(Long zeroTime)
    {
        startTime-=zeroTime;
        endTime-=zeroTime;
    }
}
