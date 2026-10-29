FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Добавлены X11 библиотеки (критично для Unity 6 даже в headless режиме)
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libxcursor1 \
    libxinerama1 \
    libxrandr2 \
    libxi6 \
    libxxf86vm1 \
    libxkbcommon0 \
    libglib2.0-0 \
    curl \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxext6 \
    libxfixes3 \
    libxrender1 \
    libxtst6 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY mybuild/ /app/

RUN chmod +x /app/mybuild.x86_64

COPY start.sh /start.sh
RUN chmod +x /start.sh

EXPOSE 5030

CMD ["/start.sh"]
