cmake_minimum_required(VERSION 3.0.0)

set(tests
    "ExecutionTest"
    "InProcessExecutionTest"
    "ListSlaveTypeInfo"
    "ModelBuilderTest"
    "NetworkedExecutionTest"
    "ProcessCaptureTest"
    "SineScenarioTest"
)

if (WIN32)
    set(pathSep ";")
else()
    set(pathSep ":")
endif()

set(genJarPath "$<TARGET_PROPERTY:jar,ARCHIVE_OUTPUT_DIRECTORY>/$<TARGET_PROPERTY:jar,ARCHIVE_OUTPUT_NAME>")
set(testSources)
foreach(className ${tests})
    set(testName "test_${className}")
    set(javaFile  "${CMAKE_CURRENT_SOURCE_DIR}/${className}.java")
    set(classFile "${className}.class")
    add_custom_command(
        OUTPUT "${classFile}"
        COMMAND "${Java_JAVAC_EXECUTABLE}" "-d" "." "-classpath" "${genJarPath}" "-Xlint:all,-try" "${javaFile}"
        MAIN_DEPENDENCY "${javaFile}"
        DEPENDS jcoral
        VERBATIM
    )
    add_custom_target(${testName} ALL DEPENDS ${classFile})
    add_test(
        NAME "${testName}"
        COMMAND "${Java_JAVA_EXECUTABLE}"
            "-classpath" "${CMAKE_CURRENT_BINARY_DIR}${pathSep}${genJarPath}"
            "-Djava.library.path=$<TARGET_FILE_DIR:jcoral>"
            "-enableassertions"
            "${className}"
    )
    set_property(TEST "${testName}" PROPERTY ENVIRONMENT
        "JCORAL_TEST_DATA_DIR=${CMAKE_CURRENT_SOURCE_DIR}/data"
        "JCORAL_TEST_OUTPUT_DIR=${CMAKE_CURRENT_BINARY_DIR}/test-output"
    )
    list(APPEND testSources "${javaFile}")
endforeach()

install(FILES ${testSources} DESTINATION "${exampleInstallDir}")
