package cn.asiontang.webping;

import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.ping.PingStats;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity
{
    private final List<String> mItems = new ArrayList<>();
    private final Map<String, String> mUrlAndResult = new HashMap<>();
    private EditText edtInput;
    private BaseAdapterEx3<String> mAdapter;

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

        ListView list = findViewById(android.R.id.list);
        list.setAdapter(mAdapter = new BaseAdapterEx3<String>(this, R.layout.item, mItems)
        {
            @Override
            public void convertView(final ViewHolder viewHolder, final String item)
            {
                viewHolder.getTextView(android.R.id.text1).setText(item);
                viewHolder.getTextView(android.R.id.text2).setText(mUrlAndResult.get(item));
            }
        });
    }

    private void startPing()
    {
        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(final Void... voids)
            {
                mItems.clear();
                mItems.addAll(Arrays.asList(edtInput.getText().toString().split("\r\n")));

                for (final String url : mItems)
                {
                    try
                    {
                        final InetAddress[] allByName = InetAddress.getAllByName(url);
                        for (InetAddress address : allByName)
                        {
                            Ping.onAddress(address.getHostAddress()).setTimeOutMillis(1000).setTimes(5).doPing(new Ping.PingListener()
                            {
                                @Override
                                public void onError(final Exception e)
                                {
                                    mUrlAndResult.put(url, e.toString());
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            mAdapter.refresh();
                                        }
                                    });
                                }

                                @Override
                                public void onFinished(final PingStats pingStats)
                                {
                                    mUrlAndResult.put(url, pingStats.toString());
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            mAdapter.refresh();
                                        }
                                    });
                                }

                                @Override
                                public void onResult(PingResult pingResult)
                                {
                                    mUrlAndResult.put(url, pingResult.toString());
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            mAdapter.refresh();
                                        }
                                    });
                                }
                            });
                            break;
                        }

                    }
                    catch (UnknownHostException e)
                    {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();
    }
}
