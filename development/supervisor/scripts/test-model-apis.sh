#!/usr/bin/env bash
# Real Docker smoke tests; no model or supervisor configuration is changed.
set -Eeuo pipefail
PROJECT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
cd "$PROJECT_DIR"
mkdir -p "$PROJECT_DIR/test-results"
RESULTS=$(mktemp -d "$PROJECT_DIR/test-results/model-apis-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXXXX")
SESSION_ID=$(basename "$RESULTS")
CONTAINERS=()
PHASE=preflight
CURRENT_LOG="$RESULTS/preflight.log"
printf 'Результаты и диагностика: %s\n' "$RESULTS"

on_error() {
    local code=$1 line=$2
    printf '\nОшибка: этап «%s», строка %s, код %s.\nЛог: %s\n' \
        "$PHASE" "$line" "$code" "$CURRENT_LOG" >&2
    if [ -f "$CURRENT_LOG" ]; then tail -n 30 "$CURRENT_LOG" >&2; fi
    printf 'Все сохранённые данные: %s\n' "$RESULTS" >&2
}

cleanup() {
    local code=$? name model folder owner saved
    trap - EXIT ERR INT TERM
    set +e
    for name in "${CONTAINERS[@]}"; do
        model=${name##*-}
        folder="$RESULTS/$model"
        # Never act on a container unless its session label matches exactly.
        owner=$(docker inspect --format '{{index .Config.Labels "skif.model-api-test"}}' "$name" 2>> "$folder/cleanup.log")
        if [ "$?" -ne 0 ] || [ "$owner" != "$SESSION_ID" ]; then
            echo "Не удалось подтвердить принадлежность $name; контейнер не изменён" >&2
            [ "$code" -ne 0 ] || code=1
            continue
        fi
        saved=1
        docker inspect "$name" > "$folder/inspect-before-cleanup.json" 2>> "$folder/cleanup.log" || saved=0
        docker logs "$name" > "$folder/docker-before-cleanup.log" 2>&1 || saved=0
        docker cp "$name:/runs" "$folder/runs-snapshot" >> "$folder/cleanup.log" 2>&1 || saved=0
        if [ "$saved" -ne 1 ]; then
            echo "Диагностика $name сохранена не полностью; контейнер оставлен. См. $folder/cleanup.log" >&2
            [ "$code" -ne 0 ] || code=1
            continue
        fi
        if docker stop "$name" >> "$folder/cleanup.log" 2>&1 && \
           docker inspect "$name" > "$folder/inspect-stopped.json" 2>> "$folder/cleanup.log" && \
           docker logs "$name" > "$folder/docker-stopped.log" 2>&1 && \
           docker rm "$name" >> "$folder/cleanup.log" 2>&1; then
            printf 'Удалён свой тестовый контейнер: %s\n' "$name"
        else
            echo "Не завершена очистка $name; см. $folder/cleanup.log" >&2
            [ "$code" -ne 0 ] || code=1
        fi
    done
    printf '%s\n' "$code" > "$RESULTS/exit-code.txt"
    if [ "$code" -eq 0 ]; then
        printf 'PASS\n' > "$RESULTS/status.txt"
        printf '\nPASS: обе модели прошли smoke-тесты.\n'
    else
        printf 'FAIL\n' > "$RESULTS/status.txt"
        printf '\nFAIL: код %s; этап «%s». См. %s\n' "$code" "$PHASE" "$CURRENT_LOG" >&2
    fi
    printf 'Результаты, журналы и диагностика: %s\n' "$RESULTS"
    exit "$code"
}
trap 'on_error "$?" "$LINENO"' ERR
trap cleanup EXIT
trap 'echo "Прервано пользователем; сохраняю диагностику" >&2; exit 130' INT
trap 'echo "Получен SIGTERM; сохраняю диагностику" >&2; exit 143' TERM

for dependency in docker curl python3; do
    if ! command -v "$dependency" >> "$CURRENT_LOG" 2>&1; then
        echo "Не найдена обязательная команда: $dependency" >> "$CURRENT_LOG"
        on_error 1 "$LINENO"
        exit 1
    fi
done
docker info >> "$CURRENT_LOG" 2>&1
docker ps -a > "$RESULTS/docker-before.txt"
docker volume ls > "$RESULTS/volumes-before.txt"
printf 'project\t%s\nsession\t%s\n' "$PROJECT_DIR" "$SESSION_ID" > "$RESULTS/session.tsv"

# Build both images before starting anything. A failed build cannot use an old tag.
for model in python c; do
    folder="$RESULTS/$model"
    mkdir -p "$folder/runs"
    case "$model" in python) source_model=model_01 ;; c) source_model=model_02 ;; esac
    PHASE="сборка $model"
    CURRENT_LOG="$folder/build.log"
    printf 'Сборка %s…\n' "$model"
    docker build --iidfile "$folder/image.id" -f "models/$source_model/Dockerfile.api" \
        -t "skif-model-$model-api:$SESSION_ID" "$PROJECT_DIR" > "$CURRENT_LOG" 2>&1
    docker image inspect "$(cat "$folder/image.id")" > "$folder/image-inspect.json"
done

for model in python c; do
    folder="$RESULTS/$model"
    name="$SESSION_ID-$model"
    PHASE="запуск $model"
    CURRENT_LOG="$folder/start.log"
    # Register before run, so even an unsuccessful Docker create is diagnosed.
    CONTAINERS+=("$name")
    docker run -d --init --restart=no --name "$name" \
        --label "skif.model-api-test=$SESSION_ID" -p 127.0.0.1::8080 \
        -v "$folder/runs:/runs" "$(cat "$folder/image.id")" > "$CURRENT_LOG" 2>&1
    base="http://$(docker port "$name" 8080/tcp)"
    printf '%s\n' "$name" > "$folder/container-name.txt"
    printf '%s\n' "$base" > "$folder/base-url.txt"
    PHASE="готовность API $model"
    CURRENT_LOG="$folder/readiness.stderr"
    ready=0
    for ((attempt=0; attempt<100; attempt++)); do
        if curl --fail --silent --show-error --connect-timeout 1 --max-time 2 \
            "$base/api/v1/readiness" -o "$folder/readiness.json" 2> "$CURRENT_LOG"; then
            ready=1
            break
        fi
        if [ "$(docker inspect --format '{{.State.Running}}' "$name")" != true ]; then
            echo 'Контейнер завершился до готовности API' >> "$CURRENT_LOG"
            break
        fi
        sleep 0.1
    done
    if [ "$ready" -ne 1 ]; then
        echo 'API не стало готовым за отведённое время наблюдения' >> "$CURRENT_LOG"
        on_error 1 "$LINENO"
        exit 1
    fi
    PHASE="smoke-тест $model"
    CURRENT_LOG="$folder/smoke.log"
    printf 'Smoke %s: %s\n' "$model" "$base"
    python3 models/http_wrapper/tests/smoke.py "$base" \
        "models/http_wrapper/examples/$model.json" "$folder/http-evidence" > "$CURRENT_LOG" 2>&1
    printf '%s: smoke пройден\n' "$model"
done
PHASE=завершение
