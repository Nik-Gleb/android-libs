cmake_minimum_required(VERSION 3.6.0)
add_library(pipe SHARED src/storage/cpp/pipe.cpp)
target_link_libraries(pipe) 
