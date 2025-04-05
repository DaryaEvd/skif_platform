package ru.nsu.fit.evdokimova.supervisor.utils.DockerGeneration;

public class CsharpDockerGenerator implements IDockerfileGenerator {
    @Override
    public String generateDockerfile() {
        return """
                FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
                WORKDIR /src
                COPY . .
                RUN dotnet publish "Step1.csproj" -c Release -o /app/publish
                                
                FROM mcr.microsoft.com/dotnet/runtime:8.0 AS runtime
                WORKDIR /app
                COPY --from=build /app/publish .
                                
                RUN mkdir -p /app/input && \\
                    mkdir -p /app/output && \\
                    chmod -R 777 /app/input && \\
                    chmod -R 777 /app/output
                                
                ENTRYPOINT ["dotnet", "Step1.dll"]
                """;
    }
}
