/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <boost/numeric/conversion/cast.hpp>
#include <boost/variant.hpp>

#include <coral/master/execution.hpp>

#include "jni_helpers.hpp"
#include "type_converters.hpp"
#include "no_viproma_coral_master_Execution.h"


// Helpers for createNative
namespace
{
    coral::master::ExecutionOptions ToExecutionOptions(
        JNIEnv* env, jobject jOptions)
    {
        const auto clazz = jcoral::GetObjectClass(env, jOptions);
        coral::master::ExecutionOptions options;
        options.startTime = jcoral::GetDoubleField(env, jOptions,
            jcoral::GetFieldID(env, clazz, "startTime_", "D"));
        options.maxTime = jcoral::GetDoubleField(env, jOptions,
            jcoral::GetFieldID(env, clazz, "maxTime_", "D"));
        options.slaveVariableRecvTimeout = std::chrono::milliseconds(
            jcoral::GetIntField(env, jOptions,
                jcoral::GetFieldID(env, clazz, "slaveVariableRecvTimeout_ms_", "I")));
        return options;
    }
}


JNIEXPORT jlong JNICALL Java_no_viproma_coral_master_Execution_createNative(
    JNIEnv* env,
    jclass,
    jstring executionName,
    jobject options)
{
    try {
        return reinterpret_cast<jlong>(new coral::master::Execution(
            jcoral::ToString(env, executionName),
            ToExecutionOptions(env, options)));
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_master_Execution_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    try {
        jcoral::EnforceNotNull(selfPtr);
        const auto exe = reinterpret_cast<coral::master::Execution*>(selfPtr);
        exe->Terminate();
        delete exe;
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
    }
}


// Helpers for addSlaves()
namespace
{
    class AddedSlaveConverter
    {
    public:
        explicit AddedSlaveConverter(JNIEnv* env)
            : env_{env}
            , slConv_{env}
            , siConv_{env}
            , addedSlaveClass_{jcoral::FindClass(env, "no/viproma/coral/master/AddedSlave")}
            , getLocator_{jcoral::GetMethodID(env, addedSlaveClass_, "getLocator", "()Lno/viproma/coral/net/SlaveLocator;")}
            , getName_{jcoral::GetMethodID(env, addedSlaveClass_, "getName", "()Ljava/lang/String;")}
            , setID_{jcoral::GetMethodID(env, addedSlaveClass_, "setID", "(Lno/viproma/coral/model/SlaveID;)V")}
            , setError_{jcoral::GetMethodID(env, addedSlaveClass_, "setError", "(Ljava/lang/String;)V")}
        {
        }

        coral::master::AddedSlave ToCppInput(jobject obj) const
        {
            assert(env_->IsInstanceOf(obj, addedSlaveClass_));
            const auto jLocator = jcoral::CallObjectMethod(env_, obj, getLocator_);
            const auto name = jcoral::CallStringMethod(env_, obj, getName_);
            return coral::master::AddedSlave{slConv_.ToCpp(jLocator), name};
        }

        void CopyToJavaOutput(const coral::master::AddedSlave& src, jobject tgt)
            const
        {
            assert(env_->IsInstanceOf(tgt, addedSlaveClass_));
            if (src.info.ID() != coral::model::INVALID_SLAVE_ID) {
                jcoral::CallVoidMethod(env_, tgt, setID_, siConv_.ToJava(src.info.ID()));
            } else {
                jcoral::CallVoidMethod(env_, tgt, setID_, nullptr);
            }
            if (src.error) {
                jcoral::CallVoidMethod(
                    env_, tgt, setError_, jcoral::ToJString(env_, src.error.message()));
            } else {
                jcoral::CallVoidMethod(env_, tgt, setError_, nullptr);
            }
        }

    private:
        JNIEnv* env_;
        jcoral::SlaveLocatorConverter slConv_;
        jcoral::SlaveIDConverter siConv_;
        jclass addedSlaveClass_;
        jmethodID getLocator_;
        jmethodID getName_;
        jmethodID setID_;
        jmethodID setError_;
    };
}


JNIEXPORT void JNICALL Java_no_viproma_coral_master_Execution_addSlavesNative(
    JNIEnv* env, jclass,
    jlong selfPtr, jobject slavesToAdd, jint commTimeout_ms)
{
    try {
        jcoral::EnforceNotNull(selfPtr);
        const auto exe = reinterpret_cast<coral::master::Execution*>(selfPtr);

        const auto asConv = AddedSlaveConverter{env};
        auto addedSlaves = std::vector<coral::master::AddedSlave>{};
        jcoral::ForEach(env, slavesToAdd, [&asConv, &addedSlaves] (jobject as) {
            addedSlaves.push_back(asConv.ToCppInput(as));
        });

        auto transferResults = [env, slavesToAdd, &asConv, &addedSlaves] () {
            auto it = addedSlaves.begin();
            jcoral::ForEach(env, slavesToAdd, [env, &asConv, &it] (jobject jas) {
                asConv.CopyToJavaOutput(*it, jas);
                ++it;
            });
            assert(it == addedSlaves.end());
        };

        try {
            exe->Reconstitute(addedSlaves, std::chrono::milliseconds(commTimeout_ms));
            transferResults();
        } catch (...) {
            // TODO: This is strictly speaking not OK, because transferResults()
            // may throw. Need to consider different ways of doing this...
            transferResults();
            throw;
        }
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
    }
}


namespace
{
    class SlaveConfigConverter
    {
    public:
        explicit SlaveConfigConverter(JNIEnv* env)
            : env_{env}
            , siConv_{env}
            , vsConv_{env}
            , slaveConfigClass_{
                jcoral::FindClass(env, "no/viproma/coral/master/SlaveConfig")}
            , getSlaveID_{
                jcoral::GetMethodID(
                    env,
                    slaveConfigClass_,
                    "getSlaveID",
                    "()Lno/viproma/coral/model/SlaveID;")}
            , getVariableSettings_{
                jcoral::GetMethodID(
                    env,
                    slaveConfigClass_,
                    "getVariableSettings",
                    "()Ljava/lang/Iterable;")}
            , setError_{
                jcoral::GetMethodID(
                    env,
                    slaveConfigClass_,
                    "setError",
                    "(Ljava/lang/String;)V")}
        {
        }

        coral::master::SlaveConfig ToCppInput(jobject obj) const
        {
            assert(env_->IsInstanceOf(obj, slaveConfigClass_));
            const auto jSlaveID = jcoral::CallObjectMethod(env_, obj, getSlaveID_);
            const auto jVariableSettings = 
                jcoral::CallObjectMethod(env_, obj, getVariableSettings_);

            coral::master::SlaveConfig slaveConfig;
            slaveConfig.slaveID = siConv_.ToCpp(jSlaveID);
            jcoral::ForEach(env_, jVariableSettings, [this, &slaveConfig] (jobject jvs)
            {
                slaveConfig.variableSettings.push_back(vsConv_.ToCpp(jvs));
            });
            return slaveConfig;
        }

        void CopyToJavaOutput(const coral::master::SlaveConfig& src, jobject tgt)
            const
        {
            assert(env_->IsInstanceOf(tgt, slaveConfigClass_));
            if (src.error) {
                jcoral::CallVoidMethod(
                    env_,
                    tgt,
                    setError_,
                    jcoral::ToJString(env_, src.error.message()));
            } else {
                jcoral::CallVoidMethod(env_, tgt, setError_, nullptr);
            }
        }

    private:
        JNIEnv* env_;
        jcoral::SlaveIDConverter siConv_;
        jcoral::VariableSettingConverter vsConv_;
        jclass slaveConfigClass_;
        jmethodID getSlaveID_;
        jmethodID getVariableSettings_;
        jmethodID setError_;
    };
}


JNIEXPORT void JNICALL Java_no_viproma_coral_master_Execution_reconfigureNative(
    JNIEnv* env, jclass,
    jlong selfPtr,
    jobject jSlaveConfigs,
    jint commTimeout_ms)
{
    try {
        jcoral::EnforceNotNull(selfPtr);
        const auto exe = reinterpret_cast<coral::master::Execution*>(selfPtr);

        const auto scConv = SlaveConfigConverter{env};
        auto slaveConfigs = std::vector<coral::master::SlaveConfig>{};
        jcoral::ForEach(env, jSlaveConfigs, [&scConv, &slaveConfigs] (jobject sc) {
            slaveConfigs.push_back(scConv.ToCppInput(sc));
        });

        auto transferResults = [env, jSlaveConfigs, &scConv, &slaveConfigs] () {
            auto it = slaveConfigs.begin();
            jcoral::ForEach(env, jSlaveConfigs, [env, &scConv, &it] (jobject jsc) {
                scConv.CopyToJavaOutput(*it, jsc);
                ++it;
            });
            assert(it == slaveConfigs.end());
        };

        try {
            exe->Reconfigure(
                slaveConfigs, std::chrono::milliseconds(commTimeout_ms));
            transferResults();
        } catch (...) {
            // TODO: This is strictly speaking not OK, because transferResults()
            // may throw. Need to consider different ways of doing this...
            transferResults();
            throw;
        }
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
    }
}


JNIEXPORT jboolean JNICALL Java_no_viproma_coral_master_Execution_stepNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jdouble stepSize,
    jint timeout_ms)
{
    try {
        jcoral::EnforceNotNull(selfPtr);
        const auto exe = reinterpret_cast<coral::master::Execution*>(selfPtr);

        const auto result = exe->Step(
            boost::numeric_cast<coral::model::TimeDuration>(stepSize),
            std::chrono::milliseconds(timeout_ms));
        return result == coral::master::StepResult::completed;
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return false;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_master_Execution_acceptStepNative(
    JNIEnv* env, jclass, jlong selfPtr, jint timeout_ms)
{
    try {
        jcoral::EnforceNotNull(selfPtr);
        const auto exe = reinterpret_cast<coral::master::Execution*>(selfPtr);
        exe->AcceptStep(std::chrono::milliseconds(timeout_ms));
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
    }
}
