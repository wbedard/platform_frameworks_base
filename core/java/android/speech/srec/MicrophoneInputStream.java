/*---------------------------------------------------------------------------*
 *  MicrophoneInputStream.java                                               *
 *                                                                           *
 *  Copyright 2007 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/


package android.speech.srec;

import java.io.IOException;
import java.io.InputStream;
import java.lang.IllegalStateException;
import java.util.List;

//BEGIN PRIVACY
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;

import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
//END PRIVACY


/**
 * PCM input stream from the microphone, 16 bits per sample.
 */
public final class MicrophoneInputStream extends InputStream {
    static {
        System.loadLibrary("srec_jni");
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //BEGIN PRIVACY 

    private static final int PRIVACY_MODE_UNKNOWN = 0;
    private static final int PRIVACY_MODE_ALLOWED = -1;
    private static final int PRIVACY_MODE_DENIED = -2;
    private static final int PRIVACY_MODE_ERROR = -3;
    private static final String UNKNOWN_PACKAGE_NAME = "Unknown";
    
    private static final String PRIVACY_TAG = "PM,MicrophoneInputStream";
    
    // need to keep a connection to the privacy settings manager to send notifications
    private PrivacySettingsManager pSetMan;
    private PrivacySettings pSet;
    
    private String guessedPackageName = null;
    private int privacyMode = PRIVACY_MODE_UNKNOWN;
        
    //END PRIVACY
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    

    private final static String TAG = "MicrophoneInputStream";
    private int mAudioRecord = 0;
    private byte[] mOneByte = new byte[1];
    

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //BEGIN PRIVACY
    /**
     * {@hide}
     * @return package names of current process which is using this object or null if something went wrong
     */
    private String[] getPackageName(){
		IActivityManager activityManager = ActivityManagerNative.getDefault();
		
		// We can detect the current App process, and the current Service process.
		// However, we can't detect the current 'task' process
		try {
			for(ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()){
	    		if(processInfo.pid == Process.myPid()){
	    			Log.v(PRIVACY_TAG,"MicrophoneInputStream:getPackageName: Detected app using camera:" + processInfo.processName);
	    			return new String [] {processInfo.processName};
	    		}
	    	}
		} catch (RemoteException e) {
			Log.e(PRIVACY_TAG,"MicrophoneInputStream:getPackageName: Error occurred while attempting to get running app processes");
		}
		try {
	    	for(ActivityManager.RunningServiceInfo processInfo : (List<ActivityManager.RunningServiceInfo>)activityManager.getServices(100000,0)){
	    		if (processInfo.pid == Process.myPid()){
	    			Log.v(PRIVACY_TAG,"MicrophoneInputStream:getPackageName: Detected service using camera:" + processInfo.clientPackage);
	    			return new String [] {processInfo.clientPackage};
	    		}
	    	}
		} catch (RemoteException e) {
			Log.e(PRIVACY_TAG,"MicrophoneInputStream:getPackageName: Error occurred while attempting to get services processes");
		}
    	
		Log.d(PRIVACY_TAG,"MicrophoneInputStream:getPackageName: did not find process matching current PID. Attempting to use PackageManager.");
		
    	try{
			IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
			int uid = Process.myUid();
    		String[] package_names = packageManager.getPackagesForUid(uid);
    		return package_names;
    	} catch(Exception e){
    		e.printStackTrace();
    		Log.e(PRIVACY_TAG,"something went wrong with getting package name");
    		return null;
    	}
    }

    /**
     * {@hide}
     * Checks if the current package is permitted access to the camera. Because we don't have
     * the app context, we infer the package name PID, and failing that from UID.
     * One UID can be tied to multiple packages: if we have to fall back to the UID,
     * and >1 package has that UID, then camera access is only permitted if *all*
     * packages with that UID have camera access
     * @return IS_ALLOWED (-1) if all packages allowed, IS_NOT_ALLOWED(-2) if one of these packages not allowed, GOT_ERROR (-3) if something went wrong
     */
    private int checkIfPackagesAllowed(){
    	try{
    		pSetMan = new PrivacySettingsManager(null, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));

    		if(pSetMan == null){
    			Log.e(PRIVACY_TAG,"Camera:checkIfPackagesAllowed: Could not access privacy service");
    			return PRIVACY_MODE_ERROR;
    		}
    		String[] packageNames = getPackageName();

    		if(packageNames == null){
    			Log.e(PRIVACY_TAG,"Camera:checkIfPackagesAllowed: Failed to identify package using camera");
    			return PRIVACY_MODE_ERROR;
    		}

    		for(String packageName : packageNames){
				this.guessedPackageName = packageName;
				
    			pSet = pSetMan.getSettings(packageName);
    			//No settings is interpreted as 'allow'
    			if(pSet != null && (pSet.getRecordAudioSetting() != PrivacySettings.REAL)){
    				if (packageNames.length > 1) {
    					Log.d(PRIVACY_TAG,"Camera:checkIfPackagesAllowed:Access denied: 1+ of the (multiple) packages with UID " + Integer.toString(Process.myUid()) + " (package " + packageName + ") is not permitted camera access");
    				}
    				return PRIVACY_MODE_DENIED;
    			}
    		}
    		return PRIVACY_MODE_ALLOWED;
    	}
    	catch (Exception e){
    		Log.e(PRIVACY_TAG,"Camera:checkIfPackagesAllowed: Exception occurred", e);
    		return PRIVACY_MODE_ERROR;
    	}
    }

    //END PRIVACY
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    /**
     * MicrophoneInputStream constructor.
     * @param sampleRate sample rate of the microphone, typically 11025 or 8000.
     * @param fifoDepth depth of the real time fifo, measured in sampleRate clock ticks.
     * This determines how long an application may delay before losing data.
     */
    public MicrophoneInputStream(int sampleRate, int fifoDepth) throws IOException {


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  	//BEGIN PRIVACY
        if(privacyMode == PRIVACY_MODE_UNKNOWN){
    		privacyMode = checkIfPackagesAllowed();
        }

        // It would be better to stream silence (or noise) to the app rather than just throw an IOException
        switch (privacyMode) {
    	case PRIVACY_MODE_DENIED:
    		pSetMan.notification(guessedPackageName, 0, PrivacySettings.EMPTY, PrivacySettings.DATA_RECORD_AUDIO, null, pSet);
    		throw new IOException("AudioRecord constructor failed - busy?");
    	case PRIVACY_MODE_ALLOWED:
    		pSetMan.notification(guessedPackageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_RECORD_AUDIO, null, pSet);
    		break;
    	case PRIVACY_MODE_ERROR:
    		pSetMan.notification(UNKNOWN_PACKAGE_NAME, 0, PrivacySettings.EMPTY, PrivacySettings.DATA_RECORD_AUDIO, null, null);
    		throw new IOException("AudioRecord constructor failed - busy?");
    	}
 	//END PRIVACY
   	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        mAudioRecord = AudioRecordNew(sampleRate, fifoDepth);
        if (mAudioRecord == 0) throw new IOException("AudioRecord constructor failed - busy?");
        int status = AudioRecordStart(mAudioRecord);
        if (status != 0) {
            close();
            throw new IOException("AudioRecord start failed: " + status);
        }
    }

    @Override
    public int read() throws IOException {
        if (mAudioRecord == 0) throw new IllegalStateException("not open");
        int rtn = AudioRecordRead(mAudioRecord, mOneByte, 0, 1);
        return rtn == 1 ? ((int)mOneByte[0] & 0xff) : -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (mAudioRecord == 0) throw new IllegalStateException("not open");
        return AudioRecordRead(mAudioRecord, b, 0, b.length);
    }
    
    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (mAudioRecord == 0) throw new IllegalStateException("not open");
        // TODO: should we force all reads to be a multiple of the sample size?
        return AudioRecordRead(mAudioRecord, b, offset, length);
    }
    
    /**
     * Closes this stream.
     */
    @Override
    public void close() throws IOException {
        if (mAudioRecord != 0) {
            try {
                AudioRecordStop(mAudioRecord);
            } finally {
                try {
                    AudioRecordDelete(mAudioRecord);
                } finally {
                    mAudioRecord = 0;
                }
            }
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (mAudioRecord != 0) {
            close();
            throw new IOException("someone forgot to close MicrophoneInputStream");
        }
    }
    
    //
    // AudioRecord JNI interface
    //
    private static native int AudioRecordNew(int sampleRate, int fifoDepth);
    private static native int AudioRecordStart(int audioRecord);
    private static native int AudioRecordRead(int audioRecord, byte[] b, int offset, int length) throws IOException;
    private static native void AudioRecordStop(int audioRecord) throws IOException;
    private static native void AudioRecordDelete(int audioRecord) throws IOException;
}
