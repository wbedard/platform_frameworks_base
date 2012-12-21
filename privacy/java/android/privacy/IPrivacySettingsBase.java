package android.privacy;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * Holds privacy settings for access to all private data types for a single application
 * @author Svyatoslav Hresyk 
 * {@hide} 
 */
public interface IPrivacySettingsBase {
    
    /**
     * Real value, provided by the unmodified Android framework.
     */
    public static final byte REAL = 0;
    
    /**
     * Empty or unavailable, depending on setting type. For String settings, it is
     * setter method caller's responsibility to make sure that the corresponding 
     * setting field will contain an empty String.
     */
    public static final byte EMPTY = 1;
    
    /**
     * Custom specified output, appropriate for relevant setting. For String settings, 
     * it is setter method caller's responsibility to make sure that the corresponding 
     * setting field will contain a custom String.
     */
    public static final byte CUSTOM = 2;
    
    /**
     * Random output, appropriate for relevant setting. When this option is set, the
     * corresponding getter methods will generate appropriate random values automatically.
     * 
     * Device ID: a random string consisting of 15 numeric digits preceded by a "+"
     * Line1Number: a random string consisting of 13 numeric digits
     */
    public static final byte RANDOM = 3;
    
    public static final byte SETTING_NOTIFY_OFF = 0;
    public static final byte SETTING_NOTIFY_ON = 1;
    
    /** used to create random android ID*/
    public static final String[] ID_PATTERN = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
    
    // constants for identification of data types transmitted in the notification intent
    public static final String DATA_DEVICE_ID = "deviceID";
    public static final String DATA_LINE_1_NUMBER = "line1Number";
    public static final String DATA_LOCATION_GPS = "locationGPS";
    public static final String DATA_LOCATION_NETWORK = "locationNetwork";
    public static final String DATA_NETWORK_INFO_CURRENT = "networkInfoCurrent";
    public static final String DATA_NETWORK_INFO_SIM = "networkInfoSIM";
    public static final String DATA_SIM_SERIAL = "simSerial";
    public static final String DATA_SUBSCRIBER_ID = "subscriberID";
    public static final String DATA_ACCOUNTS_LIST = "accountsList";
    public static final String DATA_AUTH_TOKENS = "authTokens";
    public static final String DATA_OUTGOING_CALL = "outgoingCall";
    public static final String DATA_INCOMING_CALL = "incomingCall";
    public static final String DATA_CONTACTS = "contacts";
    public static final String DATA_CALENDAR = "calendar";
    public static final String DATA_MMS = "mms";
    public static final String DATA_SMS = "sms";
    public static final String DATA_MMS_SMS = "mmsSms";
    public static final String DATA_CALL_LOG = "callLog";
    public static final String DATA_BOOKMARKS = "bookmarks";
    public static final String DATA_SYSTEM_LOGS = "systemLogs";
    public static final String DATA_INTENT_BOOT_COMPLETED = "intentBootCompleted";
//    public static final String DATA_EXTERNAL_STORAGE = "externalStorage";
    public static final String DATA_CAMERA = "camera";
    public static final String DATA_RECORD_AUDIO = "recordAudio";
    public static final String DATA_SMS_SEND = "SmsSend";
    public static final String DATA_PHONE_CALL = "phoneCall";
    public static final String DATA_ANDROID_ID = "android_id";
    public static final String DATA_ICC_ACCESS = "iccAccess";
    public static final String DATA_WIFI_INFO = "wifiInfo";
    public static final String DATA_IP_TABLES = "iptables";
    public static final String DATA_SWITCH_CONNECTIVITY = "switchconnectivity";
    public static final String DATA_SEND_MMS = "sendMms";
    public static final String DATA_SWITCH_WIFI_STATE = "switchWifiState";
    
    public byte getSwitchWifiStateSetting();
    public byte getForceOnlineState();
	public byte getSendMmsSetting();
	public byte getSwitchConnectivitySetting();	

	public byte getAndroidIdSetting();
	public String getAndroidID();
	
	public byte getWifiInfoSetting();
    public byte getIpTableProtectSetting();

	public byte getIccAccessSetting();
	public byte getAddOnManagementSetting();
    public byte getSmsSendSetting();
    public byte getPhoneCallSetting();
    public byte getRecordAudioSetting();

    public byte getCameraSetting();
    public Integer get_id();
    public String getPackageName();    
    public int getUid();

    public byte getDeviceIdSetting();
    public String getDeviceId();
    public byte getLine1NumberSetting();
    public String getLine1Number();
    public byte getLocationGpsSetting();
    public String getLocationGpsLat();
    public String getLocationGpsLon();
    public byte getLocationNetworkSetting();
    public String getLocationNetworkLat();
    public String getLocationNetworkLon();
    public byte getNetworkInfoSetting();
    public byte getSimInfoSetting();
    public byte getSimSerialNumberSetting();
    public String getSimSerialNumber();
    public byte getSubscriberIdSetting();
    public String getSubscriberId();
    public byte getAccountsSetting();
    public byte getAccountsAuthTokensSetting();
    public byte getOutgoingCallsSetting();
    public byte getIncomingCallsSetting();
    public byte getContactsSetting();
    public byte getCalendarSetting();
    public byte getMmsSetting();
    public byte getSmsSetting();
    public byte getCallLogSetting();
    public byte getBookmarksSetting();
    public byte getSystemLogsSetting();
    public byte getIntentBootCompletedSetting();
    public byte getNotificationSetting();    
    public int[] getAllowedContacts();
	
}
