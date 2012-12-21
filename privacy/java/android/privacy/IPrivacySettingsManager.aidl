package android.privacy;
import android.privacy.PrivacySettings;
import android.privacy.ImmutablePrivacySettings;

/** {@hide} */
interface IPrivacySettingsManager
{
    PrivacySettings getSettings(String packageName);
    ImmutablePrivacySettings getImmutableSettings(String packageName);
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
}
