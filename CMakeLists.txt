cmake_minimum_required(VERSION 3.0.0)
project(jdsb)
enable_testing()

find_package(Java 1.7 REQUIRED)
find_package(JNI REQUIRED)

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
