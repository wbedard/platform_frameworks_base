package android.privacy;
import android.privacy.PrivacySettings;

/** {@hide} */
interface IPrivacySettingsManager
{
    PrivacySettings getSettings(String packageName);
    boolean saveSettings(in PrivacySettings settings);
    boolean deleteSettings(String packageName);
    void notification(String packageName, byte accessMode, String dataType, String output);
    void registerObservers();
    void addObserver(String packageName);
    boolean purgeSettings();
    double getVersion();
    boolean setEnabled(boolean enable);
    boolean setNotificationsEnabled(boolean enable);
    void setBootCompleted();
    boolean getIsAuthorizedManagerApp(String packageName);
    void authorizeManagerAppKeys(String packageName);
	void authorizeManagerAppKey(String packageName, String publicKey);
	void authorizeManagerAppSignatures(String packageName);
    void deauthorizeManagerApp(String packageName);
    void deauthorizeManagerAppKeys(String packageName);
    void deauthorizeManagerAppSignatures(String packageName);
}