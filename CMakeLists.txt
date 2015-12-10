cmake_minimum_required(VERSION 3.0.0)

project(jdsb)


find_package(Java 1.7 REQUIRED)
find_package(JNI REQUIRED)
#find_package(dsb REQUIRED)

set(javaClasses
    "com.sfh.dsb.Causality"
    "com.sfh.dsb.DataType"
    "com.sfh.dsb.DomainController"
    "com.sfh.dsb.DomainLocator"
    "com.sfh.dsb.SlaveLocator"
    "com.sfh.dsb.Variability"
    "com.sfh.dsb.VariableDescription"
)
set(nativeClasses
    "com.sfh.dsb.DomainController"
    "com.sfh.dsb.DomainLocator"
    "com.sfh.dsb.SlaveLocator"
)
set(cppSources "jdsb.cpp")
set(tests "A")


set(javaSourceDir "${CMAKE_SOURCE_DIR}/src/java")
set(cppSourceDir "${CMAKE_SOURCE_DIR}/src/cpp")
set(testSourceDir "${CMAKE_SOURCE_DIR}/src/tests")

set(javaBuildDir "${CMAKE_BINARY_DIR}/java")
set(jniHeaderDir "${CMAKE_BINARY_DIR}/jni")
set(testBuildDir "${CMAKE_BINARY_DIR}/tests")


# ==============================================================================
# Compiler/language settings
# ==============================================================================
if (CMAKE_COMPILER_IS_GNUCXX)
    # Use C++11/C++14 features (to the extent possible), treat all warnings
    # as errors.
    if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS "4.9.0")
        set (gccStd "c++0x")
    else ()
        set (gccStd "c++1y")
    endif ()
    set(CMAKE_CXX_FLAGS
        "${CMAKE_CXX_FLAGS} -std=c++1y -Wall -Werror -Wno-unused-function")
elseif (MSVC)
    # Compile with warning level 3 and treat all warnings as errors,
    # but disable the following warnings:
    #     C4996: Function call with parameters that may be unsafe [...] use
    #            Visual C++ 'Checked Iterators'
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /W3 /WX /wd4996")
endif()


# ==============================================================================
# Target: java_classes
# ==============================================================================
set(javaSources)
set(javaClassFiles)
foreach(c ${javaClasses})
    string(REPLACE "." "/" s "${c}")
    list(APPEND javaSources "${javaSourceDir}/${s}.java")
    list(APPEND javaClassFiles "${javaBuildDir}/${s}.class")
endforeach()

add_custom_command(
    OUTPUT ${javaClassFiles}
    COMMAND "cmake" "-E" "make_directory" "${javaBuildDir}"
    COMMAND "${Java_JAVAC_EXECUTABLE}" "-d" "${javaBuildDir}" ${javaSources}
    DEPENDS ${javaSources}
    VERBATIM
)
add_custom_target(java_classes DEPENDS ${javaClassFiles})


# ==============================================================================
# Target: jni_headers
# ==============================================================================
add_custom_command(
    OUTPUT "${jniHeaderDir}"
    COMMAND "cmake" "-E" "make_directory" "${jniHeaderDir}"
    VERBATIM
)
    
set(jniHeaders)
foreach(c ${nativeClasses})
    string(REPLACE "." "_" s "${c}")
    set(header "${jniHeaderDir}/${s}.h")
    list(APPEND jniHeaders "${header}")

    add_custom_command(
        OUTPUT ${header}
        COMMAND "${Java_JAVAH_EXECUTABLE}" "-d" "${jniHeaderDir}" "-classpath" "${javaBuildDir}" "${c}"
        DEPENDS java_classes "${jniHeaderDir}"
        VERBATIM
    )
endforeach()
add_custom_target(jni_headers DEPENDS ${jniHeaders})


# ==============================================================================
# Target: jdsb
# ==============================================================================
set(cppSourcePaths)
foreach(f ${cppSources})
    list(APPEND cppSourcePaths "${cppSourceDir}/${f}")
endforeach()
add_library(jdsb MODULE ${cppSourcePaths})
add_dependencies(jdsb jni_headers)
target_include_directories(jdsb PRIVATE "${jniHeaderDir}" ${JNI_INCLUDE_DIRS})
target_link_libraries(jdsb ${JAVA_JVM_LIBRARY})


# ==============================================================================
# Tests
# ==============================================================================
if (WIN32)
    set(pathSep ";")
else()
    set(pathSep ":")
endif()

enable_testing()
add_custom_command(
    OUTPUT "${testBuildDir}"
    COMMAND "cmake" "-E" "make_directory" "${testBuildDir}"
    VERBATIM
)
foreach(t ${tests})
    set(testName "test_${t}")
    set(testSource "${testSourceDir}/${t}.java")
    set(testClassFile "${testBuildDir}/${t}.class")
    add_custom_command(
        OUTPUT "${testClassFile}"
        COMMAND "${Java_JAVAC_EXECUTABLE}" "-d" "${testBuildDir}" "-classpath" "${javaBuildDir}" "${testSource}"
        MAIN_DEPENDENCY "${testSource}"
        DEPENDS "${testBuildDir}" jdsb
        VERBATIM
    )
    add_custom_target(${testName} ALL DEPENDS ${testClassFile})
    add_test(
        NAME "${testName}"
        COMMAND "${Java_JAVA_EXECUTABLE}"
            "-classpath" "${testBuildDir}${pathSep}${javaBuildDir}"
            "-Djava.library.path=$<TARGET_FILE_DIR:jdsb>"
            "${t}"
    )
endforeach()
