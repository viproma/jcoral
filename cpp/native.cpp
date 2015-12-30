#include <cassert>
#include <exception>
#include <functional>
#include <iostream>
#include <string>
#include <utility>

#include "dsb/domain/controller.hpp"
#include "dsb/execution/controller.hpp"
#include "dsb/net.hpp"

#include "com_sfh_dsb_DomainController.h"
#include "com_sfh_dsb_DomainLocator.h"
#include "com_sfh_dsb_ExecutionController.h"
#include "com_sfh_dsb_ExecutionLocator.h"
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

    // Gets the field named `fieldName` from the Java enum class named `enumName`.
    jobject GetEnumField(JNIEnv* env, const char* enumName, const char* fieldName)
    {
        const auto clazz = env->FindClass(enumName);
        if (!clazz) return nullptr;
        const auto signature = 'L' + std::string(enumName) + ';';
        const auto id = env->GetStaticFieldID(clazz, fieldName, signature.c_str());
        JDSB_REQUIRE(env, id);
        const auto value = env->GetStaticObjectField(clazz, id);
        JDSB_REQUIRE(env, value);
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
        jclass clazz = env->GetObjectClass(object);
        JDSB_REQUIRE(env, clazz);
        jfieldID field = env->GetFieldID(clazz, fieldName, "I");
        JDSB_REQUIRE(env, field);
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
        jclass clazz = env->GetObjectClass(object);
        JDSB_REQUIRE(env, clazz);
        jfieldID field = env->GetFieldID(clazz, fieldName, fieldSig);
        JDSB_REQUIRE(env, field);
        env->SetObjectField(object, field, value);
    }

    // Sets the field named `fieldName`, which must be of type `String`,
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
    class DataTypeConverter
    {
    public:
        static Maybe<DataTypeConverter> Create(JNIEnv* env)
        {
            auto jClass = env->FindClass("com/sfh/dsb/DataType");
            if (!jClass) return Maybe<DataTypeConverter>();

            DataTypeConverter ret;
            ret.env_ = env;
            ret.real_ = GetEnumField(env, "com/sfh/dsb/DataType", "REAL");
            if (!ret.real_) return Maybe<DataTypeConverter>();
            ret.integer_ = GetEnumField(env, "com/sfh/dsb/DataType", "INTEGER");
            if (!ret.integer_) return Maybe<DataTypeConverter>();
            ret.boolean_ = GetEnumField(env, "com/sfh/dsb/DataType", "BOOLEAN");
            if (!ret.boolean_) return Maybe<DataTypeConverter>();
            ret.string_ = GetEnumField(env, "com/sfh/dsb/DataType", "STRING");
            if (!ret.string_) return Maybe<DataTypeConverter>();

            return Actually(ret);
        }

        DataTypeConverter() { }

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
        static Maybe<CausalityConverter> Create(JNIEnv* env)
        {
            auto jClass = env->FindClass("com/sfh/dsb/Causality");
            if (!jClass) return Maybe<CausalityConverter>();

            CausalityConverter ret;
            ret.env_ = env;
            ret.parameter_ = GetEnumField(env, "com/sfh/dsb/Causality", "PARAMETER");
            if (!ret.parameter_) return Maybe<CausalityConverter>();
            ret.calculatedParameter_ = GetEnumField(env, "com/sfh/dsb/Causality", "CALCULATED_PARAMETER");
            if (!ret.calculatedParameter_) return Maybe<CausalityConverter>();
            ret.input_ = GetEnumField(env, "com/sfh/dsb/Causality", "INPUT");
            if (!ret.input_) return Maybe<CausalityConverter>();
            ret.output_ = GetEnumField(env, "com/sfh/dsb/Causality", "OUTPUT");
            if (!ret.output_) return Maybe<CausalityConverter>();
            ret.local_ = GetEnumField(env, "com/sfh/dsb/Causality", "LOCAL");
            if (!ret.local_) return Maybe<CausalityConverter>();

            return Actually(ret);
        }

        CausalityConverter() { }

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
        static Maybe<VariabilityConverter> Create(JNIEnv* env)
        {
            auto jClass = env->FindClass("com/sfh/dsb/Variability");
            if (!jClass) return Maybe<VariabilityConverter>();

            VariabilityConverter ret;
            ret.env_ = env;
            ret.constant_ = GetEnumField(env, "com/sfh/dsb/Variability", "CONSTANT");
            if (!ret.constant_) return Maybe<VariabilityConverter>();
            ret.fixed_ = GetEnumField(env, "com/sfh/dsb/Variability", "FIXED");
            if (!ret.fixed_) return Maybe<VariabilityConverter>();
            ret.tunable_ = GetEnumField(env, "com/sfh/dsb/Variability", "TUNABLE");
            if (!ret.tunable_) return Maybe<VariabilityConverter>();
            ret.discrete_ = GetEnumField(env, "com/sfh/dsb/Variability", "DISCRETE");
            if (!ret.discrete_) return Maybe<VariabilityConverter>();
            ret.continuous_ = GetEnumField(env, "com/sfh/dsb/Variability", "CONTINUOUS");
            if (!ret.continuous_) return Maybe<VariabilityConverter>();

            return Actually(ret);
        }

        VariabilityConverter() { }

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
        if (!jVarDesc) return nullptr;

        SetField(env, jVarDesc, "id", cVarDesc.ID());
        if (!SetField(env, jVarDesc, "name", cVarDesc.Name())) return nullptr;
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
        if (!jSlaveType) return nullptr;

        if (!SetField(env, jSlaveType, "name", cSlaveType.name)) return nullptr;
        if (!SetField(env, jSlaveType, "uuid", cSlaveType.uuid)) return nullptr;
        if (!SetField(env, jSlaveType, "description", cSlaveType.description)) return nullptr;
        if (!SetField(env, jSlaveType, "author", cSlaveType.author)) return nullptr;
        if (!SetField(env, jSlaveType, "version", cSlaveType.version)) return nullptr;

        const auto vdClass = env->FindClass("com/sfh/dsb/VariableDescription");
        if (!vdClass) return nullptr;
        const auto vdCtor = env->GetMethodID(vdClass, "<init>", "()V");
        JDSB_REQUIRE(env, vdCtor);
        const auto dtConv = DataTypeConverter::Create(env);
        JDSB_REQUIRE(env, dtConv);
        const auto csConv = CausalityConverter::Create(env);
        JDSB_REQUIRE(env, csConv);
        const auto vbConv = VariabilityConverter::Create(env);
        JDSB_REQUIRE(env, vbConv);
        const auto variables = ToJArray<dsb::model::VariableDescription>(
            env,
            vdClass,
            cSlaveType.variables,
            [env, vdClass, vdCtor, &dtConv, &csConv, &vbConv]
                (const dsb::model::VariableDescription& vd)
            {
                return ToJVariableDescription(env,
                    dtConv.get(), csConv.get(), vbConv.get(),
                    vdClass, vdCtor, vd);
            });
        SetField(env, jSlaveType, "variables", "[Lcom/sfh/dsb/VariableDescription;", variables);

        auto stringClass = env->FindClass("java/lang/String");
        JDSB_REQUIRE(env, stringClass);
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

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_DomainController_instantiateSlaveNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jstring slaveUUID,
    jint timeout_ms,
    jstring provider)
{
    if (!JEnforceNotNull(env, selfPtr)) return 0;
    auto dom = reinterpret_cast<dsb::domain::Controller*>(selfPtr);
    const auto cSlaveUUID = ToString(env, slaveUUID);
    if (!cSlaveUUID) return 0;
    const auto cProvider = ToString(env, provider);
    if (!cProvider) return 0;
    try {
        const auto slaveLoc = new dsb::net::SlaveLocator(
            dom->InstantiateSlave(
                cSlaveUUID.get(),
                boost::chrono::milliseconds(timeout_ms),
                cProvider.get()));
        return reinterpret_cast<jlong>(slaveLoc);
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
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
// ExecutionController
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_ExecutionController_spawnExecutionNative(
    JNIEnv* env,
    jclass,
    jlong domainLocatorPtr,
    jstring executionName,
    jint commTimeout_s)
{
    if (!JEnforceNotNull(env, domainLocatorPtr)) return 0;
    const auto cExeName = executionName
        ? ToString(env, executionName)
        : Actually(std::string());
    if (!cExeName) return 0;
    try {
        return reinterpret_cast<jlong>(new dsb::net::ExecutionLocator(
            dsb::execution::SpawnExecution(
                *reinterpret_cast<dsb::net::DomainLocator*>(domainLocatorPtr),
                cExeName.get(),
                boost::chrono::seconds(commTimeout_s))));
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
        return 0;
    }
}

JNIEXPORT jlong JNICALL Java_com_sfh_dsb_ExecutionController_createNative(
    JNIEnv* env,
    jclass,
    jlong locatorPtr)
{
    if (!JEnforceNotNull(env, locatorPtr)) return 0;
    try {
        return reinterpret_cast<jlong>(
            new dsb::execution::Controller(
                *reinterpret_cast<dsb::net::ExecutionLocator*>(locatorPtr)));
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    if (JEnforceNotNull(env, selfPtr)) try {
        auto exe = reinterpret_cast<dsb::execution::Controller*>(selfPtr);
        exe->Terminate();
        delete exe;
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_beginConfigNative(
    JNIEnv* env, jclass, jlong selfPtr)
{
    if (JEnforceNotNull(env, selfPtr)) try {
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)->BeginConfig();
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_endConfigNative(
    JNIEnv* env, jclass, jlong selfPtr)
{
    if (JEnforceNotNull(env, selfPtr)) try {
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)->EndConfig();
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_setSimulationTimeNative__JD(
    JNIEnv* env, jclass, jlong selfPtr, jdouble startTime)
{
    if (JEnforceNotNull(env, selfPtr)) try {
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)
            ->SetSimulationTime(startTime);
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
    }
}

JNIEXPORT void JNICALL Java_com_sfh_dsb_ExecutionController_setSimulationTimeNative__JDD(
    JNIEnv* env, jclass, jlong selfPtr, jdouble startTime, jdouble stopTime)
{
    if (JEnforceNotNull(env, selfPtr)) try {
        reinterpret_cast<dsb::execution::Controller*>(selfPtr)
            ->SetSimulationTime(startTime, stopTime);
    } catch (const std::exception& e) {
        ThrowJException(env, "java/lang/Exception", e.what());
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
// SlaveLocator
// =============================================================================

JNIEXPORT void JNICALL Java_com_sfh_dsb_SlaveLocator_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    delete reinterpret_cast<dsb::net::SlaveLocator*>(selfPtr);
}
