cmake_minimum_required(VERSION 3.0.0)
project(jdsb)
enable_testing()

# Boost settings.  find_package(dsb) below also sets up Boost, so we need to
# configure them now.
set (Boost_USE_MULTITHREADED ON)
if (WIN32)
    # It's convenient to link the Boost libs statically, but we can't do that on
    # Linux because the ones in the package repos are typically not relocatable,
    # and therefore can't be linked into a shared library.
    set (Boost_USE_STATIC_LIBS ON)
endif ()
add_definitions("-DBOOST_ALL_NO_LIB") # Disables Boost's auto-linking feature.
add_definitions("-DBOOST_SYSTEM_NO_DEPRECATED") # Disable some deprecated stuff.

option (FMILIB_USE_SHARED_LIB
    "Whether to link against the shared library version of FMI Library"
    ON)

find_package(Java 1.7 REQUIRED)
find_package(JNI REQUIRED)
find_package(dsb REQUIRED) # also finds Boost

set(jniHeaderDir "${CMAKE_BINARY_DIR}/jni")

if (CMAKE_COMPILER_IS_GNUCXX)
    # Use C++11/C++14 features (to the extent possible), treat all warnings
    # as errors.
    if (CMAKE_CXX_COMPILER_VERSION VERSION_LESS "4.9.0")
        set (gccStd "c++0x")
    else ()
        set (gccStd "c++1y")
    endif ()
    set(CMAKE_CXX_FLAGS
        "${CMAKE_CXX_FLAGS} -std=${gccStd} -Wall -Werror -Wno-unused-function"
    )
elseif (MSVC)
    # Compile with warning level 3 and treat all warnings as errors,
    # but disable the following warnings:
    #     C4996: Function call with parameters that may be unsafe [...] use
    #            Visual C++ 'Checked Iterators'
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /W3 /WX /wd4996")
endif()

add_subdirectory(cpp)
add_subdirectory(java)
add_subdirectory(tests)
