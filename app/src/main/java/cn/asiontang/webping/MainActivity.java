package cn.asiontang.webping;

import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.ping.PingStats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity
{
    private final Map<String, List<String>> mUrlAndIpList = new LinkedHashMap<>();
    private final Map<String, Boolean> mUrlAndReachable = new HashMap<>();
    private final Map<String, StringBuilder> mIpAndResult = new HashMap<>();
    private final Map<String, Object[]> mUrlAndTheFastestAvgIp = new HashMap<>();
    private EditText edtInput;
    private InnerAdapter mAdapter;
    private ProgressBar mProgress;
    private Handler mProgressHandler = new Handler(new Handler.Callback()
    {
        public boolean isInvert;

        @Override
        public boolean handleMessage(final Message message)
        {
            if (mProgress == null)
                return false;

            if (mProgress.getSecondaryProgress() >= mProgress.getMax())
                isInvert = true;
            else if (mProgress.getSecondaryProgress() <= mProgress.getProgress())
                isInvert = false;

            if (isInvert)
                mProgress.setSecondaryProgress(mProgress.getSecondaryProgress() - 1);
            else
                mProgress.setSecondaryProgress(mProgress.getSecondaryProgress() + 1);

            mProgressHandler.sendEmptyMessageDelayed(0, 500);
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        findViewById(android.R.id.button1).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View view)
            {
                startPing();
            }
        });
        edtInput = findViewById(android.R.id.input);

        mProgress = findViewById(android.R.id.progress);

        if (BuildConfig.DEBUG)
            edtInput.setText("www.baidu.com\r\n" +
                    "www.qq.com\r\n" +
                    "www.google.com\r\n" +
                    "www.tumblr.com\r\n" +
                    "www.inoreader.com\r\n");

        ExpandableListView list = findViewById(android.R.id.list);
        list.setAdapter(mAdapter = new InnerAdapter(this, mUrlAndIpList));
    }

    private void refresh()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mAdapter.refresh();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void startPing()
    {
        edtInput.setEnabled(false);
        findViewById(android.R.id.button1).setEnabled(false);

        mProgress.setIndeterminate(true);
        mProgress.setMax(100);

        mUrlAndIpList.clear();
        mUrlAndReachable.clear();
        mIpAndResult.clear();
        mUrlAndTheFastestAvgIp.clear();

        for (String url : edtInput.getText().toString().split("\r\n"))
            //排除掉空网址.
            if (url != null && url.trim().length() > 0)
                mUrlAndIpList.put(url, new ArrayList<String>());

        refresh();
        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(final Void... voids)
            {
                for (final Map.Entry<String, List<String>> entry : mUrlAndIpList.entrySet())
                {
                    try
                    {
                        final String url = entry.getKey();
                        final InetAddress[] allByName = InetAddress.getAllByName(url);
                        for (InetAddress address : allByName)
                        {
                            final String ip = address.getHostAddress();
                            entry.getValue().add(ip);

                            mIpAndResult.put(ip, new StringBuilder("正在请求中\n\n"));

                            Ping.onAddress(ip).setTimeOutMillis(1000).setTimes(5).doPing(new Ping.PingListener()
                            {
                                private StringBuilder getOutput()
                                {
                                    StringBuilder s = mIpAndResult.get(ip);
                                    if (s == null)
                                    {
                                        s = new StringBuilder();
                                        mIpAndResult.put(ip, s);
                                    }
                                    return s;
                                }

                                @Override
                                public void onError(final Exception e)
                                {
                                    //更新进度,当进度完成时解冻UI.
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            mProgress.incrementProgressBy(1);

                                            if (mProgress.getProgress() >= mProgress.getMax())
                                            {
                                                edtInput.setEnabled(true);
                                                findViewById(android.R.id.button1).setEnabled(true);
                                                mProgressHandler.removeMessages(0);
                                            }
                                        }
                                    });

                                    Log.e("Ping.onError", e.toString());

                                    getOutput().append(e.toString());
                                    getOutput().append("\n");

                                    refresh();
                                }

                                @Override
                                public void onFinished(final PingStats e)
                                {
                                    Log.e("Ping.onFinished", e.toString());

                                    //更新进度,当进度完成时解冻UI.
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            mProgress.incrementProgressBy(1);

                                            if (mProgress.getProgress() >= mProgress.getMax())
                                            {
                                                edtInput.setEnabled(true);
                                                findViewById(android.R.id.button1).setEnabled(true);
                                                mProgressHandler.removeMessages(0);
                                            }
                                        }
                                    });

                                    //只要有一半的包接收到了就说明网络还算是通的.
                                    boolean isReachable = false;
                                    if (!mUrlAndReachable.containsKey(url) || !mUrlAndReachable.get(url))
                                        mUrlAndReachable.put(url, isReachable = (double) e.getPacketsLost() / (double) e.getNoPings() < 0.5d);

                                    //统计平均响应时间最短的IP
                                    if (isReachable)
                                    {
                                        Object[] ipAndAvg = mUrlAndTheFastestAvgIp.get(url);
                                        if (ipAndAvg == null)
                                        {
                                            ipAndAvg = new Object[]{ip, e.getAverageTimeTaken()};
                                            mUrlAndTheFastestAvgIp.put(url, ipAndAvg);
                                        }
                                        else
                                        {
                                            final float lastAvg = (float) ipAndAvg[1];
                                            if (e.getAverageTimeTaken() < lastAvg)
                                            {
                                                ipAndAvg = new Object[]{ip, e.getAverageTimeTaken()};
                                                mUrlAndTheFastestAvgIp.put(url, ipAndAvg);
                                            }
                                        }
                                    }

                                    getOutput().append("\n");
                                    getOutput().append("Ping 统计信息:").append("\n");

                                    getOutput().append("\t封包:");
                                    getOutput().append(" 发送=").append(e.getNoPings()).append(",");
                                    getOutput().append(" 接收=").append(e.getNoPings() - e.getPacketsLost()).append(",");
                                    getOutput().append(" 丢失=").append(e.getPacketsLost());
                                    getOutput().append("\n");

                                    getOutput().append("\t时间:");
                                    getOutput().append(" 平均=").append((int) e.getAverageTimeTaken()).append("ms,");
                                    getOutput().append(" 最短=").append((int) e.getMinTimeTaken()).append("ms,");
                                    getOutput().append(" 最长=").append((int) e.getMaxTimeTaken()).append("ms");
                                    getOutput().append("\n");

                                    refresh();
                                }

                                @Override
                                public void onResult(PingResult e)
                                {
                                    Log.e("Ping.onResult", e.toString());

                                    getOutput().append("时间=").append((int) e.timeTaken).append("ms");
                                    if (e.error != null)
                                    {
                                        getOutput().append(" ");
                                        getOutput().append("结果=").append(e.error);
                                    }
                                    getOutput().append("\n");

                                    refresh();
                                }
                            });
                        }
                    }
                    catch (UnknownHostException e)
                    {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mProgress.setMax(0);
                        mProgress.setIndeterminate(false);
                        for (final Map.Entry<String, List<String>> entry : mUrlAndIpList.entrySet())
                            mProgress.setMax(mProgress.getMax() + entry.getValue().size());
                        mProgressHandler.sendEmptyMessage(0);
                    }
                });
                refresh();
                return null;
            }
        }.execute();
    }

    class InnerAdapter extends BaseExpandableListAdapterEx<String, String>
    {
        public InnerAdapter(final Context context, final Map<String, List<String>> items)
        {
            super(context, R.layout.url, R.layout.ip, items);
        }

        @Override
        public void getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, final View convertView, final ViewGroup parent, final String ip)
        {
            TextView text1 = convertView.findViewById(android.R.id.text1);
            text1.setText(ip);

            //显示最短的平均时间
            final Object[] ipAndAvg = mUrlAndTheFastestAvgIp.get(/*url*/getGroup(groupPosition));
            if (ipAndAvg != null && ipAndAvg[0].equals(ip))
                text1.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));//加粗
            else
                text1.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));

            ((TextView) convertView.findViewById(android.R.id.text2)).setText(mIpAndResult.get(ip));
        }

        @Override
        public void getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent, final String url)
        {
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(url);

            //
            final TextView text2 = convertView.findViewById(android.R.id.text2);
            text2.setText(String.format("%d IP", mUrlAndIpList.get(url).size()));

            //Ping通状态
            if (mUrlAndReachable.containsKey(url))
            {
                //当获取到值时,不是联通状态就是不通的状态.
                text2.setBackgroundColor(mUrlAndReachable.get(url) ? Color.GREEN : Color.RED);
            }
            else
            {
                //获取不到时,说明正在请求中
                text2.setBackgroundColor(Color.YELLOW);
            }

            //显示最短的平均时间
            final Object[] ipAndAvg = mUrlAndTheFastestAvgIp.get(url);
            if (ipAndAvg == null)
                convertView.<TextView>findViewById(android.R.id.message).setText(null);
            else
                convertView.<TextView>findViewById(android.R.id.message).setText(String.format("最快IP=%s 平均=%sms", ipAndAvg[0], ((int) (float) ipAndAvg[1])));
        }
    }
}
