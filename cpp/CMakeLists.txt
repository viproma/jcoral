cmake_minimum_required(VERSION 3.0.0)
add_library(jcoral MODULE
    "common_types.hpp"
    "jni_helpers.hpp"
    "type_converters.hpp"
    "fmi_FMU.cpp"
    "fmi_Importer.cpp"
    "master_Execution.cpp"
    "master_ProviderCluster.cpp"
    "slave_InProcessRunner.cpp"
    "slave_InstanceFactory.cpp"
    "slave_OpaqueInstance.cpp"
)
add_dependencies(jcoral headers)
target_include_directories(jcoral PRIVATE
    "${jniHeaderDir}"
    ${JNI_INCLUDE_DIRS}
    ${Boost_INCLUDE_DIRS}
)
target_link_libraries(jcoral
    ${JAVA_JVM_LIBRARY}
    coral
    ${Boost_LIBRARIES}
)
if(WIN32)
    target_compile_definitions(jcoral PRIVATE "NOMINMAX")
endif()

install(TARGETS jcoral ${targetInstallDestinations})
