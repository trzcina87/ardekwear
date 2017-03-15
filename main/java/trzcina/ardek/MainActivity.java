package trzcina.ardek;

import android.app.Activity;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class MainActivity extends Activity {

    public Watek watek;
    public String telefon = null;
    public GoogleApiClient gac = null;
    public volatile boolean pobieram = false;
    public volatile boolean wysylam = false;

    public boolean sprawdzWifi() {
        WifiManager manager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    String nazwa = wifiInfo.getSSID();
                    if(nazwa.toLowerCase().contains("tenda_3fb340")) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void wyslijKomunikat(final String komunikat) {
        if (telefon != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!wysylam) {
                        wysylam = true;
                        Wearable.MessageApi.sendMessage(gac, telefon, komunikat, null);
                        wysylam = false;
                    }
                }
            }).start();
        }
    }

    private class ClickImage implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(sprawdzWifi()) {
                Komunikat komunikat = new Komunikat((String) v.getTag(), System.currentTimeMillis());
                watek.dodajDoListy(komunikat);
                watek.interrupt();
            } else {
                if (telefon != null) {
                    wyslijKomunikat((String)v.getTag());
                } else {
                    Toast.makeText(MainActivity.this, "Sprawdz polaczenie z WiFi lub telefonem!", Toast.LENGTH_SHORT).show();
                    pobierzNode();
                }
            }
        }

    }

    public void wyswietlInfo(final String info) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ustawEkran();
        ustawTagi();
        ustawClick();
        ustawWatek();
        ustawApi();
    }

    private void ustawEkran() {
        setContentView(R.layout.rect_activity_main);
    }

    private void ustawTagi() {
        findViewById(R.id.polkaon).setTag("puls=196&kod=5330227");
        findViewById(R.id.polkaoff).setTag("puls=196&kod=5330236");
        findViewById(R.id.oknoon).setTag("puls=196&kod=5330371");
        findViewById(R.id.oknooff).setTag("puls=196&kod=5330380");
        findViewById(R.id.szafaon).setTag("puls=196&kod=5330691");
        findViewById(R.id.szafaoff).setTag("puls=196&kod=5330700");
    }

    private void ustawClick() {
        ClickImage click = new ClickImage();
        findViewById(R.id.polkaon).setOnClickListener(click);
        findViewById(R.id.polkaoff).setOnClickListener(click);
        findViewById(R.id.oknoon).setOnClickListener(click);
        findViewById(R.id.oknooff).setOnClickListener(click);
        findViewById(R.id.szafaon).setOnClickListener(click);
        findViewById(R.id.szafaoff).setOnClickListener(click);
    }

    private void ustawWatek() {
        watek = new Watek(this);
        watek.start();
    }

    private void pobierzNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(! pobieram) {
                    pobieram = true;
                    Log.e("API", "pobieram nody");
                    NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(gac).await();
                    Log.e("API", "pobralem nody");
                    List<Node> nodes = result.getNodes();
                    for(int i = 0; i < nodes.size(); i++) {
                        if(nodes.get(i).getDisplayName().toLowerCase().equals("s5mini")) {
                            telefon = nodes.get(i).getId();
                            Log.e("API", nodes.get(i).getDisplayName());
                            Log.e("API", nodes.get(i).getId());
                        }
                    }
                    pobieram = false;
                }
            }
        }).start();
    }

    private void ustawApi() {
        gac = new GoogleApiClient.Builder(getApplicationContext()).addApi(Wearable.API).build();
        gac.connect();
        pobierzNode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("API", "koniec");
        watek.zakoncz = true;
        watek.interrupt();
        while(watek.isAlive()) {
            try {
                watek.join();
            } catch (InterruptedException e) {
            }
        }
        Toast.makeText(MainActivity.this, "Zakończono Ardek!", Toast.LENGTH_SHORT).show();
    }
}
