package com.stealthcopter.networktools.ping;

import java.net.InetAddress;

/**
 * Created by mat on 09/12/15.
 */
public class PingResult
{
    public final InetAddress ia;
    public boolean isReachable;
    public String error = null;
    public float timeTaken;
    public String fullString;
    public String result;

    public PingResult(InetAddress ia)
    {
        this.ia = ia;
    }

    public InetAddress getAddress()
    {
        return ia;
    }

    public String getError()
    {
        return error;
    }

    public float getTimeTaken()
    {
        return timeTaken;
    }

    public boolean hasError()
    {
        return error != null;
    }

    public boolean isReachable()
    {
        return isReachable;
    }

    @Override
    public String toString()
    {
        return "PingResult{" +
                "ia=" + ia +
                ", isReachable=" + isReachable +
                ", error='" + error + '\'' +
                ", timeTaken=" + timeTaken +
                ", fullString='" + fullString + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}
