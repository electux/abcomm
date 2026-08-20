# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/wlama/bin/android-studio/plugins/android/resources/proguard-android-optimize.txt

-keep class com.abcomm.MainViewModel { *; }
-keep class com.abcomm.BluetoothService { *; }
-keep interface com.abcomm.CommunicationProvider { *; }
