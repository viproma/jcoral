#include <cassert>
#include <exception>
#include <functional>
#include <iostream>
#include <string>
#include <utility>

#include "dsb/domain/controller.hpp"
#include "dsb/net.hpp"

#include "com_sfh_dsb_DomainController.h"
#include "com_sfh_dsb_DomainLocator.h"
#include "com_sfh_dsb_SlaveLocator.h"

#define JDSB_STRINGIFY(x) #x
#define JDSB_TOSTRING(x) JDSB_STRINGIFY(x)

// Terminates the program with the given message if `test` evaluates to false.
// If a Java exception is in flight at this point, its message will be printed
// along with a stack trace.
#define JDSB_REQUIRE(env, test) \
    if (!(test)) { \
        if (env->ExceptionOccurred()) { \
            env->ExceptionDescribe(); \
        } \
        env->FatalError("JDSB_REQUIRE failure in " __FILE__ ", line " JDSB_TOSTRING(__LINE__) "."); \
    }


namespace
{
    // Throws a Java exception of the type specified by `className` (e.g.
    // "java/lang/Exception"), with a descriptive message given by `msg`.
    void ThrowJException(JNIEnv* env, const std::string& className, const std::string& msg)
    {
        auto exClass = env->FindClass(className.c_str());
        JDSB_REQUIRE(env, exClass);
        JDSB_REQUIRE(env, 0 == env->ThrowNew(exClass, msg.c_str()));
    }

    // Throws a java.lang.NullPointerException if `ptr` is null, and returns
    // `true` iff `ptr` is not null.
    template<typename T>
    bool JEnforceNotNull(JNIEnv* env, T ptr)
    {
        if (!ptr) ThrowJException(env, "java/lang/NullPointerException", "Null pointer");
        return !!ptr;
    }

    // A basic "option type", which may have a value or a not-a-value.
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

    // Returns a Maybe object with the given value.
    template<typename T> Maybe<T> Actually(const T& value) { return Maybe<T>(value); }

    // Converts a Java string to a C++ string.
    //
    // If the function returns not-a-value it means that the conversion failed.
    // In that case, a Java exception will have been thrown, and the caller
    // should return control to the JVM as soon as possible.
    Maybe<std::string> ToString(JNIEnv* env, jstring javaString)
    {
        if (!JEnforceNotNull(env, javaString)) return Maybe<std::string>();
        const auto cString = env->GetStringUTFChars(javaString, nullptr);
        if (cString) {
            const auto ret = std::string(cString);
            env->ReleaseStringUTFChars(javaString, cString);
            return Actually(std::move(ret));
        } else {
            return Maybe<std::string>();
        }
    }

    // Converts a std::string to a Java string.
    jstring ToJString(JNIEnv* env, const std::string& cppString)
    {
        return env->NewStringUTF(cppString.c_str());
    }

    // Sets the field named `fieldName`, having the type signature `fieldSig`,
    // in object `object`, to `value`.
    void SetField(
        JNIEnv* env,
        jobject object,
        const char* fieldName,
        const char* fieldSig,
        jobject value)
    {
        jclass clazz = env->GetObjectClass(object);
        JDSB_REQUIRE(env, clazz);
        jfieldID field = env->GetFieldID(clazz, fieldName, fieldSig);
        JDSB_REQUIRE(env, field);
        env->SetObjectField(object, field, value);
    }

    // Sets the field name `fieldName`, which must be of type `String`,
    // in object `object`, to the value of `cValue`.
    // If the operation fails, a Java exception is thrown and the function
    // returns `false`.
    bool SetField(
        JNIEnv* env,
        jobject object,
        const char* fieldName,
        const std::string& cValue)
    {
        jstring jValue = ToJString(env, cValue);
        if (!jValue) return false;
        SetField(env, object, fieldName, "Ljava/lang/String;", jValue);
        return true;
    }

    // Creates a Java array containing the same elements as `vec`, where each
    // element is converted using `conv`.  `elementClass` must refer to the
    // class of the objects returned by `conv`.
    // If the operation fails, a Java exception is thrown and the function
    // returns null.
    template<typename CT>
    jobjectArray ToJArray(
        JNIEnv* env,
        jclass elementClass,
        const std::vector<CT>& vec,
        std::function<jobject(const CT&)> conv)
    {
        assert(env);
        assert(elementClass);
        assert(conv);
        const auto array = env->NewObjectArray(vec.size(), elementClass, nullptr);
        if (!array) return nullptr;
        for (size_t i = 0; i < vec.size(); ++i) {
            const auto element = conv(vec[i]);
            if (!element) return nullptr;
            env->SetObjectArrayElement(array, i, element);
        }
        return array;
    }
}


// =============================================================================
// DomainController
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_DomainController_createNative(
    JNIEnv* env,
    jclass,
    jlong domainLocatorPtr)
{
    if (!JEnforceNotNull(env, domainLocatorPtr)) return 0;
    auto domainLocator = reinterpret_cast<dsb::net::DomainLocator*>(domainLocatorPtr);
    try {
        auto domainController = new dsb::domain::Controller(*domainLocator);
        return reinterpret_cast<jlong>(domainController);
    } catch(const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_DomainController_destroyNative(
    JNIEnv* env,
    jclass,
    jlong ptr)
{
    if (JEnforceNotNull(env, ptr)) {
        delete reinterpret_cast<dsb::domain::Controller*>(ptr);
    }
}

namespace
{
    jobject ToJSlaveType(
        JNIEnv* env,
        jclass slaveTypeClass,
        jmethodID defaultCtor,
        const dsb::domain::Controller::SlaveType& cSlaveType)
    {
        jobject jSlaveType = env->NewObject(slaveTypeClass, defaultCtor);
        if (!jSlaveType) return nullptr;

        if (!SetField(env, jSlaveType, "name", cSlaveType.name)) return nullptr;
        if (!SetField(env, jSlaveType, "uuid", cSlaveType.uuid)) return nullptr;
        if (!SetField(env, jSlaveType, "description", cSlaveType.description)) return nullptr;
        if (!SetField(env, jSlaveType, "author", cSlaveType.author)) return nullptr;
        if (!SetField(env, jSlaveType, "version", cSlaveType.version)) return nullptr;

        auto stringClass = env->FindClass("java/lang/String");
        assert(stringClass);
        auto providers = ToJArray<std::string>(env, stringClass, cSlaveType.providers,
            [env] (const std::string& s) { return ToJString(env, s); });
        if (!providers) return nullptr;
        SetField(env, jSlaveType, "providers", "[Ljava/lang/String;", providers);
        return jSlaveType;
    }
}

JNIEXPORT jobjectArray JNICALL Java_com_sfh_dsb_DomainController_getSlaveTypesNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    if (!JEnforceNotNull(env, selfPtr)) return 0;
    auto dom = reinterpret_cast<dsb::domain::Controller*>(selfPtr);
    try {
        auto slaveTypes = dom->GetSlaveTypes();

        const auto slaveTypeClass = env->FindClass("com/sfh/dsb/DomainController$SlaveType");
        if (!slaveTypeClass) return nullptr;
        const auto slaveTypeCtor = env->GetMethodID(slaveTypeClass, "<init>", "()V");
        JDSB_REQUIRE(env, slaveTypeCtor);

        return ToJArray<dsb::domain::Controller::SlaveType>(env, slaveTypeClass, slaveTypes,
            [env, slaveTypeClass, slaveTypeCtor] (const dsb::domain::Controller::SlaveType& st) {
                return ToJSlaveType(env, slaveTypeClass, slaveTypeCtor, st);
            });
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
        return nullptr;
    }
}


// =============================================================================
// DomainLocator
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_DomainLocator_createNative(
    JNIEnv* env,
    jclass,
    jstring domainAddress)
{
    auto addr = ToString(env, domainAddress);
    if (!addr) return 0;
    try {
        auto ptr = new dsb::net::DomainLocator(dsb::net::GetDomainEndpoints(addr.get()));
        return reinterpret_cast<jlong>(ptr);
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_DomainLocator_destroyNative(
    JNIEnv* env,
    jclass,
    jlong ptr)
{
    if (JEnforceNotNull(env, ptr)) {
        delete reinterpret_cast<dsb::net::DomainLocator*>(ptr);
    }
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
