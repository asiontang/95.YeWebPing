package cn.asiontang.webping;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.local.Resolver;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.ping.PingStats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
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
    private final List<Ping> mPingList = new ArrayList<>();
    private SharedPreferences mSharedPreferences;
    private EditText edtInput;
    private InnerAdapter mAdapter;
    private ProgressBar mProgress;
    private Handler mProgressHandler = new Handler(new Handler.Callback()
    {
        boolean isInvert;

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
    private TextView btnDoit;
    private CheckBox ckbIsEnableHttpCheck;
    private EditText edtTimeout;
    private EditText edtTimes;
    private EditText edtDNS;

    public static boolean checkItByHttp(final String host, final int timeOut, final Ping.PingListener mPingListener)
    {
        HttpURLConnection con = null;
        try
        {
            long startTime = SystemClock.elapsedRealtime();
            final URL url = new URL(host.startsWith("http") ? host : "http://" + host);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");//通过GET会下载太多数据,用Head有的网站不支持,用POST貌似兼容性最好然后下发数据最少?结果POST还是不支持HTTPS检测.所以还是改为GET好啦.
            con.setConnectTimeout(timeOut);
            con.setReadTimeout(timeOut);
            con.setRequestProperty("Connection", "Close");
            con.connect();

            if (BuildConfig.DEBUG)
                Log.e("-----", "getIpByHost con.getResponseCode() != 200 = " + con.getResponseCode());

            final PingResult pingResult = new PingResult(null);
            pingResult.fullString = "con.getResponseCode()=" + con.getResponseCode();
            pingResult.timeTaken = SystemClock.elapsedRealtime() - startTime;
            mPingListener.onResult(pingResult);

            return true;
        }
        catch (Exception e)
        {
            mPingListener.onError(e);

            if (BuildConfig.DEBUG)
                Log.e("-----", "getIpByHost Exception:" + host + e);

            return false;
        }
        finally
        {
            if (con != null)
                con.disconnect();
        }
    }

    /**
     * 同步解析接口，首先查询缓存，若存在则返回结果，若不存在则进行同步域名解析请求，解析完成返回最新解析结果，若解析失败返回null。
     *
     * @param host 域名(如www.aliyun.com)
     * @return 域名对应的解析结果 127.1.1.1
     */
    @SuppressWarnings("all")
    public static String getIpByHost(String host)
    {
        //当传递进来的Host不是正确的Host时,直接返回它自己.
        if (TextUtils.isEmpty(host))
            return host;

        //当传递进来的Host不是正确的Host时,直接返回它自己.
        try
        {
            if (!new URL("http://" + host).getHost().equals(host))
                return host;
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return host;
        }

        //当host本来就是ip时,则不处理,直接返回.
        if (host.matches("^[\\d\\.]+$"))
            return host;

        HttpURLConnection con = null;
        try
        {
            //API - 移动解析 - 产品文档 - 帮助与文档 - 腾讯云
            //https://www.qcloud.com/document/product/379/3524
            final URL url = new URL("http://119.29.29.29/d?dn=" + host);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5 * 1000);
            con.setReadTimeout(5 * 1000);
            con.setRequestProperty("Connection", "Close");
            con.connect();

            if (con.getResponseCode() != 200)
            {
                if (BuildConfig.DEBUG)
                    Log.e("-----", "getIpByHost con.getResponseCode() != 200 = " + con.getResponseCode());
                return null;
            }

            final String ips = StreamUtils.readAllBytesAsString(con.getInputStream(), "UTF-8");

            if (BuildConfig.DEBUG)
                Log.e("-----", "getIpByHost Get:" + ips);

            if (TextUtils.isEmpty(ips))
                return null;

            //例子: http://119.29.29.29/d?dn=www.g.cn 返回的结果为
            //203.208.39.248;203.208.39.239;203.208.39.247;203.208.39.255
            final String[] ipList = ips.split(";");
            if (ipList.length == 0)
                return null;

            if (BuildConfig.DEBUG)
                Log.e("-----", "getIpByHost Result:" + ipList[0]);

            return ipList[0];
        }
        catch (Exception e)
        {
            if (BuildConfig.DEBUG)
                Log.e("-----", "getIpByHost Exception:" + host + e);

            return null;
        }
        finally
        {
            if (con != null)
                con.disconnect();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void checkIsOk(final Runnable runnable)
    {
        new AsyncTask<Void, Void, Boolean>()
        {
            @Override
            protected Boolean doInBackground(final Void... voids)
            {
                try
                {
                    if (Integer.parseInt(edtTimeout.getText().toString()) < 1000)
                    {
                        runOnUiThread(new Runnable()
                        {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run()
                            {
                                edtTimeout.setText("1000");
                                Toast.makeText(MainActivity.this, "超时时间不能小于1000毫秒!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return false;
                    }

                    final String dns = MainActivity.this.<TextView>findViewById(R.id.edtDNS).getText().toString();
                    final InetAddress dnsName = InetAddress.getByName(dns);
                    if (dnsName == null || !dns.equals(dnsName.getHostAddress()))
                    {
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(MainActivity.this, "无效的自定义DNS服务器地址!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return false;
                    }
                    return true;
                }
                catch (final UnknownHostException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(MainActivity.this, "无效的自定义DNS服务器地址!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch (final Exception e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(MainActivity.this, "Check出现未知异常", Toast.LENGTH_SHORT).show();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Check出现未知异常")
                                    .setMessage(e.toString())
                                    .setNegativeButton(android.R.string.ok, null)
                                    .show();
                        }
                    });
                }
                return false;
            }

            @Override
            protected void onPostExecute(final Boolean aBoolean)
            {
                if (aBoolean)
                    runnable.run();
            }
        }.execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mSharedPreferences = getSharedPreferences("AsionTang", MODE_PRIVATE);

        setContentView(R.layout.main);

        edtDNS = findViewById(R.id.edtDNS);

        ckbIsEnableHttpCheck = findViewById(R.id.ckbIsEnableHttpCheck);
        ckbIsEnableHttpCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean isCheckd)
            {
                if (isCheckd)
                {
                    edtTimeout.setText("5000");
                    edtTimes.setText("2");
                }
                else
                {
                    edtTimeout.setText("1000");
                    edtTimes.setText("5");
                }
            }
        });

        edtTimeout = findViewById(R.id.edtTimeout);

        edtTimes = findViewById(R.id.edtTimes);

        edtInput = findViewById(android.R.id.input);

        btnDoit = findViewById(android.R.id.button1);
        btnDoit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View view)
            {
                startPing();
            }
        });

        mProgress = findViewById(android.R.id.progress);

        ExpandableListView list = findViewById(android.R.id.list);
        list.setAdapter(mAdapter = new InnerAdapter(this, mUrlAndIpList));

        restoreUserInfo();
    }

    @Override
    protected void onPause()
    {
        mSharedPreferences.edit()
                .putString("edtDNS", edtDNS.getText().toString())
                .putBoolean("ckbIsEnableHttpCheck", ckbIsEnableHttpCheck.isChecked())
                .putString("edtTimeout", edtTimeout.getText().toString())
                .putString("edtTimes", edtTimes.getText().toString())
                .putString("edtInput", edtInput.getText().toString())
                .apply();

        super.onPause();
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

    private void reset()
    {
        for (Ping ping : mPingList)
            ping.cancel();
        mPingList.clear();

        edtInput.setEnabled(true);

        btnDoit.setText("确定");

        mProgress.setIndeterminate(false);
        mProgress.setProgress(mProgress.getMax());

        mProgressHandler.removeMessages(0);
    }

    @SuppressLint("SetTextI18n")
    private void restoreUserInfo()
    {
        edtDNS.setText(mSharedPreferences.getString("edtDNS", "8.8.8.8"));

        ckbIsEnableHttpCheck.setChecked(mSharedPreferences.getBoolean("ckbIsEnableHttpCheck", false));

        edtTimeout.setText(mSharedPreferences.getString("edtTimeout", "1000"));

        edtTimes.setText(mSharedPreferences.getString("edtTimes", "5"));

        //将所有WWW都去掉,能加快测试速度,因为一般网站都会301,302跳转到www站点,这有利于减少GET数据量.
        edtInput.setText(mSharedPreferences.getString("edtInput",
                "taobao.com\r\n" +
                        "baidu.com\r\n" +
                        "qq.com\r\n" +
                        "google.com\r\n" +
                        "tumblr.com\r\n" +
                        "inoreader.com\r\n"));
    }

    @SuppressLint("StaticFieldLeak")
    private void startPing()
    {
        startPing(false);
    }

    @SuppressLint("StaticFieldLeak")
    private void startPing(final boolean isOK)
    {
        if (mPingList.size() > 0)
        {
            reset();
            return;
        }

        if (!isOK)
        {
            checkIsOk(new Runnable()
            {
                @Override
                public void run()
                {
                    startPing(true);
                }
            });
            return;
        }

        edtInput.setEnabled(false);
        btnDoit.setText("停止");

        mProgress.setIndeterminate(true);
        mProgress.setMax(100);

        mPingList.clear();
        mUrlAndIpList.clear();
        mUrlAndReachable.clear();
        mIpAndResult.clear();
        mUrlAndTheFastestAvgIp.clear();

        for (String url : edtInput.getText().toString().split("\r\n"))
            //排除掉空网址.
            if (url != null && url.trim().length() > 0)
                mUrlAndIpList.put(url, new ArrayList<String>());

        refresh();

        if (mUrlAndIpList.isEmpty())
        {
            reset();
            return;
        }

        final int timeout = Integer.parseInt(edtTimeout.getText().toString());
        final int times = Integer.parseInt(edtTimes.getText().toString());
        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(final Void... voids)
            {
                try
                {
                    //1.先获取域名对应的所有IP
                    if (!doInBackground_getIpList())
                        return null;

                    //2.直接刷新界面以便快速响应UI.
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            refresh();

                            mProgress.setMax(0);
                            mProgress.setIndeterminate(false);
                            for (final Map.Entry<String, List<String>> entry : mUrlAndIpList.entrySet())
                                mProgress.setMax(mProgress.getMax() + entry.getValue().size());
                            mProgressHandler.sendEmptyMessage(0);
                        }
                    });

                    //3.最后再调用异步PING操作.
                    for (final Map.Entry<String, List<String>> entry : mUrlAndIpList.entrySet())
                    {
                        final String url = entry.getKey();
                        for (final String ip : entry.getValue())
                        {
                            final Ping.PingListener mPingListener = doInBackground_getListener(url, ip);

                            if (ckbIsEnableHttpCheck.isChecked())
                                doInBackground_checkByHttp(ip, mPingListener);
                            else
                                doInBackground_checkByPing(ip, mPingListener);
                        }
                    }
                }
                catch (final Exception e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            reset();

                            Toast.makeText(MainActivity.this, "Ping出现未知异常", Toast.LENGTH_SHORT).show();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Ping出现未知异常")
                                    .setMessage(e.toString())
                                    .setNegativeButton(android.R.string.ok, null)
                                    .show();
                        }
                    });
                }
                return null;
            }

            private void doInBackground_checkByHttp(final String ip, final Ping.PingListener mPingListener)
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        long startTime = SystemClock.elapsedRealtime();
                        int count = Integer.parseInt(edtTimes.getText().toString());
                        int successCount = 0;
                        for (int i = 0; i < count; i++)
                        {
                            if (checkItByHttp(ip, Integer.parseInt(edtTimeout.getText().toString()), mPingListener))
                                successCount++;
                        }
                        mPingListener.onFinished(new PingStats(null, count, count - successCount, SystemClock.elapsedRealtime() - startTime, 0, 0));
                    }
                }.start();
            }

            private void doInBackground_checkByPing(final String ip, final Ping.PingListener mPingListener)
            {
                mPingList.add(Ping.onAddress(ip).setTimeOutMillis(timeout).setTimes(times).doPing(mPingListener));
            }

            private boolean doInBackground_getIpList()
            {
                try
                {
                    //qiniu/happy-dns-android: dns library for android
                    //https://github.com/qiniu/happy-dns-android
                    IResolver[] resolvers = new IResolver[1];
                    resolvers[0] = new Resolver(InetAddress.getByName(edtDNS.getText().toString())); //自定义 DNS 服务器地址
                    //resolvers[1] = AndroidDnsServer.defaultResolver(); //系统默认 DNS 服务器
                    DnsManager dns = new DnsManager(NetworkInfo.normal, resolvers);

                    for (final Map.Entry<String, List<String>> entry : mUrlAndIpList.entrySet())
                    {
                        final String url = entry.getKey();
                        final InetAddress[] allByName = dns.queryInetAdress(new Domain(url, false, false));
                        for (InetAddress address : allByName)
                        {
                            final String ip = address.getHostAddress();
                            entry.getValue().add(ip);

                            mIpAndResult.put(ip, new StringBuilder("正在请求中\n\n"));
                        }
                        //有的网站光通过IP是无法正常访问的.所以域名也尝试访问一遍.
                        if (ckbIsEnableHttpCheck.isChecked())
                        {
                            entry.getValue().add("http://" + url);
                            entry.getValue().add("https://" + url);
                        }
                    }
                    return true;
                }
                catch (final SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            reset();

                            Toast.makeText(MainActivity.this, "使用DNS解析域名的IP时超时", Toast.LENGTH_SHORT).show();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("使用DNS解析域名的IP时超时")
                                    .setMessage("1.更换DNS服务器\n2.切换网络\n3.过段时间再重试")
                                    .setNegativeButton(android.R.string.ok, null)
                                    .show();
                        }
                    });
                }
                catch (final Exception e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            reset();

                            Toast.makeText(MainActivity.this, "解析域名的IP时出现未知异常", Toast.LENGTH_SHORT).show();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("解析域名的IP时出现未知异常")
                                    .setMessage(e.toString())
                                    .setNegativeButton(android.R.string.ok, null)
                                    .show();
                        }
                    });
                }
                return false;
            }

            private Ping.PingListener doInBackground_getListener(final String url, final String ip)
            {
                return new Ping.PingListener()
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
                        updateCurrentProgress();

                        Log.e("Ping.onError", e.toString());

                        if (!mUrlAndReachable.containsKey(url) || !mUrlAndReachable.get(url))
                            mUrlAndReachable.put(url, false);

                        getOutput().append(e.toString());
                        getOutput().append("\n");

                        refresh();
                    }

                    @Override
                    public void onFinished(final PingStats e)
                    {
                        Log.e("Ping.onFinished", e.toString());

                        //更新进度,当进度完成时解冻UI.
                        updateCurrentProgress();

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
                        getOutput().append("统计信息:").append("\n");

                        getOutput().append("\t请求:");
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
                };
            }
        }.execute();
    }

    private void updateCurrentProgress()
    {
        //更新进度,当进度完成时解冻UI.
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mProgress.incrementProgressBy(1);

                if (mProgress.getProgress() >= mProgress.getMax())
                    reset();
            }
        });
    }

    class InnerAdapter extends BaseExpandableListAdapterEx<String, String>
    {
        InnerAdapter(final Context context, final Map<String, List<String>> items)
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

        @SuppressLint("DefaultLocale")
        @Override
        public void getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent, final String url)
        {
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(url);

            //
            final TextView text2 = convertView.findViewById(android.R.id.text2);

            //当使用HTTP检测时,不能叫IP了,应该叫做检测点.CheckPoint
            if (ckbIsEnableHttpCheck.isChecked())
                text2.setText(String.format("%d CP", mUrlAndIpList.get(url).size()));
            else
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
