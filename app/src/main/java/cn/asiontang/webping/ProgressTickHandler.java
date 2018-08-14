package cn.asiontang.webping;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * 支持每间隔250毫秒回调一下进度监听器
 */
public class ProgressTickHandler extends Handler
{
    private final ProgressListener mProgressListener;
    private boolean mIsFinished;
    private int mMaxValue = 100;
    private int mCurrentValue = 0;
    private long lastProgress = 0;

    public ProgressTickHandler(final ProgressListener listener)
    {
        super(Looper.getMainLooper());

        this.mProgressListener = listener;

        this.tick();
    }

    public void finish()
    {
        this.mIsFinished = true;
        this.removeMessages(0);
    }

    @Override
    public void handleMessage(Message msg)
    {
        if (this.mIsFinished)
            return;

        if (this.lastProgress != this.mMaxValue + this.mCurrentValue)
        {
            this.lastProgress = this.mMaxValue + this.mCurrentValue;
            this.mProgressListener.onProgressUpdate(this.mMaxValue, this.mCurrentValue, "");
        }

        this.tick();
    }

    public void setProgressMaxValue(int maxValue)
    {
        this.mMaxValue = maxValue;
    }

    public void setProgressValue(int currentValue)
    {
        this.mCurrentValue = currentValue;
    }

    private void tick()
    {
        if (mIsFinished)
            return;

        this.sendEmptyMessageDelayed(0, 250);
    }
}
