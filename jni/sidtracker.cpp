#include <string.h>
#include <jni.h>

#include <android/log.h>
#include <assert.h>

// TODO Enable FP on Atom. cpuid asm bails out due to clobbering PIC register
#if __ARM_ARCH_7A__
#define USE_RESID_FP 1
#endif
#if USE_RESID_FP
#include "resid-fp/sources.cc"
typedef SIDFP SID;
#define MOS6581 MOS6581FP
static const sampling_method SAMPLING_METHOD = SAMPLE_INTERPOLATE;
#else
#include "resid-nofp/sources.cc"
using namespace reSID;
static const sampling_method SAMPLING_METHOD = SAMPLE_FAST;
#endif

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

template<typename T>
static T primArrayGet(JNIEnv* env, jarray array, jint index)
{
	T* ptr = (T*)env->GetPrimitiveArrayCritical(array, NULL);
	T ret = ptr[index];
	env->ReleasePrimitiveArrayCritical(array, ptr, 0);
	return ret;
}
template<typename T>
static void primArrayPut(JNIEnv* env, jarray array, jint index, T val)
{
	T* ptr = (T*)env->GetPrimitiveArrayCritical(array, NULL);
	ptr[index] = val;
	env->ReleasePrimitiveArrayCritical(array, ptr, 0);
}

#define SIDCLASSNAME "se/olsner/sidtracker/SID"
static SID* getNativeSIDData(JNIEnv* env, jobject sid)
{
	static bool inited = false;
	static jfieldID fid;
	if (!inited)
	{
		jclass klass = env->GetObjectClass(sid);
		fid = env->GetFieldID(klass, "nativeData", "J");
		inited = true;
	}
	return sid ? (SID*)env->GetLongField(sid, fid) : NULL;
}
#define GET_SID(ret) SID* sidfpp = getNativeSIDData(env, sid); if (!sidfpp) { env->ThrowNew(env->FindClass("java/lang/NullPointerException"), ""); return ret; } SID& sidfp = *sidfpp
void Java_se_olsner_sidtracker_SID_nativeInit(JNIEnv* env, jobject sid, jint cyclesPerSecond, jint sampleRate)
{
	SID& sidfp = *new SID();
	sidfp.set_chip_model(MOS6581);
#if USE_RESID_FP
	sidfp.set_voice_nonlinearity(0.96f);
#endif
	sidfp.enable_filter(true);
	sidfp.set_sampling_parameters(cyclesPerSecond, SAMPLING_METHOD, sampleRate);

	LOGV("Created a SID: %p", &sidfp);

	jclass klass = env->GetObjectClass(sid);
	jfieldID fid = env->GetFieldID(klass, "nativeData", "J");
	env->SetLongField(sid, fid, (jlong)&sidfp);
}
REG_JNI(se_olsner_sidtracker_SID, nativeInit, "(II)V");
void Java_se_olsner_sidtracker_SID_nativeWrite(JNIEnv* env, jobject sid,
		jint reg, jint value)
{
	LOGV("Setting SID register %#x to %#x", reg, value);
	GET_SID();
	sidfp.write(reg, value);
}
REG_JNI(se_olsner_sidtracker_SID, nativeWrite, "(II)V");
jint Java_se_olsner_sidtracker_SID_nativeClock(JNIEnv* env, jobject sid,
		jintArray cycles, jshortArray output, jint offset, jint length)
{
	GET_SID(0);
	cycle_count dt = primArrayGet<jint>(env, cycles, 0);
	//const cycle_count orig_dt = dt;
	jshort* buffer = new jshort[length];
	if (!buffer)
		return 0;

	//LOGV("Now clocking %d samples (%d cycles)", length, dt);
	jint written = 0;
	while (length > written && dt)
	{
		//cycle_count prev_dt = dt;
		int written0 = sidfp.clock(dt, buffer + written, length - written);
		//LOGV("Clocked %d and got %d samples", prev_dt - dt, written0);
		written += written0;
	}

	//LOGV("%d samples %d cycles later", written, orig_dt - dt);
	jshort* temp = (jshort*)env->GetPrimitiveArrayCritical(output, NULL);
	memcpy(temp + offset, buffer, written * sizeof(jshort));
	env->ReleasePrimitiveArrayCritical(output, buffer, 0);

	primArrayPut(env, cycles, 0, (jint)dt);
	delete[] buffer;
	return written;
}
REG_JNI(se_olsner_sidtracker_SID, nativeClock, "([I[SII)I");
REG_CLASS("se/olsner/sidtracker/SID", se_olsner_sidtracker_SID);

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
