cmake_minimum_required(VERSION 3.22.1)
project("shoppinglistapp")
add_library(
        native-lib
        SHARED
        native-lib.cpp sqlite3.c)
# Устанавливаем стандарт C++17
set_target_properties(native-lib PROPERTIES
        VERSION 1.0
        SOVERSION 1
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
)
# Добавляем флаги выравнивания памяти для всех архитектур
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")
find_library(
        log-lib
        log)
target_link_libraries(
        native-lib
        ${log-lib})