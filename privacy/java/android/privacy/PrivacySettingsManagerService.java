package android.privacy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    
    private static final String MANAGE_PRIVACY_APPLICATIONS = "android.privacy.MANAGE_PRIVACY_APPLICATIONS";

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

    public boolean saveSettings(PrivacySettings settings) throws RemoteException {
        Log.d(TAG, "saveSettings - checking if caller (UID: " + Binder.getCallingUid() + ") has sufficient permissions");
        // check permission if not being called by the system process
	//if(!context.getPackageName().equals("com.privacy.pdroid.Addon")){ //enforce permission, because declaring in manifest doesn't work well -> let my addon package save settings
        	if (Binder.getCallingUid() != 1000) {
            		context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");
        			if (!getIsAuthorizedManagerApp(Binder.getCallingPid())) {
        				throw new SecurityException("Application must be authorised to save changes");
        			}
        	}
	//}
        Log.d(TAG, "saveSettings - " + settings);
        boolean result = persistenceAdapter.saveSettings(settings);
        if (result == true) obs.addObserver(settings.getPackageName());
        return result;
    }
    
    public boolean deleteSettings(String packageName) throws RemoteException {
//        Log.d(TAG, "deleteSettings - " + packageName + " UID: " + uid + " " +
//        		"checking if caller (UID: " + Binder.getCallingUid() + ") has sufficient permissions");
        // check permission if not being called by the system process
	//if(!context.getPackageName().equals("com.privacy.pdroid.Addon")){//enforce permission, because declaring in manifest doesn't work well -> let my addon package delete settings
        	if (Binder.getCallingUid() != 1000) {
            		context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");
					if (!getIsAuthorizedManagerApp(Binder.getCallingPid())) {
						throw new SecurityException("Application must be authorised to save changes");
					}
        	}
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
    
    public boolean getIsAuthorizedManagerApp(int pid) throws RemoteException {
    	Log.d(TAG, "PrivacyPersistenceAdapter:getIsAuthorizedManagerApp(pid):Getting packages for PID " + Integer.toString(pid));

    	String packageName = null;
    	ActivityManager actMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    	for(ActivityManager.RunningAppProcessInfo processInfo : actMgr.getRunningAppProcesses()){
    		if(processInfo.pid == pid){
    			packageName = processInfo.processName;
    		}
    	}
    	if (packageName == null) {
    		Log.e(TAG, "PrivacyPersistenceAdapter:getIsAuthorizedManagerApp(pid):Package name could not be obtained from PID");
    		return false;
    	}

    	return getIsAuthorizedManagerApp(packageName);
    }

    public boolean getIsAuthorizedManagerApp(String packageName) throws RemoteException {
    	Signature [] signatures = getSignatures(packageName);
    	if (signatures == null) {
    		Log.e(TAG, "PrivacySettingsManagerService:getIsAuthorizedManagerApp: Could not obtain signatures for the app");
    		throw new RemoteException();
    	}
    	
    	Set<String> publicKeys = getPublicKeysForSignatures(signatures);
    	Set<String> signaturesSet = new HashSet<String>();

    	for (Signature signature : signatures) {
    		signaturesSet.add(signature.toCharsString());
    	}

    	return persistenceAdapter.getIsAuthorizedManagerApp(packageName, publicKeys, signaturesSet, false);
    }

    /**
     * Authorises all the keys on all the current signatures of the app to be
     * valid for identifying a package with access to the PDroid core.
     * This will allow future versions of the management app to be automatically 
     * authorised, if they are signed with the same key
     * (note that if they are signed with different keys, the user will
     * have to uninstall the app, as they will not be able to upgrade
     * due to Android signature restrictions)
     * 
     * @param packageName  Name of the package to be authorised
     * @throws RemoteException
     */
    public void authorizeManagerAppKeys(String packageName) throws RemoteException {
    	if (Binder.getCallingUid() != 1000)
    		context.enforceCallingPermission(MANAGE_PRIVACY_APPLICATIONS, "Requires MANAGE_PRIVACY_APPLICATIONS");

    	Signature [] signatures = getSignatures(packageName);
    	if (signatures == null || signatures.length == 0) {
    		Log.e(TAG, "PrivacySettingsManagerService:authorizeManagerApp: no signatures found for package:" + packageName);
    		throw new RemoteException();	
    	}
    	Set<String> publicKeys = getPublicKeysForSignatures(signatures);
    	if (publicKeys == null) {
    		Log.e(TAG, "PrivacySettingsManagerService:authorizeManagerApp: no public keys found for package:" + packageName);
    		throw new RemoteException();	
    	}

    	persistenceAdapter.authorizeManagerAppPublicKeys(packageName, publicKeys, false);
    }


    public void authorizeManagerAppKey(String packageName, String publicKey) throws RemoteException {
    	if (Binder.getCallingUid() != 1000)
    		context.enforceCallingPermission(MANAGE_PRIVACY_APPLICATIONS, "Requires MANAGE_PRIVACY_APPLICATIONS");

    	Signature [] signatures = getSignatures(packageName);
    	if (signatures == null || signatures.length == 0) {
    		Log.e(TAG, "PrivacySettingsManagerService:authorizeManagerApp: no signatures found for package:" + packageName);
    		throw new RemoteException();	
    	}
    	Set<String> publicKeys = getPublicKeysForSignatures(signatures);
    	if (publicKeys == null) {
    		Log.e(TAG, "PrivacySettingsManagerService:authorizeManagerApp: no public keys found for package:" + packageName);
    		throw new RemoteException();	
    	}
    	
    	if (publicKeys.contains(publicKey)) {
    		Set<String> inPublicKeys = new HashSet<String>();
    		inPublicKeys.add(publicKey);

    		persistenceAdapter.authorizeManagerAppPublicKeys(packageName, inPublicKeys, false);
    	}
    }


    @Override
    public void authorizeManagerAppSignatures(String packageName)
    		throws RemoteException {
    	if (Binder.getCallingUid() != 1000)
    		context.enforceCallingPermission(MANAGE_PRIVACY_APPLICATIONS, "Requires MANAGE_PRIVACY_APPLICATIONS");

    	Signature [] signatures = getSignatures(packageName);
    	if (signatures == null || signatures.length == 0) {
    		Log.e(TAG, "PrivacySettingsManagerService:authorizeManagerApp: no signatures found for package:" + packageName);
    		throw new RemoteException();	
    	}
    	Set<String> publicKeys = getPublicKeysForSignatures(signatures);
    	if (publicKeys == null) {
    		Log.e(TAG, "PrivacySettingsManagerService:authorizeManagerApp: no public keys found for package:" + packageName);
    		throw new RemoteException();	
    	}

    	persistenceAdapter.authorizeManagerAppPublicKeys(packageName, publicKeys, false);
    }

    // These functions (and their equivalents in the persistence adapter could
    // potentially be merged and a flag passed to identify what is being deleted,
    // simply to reduce the number of functions

    /**
     * Removes all authorisation (signature and public key) from the application
     * @param packageName  the application package name from which to remove all authorisation
     */
    @Override
    public void deauthorizeManagerApp(String packageName) {
    	if (Binder.getCallingUid() != 1000)
    		context.enforceCallingPermission(MANAGE_PRIVACY_APPLICATIONS, "Requires MANAGE_PRIVACY_APPLICATIONS");

    	persistenceAdapter.deauthorizeManagerApp(packageName, false);
    }

    /**
     * Removes all public key authorisation from the application.
     * If you want to change the authorised public keys for an application, use
     * 'authorizeManagerAppKeys'
     * @param packageName  the application package name from which to remove all authorisation
     */
    @Override
    public void deauthorizeManagerAppKeys(String packageName) {
    	if (Binder.getCallingUid() != 1000)
    		context.enforceCallingPermission(MANAGE_PRIVACY_APPLICATIONS, "Requires MANAGE_PRIVACY_APPLICATIONS");

    	persistenceAdapter.deauthorizeManagerAppKeys(packageName, false);
    }

    /**
     * Removes all signature authorisation from the application.
     * If you want to change the authorised signatures for an application, use
     * 'authorizeManagerAppSignatures'
     * @param packageName  the application package name from which to remove all authorisation
     */
    @Override
    public void deauthorizeManagerAppSignatures(String packageName) {
    	if (Binder.getCallingUid() != 1000)
    		context.enforceCallingPermission(MANAGE_PRIVACY_APPLICATIONS, "Requires MANAGE_PRIVACY_APPLICATIONS");

    	persistenceAdapter.deauthorizeManagerAppSignatures(packageName, false);
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
     * Returns all signatures from a package
     * @param packageName  name of package for which to read signature
     * @return  array of signatures from the package
     * @throws RemoteException
     */
    private Signature [] getSignatures(String packageName) throws RemoteException {
    	PackageManager pkgMgr = context.getPackageManager();
    	if (pkgMgr == null) {
    		Log.d(TAG, "PrivacySettingsManagerService:getSignatures: Package manager could not be obtained");
    		throw new RemoteException();
    	}
    	
    	PackageInfo pkgInfo;
    	try {
	    	//get the package info so we can get the signatures
	    	 pkgInfo = pkgMgr.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
    	} catch (NameNotFoundException e) {
    		Log.e(TAG, "PrivacySettingsManagerService:getSignatures: package not found:" + packageName);
    		throw new RemoteException();
    	}
    	if (pkgInfo == null) {
    		Log.e(TAG, "PrivacySettingsManagerService:getSignatures: getPackageInfo returned null for package:" + packageName);
    		throw new RemoteException();
    	}
    	
		Log.d(TAG, "PrivacySettingsManagerService:getSignatures: retrieving signatures for " + packageName);
		
		return pkgInfo.signatures;
    }

    
    /**
     * Extracts base64-encoded public keys from the provided signatures 
     * @param signatures  signatures from which to extract keys
     * @return base64-encoded public keys
     * @throws RemoteException
     */
    private Set<String> getPublicKeysForSignatures(Signature [] signatures) throws RemoteException {
		CertificateFactory certFactory;
		try {
			certFactory = CertificateFactory.getInstance("X509");
		} catch (CertificateException e) {
			Log.e(TAG, "PrivacyServicesManagerService:getPublicKeysForSignature:X509 certificate factory is not available", e);
			throw new RemoteException();
		}
		
		Set<String> publicKeys = new HashSet<String>();
	
		//parse certificate details from the signature
		for (Signature signature : signatures) {
			byte [] sigBytes = signature.toByteArray();
			try {
				X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(new ByteArrayInputStream(sigBytes));
				publicKeys.add(Base64.encodeToString(certificate.getPublicKey().getEncoded(), Base64.DEFAULT));
			} catch (CertificateException e) {
				Log.e(TAG, "PrivacyServicesManagerService:getPublicKeysForSignature:CertificateException occurred", e);
				throw new RemoteException();
			} catch (ClassCastException e) {
				Log.e(TAG, "PrivacyServicesManagerService:getPublicKeysForSignature:Signature certificate was not an X509 certificate", e);
				throw new RemoteException();
			}
		}
		
		return publicKeys;
    }
}
