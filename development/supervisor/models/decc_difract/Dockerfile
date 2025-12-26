FROM alpine:latest AS builder
RUN apk add --no-cache build-base cmake jansson-dev git
WORKDIR /app
COPY . .
RUN mkdir build && cd build && cmake && make

FROM alpine:latest
RUN apk add --no-cache jansson
WORKDIR /app
COPY --from=builder /app/build/difract .
RUN mkdir -p /input /output /json
CMD ["./difract"]