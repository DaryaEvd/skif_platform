FROM ubuntu:24.04 AS build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY model2.cpp ./

COPY json/include ./json/include

RUN g++ -std=c++17 -O2 -I./json/include \
    model2.cpp \
    -o /app/model2.o \
    -lssl -lcrypto

FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    libssl3 \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/model2.o /model2.o

ENTRYPOINT ["/model2.o"]