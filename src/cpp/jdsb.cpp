#include <cassert>
#include <iostream>
#include <stdexcept>
#include <string>
#include <utility>

#include "com_sfh_dsb_DomainController.h"
#include "com_sfh_dsb_DomainLocator.h"
#include "com_sfh_dsb_SlaveLocator.h"


namespace
{
    template<typename T>
    class Maybe
    {
    public:
        Maybe() : value_(), hasValue_(false) { }
        Maybe(const T& value) : value_(value), hasValue_(true) { }

        operator bool() const noexcept { return hasValue_; }

        const T& get() const
        {
            if (hasValue_) return value_;
            else throw std::logic_error("No value");
        }

        T& get()
        {
            if (hasValue_) return value_;
            else throw std::logic_error("No value");
        }

    private:
        T value_;
        bool hasValue_;
    };

    template<typename T> Maybe<T> Actually(const T& value) { return Maybe<T>(value); }


    Maybe<std::string> ToString(JNIEnv* env, jstring javaString)
    {
        const auto cString = env->GetStringUTFChars(javaString, nullptr);
        if (cString) {
            const auto ret = std::string(cString);
            env->ReleaseStringUTFChars(javaString, cString);
            return Actually(std::move(ret));
        } else {
            return Maybe<std::string>();
        }
    }
}


// =============================================================================
// DomainController
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_DomainController_ctorNative(
    JNIEnv* env,
    jclass,
    jlong domainLocatorPtr)
{
    std::cout << "DomainController.ctorNative(" << reinterpret_cast<const char*>(domainLocatorPtr) << ')' << std::endl;
    return reinterpret_cast<jlong>("DomainControllerObj");
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_DomainController_closeNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    std::cout << "DomainController.closeNative(" << reinterpret_cast<const char*>(selfPtr) << ')' << std::endl;
}


// =============================================================================
// DomainLocator
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_DomainLocator_createNative(
    JNIEnv* env,
    jclass,
    jstring domainAddress)
{
    std::cout << "DomainLocator.createNative(" << ToString(env, domainAddress).get() << ')' << std::endl;
    return reinterpret_cast<jlong>("DomainLocatorObj");
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_DomainLocator_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    std::cout << "DomainController.destroyNative(" << reinterpret_cast<const char*>(selfPtr) << ')' << std::endl;
}


// =============================================================================
// SlaveLocator
// =============================================================================

JNIEXPORT void JNICALL Java_com_sfh_dsb_SlaveLocator_closeNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    std::cout << "SlaveLocator.closeNative(" << reinterpret_cast<const char*>(selfPtr) << ')' << std::endl;
}
