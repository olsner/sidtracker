LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := sidtracker
### Add all source file names to be included in lib separated by a whitespace
LOCAL_SRC_FILES := sidtracker.cpp
LOCAL_CFLAGS := -fvisibility=hidden -fvisibility-inlines-hidden -Wall -O3
LOCAL_LDLIBS := -llog
# LOCAL_LDLIBS += -Wl,-Map,linker.map
LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)
