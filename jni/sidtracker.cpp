#include <string.h>
#include <jni.h>

#include <android/log.h>

#define EXPORT __attribute__((visibility("default")))

EXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	__android_log_print(ANDROID_LOG_INFO, "SIDTracker", "Hello world from JNI_OnLoad");
	return JNI_VERSION_1_6;
}
