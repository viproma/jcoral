#include <coral/fmi/importer.hpp>

#include "common_types.hpp"
#include "jni_helpers.hpp"
#include "type_converters.hpp"
#include "no_viproma_coral_fmi_Importer.h"


using ImporterPtr = std::shared_ptr<coral::fmi::Importer>;


JNIEXPORT jlong JNICALL Java_no_viproma_coral_fmi_Importer_createNativeP(
    JNIEnv* env,
    jclass,
    jstring cachePath)
{
    try {
        ImporterPtr importer = 
            coral::fmi::Importer::Create(jcoral::ToString(env, cachePath));
        return jcoral::WrapCppObject(env, importer);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}


JNIEXPORT jlong JNICALL Java_no_viproma_coral_fmi_Importer_createNative(
    JNIEnv* env,
    jclass)
{
    try {
        ImporterPtr importer = coral::fmi::Importer::Create();
        return jcoral::WrapCppObject(env, importer);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_fmi_Importer_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    // TODO: Specify type using std::invoke_result when we move away
    //       from VS2013.
    jcoral::DeleteWrappedCppObject<ImporterPtr>(selfPtr);
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_fmi_Importer_importFMUNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jstring fmuPath)
{
    try {
        const auto importer = jcoral::UnwrapCppObject<ImporterPtr>(selfPtr);
        jcoral::FMU fmu = importer->Import(jcoral::ToString(env, fmuPath));
        return jcoral::ConstructWithWrappedCppObject(env, "no/viproma/coral/fmi/FMU", fmu);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_fmi_Importer_importUnpackedFMUNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jstring unpackedFMUPath)
{
    try {
        const auto importer = jcoral::UnwrapCppObject<ImporterPtr>(selfPtr);
        jcoral::FMU fmu = importer->ImportUnpacked(jcoral::ToString(env, unpackedFMUPath));
        return jcoral::ConstructWithWrappedCppObject(env, "no/viproma/coral/fmi/FMU", fmu);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_fmi_Importer_cleanCacheNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        const auto importer = jcoral::UnwrapCppObject<ImporterPtr>(selfPtr);
        importer->CleanCache();
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
    }
}
