#include <string.h>
#include <jni.h>

#include <android/log.h>

#define LOG_(prio, fmt, ...) __android_log_print(ANDROID_LOG_##prio, "SIDTracker", fmt, ##__VA_ARGS__)
#define LOGV(fmt, ...) LOG_(VERBOSE, fmt, ## __VA_ARGS__)
#define LOGE(fmt, ...) LOG_(ERROR, fmt, ## __VA_ARGS__)
#define LOGI(fmt, ...) LOG_(INFO, fmt, ## __VA_ARGS__)

#define SECTION(s) __attribute__((section(s)))
#define REG_JNI(klass, name, sig) \
	const char name__##klass##__##name[] = #name; \
	JNINativeMethod jnireg__##klass##__##name SECTION("JNI_" #klass) = { name__##klass##__##name, sig, (void*)Java_##klass##_##name}

void Java_se_olsner_sidtracker_NativeTest_testFunc(JNIEnv* env, jclass apidemos,
		jint i, jboolean z)
{
	LOGI("testFunc called! %d %s", i, z ? "true" : "false");
}
REG_JNI(se_olsner_sidtracker_NativeTest, testFunc, "(IZ)V");
#define USE_CLASS(klass) \
	extern JNINativeMethod __start_JNI_##klass[1]; \
	extern JNINativeMethod __stop_JNI_##klass[1];
#define REGISTER_CLASS(env, jname, cname) \
	({ \
	USE_CLASS(cname); \
	LOGI("Registering native for %s: %p..%p", jname, __start_JNI_##cname, __stop_JNI_##cname); \
	jniRegisterNativeMethods(env, jname, __start_JNI_##cname, __stop_JNI_##cname - __start_JNI_##cname); })

#define EXPORT __attribute__((visibility("default")))

int jniRegisterNativeMethods(JNIEnv* env, const char* className,
		const JNINativeMethod* gMethods, int numMethods)
{
	jclass clazz;

	LOGV("Registering %s natives", className);
	clazz = env->FindClass(className);
	LOGV("Found class %p", clazz);
	if (clazz == NULL)
	{
		LOGE("Native registration unable to find class '%s'", className);
		return -1;
	}
	for (int i = 0; i < numMethods; i++)
	{
		const JNINativeMethod* meth = gMethods + i;
		LOGV("jnireg: %p, name: %p", &jnireg__se_olsner_sidtracker_NativeTest__testFunc, name__se_olsner_sidtracker_NativeTest__testFunc);
		LOGV("Registering native %p", meth);
		LOGV("Registering native %p %p (funptr %p)", meth->name, meth->signature, meth->fnPtr);
		LOGV("Registering native %s %s (funptr %p)", meth->name, meth->signature, meth->fnPtr);
		if (env->RegisterNatives(clazz, meth, 1) < 0)
		{
			LOGE("RegisterNatives failed for '%s'\n", className);
			return -1;
		}
	}
	return 0;
}

EXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	LOGI("Hello world from JNI_OnLoad");

	JNIEnv* env = NULL;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
	{
		return -1;
	}
	LOGI("Env: %p", env);

	if (REGISTER_CLASS(env, "se/olsner/sidtracker/NativeTest", se_olsner_sidtracker_NativeTest))
		return -1;

	return JNI_VERSION_1_6;
}
