cmake_minimum_required(VERSION 3.4.1)
add_library(pipe SHARED src/storage/cpp/pipe.cpp)
target_link_libraries(pipe) 
