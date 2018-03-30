 
#include <jni.h>
#include <unistd.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#ifdef __cplusplus
extern "C" {
#endif

	static jfieldID fieldDescriptor;
    static jmethodID constructorPfd;
    static jclass pfdClass;

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
	jint JNI_OnLoad(JavaVM* vm, void* reserved) {
		JNIEnv* env;
		if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
		else {

			jclass clazz = env->FindClass("java/io/FileDescriptor");
			fieldDescriptor = env->GetFieldID(clazz, "descriptor", "I");
			if (env->ExceptionCheck()) {env->ExceptionClear(); fieldDescriptor = NULL;}
			env->DeleteLocalRef(clazz);

            clazz = env->FindClass("android/os/ParcelFileDescriptor");
            pfdClass = (jclass) env->NewGlobalRef(clazz);
            constructorPfd = env->GetMethodID(pfdClass, "<init>", "(Ljava/io/FileDescriptor;)V");
            if (env->ExceptionCheck()) {env->ExceptionClear(); constructorPfd = NULL;}
            env->DeleteLocalRef(clazz);

			return JNI_VERSION_1_6;
		}
	}
#pragma clang diagnostic pop

	JNIEXPORT jboolean JNICALL
    Java_network_NetworkUtils_createPipe
			(JNIEnv *env, jclass type, jobject read, jobject write) {
        if (fieldDescriptor == NULL) return JNI_FALSE;
		int fds[2]; if (pipe(fds) < 0) return JNI_FALSE;
        env->SetIntField(read, fieldDescriptor, fds[0]);
        env->SetIntField(write, fieldDescriptor, fds[1]);
		return JNI_TRUE;
	}

    JNIEXPORT jobject JNICALL
    Java_network_NetworkUtils_createPfd
            (JNIEnv *env, jclass type, jobject fd) {
        if (constructorPfd == NULL) return NULL;
        return env->NewObject(pfdClass, constructorPfd, fd);
    };

}
#pragma clang diagnostic pop