#include <memory>
#include <coral/slave/instance.hpp>

#include "common_types.hpp"
#include "type_converters.hpp"
#include "no_viproma_coral_slave_OpaqueInstance.h"


JNIEXPORT void JNICALL Java_no_viproma_coral_slave_OpaqueInstance_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    jcoral::DeleteWrappedCppObject<jcoral::SlaveInstance>(selfPtr);
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_slave_OpaqueInstance_getTypeDescriptionNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        const auto instance = jcoral::UnwrapCppObject<jcoral::SlaveInstance>(selfPtr);
        return jcoral::SlaveTypeDescriptionConverter(env)
            .ToJava(instance->TypeDescription());
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}
