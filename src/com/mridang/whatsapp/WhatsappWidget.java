package com.mridang.whatsapp;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

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

	/* This is the launch intent using for starting the Hangouts application */
	private Intent ittApplication;

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("HangoutsWidget", "Created");
		BugSenseHandler.initAndStartSession(this, "fb588bc8");

		PackageManager pkgManager = getPackageManager();
		ittApplication = pkgManager.getLaunchIntentForPackage("com.whatsapp");
		ittApplication.addCategory(Intent.CATEGORY_LAUNCHER);

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

				Log.d("WhatsappWidget", "Checking if Sqlite is installed");
				if (RootTools.exists("/system/xbin/sqlite3") || RootTools.exists("/system/bin/sqlite3")) {

					Log.d("WhatsappWidget", "Reading unread messages from the databases");
					try {

						Command command = new Command(0, "cd  /data/data/com.whatsapp/databases/", "sqlite3 wa.db \"SELECT display_name, unseen_msg_count FROM wa_contacts WHERE unseen_msg_count > 0;\"") {

							@Override
							public void output(int id, String strLine) {

								try {

									edtInformation.status(Integer.toString((edtInformation.status() == null ? 0 : Integer.parseInt(edtInformation.status())) + Integer.parseInt(strLine.split("\\|")[1]))); 

									if (edtInformation.expandedBody() == null || !edtInformation.expandedBody().contains(strLine)) {
										edtInformation.expandedBody((edtInformation.expandedBody() == null ? "" : edtInformation.expandedBody() + "\n") + strLine.split("\\|")[0]);
									}

								} catch (Exception e) {
									BugSenseHandler.sendException(e);
								}

							}

						};
						RootTools.getShell(true).add(command).waitForFinish();

						Integer intMessages = Integer.parseInt(edtInformation.status() == null ? "0" : edtInformation.status());
						edtInformation.status(getResources().getQuantityString(R.plurals.message, intMessages, intMessages));
						edtInformation.visible(intMessages > 0);
						edtInformation.clickIntent(ittApplication);
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
						Log.e("WhatsappWidget", "Encountered an error", e);
						BugSenseHandler.sendException(e);
					}

				} else {
					Log.w("WhatsappWidget", "Sqlite executable doesn't seem to be installed");
					BugSenseHandler.sendException(new Exception("Sqlite executable doesn't seem to be installed"));
				}	

			} else {
				Log.w("WhatsappWidget", "Contacts database doesn't exist");
			}			

		} else {
			Log.w("WhatsappWidget", "The device is not rooted");
			Toast.makeText(getApplicationContext(), R.string.unrooted_error, Toast.LENGTH_LONG).show();
		}

		Log.d("WhatsappWidget", "Publishing update");
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