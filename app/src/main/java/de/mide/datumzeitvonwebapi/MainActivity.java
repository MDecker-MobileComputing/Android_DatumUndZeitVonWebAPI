package de.mide.datumzeitvonwebapi;


import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Demo-App für Abrufen von Daten über Web-API im JSON-Format.
 * Es werden Datum & Uhrzeit von <i>jsontest.com</i> abgerufen:
 * <a href="http://www.jsontest.com/#date">http://www.jsontest.com/#date</a>
 * <br><br>
 *
 * This file is licensed under the terms of the BSD 3-Clause License.
 */
public class MainActivity extends Activity {

	public static final String TAG4LOGGING = "DatumZeitVonWebAPI";

	/** Button mit dem der Web-Request gestartet wird. */
	protected Button _startButton = null;

	/**
	 * TextView zur Anzeige des Ergebnisses des Web-Requests (also Datum + Uhrzeit),
	 * auch zur Anzeige von Fehlermeldungen.
	 */
	protected TextView _ergebnisTextView = null;


	/**
	 * Lifecycle-Methode: Layout für UI laden und Referenzen auf UI-Elemente holen.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		_startButton      = findViewById( R.id.starteWebRequestButton );
		_ergebnisTextView = findViewById( R.id.ergebnisTextView       );

		_ergebnisTextView.setMovementMethod( new ScrollingMovementMethod() ); // vertikales Scrolling ermöglichen
	}


	/**
	 * Event-Handler für Start-Button, wird in Layout-Datei
	 * mit Attribut <tt>android:onClick</tt> zugewiesen.
	 */
	public void onStartButtonBetaetigt(View view) {

		_startButton.setEnabled(false); // Button deaktivieren während ein HTTP-Request läuft

		_ergebnisTextView.setText("Starte HTTP-Request ...");


	    // Hintergrund-Thread mit HTTP-Request starten
		MeinHintergrundThread mht = new MeinHintergrundThread();
		mht.start();
	}


	/**
	 * In dieser Methode wird der HTTP-Request zur Web-API durchgeführt.
	 * Achtung: Diese Methode darf nicht im Main-Thread ausgeführt werden,
	 * weil ein Internet-Zugriff länger dauern kann (mehrere Sekunden oder Minuten),
	 * so dass die App wegen <i>"Application Not Responding" (ANR)</i> ggf.
	 * vom Nutzer abgebrochen würde.
	 *
	 * @return String mit JSON-Dokument, das als Antwort zurückgeliefert wurde.
	 */
	protected String holeDatenVonWebAPI() throws Exception {

        URL url                                = null;
        HttpURLConnection conn                 = null;
        String            httpErgebnisDokument = "";


        url  = new URL("http://time.jsontest.com"); // keine httpS-URL, also android:usesCleartextTraffic="true" in Manifest-Datei für <application> setzen
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET"); // Eigentlich nicht nötig, weil "GET" Default-Wert ist.

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

            String errorMessage = "HTTP-Fehler: " + conn.getResponseMessage();
            throw new Exception( errorMessage );

        } else {

            InputStream is        = conn.getInputStream();
            InputStreamReader ris = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(ris);

            // JSON-Dokument zeilenweise einlesen
            String zeile = "";
            while ( (zeile = reader.readLine()) != null) {

                httpErgebnisDokument += zeile;
            }
        }

	    Log.i(TAG4LOGGING, "JSON-String erhalten: " + httpErgebnisDokument);

        return httpErgebnisDokument;
	}


	/**
	 * Parsen des JSON-Dokuments <i>jsonString</i>, das von der Web-API
	 * zurückgeliefert wurde.<br><br>
	 *
	 * Beispiel für ein JSON-Dokument von der Web-API:
	 * <pre>
	 * {
	 *   "time": "05:51:48 PM",
     *   "milliseconds_since_epoch": 1419789108674,
     *   "date": "12-28-2014"
     * }
	 * </pre>
	 * <br><br>
     *
	 * Es wird der in Android seit API-Level eingebaute JSON-Parser
	 * verwendet: <i>http://developer.android.com/reference/org/json/JSONObject.html</i> .
	 *
	 * @param jsonString JSON-Dokument, das die Web-API zurückgeliefert hat.
	 *
	 * @return String mit Ergebnis (Datum & Uhrzeit), zur Anzeige auf UI.
	 */
	protected String parseJSON(String jsonString) throws Exception {

		if (jsonString == null || jsonString.trim().length() == 0) {

			return "Leeres JSON-Objekt von Web-API erhalten.";
		}


        // eigentliches Parsen der JSON-Datei
		JSONObject jsonObject = new JSONObject( jsonString );

		// Zwei Attribute abfragen
		String zeitString  = jsonObject.getString( "time" );
		String datumString = jsonObject.getString( "date" );


		// String für Ausgabe auf UI zusammenbauen
		return "Zeit (UTC):\n"              + zeitString  +
			   "\n\nDatum (MM-DD-YYYY):\n"  + datumString;
	}


	/* *************************** */
	/* *** Start innere Klasse *** */
	/* *************************** */

	/**
	 * Zugriff auf Web-API (Internet-Zugriff) wird in
	 * eigenen Thread ausgelagert, damit der Main-Thread
	 * nicht blockiert wird.
	 */
	protected class MeinHintergrundThread extends Thread {

		/**
		 * Der Inhalt in der überschriebenen <i>run()</i>-Methode
		 * wird in einem Hintergrund-Thread ausgeführt.
		 */
		@Override
		public void run() {

			try {

				String jsonDocument = holeDatenVonWebAPI();

				String ergString = parseJSON(jsonDocument);

				ergbnisDarstellen( "Ergebnis von Web-Request:\n\n" + ergString +
						           "\n\nAchtung: Unterschied UTC zu deutscher Zeit eine oder bei Sommerzeit zwei Stunden." );
			}
			catch (Exception ex) {

				ergbnisDarstellen( "Exception aufgetreten:\n\n" + ex.getMessage() );
			}
		}


		/**
		 * Methode um Ergebnis-String in TextView darzustellen. Da
		 * es sich hierbei um einen UI-Zugriff handelt, müssen
		 * wir mit der <i>post()</i>-Methode dafür sorgen, dass die
		 * UI-Zugriffe aus dem Main-Thread heraus durchgeführt werden.
		 * Der Start-Button wird auch wieder aktiviert.
		 *
		 * @param ergebnisStr Nachricht, die in TextView-Element dargestellt werden soll.
		 */
		protected void ergbnisDarstellen(String ergebnisStr) {

			final String finalString = ergebnisStr;

			_startButton.post( new Runnable() { // wir könnten auch die post()-Methode des TextView-Elements verwenden
				@Override
				public void run() {

					_startButton.setEnabled(true);

					_ergebnisTextView.setText(finalString);
				}
			});

		}

	};

	/* *************************** */
	/* *** Ende innere Klasse  *** */
	/* *************************** */

};
