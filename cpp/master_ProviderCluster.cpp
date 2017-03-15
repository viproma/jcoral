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
            , vdConv_{env}
            , class_{
                jcoral::FindClass(env_, "no/viproma/coral/master/ProviderCluster$SlaveType")}
            , constructor_{
                jcoral::GetMethodID(env_, class_, "<init>",
                    "("
                        "Ljava/lang/String;"
                        "Ljava/lang/String;"
                        "Ljava/lang/String;"
                        "Ljava/lang/String;"
                        "Ljava/lang/String;"
                        "[Lno/viproma/coral/model/VariableDescription;"
                        "[Ljava/lang/String;"
                    ")V")}
        {
        }

        jobject ToJava(const coral::master::ProviderCluster::SlaveType& cst) const
        {
            const auto variableDescriptionRange = cst.description.Variables();
            const auto variableDescriptionVector =
                std::vector<coral::model::VariableDescription>{
                    begin(variableDescriptionRange),
                    end(variableDescriptionRange)};
            const auto variables = jcoral::ToJArray<coral::model::VariableDescription>(
                env_,
                jcoral::FindClass(env_, "no/viproma/coral/model/VariableDescription"),
                begin(variableDescriptionVector),
                end(variableDescriptionVector),
                [this] (const coral::model::VariableDescription& vd)
                {
                    return vdConv_.ToJava(vd);
                });

            auto providers = jcoral::ToJArray<std::string>(
                env_,
                jcoral::FindClass(env_, "java/lang/String"),
                begin(cst.providers),
                end(cst.providers),
                [this] (const std::string& s) { return jcoral::ToJString(env_, s); });

            return jcoral::NewObject(env_, class_, constructor_,
                jcoral::ToJString(env_, cst.description.Name()),
                jcoral::ToJString(env_, cst.description.UUID()),
                jcoral::ToJString(env_, cst.description.Description()),
                jcoral::ToJString(env_, cst.description.Author()),
                jcoral::ToJString(env_, cst.description.Version()),
                variables,
                providers);
        }

    private:
        JNIEnv* env_;
        jcoral::VariableDescriptionConverter vdConv_;
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
