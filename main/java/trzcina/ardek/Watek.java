package trzcina.ardek;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

public class Watek extends Thread {

    private MainActivity activity;
    private volatile LinkedList<Komunikat> lista;
    public volatile boolean zakoncz;

    synchronized void dodajDoListy(Komunikat komunikat) {
        lista.addLast(komunikat);
    }

    synchronized Komunikat pobierzZListy() {
        try {
            return lista.removeFirst();
        } catch (Exception e) {
            return null;
        }
    }

    public Watek (MainActivity activity) {
        this.activity = activity;
        this.lista = new LinkedList<>();
        zakoncz = false;
    }

    private void bladpolaczenia() {
        activity.wyswietlInfo("Błąd połączenia!");
    }

    public void run() {
        while (! zakoncz) {
            Komunikat pierwszy = this.pobierzZListy();
            while(pierwszy != null) {
                if(! zakoncz) {
                    if (System.currentTimeMillis() - pierwszy.datakomunikatu <= 5000) {
                        try {
                            URL url = new URL("http://192.168.0.177/" + pierwszy.url);
                            HttpURLConnection polaczenie = (HttpURLConnection) url.openConnection();
                            polaczenie.setInstanceFollowRedirects(false);
                            polaczenie.setConnectTimeout(3000);
                            polaczenie.setReadTimeout(3000);
                            int kod = polaczenie.getResponseCode();
                            if (kod != 302) {
                                this.bladpolaczenia();
                            }
                        } catch (Exception e) {
                            this.bladpolaczenia();
                        }
                    }
                }
                pierwszy = this.pobierzZListy();
            }
            if(zakoncz) {
                return;
            }
            try {
                sleep(60000);
            } catch (InterruptedException e) {
                if(zakoncz) {
                    return;
                }
            }
        }
    }
}
