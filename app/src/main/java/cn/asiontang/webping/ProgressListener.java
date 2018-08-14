package cn.asiontang.webping;

public interface ProgressListener
{
    void onProgressUpdate(int maxValue, int currentValue, CharSequence currentMessage, Object... otherArgs);
}
