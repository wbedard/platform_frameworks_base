package android.privacy;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.List;

/**
 * PrivacySettingsManager's counterpart running in the system process, which
 * allows write access to /data/
 * 
 * @author Svyatoslav Hresyk TODO: add selective contact access management API
 *         {@hide}
 */
public class PrivacySettingsManagerService extends IPrivacySettingsManager.Stub {

	private static final String TAG = "PrivacySettingsManagerService";
	private static final String WRITE_PRIVACY_SETTINGS = "android.privacy.WRITE_PRIVACY_SETTINGS";
	private static final String READ_PRIVACY_SETTINGS = "android.privacy.READ_PRIVACY_SETTINGS";
	private static final boolean LOG_EVERYTHING = true;

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
		Log.i(TAG, "PrivacySettingsManagerService - initializing for package: "
				+ context.getPackageName() + " UID: " + Binder.getCallingUid());
		this.context = context;

		persistenceAdapter = new PrivacyPersistenceAdapter(context);
		obs = new PrivacyFileObserver("/data/system/privacy", this);

		enabled = persistenceAdapter.getValue(
				PrivacyPersistenceAdapter.SETTING_ENABLED).equals(
						PrivacyPersistenceAdapter.VALUE_TRUE);
		notificationsEnabled = persistenceAdapter.getValue(
				PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED)
				.equals(PrivacyPersistenceAdapter.VALUE_TRUE);
		bootCompleted = false;
	}

	public PrivacySettings getSettings(String packageName) throws RemoteException {
		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:getSettings for " + packageName + " from UID " + Binder.getCallingUid());
		if (packageName == null || packageName.isEmpty()) {
			throw new RemoteException("A packageName must be provided");
		}

		if (enabled || checkCallerCanReadSettings()) {
			return persistenceAdapter.getSettings(packageName);
		} else {
			return null;
		}
	}

	@Override
	public List<PrivacySettings> getSettingsAll() throws RemoteException {
		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:getSettingsAll from UID " + Binder.getCallingUid());
		if (enabled || checkCallerCanReadSettings()) {
			return persistenceAdapter.getSettingsAll();
		} else {
			return null;
		}
	}

	
	@Override
	public List<PrivacySettings> getSettingsMany(List<String> packageNames)
			throws RemoteException {
		if (packageNames == null || packageNames.size() == 0) {
			throw new RemoteException("getSettingsMayn: PackageNames must be provided.");
		}
		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:getSettingsMany (" + packageNames.size() + ") from UID " + Binder.getCallingUid());
		
		if (enabled || checkCallerCanReadSettings()) {
			return persistenceAdapter.getSettingsMany(packageNames);
		} else {
			return null;
		}
	}
	
	public boolean saveSettings(PrivacySettings settings)
			throws RemoteException {
		// check permission of the caller
		checkCallerCanWriteOrThrow();
		
		if (settings == null) {
			throw new RemoteException("saveSettings: Settings must be provided");
		}
		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:saveSettings for package " + settings.getPackageName() + " from UID " + Binder.getCallingUid());
		boolean result = persistenceAdapter.saveSettings(settings);

		// Not sure what the files on the file system are for at
		// this stage, or why they need to be watched...
		if (result == true) {
			obs.addObserver(settings.getPackageName());
		}
		return result;
	}

	@Override
	public boolean saveSettingsMany(List<PrivacySettings> settingsList)
			throws RemoteException {
		checkCallerCanWriteOrThrow();
		
		if (settingsList == null || settingsList.size() == 0) {
			throw new RemoteException("saveSettingsMany: Settings must be provided");
		}

		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:saveSettingsMany (" + settingsList.size() + ") from UID " + Binder.getCallingUid());
		
		List<Boolean> resultList = persistenceAdapter.saveSettingsMany(settingsList);
		// if result list is null, no entries fot processed
		if (resultList == null) {
			return false;
		}
		boolean result = true;
		
		Iterator<Boolean> resultIterator = resultList.iterator();
		for (PrivacySettings settings : settingsList) {
			if (resultIterator.next()) {
				obs.addObserver(settings.getPackageName());
			} else {
				result = false;
			}
		}
		
		return result;
	}
	
	public boolean deleteSettings(String packageName)
			throws RemoteException {
		checkCallerCanWriteOrThrow();

		if (packageName == null) {
			throw new RemoteException("deleteSettings: PackageName must be provided");
		}

		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:deleteSettings for package " + packageName + " from UID " + Binder.getCallingUid());
		
		boolean result = persistenceAdapter.deleteSettings(packageName);
		// update observer if directory exists
		String observePath = PrivacyPersistenceAdapter.SETTINGS_DIRECTORY + "/"
				+ packageName;
		if (new File(observePath).exists() && result == true) {
			obs.addObserver(observePath);
		} else if (result == true) {
			obs.children.remove(observePath);
		}
		return result;
	}
	
	@Override
	public boolean deleteSettingsMany(List<String> packageNames)
			throws RemoteException {
		checkCallerCanWriteOrThrow();
		
		if (packageNames == null || packageNames.size() == 0) {
			throw new RemoteException("deleteSettingsMayn: Package names must be provided");
		}
		
		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:deleteSettingsMany (" + packageNames.size() + ") from UID " + Binder.getCallingUid());
		
		List<Boolean> resultList = persistenceAdapter.deleteSettingsMany(packageNames);
		// if result list is null, no entries fot processed
		if (resultList == null) {
			return false;
		}
		boolean result = true;
		boolean thisResult;
		
		Iterator<Boolean> resultIterator = resultList.iterator();
		for (String packageName : packageNames) {
			thisResult = resultIterator.next();
			String observePath = PrivacyPersistenceAdapter.SETTINGS_DIRECTORY + "/"
					+ packageName;

			if (thisResult) {
				if (new File(observePath).exists()) {
					obs.addObserver(observePath);
				} else {
					obs.children.remove(observePath);
				}
			} else {
				result = false;
			}
		}

		return result;
	}
	
	@Override
	public boolean deleteSettingsAll() throws RemoteException {
		checkCallerCanWriteOrThrow();
		
		if (LOG_EVERYTHING) Log.v(TAG, "PrivacySettingsManagerService:deleteSettingsAll from UID " + Binder.getCallingUid());
		
		//String will be the package name, boolean whether the delete was successful or not
		List<SimpleImmutableEntry<String, Boolean>> resultList = persistenceAdapter.deleteSettingsAll();
		// if result list is null, no entries fot processed
		if (resultList == null) {
			return false;
		}
		boolean result = true;
		
		for (SimpleImmutableEntry<String, Boolean> entry : resultList) {
			String observePath = PrivacyPersistenceAdapter.SETTINGS_DIRECTORY + "/"
					+ entry.getKey();

			if (entry.getValue()) {
				if (new File(observePath).exists()) {
					obs.addObserver(observePath);
				} else {
					obs.children.remove(observePath);
				}
			} else {
				result = false;
			}
		}

		return result;
	}
	
	public double getVersion() {
		return VERSION;
	}

	public void notification(final String packageName, final byte accessMode,
			final String dataType, final String output) {
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
		context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS,
				"Requires WRITE_PRIVACY_SETTINGS");
		obs = new PrivacyFileObserver("/data/system/privacy", this);
	}

	public void addObserver(String packageName) {
		context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS,
				"Requires WRITE_PRIVACY_SETTINGS");
		obs.addObserver(packageName);
	}

	public boolean purgeSettings() {
		return persistenceAdapter.purgeSettings();
	}

	public void setBootCompleted() {
		bootCompleted = true;
	}

	public boolean setNotificationsEnabled(boolean enable) {
		String value = enable ? PrivacyPersistenceAdapter.VALUE_TRUE
				: PrivacyPersistenceAdapter.VALUE_FALSE;
		if (persistenceAdapter.setValue(
				PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED, value)) {
			this.notificationsEnabled = true;
			this.bootCompleted = true;
			return true;
		} else {
			return false;
		}
	}

	public boolean setEnabled(boolean enable) {
		String value = enable ? PrivacyPersistenceAdapter.VALUE_TRUE
				: PrivacyPersistenceAdapter.VALUE_FALSE;
		if (persistenceAdapter.setValue(
				PrivacyPersistenceAdapter.SETTING_ENABLED, value)) {
			this.enabled = true;
			return true;
		} else {
			return false;
		}
	}



	/**
	 * Check the caller of the service has privileges to write to it
	 * Throw an exception if not. 
	 */
	private void checkCallerCanWriteOrThrow() {
		if (Binder.getCallingUid() == 1000) {
			return;
		}
		context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS,
				"Requires WRITE_PRIVACY_SETTINGS");
		//for future:
		// if not allowed then throw
		//			throw new SecurityException("Attempted to write without sufficient priviliges");

	}
	
	/**
	 * Check that the caller of the service has privileges to write to it.
	 * @return true if caller can write, false otherwise.
	 */
	private boolean checkCallerCanWriteSettings() {
		try {
			checkCallerCanWriteOrThrow();
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Check the caller of the service has privileges to read from it
	 * Throw an exception if not. 
	 */
	private void checkCallerCanReadOrThrow() {
		if (Binder.getCallingUid() == 1000) {
			return;
		}
		context.enforceCallingPermission(READ_PRIVACY_SETTINGS,
				"Requires READ_PRIVACY_SETTINGS");
		//for future:
		// if not allowed then throw
		//			throw new SecurityException("Attempted to read without sufficient priviliges");

	}
	
	/**
	 * Check that the caller of the service has privileges to read from it.
	 * @return true if caller can read, false otherwise.
	 */
	private boolean checkCallerCanReadSettings() {
		try {
			checkCallerCanReadOrThrow();
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}
}
