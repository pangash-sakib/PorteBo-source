package com.sakib.portebo.update;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
public class UpdateInstallReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){if(i==null)return;int status=i.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE);if(status==PackageInstaller.STATUS_PENDING_USER_ACTION){Intent confirm=(Intent)i.getParcelableExtra(Intent.EXTRA_INTENT);if(confirm!=null){confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(confirm);}}} }
