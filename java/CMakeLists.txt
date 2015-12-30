cmake_minimum_required(VERSION 3.0.0)

# Java classes with native code
set(nativeClasses
    "com.sfh.dsb.DomainController"
    "com.sfh.dsb.DomainLocator"
    "com.sfh.dsb.ExecutionController"
    "com.sfh.dsb.ExecutionLocator"
    "com.sfh.dsb.SlaveLocator"
)

# Java classes without native code
set(normalClasses
    "com.sfh.dsb.Causality"
    "com.sfh.dsb.DataType"
    "com.sfh.dsb.Variability"
    "com.sfh.dsb.VariableDescription"
)

# ==============================================================================
# Target: jar
# ==============================================================================

set(javaFiles)
set(classFiles)
foreach(class ${nativeClasses} ${normalClasses})
    string(REPLACE "." "/" s "${class}")
    list(APPEND javaFiles "${CMAKE_CURRENT_SOURCE_DIR}/${s}.java")
    list(APPEND classFiles "classes/${s}.class")
endforeach()
add_custom_command(
    OUTPUT ${classFiles}
    COMMAND "${CMAKE_COMMAND}" "-E" "make_directory" "classes"
    COMMAND "${Java_JAVAC_EXECUTABLE}" "-d" "classes" ${javaFiles}
    DEPENDS ${javaFiles}
    VERBATIM
)

set(jarFilename "jdsb.jar")
add_custom_command(
    OUTPUT "${jarFilename}"
    COMMAND "${Java_JAR_EXECUTABLE}" "cf" "${CMAKE_CURRENT_BINARY_DIR}/${jarFilename}" "*"
    WORKING_DIRECTORY "classes"
    DEPENDS ${classFiles}
)

add_custom_target(jar DEPENDS "${jarFilename}")
set_target_properties(jar PROPERTIES
    ARCHIVE_OUTPUT_NAME "${jarFilename}"
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}"
)

# ==============================================================================
# Target: headers
# ==============================================================================

# jniHeaderDir is defined in root CMakeLists.txt
file(MAKE_DIRECTORY "${jniHeaderDir}")

set(headerFiles)
foreach(class ${nativeClasses})
    string(REPLACE "." "_" s "${class}")
    set(h "${jniHeaderDir}/${s}.h")
    list(APPEND headerFiles "${h}")
    # Note: We need -force in the following command because javah only
    # regenerates headers for classes which have been updated, while we
    # have specified that it should depend on the entire JAR file.
    # If we don't use -force, some headers will always be older than the
    # JAR file, and their command will consequently always be run.
    add_custom_command(
        OUTPUT ${h}
        COMMAND "${Java_JAVAH_EXECUTABLE}" "-force" "-d" "${jniHeaderDir}"
            "-classpath" "${jarFilename}" "${class}"
        DEPENDS ${jarFilename}
        VERBATIM
    )
endforeach()
add_custom_target(headers DEPENDS ${headerFiles})
