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
public final class ImmutablePrivacySettings implements IPrivacySettingsBase, Parcelable {
    
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
    
    // Database entry ID
    private final Integer _id;
    
    // Application identifiers
    private final String packageName;
    private final int uid;
    
    //
    // Privacy settings
    //
    
    private final byte deviceIdSetting;
    private final String deviceId;
    
    // Phone and Voice Mailbox Number
    private final byte line1NumberSetting; 
    private final String line1Number;
    
    private final byte locationGpsSetting;
    private final String locationGpsLat;
    private final String locationGpsLon;
    private final byte locationNetworkSetting;
    private final String locationNetworkLat;
    private final String locationNetworkLon;
    
    // CountryIso, Operator Code, Operator Name
    private final byte networkInfoSetting;
    private final byte simInfoSetting;
    
    private final byte simSerialNumberSetting;
    private final String simSerialNumber;
    private final byte subscriberIdSetting;
    private final String subscriberId;
    
    private final byte accountsSetting;
    private final byte accountsAuthTokensSetting;
    private final byte outgoingCallsSetting;
    private final byte incomingCallsSetting;
    
    private final byte contactsSetting;
    private final byte calendarSetting;
    private final byte mmsSetting;
    private final byte smsSetting;
    private final byte callLogSetting;
    private final byte bookmarksSetting; // browser bookmarks and history
    
    private final byte systemLogsSetting;
    
    private final byte notificationSetting;
    
    private final byte intentBootCompletedSetting;
//    private final byte externalStorageSetting;
    private final byte cameraSetting;
    private final byte recordAudioSetting;
    private final byte smsSendSetting;
    private final byte phoneCallSetting;

    private final byte ipTableProtectSetting;
    private final byte iccAccessSetting;
    private final byte addOnManagementSetting;
    
    private final byte androidIdSetting;
    private final String androidID;
    
    private final byte wifiInfoSetting;
    
    private final byte switchConnectivitySetting;
    
    private final byte sendMmsSetting;
    
    private final byte forceOnlineState; //used to fake online state
    
    private final byte switchWifiStateSetting;
   

	private final int[] allowedContacts;
	
    public ImmutablePrivacySettings(Integer id, String packageName, int uid, byte deviceIdSetting, String deviceId,
            byte line1NumberSetting, String line1Number, byte locationGpsSetting, String locationGpsLat,
            String locationGpsLon, byte locationNetworkSetting, String locationNetworkLat, 
            String locationNetworkLon, byte networkInfoSetting, byte simInfoSetting, byte simSerialNumberSetting,
            String simSerialNumber, byte subscriberIdSetting, String subscriberId, byte accountsSetting, 
            byte accountsAuthTokensSetting, byte outgoingCallsSetting, byte incomingCallsSetting, byte contactsSetting,
            byte calendarSetting, byte mmsSetting, byte smsSetting, byte callLogSetting, byte bookmarksSetting, 
            byte systemLogsSetting, byte externalStorageSetting, byte cameraSetting, byte recordAudioSetting, 
            byte notificationSetting, byte intentBootCompletedSetting, int[] allowedContacts, byte smsSendSetting, byte phoneCallSetting, byte ipTableProtectSetting,
            byte iccAccessSetting, byte addOnManagementSetting, byte androidIdSetting, String androidID, byte wifiInfoSetting, byte switchConnectivitySetting, byte sendMmsSetting,
            byte forceOnlineState, byte switchWifiStateSetting) {
        this._id = id;
        
        this.packageName = packageName;
        this.uid = uid;
        
        this.deviceIdSetting = deviceIdSetting;
        this.deviceId = deviceId;
        this.line1NumberSetting = line1NumberSetting;
        this.line1Number = line1Number;
        this.locationGpsSetting = locationGpsSetting;
        this.locationGpsLat = locationGpsLat;
        this.locationGpsLon = locationGpsLon;
        this.locationNetworkSetting = locationNetworkSetting;
        this.locationNetworkLat = locationNetworkLat;
        this.locationNetworkLon = locationNetworkLon;
        this.networkInfoSetting = networkInfoSetting;
        this.simInfoSetting = simInfoSetting;
        this.simSerialNumberSetting = simSerialNumberSetting;
        this.simSerialNumber = simSerialNumber;
        this.subscriberIdSetting = subscriberIdSetting;
        this.subscriberId = subscriberId;
        this.accountsSetting = accountsSetting;
        this.accountsAuthTokensSetting = accountsAuthTokensSetting;
        this.outgoingCallsSetting = outgoingCallsSetting;
        this.incomingCallsSetting = incomingCallsSetting;
        this.contactsSetting = contactsSetting;
        this.calendarSetting = calendarSetting;
        this.mmsSetting = mmsSetting;
        this.smsSetting = smsSetting;
        this.callLogSetting = callLogSetting;
        this.bookmarksSetting = bookmarksSetting;
        this.systemLogsSetting = systemLogsSetting;
        this.notificationSetting = notificationSetting;
        this.intentBootCompletedSetting = intentBootCompletedSetting;
//        this.externalStorageSetting = externalStorageSetting;
        this.cameraSetting = cameraSetting;
        this.recordAudioSetting = recordAudioSetting;
        this.allowedContacts = allowedContacts;
        this.smsSendSetting = smsSendSetting;
        this.phoneCallSetting = phoneCallSetting;
        this.ipTableProtectSetting = ipTableProtectSetting;
        this.iccAccessSetting = iccAccessSetting;
        this.addOnManagementSetting = addOnManagementSetting;
        this.androidIdSetting = androidIdSetting;
        this.androidID = androidID;
        this.wifiInfoSetting = wifiInfoSetting;
        this.switchConnectivitySetting = switchConnectivitySetting;
        this.sendMmsSetting = sendMmsSetting;
        this.forceOnlineState = forceOnlineState;
        this.switchWifiStateSetting = switchWifiStateSetting;
    }
    
    public byte getSwitchWifiStateSetting() {
		return switchWifiStateSetting;
	}

    public byte getForceOnlineState() {
		return forceOnlineState;
	}

	public byte getSendMmsSetting() {
		return sendMmsSetting;
	}

	public byte getSwitchConnectivitySetting() {
		return switchConnectivitySetting;
	}

    public byte getAndroidIdSetting() {
		return androidIdSetting;
	}
	
	/**
	 * @return random ID, constant fake id or null
	 */
	public String getAndroidID() {
		if(androidIdSetting == EMPTY) return "q4a5w896ay21dr46"; //we can not pull out empty android id, because we get bootlops then
		if(androidIdSetting == RANDOM) {
			Random value = new Random();
			StringBuilder localBuilder = new StringBuilder();
			for(int i = 0; i < ID_PATTERN.length; i++)
				localBuilder.append(ID_PATTERN[value.nextInt(ID_PATTERN.length-1)]);
			return localBuilder.toString();
		}
		return androidID;
	}
	
	public byte getWifiInfoSetting() {
		return wifiInfoSetting;
	}
    
    public byte getIpTableProtectSetting() {
		return ipTableProtectSetting;
	}

	public byte getIccAccessSetting() {
		return iccAccessSetting;
	}

	public byte getAddOnManagementSetting() {
		return addOnManagementSetting;
	}

	public byte getSmsSendSetting(){
		return smsSendSetting;
    }

    public byte getPhoneCallSetting(){
    	return phoneCallSetting;
    }

    public byte getRecordAudioSetting(){
    	return recordAudioSetting;
    }

    public byte getCameraSetting(){
    	return cameraSetting;
    }

    public Integer get_id() {
        return _id;
    }

    public String getPackageName() {
        return packageName;
    }
        
    public int getUid() {
        return uid;
    }

    public byte getDeviceIdSetting() {
        return deviceIdSetting;
    }

    public String getDeviceId() {
        if (deviceIdSetting == EMPTY) return "";
        if (deviceIdSetting == RANDOM) {
            Random rnd = new Random();
            String rndId = Math.abs(rnd.nextLong()) + "";
	    if(rndId.length() > 15)
            	return rndId.substring(0, 15);
	    else{
		for(int i = rndId.length(); i <= 16; i++)
			rndId += rnd.nextInt(9);
		return rndId.substring(0, 15);
	    }
            //return rndId.substring(0, 15);
        }
        return deviceId;
    }

    public byte getLine1NumberSetting() {
        return line1NumberSetting;
    }

    public String getLine1Number() {
        if (line1NumberSetting == EMPTY) return "";
        if (line1NumberSetting == RANDOM) {
            Random rnd = new Random();
            String rndId = "+" + Math.abs(rnd.nextLong()) + "";
	    if(rndId.length() > 13)
            	return rndId.substring(0, 13);
	    else{
		for(int i = rndId.length(); i <= 14; i++)
			rndId += rnd.nextInt(9);
		return rndId.substring(0, 13);
	    }
            //return rndId.substring(0, 13);
        }
        return line1Number;
    }

    public byte getLocationGpsSetting() {
        return locationGpsSetting;
    }

    public String getLocationGpsLat() {
        if (locationGpsSetting == EMPTY) return "";
        if (locationGpsSetting == RANDOM) return getRandomLat();
        return locationGpsLat;
    }

    public String getLocationGpsLon() {
        if (locationGpsSetting == EMPTY) return "";        
        if (locationGpsSetting == RANDOM) return getRandomLon();
        return locationGpsLon;
    }

    public byte getLocationNetworkSetting() {
        return locationNetworkSetting;
    }

    public String getLocationNetworkLat() {
        if (locationNetworkSetting == EMPTY) return "";
        if (locationNetworkSetting == RANDOM) return getRandomLat();  
        return locationNetworkLat;
    }

    public String getLocationNetworkLon() {
        if (locationNetworkSetting == EMPTY) return "";
        if (locationNetworkSetting == RANDOM) return getRandomLon();
        return locationNetworkLon;
    }

    public byte getNetworkInfoSetting() {
        return networkInfoSetting;
    }

    public byte getSimInfoSetting() {
        return simInfoSetting;
    }

    public byte getSimSerialNumberSetting() {
        return simSerialNumberSetting;
    }

    public String getSimSerialNumber() {
        if (simSerialNumberSetting == EMPTY) return "";
        if (simSerialNumberSetting == RANDOM) {
            Random rnd = new Random();
            return Math.abs(rnd.nextLong()) + "";
        }
        return simSerialNumber;
    }

    public byte getSubscriberIdSetting() {
        return subscriberIdSetting;
    }

    public String getSubscriberId() {
        if (subscriberIdSetting == EMPTY) return "";
        if (subscriberIdSetting == RANDOM) {
            Random rnd = new Random();
            String rndId = Math.abs(rnd.nextLong()) + "";
	    if(rndId.length() > 15)
            	return rndId.substring(0, 15);
	    else{
		for(int i = rndId.length(); i <= 16; i++)
			rndId += rnd.nextInt(9);
		return rndId.substring(0, 15);
	    }
        }
        return subscriberId;
    }

    public byte getAccountsSetting() {
        return accountsSetting;
    }

    public byte getAccountsAuthTokensSetting() {
        return accountsAuthTokensSetting;
    }

    public byte getOutgoingCallsSetting() {
        return outgoingCallsSetting;
    }
    
    public byte getIncomingCallsSetting() {
        return incomingCallsSetting;
    }
    
    public byte getContactsSetting() {
        return contactsSetting;
    }

    public byte getCalendarSetting() {
        return calendarSetting;
    }

    public byte getMmsSetting() {
        return mmsSetting;
    }

    public byte getSmsSetting() {
        return smsSetting;
    }

    public byte getCallLogSetting() {
        return callLogSetting;
    }

    public byte getBookmarksSetting() {
        return bookmarksSetting;
    }

    public byte getSystemLogsSetting() {
        return systemLogsSetting;
    }

    public byte getIntentBootCompletedSetting() {
        return intentBootCompletedSetting;
    }

    public byte getNotificationSetting() {
        return notificationSetting;
    }
    
    public int[] getAllowedContacts() {
        return allowedContacts;
    }

    @Override
    public String toString() {
        return "ImmutablePrivacySettings [_id=" + _id + ", accountsAuthTokensSetting=" + accountsAuthTokensSetting
                + ", accountsSetting=" + accountsSetting + ", bookmarksSetting=" + bookmarksSetting
                + ", calendarSetting=" + calendarSetting + ", callLogSetting=" + callLogSetting + ", contactsSetting="
                + contactsSetting + ", deviceId=" + deviceId + ", deviceIdSetting=" + deviceIdSetting
                + ", incomingCallsSetting=" + incomingCallsSetting + ", intentBootCompletedSetting="
                + intentBootCompletedSetting + ", line1Number=" + line1Number + ", line1NumberSetting="
                + line1NumberSetting + ", locationGpsLat=" + locationGpsLat + ", locationGpsLon=" + locationGpsLon
                + ", locationGpsSetting=" + locationGpsSetting + ", locationNetworkLat=" + locationNetworkLat
                + ", locationNetworkLon=" + locationNetworkLon + ", locationNetworkSetting=" + locationNetworkSetting
                + ", mmsSetting=" + mmsSetting + ", networkInfoSetting=" + networkInfoSetting
                + ", notificationSetting=" + notificationSetting + ", outgoingCallsSetting=" + outgoingCallsSetting
                + ", packageName=" + packageName + ", simInfoSetting=" + simInfoSetting + ", simSerialNumber="
                + simSerialNumber + ", simSerialNumberSetting=" + simSerialNumberSetting + ", smsSetting=" + smsSetting
                + ", subscriberId=" + subscriberId + ", subscriberIdSetting=" + subscriberIdSetting
                + ", systemLogsSetting=" + systemLogsSetting + ", uid=" + uid + ", phoneCallSetting=" + phoneCallSetting 
                + ", smsSendSetting=" + smsSendSetting + ", recordAudioSetting=" + recordAudioSetting + ", cameraSetting=" 
                + cameraSetting + ", ipTableProtectSetting=" + ipTableProtectSetting + ", iccAccessSetting=" + iccAccessSetting 
                + ", addOnManagementSetting=" + addOnManagementSetting + ", android ID=" + androidID + ", androidIdSetting="
                + androidIdSetting + ", wifiInfoSetting=" + wifiInfoSetting + ", switchConnectivitySetting=" + switchConnectivitySetting 
                + ", sendMmsSetting=" + sendMmsSetting + ", forceOnlineState=" + forceOnlineState + ", switchWifiStateSetting=" 
                + switchWifiStateSetting + "]";
    }

    /**
     * Util methods
     */
    
    private String getRandomLat() {
        BigDecimal latitude;
        double lat = Math.random() * 180;
        if (lat > 90) latitude = new BigDecimal(lat - 90);
        else latitude = new BigDecimal(-lat);
        return latitude.setScale(6, BigDecimal.ROUND_HALF_UP) + "";
    }
    
    private String getRandomLon() {
        BigDecimal longitude;
        double lon = Math.random() * 360;
        if (lon > 180) longitude = new BigDecimal(lon - 180);
        else longitude = new BigDecimal(-lon);
        return longitude.setScale(6, BigDecimal.ROUND_HALF_UP) + "";
    }

    /**
     * Parcelable implementation
     */

    public static final Parcelable.Creator<ImmutablePrivacySettings> CREATOR = new
            Parcelable.Creator<ImmutablePrivacySettings>() {
                public ImmutablePrivacySettings createFromParcel(Parcel in) {
                    return new ImmutablePrivacySettings(in);
                }

                public ImmutablePrivacySettings[] newArray(int size) {
                    return new ImmutablePrivacySettings[size];
                }
            };
    
    public ImmutablePrivacySettings(Parcel in) {
        int _id = in.readInt();
        this._id = (_id == -1) ? null : _id;
        
        this.packageName = in.readString();
        this.uid = in.readInt();
        
        this.deviceIdSetting = in.readByte();
        this.deviceId = in.readString();
        this.line1NumberSetting = in.readByte();
        this.line1Number = in.readString();
        this.locationGpsSetting = in.readByte();
        this.locationGpsLat = in.readString();
        this.locationGpsLon = in.readString();
        this.locationNetworkSetting = in.readByte();
        this.locationNetworkLat = in.readString();
        this.locationNetworkLon = in.readString();
        this.networkInfoSetting = in.readByte();
        this.simInfoSetting = in.readByte();
        this.simSerialNumberSetting = in.readByte();
        this.simSerialNumber = in.readString();
        this.subscriberIdSetting = in.readByte();
        this.subscriberId = in.readString();
        this.accountsSetting = in.readByte();
        this.accountsAuthTokensSetting = in.readByte();
        this.outgoingCallsSetting = in.readByte();
        this.incomingCallsSetting = in.readByte();
        this.contactsSetting = in.readByte();
        this.calendarSetting = in.readByte();
        this.mmsSetting = in.readByte();
        this.smsSetting = in.readByte();
        this.callLogSetting = in.readByte();
        this.bookmarksSetting = in.readByte();
        this.systemLogsSetting = in.readByte();
        this.notificationSetting = in.readByte();
        this.intentBootCompletedSetting = in.readByte();
//        this.externalStorageSetting = in.readByte();
        this.cameraSetting = in.readByte();
        this.recordAudioSetting = in.readByte();
//        int[] buffer = in.createIntArray();
//        if (buffer != null && buffer.length > 0) {
//            in.readIntArray(buffer);
//            int count = 0;
//            for (int i = 0; i < buffer.length; i++) if (buffer[i] != 0) count++; else break;
//            this.allowedContacts = new int[count];
//            System.arraycopy(buffer, 0, allowedContacts, 0, count);
//        } // else it will be null
        
        this.allowedContacts = in.createIntArray();
        this.smsSendSetting = in.readByte();
        this.phoneCallSetting = in.readByte();
        this.ipTableProtectSetting = in.readByte();
        this.iccAccessSetting = in.readByte();
        this.addOnManagementSetting = in.readByte();
        this.androidIdSetting = in.readByte();
        this.androidID = in.readString();
        this.wifiInfoSetting = in.readByte();
        this.switchConnectivitySetting = in.readByte();
        this.sendMmsSetting = in.readByte();
        this.forceOnlineState = in.readByte();
        this.switchWifiStateSetting = in.readByte();
        
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt((_id == null) ? -1 : _id);
        
        dest.writeString(packageName);
        dest.writeInt(uid);
        
        dest.writeByte(deviceIdSetting);
        dest.writeString(deviceId);
        dest.writeByte(line1NumberSetting);
        dest.writeString(line1Number);
        dest.writeByte(locationGpsSetting);
        dest.writeString(locationGpsLat);
        dest.writeString(locationGpsLon);
        dest.writeByte(locationNetworkSetting);
        dest.writeString(locationNetworkLat);
        dest.writeString(locationNetworkLon);
        dest.writeByte(networkInfoSetting);
        dest.writeByte(simInfoSetting);
        dest.writeByte(simSerialNumberSetting);
        dest.writeString(simSerialNumber);
        dest.writeByte(subscriberIdSetting);
        dest.writeString(subscriberId);
        dest.writeByte(accountsSetting);
        dest.writeByte(accountsAuthTokensSetting);
        dest.writeByte(outgoingCallsSetting);
        dest.writeByte(incomingCallsSetting);
        dest.writeByte(contactsSetting);
        dest.writeByte(calendarSetting);
        dest.writeByte(mmsSetting);
        dest.writeByte(smsSetting);
        dest.writeByte(callLogSetting);
        dest.writeByte(bookmarksSetting);
        dest.writeByte(systemLogsSetting);
        dest.writeByte(notificationSetting);
        dest.writeByte(intentBootCompletedSetting);
//        dest.writeByte(externalStorageSetting);
        dest.writeByte(cameraSetting);
        dest.writeByte(recordAudioSetting);
        dest.writeIntArray(allowedContacts);
        dest.writeByte(smsSendSetting);
        dest.writeByte(phoneCallSetting);
        dest.writeByte(ipTableProtectSetting);
        dest.writeByte(iccAccessSetting);
        dest.writeByte(addOnManagementSetting);
        dest.writeByte(androidIdSetting);
        dest.writeString(androidID);
        dest.writeByte(wifiInfoSetting);
        dest.writeByte(switchConnectivitySetting);
        dest.writeByte(sendMmsSetting);
        dest.writeByte(forceOnlineState);
        dest.writeByte(switchWifiStateSetting);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
	
}
