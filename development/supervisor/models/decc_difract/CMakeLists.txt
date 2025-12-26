cmake_minimum_required(VERSION 3.10)
project(difract)

set(SOURCES
	src/main.c
	src/algorithm_body.c
	src/body.c
	src/functions.c
	src/output.c
	src/random.c
	src/setting.c
	src/variables.c
	cJSON/cJSON.c
)

add_executable(difract ${SOURCES})
target_link_libraries(difract m)