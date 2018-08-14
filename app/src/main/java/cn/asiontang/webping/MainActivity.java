package cn.asiontang.webping;

import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.ping.PingStats;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity
{
    private final Map<String, List<String>> mUrlAndResult = new HashMap<>();
    private final Map<String, StringBuilder> mIpAndResult = new HashMap<>();
    private EditText edtInput;
    private InnerAdapter mAdapter;

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

        if (BuildConfig.DEBUG)
            edtInput.setText("qq.com\r\nbaidu.com\r\ngoogle.com");

        ExpandableListView list = findViewById(android.R.id.list);
        list.setAdapter(mAdapter = new InnerAdapter(this, mUrlAndResult));
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

    private void startPing()
    {
        mUrlAndResult.clear();
        mIpAndResult.clear();
        
        for (String url : edtInput.getText().toString().split("\r\n"))
            mUrlAndResult.put(url, new ArrayList<String>());

        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(final Void... voids)
            {
                for (final Map.Entry<String, List<String>> entry : mUrlAndResult.entrySet())
                {
                    try
                    {
                        final String url = entry.getKey();
                        final InetAddress[] allByName = InetAddress.getAllByName(url);
                        for (InetAddress address : allByName)
                        {
                            final String ip = address.getHostAddress();
                            entry.getValue().add(ip);

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
                                    Log.e("Ping.onError", e.toString());

                                    getOutput().append(e.toString());
                                    getOutput().append("\n");

                                    refresh();
                                }

                                @Override
                                public void onFinished(final PingStats e)
                                {
                                    Log.e("Ping.onFinished", e.toString());

                                    getOutput().append("\n");
                                    getOutput().append("Ping 统计信息:").append("\n\t");
                                    getOutput().append(" 数据包:");
                                    getOutput().append(" 已发送=").append(e.getNoPings()).append(",");
                                    getOutput().append(" 已接收=").append(e.getNoPings() - e.getPacketsLost()).append(",");
                                    getOutput().append(" 丢失=").append(e.getPacketsLost());
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
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(ip);
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(mIpAndResult.get(ip));
        }

        @Override
        public void getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent, final String url)
        {
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(url);
            ((TextView) convertView.findViewById(android.R.id.text2)).setText("" + mUrlAndResult.get(url).size());
        }
    }
}
