#include <coral/slave/logging.hpp>

#include "common_types.hpp"
#include "type_converters.hpp"
#include "jni_helpers.hpp"
#include "no_viproma_coral_slave_InstanceFactory.h"


JNIEXPORT jobject JNICALL Java_no_viproma_coral_slave_InstanceFactory_newCSVLoggingInstanceNative(
    JNIEnv* env,
    jclass,
    jlong instanceToWrapPtr,
    jstring outputFilePrefix)
{
    try {
        const auto instanceToWrap =
            jcoral::UnwrapCppObject<jcoral::SlaveInstance>(instanceToWrapPtr);
        jcoral::SlaveInstance loggingInstance =
            std::make_shared<coral::slave::LoggingInstance>(
                instanceToWrap,
                jcoral::ToString(env, outputFilePrefix));
        return jcoral::ConstructWithWrappedCppObject(
            env,
            "no/viproma/coral/slave/OpaqueInstance",
            loggingInstance);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}
