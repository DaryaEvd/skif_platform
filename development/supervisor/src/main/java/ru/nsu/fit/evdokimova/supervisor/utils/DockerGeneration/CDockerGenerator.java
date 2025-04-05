package ru.nsu.fit.evdokimova.supervisor.utils.DockerGeneration;

import org.springframework.stereotype.Component;

@Component
public class CDockerGenerator implements IDockerfileGenerator {
    @Override
    public String generateDockerfile() {
        return """
                FROM alpine:latest as builder
                 RUN apk add --no-cache \\
                     build-base \\
                     cmake \\
                     jansson-dev \\
                     git
                 
                 WORKDIR /app
                 COPY . .
                 
                 RUN mkdir build && cd build && \\
                     cmake .. && \\
                     make
                 
                 FROM alpine:latest
                 
                 RUN apk add --no-cache jansson
                 
                 WORKDIR /app
                 COPY --from=builder /app/build/modelka  .                                  
             
                 RUN mkdir -p /app/input && \\
                     mkdir -p /app/output && \\
                     chmod -R 777 /app/input && \\
                     chmod -R 777 /app/output
                 
                 CMD ["./modelka"]
                """;
    }
}
