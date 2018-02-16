/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
#include <string>
#include <vector>

#include <boost/numeric/conversion/cast.hpp>
#include <coral/master/cluster.hpp>

#include "jni_helpers.hpp"
#include "type_converters.hpp"
#include "no_viproma_coral_master_ProviderCluster.h"


JNIEXPORT jlong JNICALL Java_no_viproma_coral_master_ProviderCluster_createNative(
    JNIEnv* env,
    jclass,
    jstring networkInterface,
    jint discoveryPort)
{
    try {
        return reinterpret_cast<jlong>(new coral::master::ProviderCluster{
            jcoral::ToString(env, networkInterface),
            boost::numeric_cast<std::uint16_t>(discoveryPort)});
    } catch(...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}


JNIEXPORT void JNICALL Java_no_viproma_coral_master_ProviderCluster_destroyNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr)
{
    delete reinterpret_cast<coral::master::ProviderCluster*>(selfPtr);
}


namespace
{
    // Converts between coral::master::ProviderCluster::SlaveType
    // and no.viproma.coral.master.ProviderCluster.SlaveType.
    class SlaveTypeConverter
    {
    public:
        SlaveTypeConverter(JNIEnv* env)
            : env_{env}
            , stdConv_{env}
            , class_{
                jcoral::FindClass(env_, "no/viproma/coral/master/ProviderCluster$SlaveType")}
            , constructor_{
                jcoral::GetMethodID(env_, class_, "<init>",
                    "("
                        "Lno/viproma/coral/model/SlaveTypeDescription;"
                        "[Ljava/lang/String;"
                    ")V")}
        {
        }

        jobject ToJava(const coral::master::ProviderCluster::SlaveType& cst) const
        {
            auto providers = jcoral::ToJArray<std::string>(
                env_,
                jcoral::FindClass(env_, "java/lang/String"),
                begin(cst.providers),
                end(cst.providers),
                [this] (const std::string& s) { return jcoral::ToJString(env_, s); });

            return jcoral::NewObject(env_, class_, constructor_,
                stdConv_.ToJava(cst.description),
                providers);
        }

    private:
        JNIEnv* env_;
        jcoral::SlaveTypeDescriptionConverter stdConv_;
        jclass class_;
        jmethodID constructor_;
    };
}


JNIEXPORT jobjectArray JNICALL Java_no_viproma_coral_master_ProviderCluster_getSlaveTypesNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jint timeout_ms)
{
    try {
        jcoral::EnforceNotNull(selfPtr);
        const auto cluster =
            reinterpret_cast<coral::master::ProviderCluster*>(selfPtr);
        const auto slaveTypes =
            cluster->GetSlaveTypes(std::chrono::milliseconds(timeout_ms));

        const auto stConv = SlaveTypeConverter{env};
        return jcoral::ToJArray<coral::master::ProviderCluster::SlaveType>(
            env,
            jcoral::FindClass(env, "no/viproma/coral/master/ProviderCluster$SlaveType"),
            begin(slaveTypes),
            end(slaveTypes),
            [&stConv] (const coral::master::ProviderCluster::SlaveType& st) {
                return stConv.ToJava(st);
            });
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return nullptr;
    }
}


JNIEXPORT jobject JNICALL Java_no_viproma_coral_master_ProviderCluster_instantiateSlaveNative(
    JNIEnv* env,
    jclass,
    jlong selfPtr,
    jstring slaveProviderID,
    jstring slaveTypeUUID,
    jint timeout_ms)
{
    try {
        jcoral::EnforceNotNull(selfPtr);
        const auto cluster =
            reinterpret_cast<coral::master::ProviderCluster*>(selfPtr);
        const auto loc = cluster->InstantiateSlave(
            jcoral::ToString(env, slaveProviderID),
            jcoral::ToString(env, slaveTypeUUID),
            std::chrono::milliseconds(timeout_ms));
        return jcoral::SlaveLocatorConverter{env}.ToJava(loc);
    } catch (...) {
        jcoral::RethrowAsJavaException(env);
        return 0;
    }
}
