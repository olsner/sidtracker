LOCAL_PATH := $(call my-dir)
MY_JNI_DIR := $(LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE    := sidtracker
### Add all source file names to be included in lib separated by a whitespace
LOCAL_SRC_FILES := sidtracker.cpp
LOCAL_CFLAGS := -fvisibility=hidden -fvisibility-inlines-hidden -Wall
# The unwind tables take about 8kB on ARM, 4kB on x86. That's almost 50% of
# the armv7 library.
LOCAL_CFLAGS += -fno-unwind-tables
LOCAL_CFLAGS += -finline-small-functions -findirect-inlining -fmerge-all-constants -fmodulo-sched -fmodulo-sched-allow-regmoves -fgcse-sm -fgcse-las -fgcse-after-reload -funsafe-loop-optimizations -Wunsafe-loop-optimizations
# Since we don't need linker-based dead-code removal, this reduces size
# (-fwhole-program allows the compiler to do that instead)
LOCAL_CFLAGS += -fno-data-sections -fno-function-sections
# whole program: saves 3k on armv7, 15k on x86/armeabi
LOCAL_CFLAGS += -fwhole-program
LOCAL_CFLAGS += -ffast-math -fassociative-math -fno-signed-zeros -fno-trapping-math
# Test code to evaluate use of VFP on ARMv6
#ifeq ($(TARGET_ARCH_ABI),armeabi)
#LOCAL_CFLAGS += -march=armv6 -mfloat-abi=softfp -mfpu=vfp
#endif
LOCAL_LDLIBS := -llog
LOCAL_LDLIBS += -Wl,--version-script,$(MY_JNI_DIR)/version-script.txt
# LOCAL_LDLIBS += -Wl,-Map,linker.map
LOCAL_LDFLAGS += -Wl,-z,max-page-size=0x1000
LOCAL_ARM_MODE := arm
PYTHON ?= python

SAMP2SRC := $(MY_JNI_DIR)/resid-nofp/samp2src.py

%.h: %.dat $(SAMP2SRC)
	$(PYTHON) $(SAMP2SRC) $(notdir $*) $< $@

include $(BUILD_SHARED_LIBRARY)
