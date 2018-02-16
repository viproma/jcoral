#include <random>
#include <coral/slave/runner.hpp>

#include "common_types.hpp"
#include "jni_helpers.hpp"
#include "type_converters.hpp"
#include "no_viproma_coral_slave_InProcessRunner.h"


namespace
{
    std::string RandomPrintableString(std::size_t size)
    {
        std::random_device randomDev;
        std::minstd_rand randomGen(randomDev());
        std::uniform_int_distribution<> randomDist('0', 'z');
        std::string str;
        for (std::size_t i = 0; i < size; ++i) {
            str += static_cast<char>(randomDist(randomGen));
        }
        return str;
    }
}


JNIEXPORT jlong JNICALL Java_no_viproma_coral_slave_InProcessRunner_createNative(
    JNIEnv* env,
    jclass,
    jlong nativeInstancePtr)
{
    try {
        const auto instance =
            jcoral::UnwrapCppObject<jcoral::SlaveInstance>(nativeInstancePtr);
        return jcoral::WrapCppObject(
            env,
            coral::slave::Runner(
                instance,
                coral::net::Endpoint("inproc", RandomPrintableString(20)),
                coral::net::Endpoint("inproc", RandomPrintableString(20)),
                std::chrono::seconds(-1)));
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_slave_InProcessRunner_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    jcoral::DeleteWrappedCppObject<coral::slave::Runner>(selfPtr);
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_slave_InProcessRunner_getLocatorNative(
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


JNIEXPORT void JNICALL Java_no_viproma_coral_slave_InProcessRunner_runNative(
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
