LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE        := reflection
LOCAL_C_INCLUDES    := $(LOCAL_PATH)
LOCAL_SRC_FILES     := reflection.cpp
include $(BUILD_SHARED_LIBRARY)

