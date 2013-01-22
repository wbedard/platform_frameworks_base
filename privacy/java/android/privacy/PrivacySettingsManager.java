/**
 * Copyright (C) 2012 Svyatoslav Hresyk
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 */

package android.privacy;

import java.util.Map;
import android.content.Context;
import android.os.RemoteException;
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacyServiceDisconnectedException;
import android.privacy.PrivacyServiceInvalidException;
import android.privacy.PrivacySettingsManagerService;
import android.util.Log;

/**
 * Provides API access to the privacy settings
 * @author Svyatoslav Hresyk
 * TODO: selective contacts access
 * {@hide}
 */
public final class PrivacySettingsManager {

    private static final String TAG = "PrivacySettingsManager";

    public static final String ACTION_PRIVACY_NOTIFICATION = "com.privacy.pdroid.PRIVACY_NOTIFICATION";
    public static final String ACTION_PRIVACY_NOTIFICATION_ADDON = "com.privacy.pdroid.PRIVACY_NOTIFICATION_ADDON";
    private static final String SERVICE_CLASS = "android.privacy.IPrivacySettingsManager.Stub.Proxy";

    private IPrivacySettingsManager service;
    //private android.privacy.IPrivacySettingsManager.Stub.Proxy service;

    /**
     * @hide - this should be instantiated through Context.getSystemService
     * @param context
     */
    public PrivacySettingsManager(Context context, IPrivacySettingsManager service) {
        //        Log.d(TAG, "PrivacySettingsManager - initializing for package: " + context.getPackageName() + 
        //                " UID:" + Binder.getCallingUid());
        try {
            Log.d(TAG, "PrivacySettingsManager:PrivacySettingsManager: service is of class: " + service.getClass().getCanonicalName());
        } catch (Exception e) {
            Log.d(TAG, "PrivacySettingsManager:PrivacySettingsManager: service class not known: exception happened", e);
        }

        this.service = service;
    }

    @Deprecated
    public PrivacySettings getSettings(String packageName, int uid)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        return getSettings(packageName);
    }

    public PrivacySettings getSettings(String packageName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();
        try {
            return service.getSettings(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:getSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }


    public boolean saveSettings(PrivacySettings settings)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();
        try {
            return service.saveSettings(settings);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:saveSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }


    public boolean deleteSettings(String packageName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();
        try {
            return service.deleteSettings(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:deleteSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }


    @Deprecated
    public boolean deleteSettings(String packageName, int uid)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        return deleteSettings(packageName);
    }

    @Deprecated
    public void notification(String packageName, int uid, byte accessMode, String dataType, String output, PrivacySettings pSet) {
        notification(packageName, accessMode, dataType, output);
    }

    @Deprecated
    public void notification(String packageName, byte accessMode, String dataType, String output, PrivacySettings pSet) {
        notification(packageName, accessMode, dataType, output);
    }

    public void notification(String packageName, byte accessMode, String dataType, String output) {
        try {
            if (this.isServiceAvailable() && this.isServiceValid()) {
                service.notification(packageName, accessMode, dataType, output);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:notification: Exception occurred in the remote privacy service", e);
        }
    }

    public void registerObservers()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            service.registerObservers();
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:registerObservers: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void addObserver(String packageName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            service.addObserver(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:addObserver: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public boolean purgeSettings()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();
        try {
            return service.purgeSettings();
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:purgeSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    @Deprecated
    public double getVersion() {
        return PrivacySettingsManagerService.API_VERSION;
    }

    public double getApiVersion() {
        return PrivacySettingsManagerService.API_VERSION;
    }

    public double getModVersion() {
        return PrivacySettingsManagerService.MOD_VERSION;
    }

    public String getModDetails() {
        return PrivacySettingsManagerService.MOD_DETAILS;
    }

    public boolean setEnabled(boolean enable)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            return service.setEnabled(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:addObserver: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public boolean setNotificationsEnabled(boolean enable)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            return service.setNotificationsEnabled(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:setNotificationsEnabled: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void setBootCompleted()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            service.setBootCompleted();
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:setBootCompleted: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void setDebugFlagInt(String flagName, int value)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            service.setDebugFlagInt(flagName, value);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:setDebugFlagInt: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public Integer getDebugFlagInt(String flagName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            return service.getDebugFlagInt(flagName);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:getDebugFlagInt: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void setDebugFlagBool(String flagName, boolean value) throws PrivacyServiceDisconnectedException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            service.setDebugFlagBool(flagName, value);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:setDebugFlagBool: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public Boolean getDebugFlagBool(String flagName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            return service.getDebugFlagBool(flagName);
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:getDebugFlagBoolInt: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public Map<String, Integer> getDebugFlags()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.isServiceAvailableOrThrow();
        this.isServiceValidOrThrow();

        try {
            return service.getDebugFlags();
        } catch (RemoteException e) {
            Log.e(TAG, "PrivacySettingsManager:getDebugFlags: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    /**
     * Checks that the 
     * @return true if service class name matches expectations, otherwise false
     */
    public boolean isServiceValid() {
        if (this.service.getClass().equals(SERVICE_CLASS)) {
            return true;
        } else {
            Log.e(TAG, "PrivacySettingsManager:isServiceValid:PrivacySettingsManagerService is of an incorrect class (" + service.getClass().getCanonicalName() + ")");
            return false;
        }
    }

    private void isServiceValidOrThrow() throws PrivacyServiceInvalidException {
        if (!this.isServiceValid()) {
            throw new PrivacyServiceInvalidException();
        }
    }

    /**
     * Checks whether the PrivacySettingsManagerService is available. For some reason,
     * occasionally it appears to be null. In this case it should be initialized again.
     * @return true if service is connected, otherwise false
     */
    public boolean isServiceAvailable() {
        if (service == null) {
            Log.e(TAG, "PrivacySettingsManager:isServiceAvailable:PrivacySettingsManagerService is null");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks whether the PrivacySettingsManagerService is available. For some reason,
     * occasionally it appears to be null. In this case it should be initialized again.
     */
    private void isServiceAvailableOrThrow() throws PrivacyServiceDisconnectedException {
        if (!this.isServiceAvailable()) {
            throw new PrivacyServiceDisconnectedException();
        }
    }
}
