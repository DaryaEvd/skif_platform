package ru.nsu.fit.evdokimova.supervisor.service;

import org.springframework.stereotype.Service;
import ru.nsu.fit.evdokimova.supervisor.utils.DockerGeneration.IDockerfileGenerator;

@Service
public class CppDockerGenerator implements IDockerfileGenerator {
    @Override
    public String generateDockerfile() {
        return """
                FROM gcc:13-alpine
                RUN apk add --no-cache jansson-dev
                WORKDIR /app
                COPY . .
                RUN find . -name "*.c" -type f | head -n1 | xargs -I {} gcc -O2 -o model {} -ljansson
                RUN mkdir -p /input /output
                CMD ["./model"]
                """;
    }
}