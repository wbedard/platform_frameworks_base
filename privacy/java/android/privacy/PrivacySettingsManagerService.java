package android.privacy;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.privacy.PrivacySettings;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PrivacySettingsManager's counterpart running in the system process, which
 * allows write access to /data/
 * @author Svyatoslav Hresyk
 * TODO: add selective contact access management API
 * {@hide}
 */
public class PrivacySettingsManagerService extends IPrivacySettingsManager.Stub {

    private static final String TAG = "PrivacySettingsManagerService";
    
    private static final String WRITE_PRIVACY_SETTINGS = "android.privacy.WRITE_PRIVACY_SETTINGS";

    private static final String READ_PRIVACY_SETTINGS = "android.privacy.READ_PRIVACY_SETTINGS";

    private PrivacyPersistenceAdapter persistenceAdapter;

    private Context context;
    
    public static PrivacyFileObserver obs;
    
    private boolean enabled;
    private boolean notificationsEnabled;
    private boolean bootCompleted;
    
    private static final double VERSION = 1.51;
    
    /**
     * @hide - this should be instantiated through Context.getSystemService
     * @param context
     */
    public PrivacySettingsManagerService(Context context) {
        Log.i(TAG, "PrivacySettingsManagerService - initializing for package: " + context.getPackageName() + 
                " UID: " + Binder.getCallingUid());
        this.context = context;
        
        persistenceAdapter = new PrivacyPersistenceAdapter(context);
        obs = new PrivacyFileObserver("/data/system/privacy", this);
        
        enabled = persistenceAdapter.getValue(PrivacyPersistenceAdapter.SETTING_ENABLED).equals(PrivacyPersistenceAdapter.VALUE_TRUE);
        notificationsEnabled = persistenceAdapter.getValue(PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED).equals(PrivacyPersistenceAdapter.VALUE_TRUE);
        bootCompleted = false;
    }
    
    public PrivacySettings getSettings(String packageName) {
//        Log.d(TAG, "getSettings - " + packageName);
          if (enabled || context.getPackageName().equals("com.privacy.pdroid") || context.getPackageName().equals("com.privacy.pdroid.Addon") 
	      || context.getPackageName().equals("com.android.privacy.pdroid.extension"))  //we have to add our addon package here, to get real settings
//	  if (Binder.getCallingUid() != 1000)
//            	context.enforceCallingPermission(READ_PRIVACY_SETTINGS, "Requires READ_PRIVACY_SETTINGS");
          return persistenceAdapter.getSettings(packageName, false);
          else return null;
    }

    public boolean saveSettings(PrivacySettings settings) {
        Log.d(TAG, "saveSettings - checking if caller (UID: " + Binder.getCallingUid() + ") has sufficient permissions");
        // check permission if not being called by the system process
	//if(!context.getPackageName().equals("com.privacy.pdroid.Addon")){ //enforce permission, because declaring in manifest doesn't work well -> let my addon package save settings
        	if (Binder.getCallingUid() != 1000)
            		context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");
	//}
        Log.d(TAG, "saveSettings - " + settings);
        boolean result = persistenceAdapter.saveSettings(settings);
        if (result == true) obs.addObserver(settings.getPackageName());
        return result;
    }
    
    public boolean deleteSettings(String packageName) {
//        Log.d(TAG, "deleteSettings - " + packageName + " UID: " + uid + " " +
//        		"checking if caller (UID: " + Binder.getCallingUid() + ") has sufficient permissions");
        // check permission if not being called by the system process
	//if(!context.getPackageName().equals("com.privacy.pdroid.Addon")){//enforce permission, because declaring in manifest doesn't work well -> let my addon package delete settings
        	if (Binder.getCallingUid() != 1000)
            		context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");
	//}
        boolean result = persistenceAdapter.deleteSettings(packageName);
        // update observer if directory exists
        String observePath = PrivacyPersistenceAdapter.SETTINGS_DIRECTORY + "/" + packageName;
        if (new File(observePath).exists() && result == true) {
            obs.addObserver(observePath);
        } else if (result == true) {
            obs.children.remove(observePath);
        }
        return result;
    }
    
    public double getVersion() {
        return VERSION;
    }
    
    public void notification(final String packageName, final byte accessMode, final String dataType, final String output) {
        if (bootCompleted && notificationsEnabled) {
	    Intent intent = new Intent();
            intent.setAction(PrivacySettingsManager.ACTION_PRIVACY_NOTIFICATION);
            intent.putExtra("packageName", packageName);
            intent.putExtra("uid", PrivacyPersistenceAdapter.DUMMY_UID);
            intent.putExtra("accessMode", accessMode);
            intent.putExtra("dataType", dataType);
            intent.putExtra("output", output);
            context.sendBroadcast(intent);
        }
    }
    
    public void registerObservers() {
        context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");        
        obs = new PrivacyFileObserver("/data/system/privacy", this);
    }
    
    public void addObserver(String packageName) {
        context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");        
        obs.addObserver(packageName);
    }
    
    public boolean purgeSettings() {
        return persistenceAdapter.purgeSettings();
    }
    
    public void setBootCompleted() {
        bootCompleted = true;
    }
    
    public boolean setNotificationsEnabled(boolean enable) {
        String value = enable ? PrivacyPersistenceAdapter.VALUE_TRUE : PrivacyPersistenceAdapter.VALUE_FALSE;
        if (persistenceAdapter.setValue(PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED, value)) {
            this.notificationsEnabled = true;
            this.bootCompleted = true;
            return true;
        } else {
            return false;
        }
    }
    
    public boolean setEnabled(boolean enable) {
        String value = enable ? PrivacyPersistenceAdapter.VALUE_TRUE : PrivacyPersistenceAdapter.VALUE_FALSE;
        if (persistenceAdapter.setValue(PrivacyPersistenceAdapter.SETTING_ENABLED, value)) {
            this.enabled = true;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Get all matching privacy settings for the provided PID or UID. Because we don't have
     * the app context, we infer the package name PID, and failing that from UID.
     * One UID can be tied to multiple packages: settings for all packages matching
     * the PID or UID are returned
     * @param pid
     * @param uid
     * @return  List of PrivacySettings for the provided PID, or failing that for the provided UID
     */
    public List<PrivacySettings> getSettingsByPidUid(int pid, int uid) throws RemoteException {
		IActivityManager activityManager = ActivityManagerNative.getDefault();
		
		String [] packageNames = null;
		
		// We can detect the current App process, and the current Service process.
		// However, we can't detect the current 'task' process
		if (pid != 0) {
			try {
				for(ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()){
		    		if(processInfo.pid == pid){
		    			Log.v(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: Detected app using camera:" + processInfo.processName);
		    			packageNames = new String [] {processInfo.processName};
		    		}
		    	}
			} catch (Exception e) {
				Log.e(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: Error occurred while attempting to get running app processes");
			}

			try {
		    	for(ActivityManager.RunningServiceInfo processInfo : (List<ActivityManager.RunningServiceInfo>)activityManager.getServices(100000,0)){
		    		if (processInfo.pid == Process.myPid()){
		    			Log.v(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: Detected service using camera:" + processInfo.clientPackage);
		    			packageNames = new String [] {processInfo.clientPackage};
		    		}
		    	}
			} catch (Exception e) {
				Log.e(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: Error occurred while attempting to get services processes");
			}
			
			if (packageNames == null) {
				Log.d(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: did not find process matching current PID. Attempting to use PackageManager.");
			}
		} else {
			Log.d(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: Provided PID was 0, so could not detect package name from PID. Attempting to use PackageManager");
		}

		if (packageNames == null && uid != 0) {
	    	try{
				IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
	    		packageNames = packageManager.getPackagesForUid(uid);
	    		Log.v(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: Detected " + Integer.toString(packageNames.length) + " possible apps using camera by uid:" + Integer.toString(uid));
	    	} catch(Exception e){
	    		e.printStackTrace();
	    		Log.d(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: An exception occurred while reading package names ", e);
	    		throw new RemoteException();
	    	}
		} else {
			Log.d(TAG,"PrivacySettingsManagerService:getSettingsByPidUid: Provided UID was 0, so could not detect package name from UID");
		}
		
		if (packageNames == null || packageNames.length == 0) {
			Log.d(TAG,"PrivacySettingsManagerService:getPackageName: Failed to obtain package name with either PID or UID");
			throw new RemoteException();
		}
		
    	try{
    		List<PrivacySettings> privacySettingsList = new ArrayList<PrivacySettings>(packageNames.length);
    		PrivacySettings pSet = null;
    		
    		for(String packageName : packageNames){
    			this.getSettings(packageName);
    			//No settings is interpreted as 'allow'
    			if(pSet != null){
    				privacySettingsList.add(pSet);
    			}
    		}
    		
    		// if there are no matching settings, return an empty package list.
    		// this (poorly) allows differentiation between an error (which should lead to 'denied' behaviour),
    		// and no settings being found (which should lead to 'allow' behaviour
    		return privacySettingsList;
    	}
    	catch (Exception e){
    		Log.e(TAG,"PrivacySettingsManagerService:getPackageName: An exception occurred while retrieving settings for packages", e);
    		throw new RemoteException();
    	}
    }
}
