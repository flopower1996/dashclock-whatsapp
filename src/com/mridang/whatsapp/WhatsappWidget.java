package com.mridang.whatsapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.TimeoutException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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

		try {

			PackageManager pkgManager = getPackageManager();
			ittApplication = pkgManager.getLaunchIntentForPackage("com.whatsapp");
			ittApplication.addCategory(Intent.CATEGORY_LAUNCHER);

		} catch (Exception e) {
			return;
		}

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

						Command cmdQuery = new Command(0, "cd  /data/data/com.whatsapp/databases/", "sqlite3 wa.db \"SELECT display_name, unseen_msg_count FROM wa_contacts WHERE unseen_msg_count > 0;\"") {

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
						RootTools.getShell(true).add(cmdQuery).waitForFinish();

						Integer intMessages = Integer.parseInt(edtInformation.status() == null ? "0" : edtInformation.status());
						edtInformation.status(getResources().getQuantityString(R.plurals.message, intMessages, intMessages));
						edtInformation.visible(intMessages > 0);
						if (ittApplication != null)
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
					AssetManager assManager = this.getAssets();

					Log.d("WhatsappWidget", "Unpacking the Sqlite binary");
					InputStream istData = null;
					OutputStream ostFile = null;
					try {

						istData = assManager.open("sqlite3");
						String strFilename = "/data/data/" + this.getPackageName() + "/sqlite3";
						ostFile = new FileOutputStream(strFilename);

						byte[] buffer = new byte[1024];
						int read;
						while ((read = istData.read(buffer)) != -1) {
							ostFile.write(buffer, 0, read);
						}
						istData.close();
						istData = null;
						ostFile.flush();
						ostFile.close();
						ostFile = null;

						Log.d("WhatsappWidget", "Installled successfully");
						
					} catch (Exception e) {
						Log.e("WhatsappWidget", "Error unpacking Sqlite", e);
						BugSenseHandler.sendException(e);
						return;
					}

					Log.d("WhatsappWidget", "Installing the Sqlite binary");
					try {

						Command cmdInstall = new Command(0, "mount -o remount,rw /system", "cp /data/data/" + this.getPackageName() + "/sqlite3 /system/bin/sqlite3", "chmod 4755 /system/bin/sqlite3", "mount -o remount,ro /system") {

							@Override
							public void output(int arg0, String strLine) {								

								System.out.println(strLine);								

							}

						};
						RootTools.getShell(true).add(cmdInstall).waitForFinish();
						Log.d("WhatsappWidget", "Installled successfully");

					} catch (Exception e) {
						Log.e("WhatsappWidget", "Error installing Sqlite", e);
						BugSenseHandler.sendException(e);
						return;
					}

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