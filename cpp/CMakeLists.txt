cmake_minimum_required(VERSION 3.0.0)
add_library(jcoral MODULE
    "jni_helpers.hpp"
    "type_converters.hpp"
    "master_Execution.cpp"
    "master_ProviderCluster.cpp")
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
install(TARGETS jcoral ${targetInstallDestinations})
