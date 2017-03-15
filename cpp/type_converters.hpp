/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * Conversions between Coral/C++ types and JCoral/Java types.
 */
#ifndef JCORAL_TYPE_CONVERTERS_HPP
#define JCORAL_TYPE_CONVERTERS_HPP

#include <cassert>
#include <boost/numeric/conversion/cast.hpp>
#include <coral/model.hpp>
#include "jni_helpers.hpp"


namespace jcoral
{


// Converts between coral::model::DataType and no.viproma.coral.model.DataType
class DataTypeConverter
{
public:
    DataTypeConverter(JNIEnv* env)
        : env_{env}
        , real_   {jcoral::GetEnumField(env, "no/viproma/coral/model/DataType", "REAL")}
        , integer_{jcoral::GetEnumField(env, "no/viproma/coral/model/DataType", "INTEGER")}
        , boolean_{jcoral::GetEnumField(env, "no/viproma/coral/model/DataType", "BOOLEAN")}
        , string_ {jcoral::GetEnumField(env, "no/viproma/coral/model/DataType", "STRING")}
    {
    }

    jobject ToJava(coral::model::DataType dt) const
    {
        switch (dt) {
            case coral::model::REAL_DATATYPE:     return real_;
            case coral::model::INTEGER_DATATYPE:  return integer_;
            case coral::model::BOOLEAN_DATATYPE:  return boolean_;
            case coral::model::STRING_DATATYPE:   return string_;
            default: JCORAL_FATAL(env_, "Unsupported data type encountered");
        }
    }

    coral::model::DataType ToCpp(jobject x) const
    {
        if (env_->IsSameObject(x, real_))         return coral::model::REAL_DATATYPE;
        else if (env_->IsSameObject(x, integer_)) return coral::model::INTEGER_DATATYPE;
        else if (env_->IsSameObject(x, boolean_)) return coral::model::BOOLEAN_DATATYPE;
        else if (env_->IsSameObject(x, string_))  return coral::model::STRING_DATATYPE;
        else JCORAL_FATAL(env_, "Unsupported data type encountered");
    }

private:
    JNIEnv* env_;
    jobject real_;
    jobject integer_;
    jobject boolean_;
    jobject string_;
};


// Converts between coral::model::Causality and no.viproma.coral.model.Causality
class CausalityConverter
{
public:
    CausalityConverter(JNIEnv* env)
        : env_{env}
        , parameter_          {jcoral::GetEnumField(env, "no/viproma/coral/model/Causality", "PARAMETER")}
        , calculatedParameter_{jcoral::GetEnumField(env, "no/viproma/coral/model/Causality", "CALCULATED_PARAMETER")}
        , input_              {jcoral::GetEnumField(env, "no/viproma/coral/model/Causality", "INPUT")}
        , output_             {jcoral::GetEnumField(env, "no/viproma/coral/model/Causality", "OUTPUT")}
        , local_              {jcoral::GetEnumField(env, "no/viproma/coral/model/Causality", "LOCAL")}
    {
    }

    jobject ToJava(coral::model::Causality c) const
    {
        switch (c) {
            case coral::model::PARAMETER_CAUSALITY:            return parameter_;
            case coral::model::CALCULATED_PARAMETER_CAUSALITY: return calculatedParameter_;
            case coral::model::INPUT_CAUSALITY:                return input_;
            case coral::model::OUTPUT_CAUSALITY:               return output_;
            case coral::model::LOCAL_CAUSALITY:                return local_;
            default: JCORAL_FATAL(env_, "Unsupported variable causality encountered");
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


// Converts between coral::model::Variability and no.viproma.coral.model.Variability
class VariabilityConverter
{
public:
    VariabilityConverter(JNIEnv* env)
        : env_(env)
        , constant_  {jcoral::GetEnumField(env, "no/viproma/coral/model/Variability", "CONSTANT")}
        , fixed_     {jcoral::GetEnumField(env, "no/viproma/coral/model/Variability", "FIXED")}
        , tunable_   {jcoral::GetEnumField(env, "no/viproma/coral/model/Variability", "TUNABLE")}
        , discrete_  {jcoral::GetEnumField(env, "no/viproma/coral/model/Variability", "DISCRETE")}
        , continuous_{jcoral::GetEnumField(env, "no/viproma/coral/model/Variability", "CONTINUOUS")}
    {
    }

    jobject ToJava(coral::model::Variability c) const
    {
        switch (c) {
            case coral::model::CONSTANT_VARIABILITY:   return constant_;
            case coral::model::FIXED_VARIABILITY:      return fixed_;
            case coral::model::TUNABLE_VARIABILITY:    return tunable_;
            case coral::model::DISCRETE_VARIABILITY:   return discrete_;
            case coral::model::CONTINUOUS_VARIABILITY: return continuous_;
            default: JCORAL_FATAL(env_, "Unsupported variable variability encountered");
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


// Converts between coral::model::SlaveID and no.viproma.coral.model.SlaveID
class SlaveIDConverter
{
public:
    explicit SlaveIDConverter(JNIEnv* env)
        : env_{env}
        , slaveIDClass_{jcoral::FindClass(env, "no/viproma/coral/model/SlaveID")}
        , init_{jcoral::GetMethodID(env, slaveIDClass_, "<init>", "(I)V")}
        , getID_{jcoral::GetMethodID(env, slaveIDClass_, "getID", "()I")}
    {
    }

    coral::model::SlaveID ToCpp(jobject obj) const
    {
        assert(env_->IsInstanceOf(obj, slaveIDClass_));
        return boost::numeric_cast<coral::model::SlaveID>(
            jcoral::CallIntMethod(env_, obj, getID_));
    }

    jobject ToJava(coral::model::SlaveID slaveID) const
    {
        return jcoral::NewObject(
            env_,
            slaveIDClass_,
            init_,
            boost::numeric_cast<jint>(slaveID));
    }

private:
    JNIEnv* env_;
    jclass slaveIDClass_;
    jmethodID init_;
    jmethodID getID_;
};


// Converts between coral::model::ScalarValue and no.viproma.coral.model.ScalarValue
class ScalarValueConverter
{
public:
    ScalarValueConverter(JNIEnv* env)
        : env_{env}
        , dtConv_{env}
        , scalarValueClass_{
            jcoral::FindClass(env, "no/viproma/coral/model/ScalarValue")}
        , getDataType_{
            jcoral::GetMethodID(
                env, scalarValueClass_, "getDataType", "()Lno/viproma/coral/model/DataType;")}
        , getRealValue_{
            jcoral::GetMethodID(env, scalarValueClass_, "getRealValue", "()D")}
        , getIntegerValue_{
            jcoral::GetMethodID(env, scalarValueClass_, "getIntegerValue", "()I")}
        , getBooleanValue_{
            jcoral::GetMethodID(env, scalarValueClass_, "getBooleanValue", "()Z")}
        , getStringValue_{
            jcoral::GetMethodID(
                env, scalarValueClass_, "getStringValue", "()Ljava/lang/String;")}
    {
    }

    coral::model::ScalarValue ToCpp(jobject obj) const
    {
        assert(env_->IsInstanceOf(obj, scalarValueClass_));
        const auto jDataType = jcoral::CallObjectMethod(env_, obj, getDataType_);
        const auto dataType = dtConv_.ToCpp(jDataType);

        coral::model::ScalarValue sv;
        switch (dataType) {
            case coral::model::REAL_DATATYPE:
                sv = jcoral::CallDoubleMethod(env_, obj, getRealValue_);
                break;
            case coral::model::INTEGER_DATATYPE:
                sv = jcoral::CallIntMethod(env_, obj, getIntegerValue_);
                break;
            case coral::model::BOOLEAN_DATATYPE:
                sv = (jcoral::CallBooleanMethod(env_, obj, getBooleanValue_) ? true : false);
                break;
            case coral::model::STRING_DATATYPE:
                sv = jcoral::ToString(
                    env_,
                    static_cast<jstring>(jcoral::CallObjectMethod(env_, obj, getStringValue_)));
                break;
        }
        jcoral::CheckNotThrown(env_);
        return sv;
    }

private:
    JNIEnv* env_;
    jcoral::DataTypeConverter dtConv_;
    jclass scalarValueClass_;
    jmethodID getDataType_;
    jmethodID getRealValue_;
    jmethodID getIntegerValue_;
    jmethodID getBooleanValue_;
    jmethodID getStringValue_;
};


// Converts between coral::model::VariableDescription and no.viproma.coral.model.VariableDescription
class VariableDescriptionConverter
{
public:
    VariableDescriptionConverter(JNIEnv* env)
        : env_{env}
        , dtConv_{env}
        , csConv_{env}
        , vbConv_{env}
        , class_{jcoral::FindClass(env, "no/viproma/coral/model/VariableDescription")}
        , constructor_{jcoral::GetMethodID(env, class_, "<init>", "()V")}
        , setID_{jcoral::GetMethodID(env, class_, "setID", "(I)V")}
        , setName_{
            jcoral::GetMethodID(env, class_, "setName", "(Ljava/lang/String;)V")}
        , setDataType_{
            jcoral::GetMethodID(
                env,
                class_,
                "setDataType",
                "(Lno/viproma/coral/model/DataType;)V")}
        , setCausality_{
            jcoral::GetMethodID(
                env,
                class_,
                "setCausality",
                "(Lno/viproma/coral/model/Causality;)V")}
        , setVariability_{
            jcoral::GetMethodID(
                env,
                class_,
                "setVariability",
                "(Lno/viproma/coral/model/Variability;)V")}
    {
    }

    jobject ToJava(const coral::model::VariableDescription& cvd) const
    {
        const auto jvd = jcoral::NewObject(env_, class_, constructor_);
        jcoral::CallVoidMethod(
            env_, jvd, setID_, boost::numeric_cast<jint>(cvd.ID()));
        jcoral::CallVoidMethod(
            env_, jvd, setName_, ToJString(env_, cvd.Name()));
        jcoral::CallVoidMethod(
            env_, jvd, setDataType_, dtConv_.ToJava(cvd.DataType()));
        jcoral::CallVoidMethod(
            env_, jvd, setCausality_, csConv_.ToJava(cvd.Causality()));
        jcoral::CallVoidMethod(
            env_, jvd, setVariability_, vbConv_.ToJava(cvd.Variability()));
        return jvd;
    }

private:
    JNIEnv* env_;
    jcoral::DataTypeConverter dtConv_;
    jcoral::CausalityConverter csConv_;
    jcoral::VariabilityConverter vbConv_;
    jclass class_;
    jmethodID constructor_;
    jmethodID setID_;
    jmethodID setName_;
    jmethodID setDataType_;
    jmethodID setCausality_;
    jmethodID setVariability_;
};


// Converts between coral::model::Variable and no.viproma.coral.model.Variable
class VariableConverter
{
public:
    explicit VariableConverter(JNIEnv* env)
        : env_{env}
        , siConv_{env}
        , variableClass_{jcoral::FindClass(env, "no/viproma/coral/model/Variable")}
        , getSlaveID_{
            jcoral::GetMethodID(
                env, variableClass_, "getSlaveID", "()Lno/viproma/coral/model/SlaveID;")}
        , getVariableID_{
            jcoral::GetMethodID(env, variableClass_, "getVariableID", "()I")}
    {
    }

    coral::model::Variable ToCpp(jobject obj) const
    {
        assert(env_->IsInstanceOf(obj, variableClass_));
        const auto jSlaveID = jcoral::CallObjectMethod(env_, obj, getSlaveID_);
        const auto jVariableID = jcoral::CallIntMethod(env_, obj, getVariableID_);
        return coral::model::Variable{
            siConv_.ToCpp(jSlaveID),
            boost::numeric_cast<coral::model::VariableID>(jVariableID)};
    }

private:
    JNIEnv* env_;
    SlaveIDConverter siConv_;
    jclass variableClass_;
    jmethodID getSlaveID_;
    jmethodID getVariableID_;
};


// Converts between coral::model::VariableSetting and no.viproma.coral.model.VariableSetting
class VariableSettingConverter
{
public:
    explicit VariableSettingConverter(JNIEnv* env)
        : env_{env}
        , scalarConv_{env}
        , varConv_{env}
        , variableSettingClass_{jcoral::FindClass(env, "no/viproma/coral/model/VariableSetting")}
        , getVariableID_{
            jcoral::GetMethodID(
                env, variableSettingClass_, "getVariableID", "()I")}
        , getValue_{
            jcoral::GetMethodID(
                env, variableSettingClass_, "getValue", "()Lno/viproma/coral/model/ScalarValue;")}
        , getConnectedOutput_{
            jcoral::GetMethodID(
                env, variableSettingClass_, "getConnectedOutput", "()Lno/viproma/coral/model/Variable;")}
    {
    }

    coral::model::VariableSetting ToCpp(jobject obj) const
    {
        assert(env_->IsInstanceOf(obj, variableSettingClass_));
        const auto jVariableID = jcoral::CallIntMethod(env_, obj, getVariableID_);
        const auto jValue = jcoral::CallObjectMethod(env_, obj, getValue_);
        const auto jConnectedOutput =
            jcoral::CallObjectMethod(env_, obj, getConnectedOutput_);

        const auto variableID =
            boost::numeric_cast<coral::model::VariableID>(jVariableID);
        if (jValue) {
            if (jConnectedOutput) {
                return coral::model::VariableSetting(
                    variableID,
                    scalarConv_.ToCpp(jValue),
                    varConv_.ToCpp(jConnectedOutput));
            } else {
                return coral::model::VariableSetting(
                    variableID,
                    scalarConv_.ToCpp(jValue));
            }
        } else {
            if (jConnectedOutput) {
                return coral::model::VariableSetting(
                    variableID,
                    varConv_.ToCpp(jConnectedOutput));
            } else {
                JCORAL_FATAL(env_, "Invalid VariableSetting object encountered");
            }
        }

    }

private:
    JNIEnv* env_;
    ScalarValueConverter scalarConv_;
    VariableConverter varConv_;
    jclass variableSettingClass_;
    jmethodID getVariableID_;
    jmethodID getValue_;
    jmethodID getConnectedOutput_;
};


// Converts between coral::net::SlaveLocator and no.viproma.coral.net.SlaveLocator
class SlaveLocatorConverter
{
public:
    explicit SlaveLocatorConverter(JNIEnv* env)
        : env_{env}
        , class_{jcoral::FindClass(env, "no/viproma/coral/net/SlaveLocator")}
        , constructor_{jcoral::GetMethodID(env, class_, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V")}
        , getControlEndpoint_{jcoral::GetMethodID(env, class_, "getControlEndpoint", "()Ljava/lang/String;")}
        , getDataPubEndpoint_{jcoral::GetMethodID(env, class_, "getDataPubEndpoint", "()Ljava/lang/String;")}
    {
    }

    coral::net::SlaveLocator ToCpp(jobject obj) const
    {
        assert(env_->IsInstanceOf(obj, class_));
        return coral::net::SlaveLocator{
            coral::net::Endpoint{jcoral::CallStringMethod(env_, obj, getControlEndpoint_)},
            coral::net::Endpoint{jcoral::CallStringMethod(env_, obj, getDataPubEndpoint_)}};
    }

    jobject ToJava(const coral::net::SlaveLocator& loc) const
    {
        return jcoral::NewObject(env_, class_, constructor_,
            jcoral::ToJString(env_, loc.ControlEndpoint().URL()),
            jcoral::ToJString(env_, loc.DataPubEndpoint().URL()));
    }

private:
    JNIEnv* env_;
    jclass class_;
    jmethodID constructor_;
    jmethodID getControlEndpoint_;
    jmethodID getDataPubEndpoint_;
};


} // namespace
#endif // header guard
