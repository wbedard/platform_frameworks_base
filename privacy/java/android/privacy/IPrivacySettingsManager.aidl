package android.privacy;
import android.privacy.PrivacySettings;

/** {@hide} */
interface IPrivacySettingsManager
{
    PrivacySettings getSettings(String packageName);
    List<PrivacySettings> getSettingsMany(in List<String> packageName);
    List<PrivacySettings> getSettingsAll();
    boolean saveSettings(in PrivacySettings settings);
    boolean saveSettingsMany(in List<PrivacySettings> settings);
    boolean deleteSettings(String packageName);
    boolean deleteSettingsMany(in List<String> packageName);
    boolean deleteSettingsAll();
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
