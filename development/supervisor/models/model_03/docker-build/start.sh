#!/bin/bash

echo "Запуск Unity модели в headless режиме (порт 5030)..."

# Переменные окружения для отключения графики и звуковой системы
export DISPLAY=:99
export UNITY_DISABLE_WINDOWING=1
export SDL_VIDEODRIVER=dummy
export SDL_AUDIODRIVER=dummy

# Запускаем Unity
/app/mybuild.x86_64 -batchmode -nographics &

UNITY_PID=$!

echo "Unity запущен с PID: $UNITY_PID"
echo "Ожидаю завершения процесса..."

wait $UNITY_PID

echo "Unity модель завершила работу."
