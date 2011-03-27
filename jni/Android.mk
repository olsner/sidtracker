LOCAL_PATH := $(call my-dir)
MY_JNI_DIR := $(LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE    := sidtracker
### Add all source file names to be included in lib separated by a whitespace
LOCAL_SRC_FILES := sidtracker.cpp
LOCAL_CFLAGS := -fvisibility=hidden -fvisibility-inlines-hidden -Wall -O3
LOCAL_LDLIBS := -llog
# LOCAL_LDLIBS += -Wl,-Map,linker.map
LOCAL_ARM_MODE := arm
PERL ?= perl
PYTHON ?= python

SAMP2SRC := $(MY_JNI_DIR)/resid-nofp/samp2src.py

%.h: %.dat $(SAMP2SRC)
	$(PYTHON) $(SAMP2SRC) $(notdir $*) $< $@

include $(BUILD_SHARED_LIBRARY)
