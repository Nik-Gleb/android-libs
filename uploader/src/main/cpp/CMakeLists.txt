cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_INCLUDE_CURRENT_DIR ON)

#set(SOURCES
#  "lovehash.cpp"
#  "lovehash-lib.cpp")


#add_library(lovehash-lib SHARED ${SOURCES})
add_library(lovehash-lib SHARED "lovehash.cpp"
                                "lovehash-lib.cpp")

set_target_properties(lovehash-lib PROPERTIES LINKER_LANGUAGE CXX)