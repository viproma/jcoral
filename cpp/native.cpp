#include <cassert>
#include <exception>
#include <functional>
#include <iostream>
#include <stdexcept>
#include <string>
#include <utility>

#include "boost/numeric/conversion/cast.hpp"
#include "boost/variant.hpp"

#include "dsb/domain/controller.hpp"
#include "dsb/execution/controller.hpp"
#include "dsb/net.hpp"

#include "com_sfh_dsb_DomainController.h"
#include "com_sfh_dsb_DomainLocator.h"
#include "com_sfh_dsb_ExecutionController.h"
#include "com_sfh_dsb_ExecutionLocator.h"
#include "com_sfh_dsb_Future.h"
#include "com_sfh_dsb_Future_SlaveID.h"
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

    // Signals that a Java exception has been thrown (and consequently that
    // control should be returned to the JVM).
    class JavaExceptionThrown : public std::runtime_error
    {
    public:
        JavaExceptionThrown()
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

    // Throws JavaExceptionThrown if `test` is false. The purpose of this
    // function is to test the return values of JNI calls.
    void CheckJNIReturn(bool test)
    {
        if (!test) throw JavaExceptionThrown();
    }

    // This function converts an in-flight C++ exception to an in-flight Java
    // exception.  It may therefore only be called from a catch block (so that
    // there is in fact a C++ exception in flight).  The calling code should
    // return to the JVM immediately afterwards, because there is now a Java
    // exception in flight.  (It is therefore recommended to call this only
    // from top-level native functions.
    void RethrowAsJavaException(JNIEnv* env)
    {
        try {
            throw;
        } catch (const JavaExceptionThrown&) {
            // Do nothing, Java exception is already in flight
        } catch (const JavaException& e) {
            ThrowJException(env, e.ClassName(), e.Message());
        } catch (const std::logic_error& e) {
            ThrowJException(env, "java/lang/RuntimeException", e.what());
        } catch (const std::exception& e) {
            ThrowJException(env, "java/lang/Exception", e.what());
        } catch (...) {
            ThrowJException(
                env,
                "java/lang/Error",
                "An unidentified error occurred in DSB");
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

    // Converts a Java string to a C++ string.
    std::string ToString(JNIEnv* env, jstring javaString)
    {
        EnforceNotNull(javaString);
        const auto cString = env->GetStringUTFChars(javaString, nullptr);
        CheckJNIReturn(cString);
        const auto cppString = std::string(cString);
        env->ReleaseStringUTFChars(javaString, cString);
        return cppString;
    }

    // Converts a std::string to a Java string.
    jstring ToJString(JNIEnv* env, const std::string& cppString)
    {
        const auto jString = env->NewStringUTF(cppString.c_str());
        CheckJNIReturn(jString);
        return jString;
    }

    // Gets the field named `fieldName` from the Java enum class named `enumName`.
    jobject GetEnumField(JNIEnv* env, const char* enumName, const char* fieldName)
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

    // Sets the field named `fieldName`, which must be of type `int`,
    // in object `object` to `value`.
    void SetField(
        JNIEnv* env,
        jobject object,
        const char* fieldName,
        jint value)
    {
        const auto clazz = env->GetObjectClass(object);
        CheckJNIReturn(clazz);
        const auto field = env->GetFieldID(clazz, fieldName, "I");
        CheckJNIReturn(field);
        env->SetIntField(object, field, value);
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
        const auto clazz = env->GetObjectClass(object);
        CheckJNIReturn(clazz);
        const auto field = env->GetFieldID(clazz, fieldName, fieldSig);
        CheckJNIReturn(field);
        env->SetObjectField(object, field, value);
    }

    // Sets the field named `fieldName`, which must be of type `String`,
    // in object `object`, to the value of `cValue`.
    void SetField(
        JNIEnv* env,
        jobject object,
        const char* fieldName,
        const std::string& cValue)
    {
        SetField(env, object, fieldName, "Ljava/lang/String;", ToJString(env, cValue));
    }

    // Creates a Java array containing the same elements as `vec`, where each
    // element is converted using `conv`.  `elementClass` must refer to the
    // class of the objects returned by `conv`.
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
        CheckJNIReturn(array);
        for (size_t i = 0; i < vec.size(); ++i) {
            const auto element = conv(vec[i]);
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
    try {
        EnforceNotNull(domainLocatorPtr);
        const auto domainLocator =
            reinterpret_cast<dsb::net::DomainLocator*>(domainLocatorPtr);
        const auto domainController = new dsb::domain::Controller(*domainLocator);
        return reinterpret_cast<jlong>(domainController);
    } catch(...) {
        RethrowAsJavaException(env);
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_DomainController_destroyNative(
    JNIEnv* env,
    jclass,
    jlong ptr)
{
    delete reinterpret_cast<dsb::domain::Controller*>(ptr);
}

namespace
{
    class DataTypeConverter
    {
    public:
        DataTypeConverter(JNIEnv* env)
            : env_(env),
              real_(nullptr),
              integer_(nullptr),
              boolean_(nullptr),
              string_(nullptr)
        {
            real_ = GetEnumField(env, "com/sfh/dsb/DataType", "REAL");
            integer_ = GetEnumField(env, "com/sfh/dsb/DataType", "INTEGER");
            boolean_ = GetEnumField(env, "com/sfh/dsb/DataType", "BOOLEAN");
            string_ = GetEnumField(env, "com/sfh/dsb/DataType", "STRING");
        }

        jobject JavaValue(dsb::model::DataType dt) const
        {
            switch (dt) {
                case dsb::model::REAL_DATATYPE:     return real_;
                case dsb::model::INTEGER_DATATYPE:  return integer_;
                case dsb::model::BOOLEAN_DATATYPE:  return boolean_;
                case dsb::model::STRING_DATATYPE:   return string_;
                default:
                    JDSB_REQUIRE(env_, false);
                    return nullptr; // never get here
            }
        }

    private:
        JNIEnv* env_;
        jobject real_;
        jobject integer_;
        jobject boolean_;
        jobject string_;
    };

    class CausalityConverter
    {
    public:
        CausalityConverter(JNIEnv* env)
            : env_(env),
              parameter_(nullptr),
              calculatedParameter_(nullptr),
              input_(nullptr),
              output_(nullptr),
              local_(nullptr)
        {
            parameter_ = GetEnumField(env, "com/sfh/dsb/Causality", "PARAMETER");
            calculatedParameter_ = GetEnumField(env, "com/sfh/dsb/Causality", "CALCULATED_PARAMETER");
            input_ = GetEnumField(env, "com/sfh/dsb/Causality", "INPUT");
            output_ = GetEnumField(env, "com/sfh/dsb/Causality", "OUTPUT");
            local_ = GetEnumField(env, "com/sfh/dsb/Causality", "LOCAL");
        }

        jobject JavaValue(dsb::model::Causality c) const
        {
            switch (c) {
                case dsb::model::PARAMETER_CAUSALITY:            return parameter_;
                case dsb::model::CALCULATED_PARAMETER_CAUSALITY: return calculatedParameter_;
                case dsb::model::INPUT_CAUSALITY:                return input_;
                case dsb::model::OUTPUT_CAUSALITY:               return output_;
                case dsb::model::LOCAL_CAUSALITY:                return local_;
                default:
                    JDSB_REQUIRE(env_, false);
                    return nullptr; // never get here
            }
        }

    private:
        JNIEnv* env_;
        jobject parameter_;
        jobject calculatedParameter_;
        jobject input_;
        jobject output_;
        jobject local_;
    };

    class VariabilityConverter
    {
    public:
        VariabilityConverter(JNIEnv* env)
            : env_(env),
              constant_(nullptr),
              fixed_(nullptr),
              tunable_(nullptr),
              discrete_(nullptr),
              continuous_(nullptr)
        {
            constant_ = GetEnumField(env, "com/sfh/dsb/Variability", "CONSTANT");
            fixed_ = GetEnumField(env, "com/sfh/dsb/Variability", "FIXED");
            tunable_ = GetEnumField(env, "com/sfh/dsb/Variability", "TUNABLE");
            discrete_ = GetEnumField(env, "com/sfh/dsb/Variability", "DISCRETE");
            continuous_ = GetEnumField(env, "com/sfh/dsb/Variability", "CONTINUOUS");
        }

        jobject JavaValue(dsb::model::Variability c) const
        {
            switch (c) {
                case dsb::model::CONSTANT_VARIABILITY:   return constant_;
                case dsb::model::FIXED_VARIABILITY:      return fixed_;
                case dsb::model::TUNABLE_VARIABILITY:    return tunable_;
                case dsb::model::DISCRETE_VARIABILITY:   return discrete_;
                case dsb::model::CONTINUOUS_VARIABILITY: return continuous_;
                default:
                    JDSB_REQUIRE(env_, false);
                    return nullptr; // never get here
            }
        }

    private:
        JNIEnv* env_;
        jobject constant_;
        jobject fixed_;
        jobject tunable_;
        jobject discrete_;
        jobject continuous_;
    };


    jobject ToJVariableDescription(
        JNIEnv* env,
        const DataTypeConverter& dtConv,
        const CausalityConverter& csConv,
        const VariabilityConverter& vbConv,
        jclass variableDescriptionClass,
        jmethodID defaultCtor,
        const dsb::model::VariableDescription& cVarDesc)
    {
        auto jVarDesc = env->NewObject(variableDescriptionClass, defaultCtor);
        CheckJNIReturn(jVarDesc);
        SetField(env, jVarDesc, "id", cVarDesc.ID());
        SetField(env, jVarDesc, "name", cVarDesc.Name());
        SetField(env, jVarDesc, "dataType", "Lcom/sfh/dsb/DataType;", dtConv.JavaValue(cVarDesc.DataType()));
        SetField(env, jVarDesc, "causality", "Lcom/sfh/dsb/Causality;", csConv.JavaValue(cVarDesc.Causality()));
        SetField(env, jVarDesc, "variability", "Lcom/sfh/dsb/Variability;", vbConv.JavaValue(cVarDesc.Variability()));
        return jVarDesc;
    }

    jobject ToJSlaveType(
        JNIEnv* env,
        jclass slaveTypeClass,
        jmethodID defaultCtor,
        const dsb::domain::Controller::SlaveType& cSlaveType)
    {
        jobject jSlaveType = env->NewObject(slaveTypeClass, defaultCtor);
        CheckJNIReturn(jSlaveType);
        SetField(env, jSlaveType, "name", cSlaveType.name);
        SetField(env, jSlaveType, "uuid", cSlaveType.uuid);
        SetField(env, jSlaveType, "description", cSlaveType.description);
        SetField(env, jSlaveType, "author", cSlaveType.author);
        SetField(env, jSlaveType, "version", cSlaveType.version);

        const auto vdClass = env->FindClass("com/sfh/dsb/VariableDescription");
        CheckJNIReturn(vdClass);
        const auto vdCtor = env->GetMethodID(vdClass, "<init>", "()V");
        CheckJNIReturn(vdCtor);

        const auto dtConv = DataTypeConverter(env);
        const auto csConv = CausalityConverter(env);
        const auto vbConv = VariabilityConverter(env);
        const auto variables = ToJArray<dsb::model::VariableDescription>(
            env,
            vdClass,
            cSlaveType.variables,
            [env, vdClass, vdCtor, &dtConv, &csConv, &vbConv]
                (const dsb::model::VariableDescription& vd)
            {
                return ToJVariableDescription(env,
                    dtConv, csConv, vbConv,
                    vdClass, vdCtor, vd);
            });
        SetField(env, jSlaveType, "variables", "[Lcom/sfh/dsb/VariableDescription;", variables);

        auto stringClass = env->FindClass("java/lang/String");
        CheckJNIReturn(stringClass);
        auto providers = ToJArray<std::string>(env, stringClass, cSlaveType.providers,
            [env] (const std::string& s) { return ToJString(env, s); });
        SetField(env, jSlaveType, "providers", "[Ljava/lang/String;", providers);
        return jSlaveType;
    }
}

JNIEXPORT jobjectArray JNICALL Java_com_sfh_dsb_DomainController_getSlaveTypesNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        EnforceNotNull(selfPtr);
        const auto dom = reinterpret_cast<dsb::domain::Controller*>(selfPtr);
        const auto slaveTypes = dom->GetSlaveTypes();

        const auto slaveTypeClass =
            env->FindClass("com/sfh/dsb/DomainController$SlaveType");
        CheckJNIReturn(slaveTypeClass);
        const auto slaveTypeCtor =
            env->GetMethodID(slaveTypeClass, "<init>", "()V");
        CheckJNIReturn(slaveTypeCtor);

        return ToJArray<dsb::domain::Controller::SlaveType>(env, slaveTypeClass, slaveTypes,
            [env, slaveTypeClass, slaveTypeCtor] (const dsb::domain::Controller::SlaveType& st) {
                return ToJSlaveType(env, slaveTypeClass, slaveTypeCtor, st);
            });
    } catch (...) {
        RethrowAsJavaException(env);
        return nullptr;
    }
}

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_DomainController_instantiateSlaveNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jstring slaveUUID,
    jint timeout_ms,
    jstring provider)
{
    try {
        EnforceNotNull(selfPtr);
        const auto dom = reinterpret_cast<dsb::domain::Controller*>(selfPtr);
        const auto slaveLoc = new dsb::net::SlaveLocator(
            dom->InstantiateSlave(
                ToString(env, slaveUUID),
                std::chrono::milliseconds(timeout_ms),
                ToString(env, provider)));
        return reinterpret_cast<jlong>(slaveLoc);
    } catch (...) {
        RethrowAsJavaException(env);
        return 0;
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
    try {
        const auto addr = ToString(env, domainAddress);
        const auto ptr = new dsb::net::DomainLocator(
            dsb::net::GetDomainEndpoints(addr));
        return reinterpret_cast<jlong>(ptr);
    } catch (...) {
        RethrowAsJavaException(env);
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_DomainLocator_destroyNative(
    JNIEnv* env,
    jclass,
    jlong ptr)
{
    delete reinterpret_cast<dsb::net::DomainLocator*>(ptr);
}


// =============================================================================
// ExecutionController
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_ExecutionController_spawnExecutionNative(
    JNIEnv* env,
    jclass,
    jlong domainLocatorPtr,
    jstring executionName,
    jint commTimeout_s)
{
    try {
        EnforceNotNull(domainLocatorPtr);
        const auto cExeName = executionName ? ToString(env, executionName) : std::string();
        return reinterpret_cast<jlong>(new dsb::net::ExecutionLocator(
            dsb::execution::SpawnExecution(
                *reinterpret_cast<dsb::net::DomainLocator*>(domainLocatorPtr),
                cExeName,
                std::chrono::seconds(commTimeout_s))));
    } catch (...) {
        RethrowAsJavaException(env);
        return 0;
    }
}

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_ExecutionController_createNative(
    JNIEnv* env,
    jclass,
    jlong locatorPtr)
{
    try {
        EnforceNotNull(locatorPtr);
        return reinterpret_cast<jlong>(
            new dsb::execution::Controller(
                *reinterpret_cast<dsb::net::ExecutionLocator*>(locatorPtr)));
    } catch (...) {
        RethrowAsJavaException(env);
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        EnforceNotNull(selfPtr);
        const auto exe = reinterpret_cast<dsb::execution::Controller*>(selfPtr);
        exe->Terminate();
        delete exe;
    } catch (...) {
        RethrowAsJavaException(env);
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_beginConfigNative(
    JNIEnv* env, jclass, jlong selfPtr)
{
    try {
        EnforceNotNull(selfPtr);
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)->BeginConfig();
    } catch (...) {
        RethrowAsJavaException(env);
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_endConfigNative(
    JNIEnv* env, jclass, jlong selfPtr)
{
    try {
        EnforceNotNull(selfPtr);
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)->EndConfig();
    } catch (...) {
        RethrowAsJavaException(env);
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_setSimulationTimeNative__JD(
    JNIEnv* env, jclass, jlong selfPtr, jdouble startTime)
{
    try {
        EnforceNotNull(selfPtr);
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)
            ->SetSimulationTime(startTime);
    } catch (...) {
        RethrowAsJavaException(env);
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_setSimulationTimeNative__JDD(
    JNIEnv* env, jclass, jlong selfPtr, jdouble startTime, jdouble stopTime)
{
    try {
        EnforceNotNull(selfPtr);
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)
            ->SetSimulationTime(startTime, stopTime);
    } catch (...) {
        RethrowAsJavaException(env);
    }
}

namespace
{
    typedef boost::variant<
            std::future<void>,
            std::future<dsb::model::SlaveID>
        > FutureVariant;
}

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_ExecutionController_addSlaveNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jlong slaveLocatorPtr,
    jint commTimeout_ms)
{
    try {
        EnforceNotNull(selfPtr);
        EnforceNotNull(slaveLocatorPtr);
        const auto exe = reinterpret_cast<dsb::execution::Controller*>(selfPtr);
        const auto slaveLoc = reinterpret_cast<const dsb::net::SlaveLocator*>(slaveLocatorPtr);
        return reinterpret_cast<jlong>(new FutureVariant(
            exe->AddSlave(*slaveLoc, std::chrono::milliseconds(commTimeout_ms))));
    } catch (...) {
        RethrowAsJavaException(env);
        return 0;
    }
}


// =============================================================================
// ExecutionLocator
// =============================================================================

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionLocator_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    delete reinterpret_cast<dsb::net::ExecutionLocator*>(selfPtr);
}


// =============================================================================
// Future
// =============================================================================

namespace
{
    // Visitors for FutureVariant operations
    class Wait : public boost::static_visitor<>
    {
    public:
        template<typename T>
        void operator()(const std::future<T>& f) const { f.wait(); }
    };

    class WaitFor : public boost::static_visitor<bool>
    {
    public:
        WaitFor(std::chrono::milliseconds timeout) : timeout_(timeout) { }

        template<typename T>
        bool operator()(const std::future<T>& f)
            const
        {
            return f.wait_for(timeout_) == std::future_status::ready;
        }
    private:
        std::chrono::milliseconds timeout_;
    };
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_Future_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    delete reinterpret_cast<FutureVariant*>(selfPtr);
}


JNIEXPORT void JNICALL Java_com_sfh_dsb_Future_waitForResultNative__J(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        EnforceNotNull(selfPtr);
        const auto f = reinterpret_cast<const FutureVariant*>(selfPtr);
        boost::apply_visitor(Wait(), *f);
    } catch (...) {
        RethrowAsJavaException(env);
    }
}

JNIEXPORT jboolean JNICALL Java_com_sfh_dsb_Future_waitForResultNative__JI(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jint timeout_ms)
{
    try {
        EnforceNotNull(selfPtr);
        const auto f = reinterpret_cast<const FutureVariant*>(selfPtr);
        return boost::apply_visitor(
            WaitFor(std::chrono::milliseconds(timeout_ms)),
            *f);
    } catch (...) {
        RethrowAsJavaException(env);
        return false;
    }
}

JNIEXPORT jint JNICALL Java_com_sfh_dsb_Future_00024SlaveID_getValueNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        EnforceNotNull(selfPtr);
        const auto futureVariant = reinterpret_cast<FutureVariant*>(selfPtr);
        const auto future = boost::get<std::future<dsb::model::SlaveID>>(futureVariant);
        JDSB_REQUIRE(env, future);
        return boost::numeric_cast<jint>(future->get());
    } catch (...) {
        RethrowAsJavaException(env);
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_Future_00024Void_getValueNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        EnforceNotNull(selfPtr);
        const auto futureVariant = reinterpret_cast<FutureVariant*>(selfPtr);
        const auto future = boost::get<std::future<void>>(futureVariant);
        JDSB_REQUIRE(env, future);
        future->get();
    } catch (...) {
        RethrowAsJavaException(env);
    }
}


// =============================================================================
// SlaveLocator
// =============================================================================

JNIEXPORT void JNICALL Java_com_sfh_dsb_SlaveLocator_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    delete reinterpret_cast<dsb::net::SlaveLocator*>(selfPtr);
}
