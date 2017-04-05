/**
 * Originally from the CIM Auto-Connect app,
 * package com.nerrdit.freelancer.autoconnector
 */

package com.erichamion.freelance.oakglen;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

@SuppressWarnings("ALL")
public class WiFiService extends Service {
	 private final String NETWORK_SSID = "CIM";
	 private final String NETWORK_PASSWORD = "upforair";


	private WifiManager mWifiManager;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		wifiStatusEnabled = mWifiManager.isWifiEnabled();

		if (wifiStatusEnabled)
			mWifiManager.startScan();

		registerReceiver(mWifiReceiver, new IntentFilter(
				"android.net.wifi.STATE_CHANGE"));
		registerReceiver(mWifiScanReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean wifiStatusEnabled = false;
	private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (mWifiManager.isWifiEnabled() && !wifiStatusEnabled) {
				wifiStatusEnabled = mWifiManager.isWifiEnabled();
				mWifiManager.startScan();
			} else if (!mWifiManager.isWifiEnabled()) {
				wifiStatusEnabled = mWifiManager.isWifiEnabled();
				return;
			}

			if (intent.getAction().equals(
					WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				NetworkInfo networkInfo = intent
						.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (networkInfo.isConnected()) {
					if (ConnectivityManager.TYPE_WIFI == networkInfo.getType()) {
						// Network Connected through WiFi
						WifiInfo info = mWifiManager.getConnectionInfo();
						String ssid = info.getSSID();

						if (ssid.length() > 3)
							if (ssid.charAt(0) == '\"'
									&& ssid.charAt(ssid.length() - 1) == '\"')
								ssid = ssid.substring(1, ssid.length() - 1);

						if (ssid.equalsIgnoreCase(NETWORK_SSID)) {
							playNotifyTune();

							Intent activityIntent = new Intent(
									WiFiService.this, EmbeddedBrowser.class);
							activityIntent
									.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(activityIntent);

							Log.e("Connected to X: ", ssid);
						} else
							Log.e("Connected to: ", ssid);
					}
				}
			}

			// Toast.makeText(instance, "WiFi " + mWifiManager.isWifiEnabled(),
			// Toast.LENGTH_SHORT).show();
		}
	};

	private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			// Toast.makeText(instance, "scanning ", Toast.LENGTH_SHORT).show();

			List<ScanResult> mScanResults = mWifiManager.getScanResults();
			for (int i = 0; i < mScanResults.size(); i++) {
				String ssid = mScanResults.get(i).SSID;
				String securityType = getScanResultSecurity(mScanResults.get(i));
				if (ssid.equalsIgnoreCase(NETWORK_SSID)) {
					connectToAP(ssid, NETWORK_PASSWORD, securityType);
					Log.e("Found", "yes");
					return;
				}
			}
		}
	};

	public boolean connectToAP(String ssid, String passkey, String securityMode) {
		WifiConfiguration wifiConfiguration = new WifiConfiguration();

		String networkSSID = ssid;
		String networkPass = passkey;

		if (securityMode.equalsIgnoreCase("OPEN")) {

			wifiConfiguration.SSID = "\"" + networkSSID + "\"";
			wifiConfiguration.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.NONE);

			// mWifiManager.setWifiEnabled(true);

		} else if (securityMode.equalsIgnoreCase("WEP")) {

			wifiConfiguration.SSID = "\"" + networkSSID + "\"";
			wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
			wifiConfiguration.wepTxKeyIndex = 0;
			wifiConfiguration.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.NONE);
			wifiConfiguration.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.WEP40);

			// mWifiManager.setWifiEnabled(true);
		} else {
			wifiConfiguration.SSID = "\"" + networkSSID + "\"";
			wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
			wifiConfiguration.hiddenSSID = true;
			wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
			wifiConfiguration.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);
			wifiConfiguration.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
			wifiConfiguration.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			wifiConfiguration.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			wifiConfiguration.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
			wifiConfiguration.allowedProtocols
					.set(WifiConfiguration.Protocol.RSN);
			wifiConfiguration.allowedProtocols
					.set(WifiConfiguration.Protocol.WPA);
		}

		mWifiManager.setWifiEnabled(true);

		int res = mWifiManager.addNetwork(wifiConfiguration);
		boolean connectionSuccess = mWifiManager.enableNetwork(res, true);

		boolean changeHappen = mWifiManager.saveConfiguration();

		if (res != -1 && changeHappen && connectionSuccess) {
			return true;
		} else {
			return false;
		}
	}

	public String getScanResultSecurity(ScanResult scanResult) {
		final String cap = scanResult.capabilities;
		final String[] securityModes = { "WEP", "PSK", "EAP" };

		for (int i = securityModes.length - 1; i >= 0; i--) {
			if (cap.contains(securityModes[i])) {
				return securityModes[i];
			}
		}

		return "OPEN";
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mWifiReceiver);
		unregisterReceiver(mWifiScanReceiver);
		super.onDestroy();
	}

	public void playNotifyTune() {
		MediaPlayer player = MediaPlayer.create(this, R.raw.notification);
		player.setLooping(false); // Set looping
		player.setVolume(100, 100);
		player.start();
	}

}
