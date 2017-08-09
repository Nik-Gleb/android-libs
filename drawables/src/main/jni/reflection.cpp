#include <jni.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

#ifndef NULL
//#define NULL 0
#endif

#ifdef __cplusplus
extern "C" {
#endif

	static jmethodID updateMethodId;
    static jfieldID matrixFieldId;

	#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
	jint JNI_OnLoad(JavaVM* vm, void* reserved) {
		JNIEnv* env;
		if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
		else {

			jclass clazz = env->FindClass("android/graphics/ColorMatrixColorFilter");
			updateMethodId = env->GetMethodID(clazz, "update", "()V");
			if (env->ExceptionCheck()) {env->ExceptionClear(); updateMethodId = NULL;}

            matrixFieldId = env->GetFieldID(clazz, "mMatrix", "Landroid/graphics/ColorMatrix;");
            if (env->ExceptionCheck()) {env->ExceptionClear(); matrixFieldId = NULL;}
            env->DeleteLocalRef(clazz);

			return JNI_VERSION_1_6;
		}
	}

	JNIEXPORT jboolean JNICALL
    Java_drawables_BitmapDrawable_update
	(JNIEnv* env, jclass javaThis, jobject object) {
		if (updateMethodId == NULL) return JNI_FALSE;
		env->CallVoidMethod(object, updateMethodId);
		return JNI_TRUE;
	}

    JNIEXPORT jobject JNICALL
    Java_drawables_BitmapDrawable_getMatrix
            (JNIEnv* env, jclass javaThis, jobject object) {

        if (matrixFieldId == NULL) return NULL;

        return env->GetObjectField(object, matrixFieldId);
    }


#ifdef __cplusplus
}
#endif

#pragma clang diagnostic pop
