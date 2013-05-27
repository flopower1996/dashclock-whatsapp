package com.mridang.whatsapp;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;

/*
 * This class is the main class that provides the widget
 */
@SuppressLint("SdCardPath")
public class WhatsappWidget extends DashClockExtension {

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("WhatsappWidget", "Created");
		BugSenseHandler.initAndStartSession(this, "fb588bc8");

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int arg0) {

		setUpdateWhenScreenOn(true);

		Log.d("WhatsappWidget", "Checking for unread Whatsapp messages");
		final ExtensionData edtInformation = new ExtensionData();
		edtInformation.visible(false);

		Log.d("WhatsappWidget", "Checking if the device is rooted");	
		if (RootTools.isRootAvailable()) {

			Log.d("WhatsappWidget", "Checking if the contacts database exists");
			if (RootTools.exists("/data/data/com.whatsapp/databases/wa.db")) {

				Log.d("WhatsappWidget", "Reading unread messages from the databases");
				try {

					Command command = new Command(0, "cd  /data/data/com.whatsapp/databases/", "sqlite3 wa.db \"SELECT display_name FROM wa_contacts WHERE unseen_msg_count > 0;\"") {

						@Override
						public void output(int id, String strLine) {

							edtInformation.expandedBody((edtInformation.expandedBody() == null ? "" : edtInformation.expandedBody() + "\n") + strLine);

						}

					};
					RootTools.getShell(true).add(command).waitForFinish();

					edtInformation.status(String.format(getString(R.string.status), (edtInformation.expandedBody() == null ? 0 : edtInformation.expandedBody().split("\n").length)));
				  edtInformation.visible(edtInformation.expandedBody() != null && edtInformation.expandedBody().split("\n").length > 0);
				  Log.d("WhatsappWidget", (edtInformation.expandedBody() == null ? 0 : edtInformation.expandedBody().split("\n").length) + " unread");		

				} catch (InterruptedException e) {
					Log.w("WhatsappWidget", "Command execution interrupted", e);
				} catch (IOException e) {
					Log.w("WhatsappWidget", "Input output error", e);
				} catch (TimeoutException e) {
					Log.w("WhatsappWidget", "Command timed out", e);
				} catch (RootDeniedException e) {
					Log.w("WhatsappWidget", "Root access denied", e);
				} catch (Exception e) {
					BugSenseHandler.sendException(e);
				}

			} else {
				Log.d("WhatsappWidget", "Contacts database doesn't exist");
			}			

		} else {
			Log.d("WhatsappWidget", "The device is not rooted");
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		publishUpdate(edtInformation);
		Log.d("WhatsappWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();
		Log.d("WhatsappWidget", "Destroyed");
		BugSenseHandler.closeSession(this);

	}

}