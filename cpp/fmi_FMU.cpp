#include <coral/fmi/fmu.hpp>

#include "common_types.hpp"
#include "jni_helpers.hpp"
#include "type_converters.hpp"
#include "no_viproma_coral_fmi_FMU.h"


JNIEXPORT void JNICALL Java_no_viproma_coral_fmi_FMU_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    jcoral::DeleteWrappedCppObject<jcoral::FMU>(selfPtr);
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_fmi_FMU_getFMIVersionNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        const auto fmu = jcoral::UnwrapCppObject<jcoral::FMU>(selfPtr);
        const auto ver = fmu->FMIVersion();
        if (ver == coral::fmi::FMIVersion::v1_0) {
            return jcoral::GetEnumField(env, "no/viproma/coral/fmi/FMIVersion", "V1_0");
        } else if (ver == coral::fmi::FMIVersion::v2_0) {
            return jcoral::GetEnumField(env, "no/viproma/coral/fmi/FMIVersion", "V2_0");
        } else {
            // This should never happen.
            throw std::logic_error("Unknown FMI version");
        }
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_fmi_FMU_getDescriptionNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        const auto fmu = jcoral::UnwrapCppObject<jcoral::FMU>(selfPtr);
        return jcoral::SlaveTypeDescriptionConverter(env)
            .ToJava(fmu->Description());
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_fmi_FMU_instantiateSlaveNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        const auto fmu = jcoral::UnwrapCppObject<jcoral::FMU>(selfPtr);
        jcoral::SlaveInstance instance = fmu->InstantiateSlave();
        return jcoral::ConstructWithWrappedCppObject(
            env, "no/viproma/coral/slave/OpaqueInstance", instance);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}
