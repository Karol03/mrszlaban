# Instruction

1. Unlock bootloader: https://www.getdroidtips.com/unlock-bootloader-moto-g5-plus/
2. ROM reinstall (LineageOS 14.1): https://xdaforums.com/t/7-1-x-lineageos-14-1-for-moto-g5-cedric-unofficial.3611973/page-75
3. Set flag to start the device when power is detected: fastboot oem off-mode-charge 0 
4. Build and install the app: adb install -r path/to/the/app.apk
