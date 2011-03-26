#include <string.h>
#include <jni.h>

#include <android/log.h>

#include "resid/sources.cc"

#define LOG_(prio, fmt, ...) __android_log_print(ANDROID_LOG_##prio, "SIDTracker", fmt, ##__VA_ARGS__)
#define LOGV(fmt, ...) LOG_(VERBOSE, fmt, ## __VA_ARGS__)
#define LOGE(fmt, ...) LOG_(ERROR, fmt, ## __VA_ARGS__)
#define LOGI(fmt, ...) LOG_(INFO, fmt, ## __VA_ARGS__)

struct JNINativeClass
{
	const char* className; // In JNI format (foo/bar/Bar)
	JNINativeMethod* start;
	JNINativeMethod* stop;
};

#define SECTION(s) __attribute__((section(s)))
#define REG_JNI_(klass, jname, cname, sig) \
	JNINativeMethod jnireg__##klass##__##cname SECTION("JNI_" #klass) = { #jname, sig, (void*)Java_##klass##_##cname}
#define REG_JNI(klass, name, sig) REG_JNI_(klass, name, name, sig)
#define REG_CLASS(name, klass) \
	extern JNINativeMethod __start_JNI_##klass[1]; \
	extern JNINativeMethod __stop_JNI_##klass[1]; \
	JNINativeClass jnireg_class__##klass SECTION("JNI_classes") = { name, __start_JNI_##klass, __stop_JNI_##klass }

extern JNINativeClass __start_JNI_classes[1];
extern JNINativeClass __stop_JNI_classes[1];

void Java_se_olsner_sidtracker_NativeTest_testFunc(JNIEnv* env, jclass apidemos, jint i, jboolean z)
{
	LOGI("testFunc called! %d %s", i, z ? "true" : "false");
}
REG_JNI(se_olsner_sidtracker_NativeTest, testFunc, "(IZ)V");
void Java_se_olsner_sidtracker_NativeTest_testFunc_ii(JNIEnv* env, jclass apidemos, jint i, jint j)
{
	LOGI("testFunc(int,int) called! %d %d", i, j);
}
REG_JNI_(se_olsner_sidtracker_NativeTest, testFunc, testFunc_ii, "(II)V");
REG_CLASS("se/olsner/sidtracker/NativeTest", se_olsner_sidtracker_NativeTest);

int jniRegisterNativeMethods(JNIEnv* env, const char* className,
		const JNINativeMethod* gMethods, int numMethods)
{
	jclass clazz;

	LOGV("Registering %s natives", className);
	clazz = env->FindClass(className);
	if (clazz == NULL)
	{
		LOGE("Native registration unable to find class '%s'", className);
		return -1;
	}
	if (env->RegisterNatives(clazz, gMethods, numMethods) < 0)
	{
		LOGE("RegisterNatives failed for '%s'\n", className);
		return -1;
	}
	return 0;
}

#define EXPORT __attribute__((visibility("default")))

EXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
	{
		return -1;
	}

	for (JNINativeClass* p = __start_JNI_classes; p < __stop_JNI_classes; p++)
	{
		if (jniRegisterNativeMethods(env, p->className, p->start, p->stop - p->start))
			return -1;
	}

	return JNI_VERSION_1_6;
}
