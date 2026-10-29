# Проверка HTTP API моделей

Нужны Linux, Bash, Docker с доступным daemon, curl и Python 3. Из корня проекта:

```bash
bash scripts/test-model-apis.sh
```

Скрипт сам находит проект, собирает оба образа, создаёт отдельные контейнеры с
свободными локальными портами и выполняет реальные smoke-тесты Python и C.
Переменные и функции заранее определять не нужно. Из другого каталога:

```bash
bash /home/onegigaflops/diploma_dev/skif_platform/development/supervisor/scripts/test-model-apis.sh
```

Успех — `PASS` и код завершения **0**. Ошибка сборки, API, smoke или очистки даёт
ненулевой код, понятное сообщение и путь к логу. Повторный запуск создаёт новую
сессию; старый образ после неуспешной сборки не используется.

Данные сохраняются в **`test-results/model-apis-<уникальный идентификатор>/`**:

- `status.txt`, `exit-code.txt` — итог скрипта;
- `python/` и `c/`: build.log, smoke.log, HTTP-ответы в `http-evidence/`;
- `http-evidence/result.json` — полный результат; матрица C сохраняется в файл;
- `runs/` — исходные рабочие файлы и полные журналы расчёта;
- inspect/logs и `runs-snapshot/` — диагностика перед очисткой контейнера.

Диагностика сохраняется и при ошибке. Удаляются только контейнеры с меткой текущей
сессии; volumes и результаты не удаляются. Если сохранить диагностику не удалось,
контейнер остаётся для разбора. Тестовые образы также остаются. Каталог
`test-results/` исключён из Git, но остаётся на диске. При Ctrl+C/SIGTERM скрипт
пытается сохранить диагностику и очистить свои контейнеры; SIGKILL этого не позволяет.

Smoke проверяет реальный результат, доступность статуса, повтор запроса без
повторного процесса, конфликты runId, неверный вход и повторное скачивание с
проверкой SHA-256. Он сам отправляет POST; ручной запуск перед ним не нужен.
Сервер после расчёта остаётся доступен до очистки скриптом. Подробные фактические
проверки и ограничения — в [VERIFICATION.md](models/http_wrapper/VERIFICATION.md).

## Ручные curl-запросы для изучения API

Это отдельный сценарий: используйте **свежий контейнер**, а не завершённый smoke.
Команды ниже выполняются из корня supervisor. Сначала соберите отдельный образ;
точка — контекст сборки, содержащий модели и общую обёртку. Не продолжайте при
ошибке сборки.

```bash
set -euo pipefail
MODEL=python                 # либо c
MODEL_DIR=model_01           # для c: model_02
mkdir -p test-results
MANUAL=$(mktemp -d "$PWD/test-results/manual-$MODEL-XXXXXXXX")
NAME=$(basename "$MANUAL")
IMAGE="skif-model-$MODEL-api:$NAME"
docker build --iidfile "$MANUAL/image.id" -f "models/$MODEL_DIR/Dockerfile.api" -t "$IMAGE" . > "$MANUAL/build.log" 2>&1
mkdir "$MANUAL/runs"
docker run -d --init --restart=no --name "$NAME" -p 127.0.0.1::8080 \
    -v "$MANUAL/runs:/runs" "$(cat "$MANUAL/image.id")"
BASE="http://$(docker port "$NAME" 8080/tcp)"
EXAMPLE="models/http_wrapper/examples/$MODEL.json"
RUN_ID=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["runId"])' "$EXAMPLE")
until curl -fsS --max-time 2 "$BASE/api/v1/readiness"; do sleep 0.2; done
curl --fail-with-body -sS -H 'Content-Type: application/json' \
    --data-binary "@$EXAMPLE" "$BASE/api/v1/runs"
```

`202` означает принятие команды. Дождитесь терминального статуса:

```bash
while true; do
    curl -fsS "$BASE/api/v1/runs/$RUN_ID" -o "$MANUAL/status.json"
    STATE=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["status"])' "$MANUAL/status.json")
    echo "$STATE"
    case "$STATE" in SUCCEEDED|FAILED|CANCELLED) break ;; esac
    sleep 0.2
done
cat "$MANUAL/status.json"
curl -fsS "$BASE/api/v1/runs/$RUN_ID/logs?after=0&limit=100" -o "$MANUAL/logs.json"
cat "$MANUAL/logs.json"
if [ "$STATE" = SUCCEEDED ]; then
    curl --fail-with-body -sS "$BASE/api/v1/runs/$RUN_ID/result/metadata" -o "$MANUAL/metadata.json"
    curl --fail-with-body -sS "$BASE/api/v1/runs/$RUN_ID/result" -o "$MANUAL/result.json"
fi
curl -fsS "$BASE/api/v1/runs/$RUN_ID/publications?after=0&limit=100" -o "$MANUAL/publications.json"
```

При FAILED смотрите `error` в статусе и логи; итоговый результат не скачивается.
Для следующей страницы логов используйте `after=nextAfter`, если `hasMore=true`.
Промежуточных публикаций у существующих моделей нет: пустой список нормален.

Во время расчёта можно запросить отмену:
`curl -X POST "$BASE/api/v1/runs/$RUN_ID/cancel"`.
CANCELLING ещё не означает остановку; дождитесь CANCELLED.

Повтор исходного POST возвращает существующий расчёт, новый runId в том же
контейнере даёт 409. Readiness после принятия возвращает 503: контейнер уже
использован, хотя статус, результат и логи доступны. Для нового расчёта создайте
новый контейнер. Восстановление после перезапуска не реализовано.

После изучения API сохраните диагностику и удалите **только свой** контейнер
(переменные NAME/MANUAL должны относиться к этому ручному запуску):

```bash
docker inspect "$NAME" > "$MANUAL/inspect.json"
docker logs "$NAME" > "$MANUAL/docker.log" 2>&1
docker cp "$NAME:/runs" "$MANUAL/runs-snapshot"
docker stop "$NAME"
docker rm "$NAME"
echo "Данные сохранены: $MANUAL"
```
