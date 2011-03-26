LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := sidtracker
### Add all source file names to be included in lib separated by a whitespace
LOCAL_SRC_FILES := sidtracker.cpp

include $(BUILD_SHARED_LIBRARY)
