package com.example.mywifi;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.*;

public class MyActivity extends Activity {
    private int last_network_id = -1;
    private int selected_network_id = -1;
    private int selected_level = -1;
    private int top_level = -1;
    private int scan_interval = 10; //seconds
    private int level_intervel = 10; //
    private HashMap<Integer, Integer> levelMap = new HashMap<Integer, Integer>();
    private int totalTimes = 0;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private TextView infoText;
    private TextView infoText2;
    private WifiManager wifi;
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1 :
                    infoText.setText(msg.getData().getString("infoText"));
                    break;
                case 2 :
                    infoText2.setText(msg.getData().getString("infoText2"));
                    break;
                default :
                    break;
            }
        }
    };
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        infoText = (TextView) this.findViewById(R.id.info);
        infoText2 = (TextView) this.findViewById(R.id.info2);
        infoText2.setMovementMethod(ScrollingMovementMethod.getInstance());
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        class Mythread extends Thread {
            @Override
            public void run() {
                while (true){
                    try {
                        totalTimes++;
                        connectTop();
                        Thread.sleep(scan_interval*1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Mythread().start();
    }

    public void connectTop() throws Exception{
        wifi.startScan();
        sleep(1);

        List<WifiConfiguration> wifiCfgs = wifi.getConfiguredNetworks();
        HashMap<String, Integer> cfgMap = new HashMap<String, Integer>();
        for (WifiConfiguration c: wifiCfgs){
            cfgMap.put(c.SSID.replaceAll("\"",""), c.networkId);
        }

        String topSSID = getTopSSID(wifi);
        int topNetworkId = cfgMap.get(topSSID);
        if(totalTimes == 1){
            wifi.enableNetwork(topNetworkId, true);
            selected_network_id = topNetworkId;
            selected_level = top_level;
            sleep(2);
        }

        if(totalTimes > 1 && topNetworkId != selected_network_id && Math.abs(selected_level-levelMap.get(selected_network_id)) > level_intervel){
            wifi.enableNetwork(topNetworkId, true);
            selected_network_id = topNetworkId;
            selected_level = top_level;
            sleep(2);
        }

        sendMsg(1, "infoText", getInfo(wifi));
        sendMsg(2, "infoText2", getInfo2(cfgMap, topSSID));
        last_network_id = selected_network_id;
    }

    public String getInfo2(HashMap<String, Integer> cfgMap, String topSSID) {
        StringBuffer info2 = new StringBuffer();
        info2.append("======================");
        info2.append("\r\n");
        info2.append("Cfg Map:");
        info2.append("\r\n");
        info2.append(cfgMap);
        info2.append("\r\n");
        info2.append("======================");
        info2.append("\r\n");
        info2.append("TOP: ");
        info2.append(topSSID);
        info2.append("\r\n");
        info2.append("======================");
        info2.append("\r\n");
        info2.append("Sorted cfg list:");
        info2.append("\r\n");
        info2.append(getSortedCfgList(wifi));
        info2.append("\r\n");
        info2.append("======================");
        info2.append("\r\n");
        info2.append("Sorted scan list:");
        info2.append("\r\n");
        info2.append(getSortedScanList(wifi));
        info2.append("\r\n");
        info2.append("======================");
        return info2.toString();
    }

    public void sendMsg(int what, String key, String value) {
        Message msg = new Message();
        Bundle bdl = new Bundle();
        bdl.putString(key, value);
        msg.setData(bdl);
        msg.what = what;
        mHandler.sendMessage(msg);
    }

    public void sleep(int sec) {
        try {
            Thread.sleep(sec*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getInfo(WifiManager wifi)
    {
        WifiInfo info = wifi.getConnectionInfo();
        String maxText = info.getMacAddress();
        String ipText = intToIp(info.getIpAddress());
        String ssid = info.getSSID();
        int networkID = info.getNetworkId();
        int speed = info.getLinkSpeed();
        return " total refresh times：" + totalTimes + "\n\r"
                + "time：" + sdf.format(new Date()) + "\n\r"
                + "mac：" + maxText + "\n\r"
                + "ip：" + ipText + "\n\r"
                + "ssid :" + ssid + "\n\r"
                + "net work id :" + networkID + "\n\r"
                + "last net work id :" + last_network_id + "\n\r"
                + "level:" + selected_level + "\n\r"
                + "connection speed:" + speed + "\n\r"
                ;
    }
    public String intToIp(int ip){
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 24) & 0xFF);
    }

    public String getSortedScanList(WifiManager wifi) {
        ArrayList<ScanResult> list = (ArrayList<ScanResult>) wifi.getScanResults();
        Collections.sort(list, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return -(lhs.level - rhs.level);
            }
        });
        StringBuffer sb = new StringBuffer();
        for (ScanResult sr: list) {
            sb.append(sr.SSID);
            sb.append(":");
            sb.append(sr.level);
            sb.append("\r\n");
        }
        return sb.toString();
    }

    public String getSortedCfgList(WifiManager wifi) {
        ArrayList<ScanResult> scanList = (ArrayList<ScanResult>) wifi.getScanResults();
        HashMap<String, Integer> scanMap = new HashMap<String, Integer>();
        for (ScanResult sr: scanList) {
            scanMap.put(sr.SSID, sr.level);
        }

        List<WifiConfiguration> cfgList = wifi.getConfiguredNetworks();
        List<WifiConfigurationExt> cfgExtList = getExt(cfgList);
        for (WifiConfigurationExt wce : cfgExtList){
            if(scanMap.get(wce.SSID) != null){
                wce.level = scanMap.get(wce.SSID);
            } else {
                wce.level = -1000;
            }
            levelMap.put(wce.networkId, wce.level);
        }

        Collections.sort(cfgExtList, new Comparator<WifiConfigurationExt>() {
            @Override
            public int compare(WifiConfigurationExt lwce, WifiConfigurationExt rwce) {
                return -(lwce.level - rwce.level);
            }
        });

        StringBuffer sb = new StringBuffer();
        for (WifiConfigurationExt wce: cfgExtList) {
            sb.append(wce.SSID);
            sb.append(":");
            sb.append(wce.level);
            sb.append("\r\n");
        }
        return sb.toString();
    }

    public String getTopSSID(WifiManager wifi) {
        ArrayList<ScanResult> scanList = (ArrayList<ScanResult>) wifi.getScanResults();
        HashMap<String, Integer> scanMap = new HashMap<String, Integer>();
        for (ScanResult sr: scanList) {
            scanMap.put(sr.SSID, sr.level);
        }

        List<WifiConfiguration> cfgList = wifi.getConfiguredNetworks();
        List<WifiConfigurationExt> cfgExtList = getExt(cfgList);
        for (WifiConfigurationExt wce : cfgExtList){
            if(scanMap.get(wce.SSID) != null){
                wce.level = scanMap.get(wce.SSID);
            } else {
                wce.level = -1000;
            }
            levelMap.put(wce.networkId, wce.level);
        }

        Collections.sort(cfgExtList, new Comparator<WifiConfigurationExt>() {
            @Override
            public int compare(WifiConfigurationExt lwce, WifiConfigurationExt rwce) {
                return -(lwce.level - rwce.level);
            }
        });

        top_level = cfgExtList.get(0).level;
        return cfgExtList.get(0).SSID;
    }

    public List<WifiConfigurationExt> getExt(List<WifiConfiguration> cfgList){
        List<WifiConfigurationExt> cfgExtList = new ArrayList<WifiConfigurationExt>();
        for (WifiConfiguration wc: cfgList) {
            cfgExtList.add(new WifiConfigurationExt(wc));
        }
        return cfgExtList;
    }
}

class WifiConfigurationExt extends WifiConfiguration{
    public int level;

    public WifiConfigurationExt(WifiConfiguration wc){
        super();
        this.SSID = wc.SSID.replaceAll("\"","");
        this.networkId = wc.networkId;
        this.level = 0;
    }
}