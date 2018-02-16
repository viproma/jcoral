/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * Functions and macros that wrap the JNI functions and help to write safer,
 * higher-level C++ code.
 */
#ifndef JCORAL_JNI_HELPERS_HPP
#define JCORAL_JNI_HELPERS_HPP

#include <cassert>
#include <cstdarg>
#include <exception>
#include <functional>
#include <stdexcept>
#include <string>
#include <vector>

#include <jni.h>
#include <boost/numeric/conversion/cast.hpp>


// Compiler-agnostic noreturn attribute
#if __cplusplus >= 201103L
#   define JCORAL_NORETURN [[noreturn]]
#elif defined(_MSC_VER)
#   define JCORAL_NORETURN __declspec(noreturn)
#else
#   define JCORAL_NORETURN
#endif

// Helpers for JCORAL_FATAL
#define JCORAL_STRINGIFY(x) #x
#define JCORAL_TOSTRING(x) JCORAL_STRINGIFY(x)

// Terminates the program, displaying the given error message.
// `msg` MUST be a string literal, and not a runtime variable.
#define JCORAL_FATAL(env, msg) \
    do { \
        jcoral::FatalError(env, "Fatal error in " __FILE__ "(" JCORAL_TOSTRING(__LINE__) "): " msg); \
    } while(false)

// Terminates the program with the given message if `test` evaluates to false.
// If a Java exception is in flight at this point, its message will be printed
// along with a stack trace.
#define JCORAL_REQUIRE(env, test) \
    if (!(test)) { \
        if (env->ExceptionOccurred()) { \
            env->ExceptionDescribe(); \
        } \
        JCORAL_FATAL(env, "Requirement not satisfied: " #test); \
    }


namespace jcoral
{


// =========================================================================
// ERROR HANDLING
// =========================================================================

// Terminates the program forcefully and abruptly.
// This is mainly to attach a noreturn attribute to the JNI FatalError()
// function.
JCORAL_NORETURN inline void FatalError(JNIEnv* env, const char* msg)
{
    env->FatalError(msg);
    // We never get here, the following is just to shut the
    // compiler up about the function returning.
    assert(false);
    std::terminate();
}

// Signals that a Java exception has been thrown (and consequently that
// control should be returned to the JVM).
class PendingJavaException : public std::runtime_error
{
public:
    PendingJavaException()
        : std::runtime_error("A java exception has been thrown")
    { }
};

// Signals that an error occurred and that a Java exception of a specific
// type should be thrown for it.
class JavaException : public std::runtime_error
{
public:
    JavaException(const char* className, std::string message = std::string())
        : std::runtime_error(
            std::string("An error occurred, for which a Java exception of type ")
            + className + " should be thrown"),
            className_(className),
            message_(std::move(message))
    { }

    const char* ClassName() const { return className_; }

    const char* Message() const { return message_.c_str(); }

private:
    const char* className_;
    std::string message_;
};

// Throws PendingJavaException if `test` is false. The purpose of this
// function is to test the return values of JNI calls.
template<typename Testable>
inline void CheckJNIReturn(Testable test)
{
    if (!test) throw PendingJavaException();
}

// Throws a PendingJavaException if `env->ExceptionOccurred()` is true.
// The purpose of this function is to handle exceptions from JNI calls where
// the return value does not signal success or failure.
inline void CheckNotThrown(JNIEnv* env)
{
    if (env->ExceptionOccurred()) throw PendingJavaException();
}

namespace detail
{
    // Throws a Java exception of the type specified by `className` (e.g.
    // "java/lang/Exception"), with a descriptive message given by `msg`.
    inline void ThrowJavaException(
        JNIEnv* env, const std::string& className, const std::string& msg)
    {
        auto exClass = env->FindClass(className.c_str());
        JCORAL_REQUIRE(env, exClass);
        JCORAL_REQUIRE(env, 0 == env->ThrowNew(exClass, msg.c_str()));
    }
}

// This function converts an in-flight C++ exception to an in-flight Java
// exception.  It may therefore only be called from a catch block (so that
// there is in fact a C++ exception in flight).  The calling code should
// return to the JVM immediately afterwards, because there is now a Java
// exception in flight.  (It is therefore recommended to call this only
// from top-level native functions.
inline void RethrowAsJavaException(JNIEnv* env)
{
    try {
        throw;
    } catch (const PendingJavaException&) {
        // Do nothing, Java exception is already in flight
    } catch (const JavaException& e) {
        detail::ThrowJavaException(env, e.ClassName(), e.Message());
    } catch (const std::logic_error& e) {
        detail::ThrowJavaException(env, "java/lang/RuntimeException", e.what());
    } catch (const std::exception& e) {
        detail::ThrowJavaException(env, "java/lang/Exception", e.what());
    } catch (...) {
        detail::ThrowJavaException(
            env,
            "java/lang/Error",
            "An unidentified error occurred in Coral");
    }
}

// Throws a JavaException corresponding to a java.lang.NullPointerException
// if `ptr` is null.
template<typename T>
void EnforceNotNull(T ptr)
{
    if (!ptr) {
        throw JavaException("java/lang/NullPointerException", "Null pointer");
    }
}


// =========================================================================
// JNI FUNCTION WRAPPERS
// =========================================================================

inline jclass FindClass(JNIEnv* env, const char* name)
{
    const auto clazz = env->FindClass(name);
    CheckJNIReturn(clazz);
    return clazz;
}

inline jclass GetObjectClass(JNIEnv* env, jobject obj)
{
    EnforceNotNull(obj);
    const auto clazz = env->GetObjectClass(obj);
    CheckJNIReturn(clazz);
    return clazz;
}

inline jobject NewObject(JNIEnv* env, jclass clazz, jmethodID methodID, ...)
{
    std::va_list args;
    va_start(args, methodID);
    const auto obj = env->NewObjectV(clazz, methodID, args);
    va_end(args);
    CheckJNIReturn(obj);
    return obj;
}

inline jmethodID GetMethodID(
    JNIEnv* env, jclass clazz, const char* name, const char* sig)
{
    const auto methodID = env->GetMethodID(clazz, name, sig);
    CheckJNIReturn(methodID);
    return methodID;
}

inline void CallVoidMethod(
    JNIEnv* env, jobject obj, jmethodID methodID, ...)
{
    std::va_list args;
    va_start(args, methodID);
    env->CallVoidMethodV(obj, methodID, args);
    va_end(args);
    CheckNotThrown(env);
}

inline jobject CallObjectMethod(
    JNIEnv* env, jobject obj, jmethodID methodID, ...)
{
    std::va_list args;
    va_start(args, methodID);
    const auto ret = env->CallObjectMethodV(obj, methodID, args);
    va_end(args);
    CheckNotThrown(env);
    return ret;
}

inline jboolean CallBooleanMethod(
    JNIEnv* env, jobject obj, jmethodID methodID, ...)
{
    std::va_list args;
    va_start(args, methodID);
    const auto ret = env->CallBooleanMethodV(obj, methodID, args);
    va_end(args);
    CheckNotThrown(env);
    return ret;
}

inline jint CallIntMethod(
    JNIEnv* env, jobject obj, jmethodID methodID, ...)
{
    std::va_list args;
    va_start(args, methodID);
    const auto ret = env->CallIntMethodV(obj, methodID, args);
    va_end(args);
    CheckNotThrown(env);
    return ret;
}

inline jdouble CallDoubleMethod(
    JNIEnv* env, jobject obj, jmethodID methodID, ...)
{
    std::va_list args;
    va_start(args, methodID);
    const auto ret = env->CallDoubleMethodV(obj, methodID, args);
    va_end(args);
    CheckNotThrown(env);
    return ret;
}

inline jfieldID GetFieldID(
    JNIEnv* env, jclass clazz, const char* name, const char* sig)
{
    const auto fieldID = env->GetFieldID(clazz, name, sig);
    CheckJNIReturn(fieldID);
    return fieldID;
}

inline jobject GetObjectField(JNIEnv* env, jobject obj, jfieldID fieldID)
{
    EnforceNotNull(obj);
    const auto field = env->GetObjectField(obj, fieldID);
    CheckNotThrown(env);
    return field;
}

inline jint GetIntField(JNIEnv* env, jobject obj, jfieldID fieldID)
{
    EnforceNotNull(obj);
    const auto field = env->GetIntField(obj, fieldID);
    CheckNotThrown(env);
    return field;
}

inline jdouble GetDoubleField(JNIEnv* env, jobject obj, jfieldID fieldID)
{
    EnforceNotNull(obj);
    const auto field = env->GetDoubleField(obj, fieldID);
    CheckNotThrown(env);
    return field;
}


// =============================================================================
// CONVENIENCE FUNCTIONS
// =============================================================================

// Converts a Java string to a C++ string.
inline std::string ToString(JNIEnv* env, jstring javaString)
{
    EnforceNotNull(javaString);
    const auto cString = env->GetStringUTFChars(javaString, nullptr);
    CheckJNIReturn(cString);
    const auto cppString = std::string(cString);
    env->ReleaseStringUTFChars(javaString, cString);
    return cppString;
}

// Converts a std::string to a Java string.
inline jstring ToJString(JNIEnv* env, const std::string& cppString)
{
    const auto jString = env->NewStringUTF(cppString.c_str());
    CheckJNIReturn(jString);
    return jString;
}

// Wraps CallObjectMethod() and converts the result to std::string.
inline std::string CallStringMethod(
    JNIEnv* env, jobject obj, jmethodID methodID, ...)
{
    std::va_list args;
    va_start(args, methodID);
    const auto ret = env->CallObjectMethodV(obj, methodID, args);
    va_end(args);
    CheckNotThrown(env);
    assert(env->IsInstanceOf(ret, jcoral::FindClass(env, "java/lang/String")));
    return ToString(env, static_cast<jstring>(ret));
}

// Gets the field named `fieldName` from the Java enum class named `enumName`.
inline jobject GetEnumField(
    JNIEnv* env, const char* enumName, const char* fieldName)
{
    const auto clazz = env->FindClass(enumName);
    CheckJNIReturn(clazz);
    const auto signature = 'L' + std::string(enumName) + ';';
    const auto id = env->GetStaticFieldID(clazz, fieldName, signature.c_str());
    CheckJNIReturn(id);
    const auto value = env->GetStaticObjectField(clazz, id);
    CheckJNIReturn(value);
    return value;
}

// Get the value of a field named `fieldName` of type `java.lang.String`
// from the given object.
inline std::string FieldToString(JNIEnv* env, jobject obj, const char* fieldName)
{
    const auto clazz = GetObjectClass(env, obj);
    const auto fieldID = GetFieldID(env, clazz, fieldName, "Ljava/lang/String;");
    return ToString(env, static_cast<jstring>(GetObjectField(env, obj, fieldID)));
}

// Sets the field named `fieldName`, which must be of type `int`,
// in object `object` to `value`.
inline void SetField(
    JNIEnv* env,
    jobject object,
    const char* fieldName,
    jint value)
{
    const auto clazz = GetObjectClass(env, object);
    const auto field = GetFieldID(env, clazz, fieldName, "I");
    env->SetIntField(object, field, value);
    CheckNotThrown(env);
}

// Sets the field named `fieldName`, having the type signature `fieldSig`,
// in object `object`, to `value`.
inline void SetField(
    JNIEnv* env,
    jobject object,
    const char* fieldName,
    const char* fieldSig,
    jobject value)
{
    const auto clazz = GetObjectClass(env, object);
    const auto field = GetFieldID(env, clazz, fieldName, fieldSig);
    env->SetObjectField(object, field, value);
    CheckNotThrown(env);
}

// Sets the field named `fieldName`, which must be of type `String`,
// in object `object`, to the value of `cValue`.
inline void SetField(
    JNIEnv* env,
    jobject object,
    const char* fieldName,
    const std::string& cValue)
{
    SetField(env, object, fieldName, "Ljava/lang/String;", ToJString(env, cValue));
}

// Creates a Java array containing the same elements as the random-access
// range defined by the `begin` and `end` iterators.  Each element is converted
// using `conv`.  `elementClass` will be the element type of the array, and
// `conv` must return instances of this class (or one of its subclasses).
template<typename CT, typename RandomAccessIterator>
jobjectArray ToJArray(
    JNIEnv* env,
    jclass elementClass,
    RandomAccessIterator begin,
    RandomAccessIterator end,
    std::function<jobject(const CT&)> conv)
{
    assert(env);
    assert(elementClass);
    assert(conv);
    const auto array = env->NewObjectArray(
        boost::numeric_cast<jsize>(end-begin), elementClass, nullptr);
    CheckJNIReturn(array);
    for (size_t i = 0; begin != end; ++i, ++begin) {
        env->SetObjectArrayElement(
            array, boost::numeric_cast<jsize>(i), conv(*begin));
    }
    return array;
}


// Calls `fun` for each element in the Iterable object `iterable`.
inline void ForEach(
    JNIEnv* env, jobject iterable, std::function<void(jobject)> fun)
{
    const auto iterableClass = jcoral::FindClass(env, "java/lang/Iterable");
    const auto iteratorClass = jcoral::FindClass(env, "java/util/Iterator");
    assert(env->IsInstanceOf(iterable, iterableClass));
    const auto getIteratorMID = jcoral::GetMethodID(
        env,
        iterableClass,
        "iterator",
        "()Ljava/util/Iterator;");
    const auto hasNextMID =
        jcoral::GetMethodID(env, iteratorClass, "hasNext", "()Z");
    const auto nextMID =
        jcoral::GetMethodID(env, iteratorClass, "next", "()Ljava/lang/Object;");

    const auto iterator =
        jcoral::CallObjectMethod(env, iterable, getIteratorMID);
    assert(env->IsInstanceOf(iterator, iteratorClass));

    while (jcoral::CallBooleanMethod(env, iterator, hasNextMID)) {
        fun(jcoral::CallObjectMethod(env, iterator, nextMID));
    }
}


} // namespace
#endif // header guard
