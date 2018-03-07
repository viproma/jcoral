#include <stdexcept>
#include <coral/net.hpp>
#include <coral/slave/runner.hpp>

#include "common_types.hpp"
#include "jni_helpers.hpp"
#include "type_converters.hpp"
#include "no_viproma_coral_slave_NetworkedRunner.h"


namespace
{
    coral::net::Endpoint MakeEndpoint(JNIEnv* env, jstring address, jint port)
    {
        if (port < 0 || port > 65535) {
            throw std::invalid_argument(
                "Invalid port number: " + std::to_string(port));
        }
        const auto a = coral::net::ip::Address(jcoral::ToString(env, address));
        const auto p = (port == 0)
            ? coral::net::ip::Port("*")
            : coral::net::ip::Port(static_cast<std::uint16_t>(port));
        return coral::net::ip::Endpoint(a, p).ToEndpoint("tcp");
    }
}


JNIEXPORT jlong JNICALL Java_no_viproma_coral_slave_NetworkedRunner_createNative(
    JNIEnv* env,
    jclass,
    jlong nativeInstancePtr,
    jstring bindAddress,
    jint controlPort,
    jint dataPubPort,
    jint commTimeout_s)
{
    try {
        const auto instance =
            jcoral::UnwrapCppObject<jcoral::SlaveInstance>(nativeInstancePtr);
        return jcoral::WrapCppObject(
            env,
            coral::slave::Runner(
                instance,
                MakeEndpoint(env, bindAddress, controlPort),
                MakeEndpoint(env, bindAddress, dataPubPort),
                std::chrono::seconds(commTimeout_s)));
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_slave_NetworkedRunner_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    jcoral::DeleteWrappedCppObject<coral::slave::Runner>(selfPtr);
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_slave_NetworkedRunner_getLocatorNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        auto& runner = jcoral::UnwrapCppObject<coral::slave::Runner>(selfPtr);
        const auto locator = coral::net::SlaveLocator(
            runner.BoundControlEndpoint(),
            runner.BoundDataPubEndpoint());
        return jcoral::SlaveLocatorConverter(env).ToJava(locator);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_slave_NetworkedRunner_runNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        auto& runner = jcoral::UnwrapCppObject<coral::slave::Runner>(selfPtr);
        runner.Run();
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
    }

}
