FROM ubuntu:24.04 AS build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY model1.cpp ./

COPY json/include ./json/include

RUN g++ -std=c++17 -O2 -I./json/include \
    model1.cpp \
    -o /app/model1.o \
    -lssl -lcrypto

FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    libssl3 \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/model1.o /model1.o

ENTRYPOINT ["/model1.o"]