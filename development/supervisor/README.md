# Супервизор вычислительных моделей

Java-супервизор запускает эксперимент через прежний `/api/experiments/start`.
Модели работают в отдельных Docker-контейнерах с HTTP API; успешность определяется
статусом расчёта и проверенным результатом, а не завершением контейнера.
Unity на этом этапе не включена.

## Запуск

Нужны Java 21, Docker CLI с доступом к daemon, Linux и Gradle wrapper из проекта.
Из каталога supervisor выполните:

```bash
cd /home/onegigaflops/diploma_dev/skif_platform/development/supervisor
docker info >/dev/null && ./gradlew test bootJar && java -jar build/libs/supervisor-0.0.1-SNAPSHOT.jar --server.port=18090
```

Swagger UI: **http://localhost:18090/swagger-ui/index.html**.
OpenAPI JSON: http://localhost:18090/v3/api-docs.
Путь Swagger сохранён; без `--server.port` остаётся прежний порт 8080.
Сервер запускается на хосте: ему нужны Docker CLI, исходники проекта и доступ
к публикуемым на localhost портам моделей. Старый Dockerfile самого супервизора
для такого развёртывания не обновлялся; запуск Java внутри контейнера здесь не проверен.

Из другого каталога передайте абсолютный `--supervisor.paths.project-root=...`.
Данные по умолчанию сохраняются в `<project-root>/experiment-data`;
можно задать абсолютный `--supervisor.paths.experiments-root=...`.
В конфигурации есть readiness-seconds (готовность API), poll-millis (опрос) и
expected-seconds (только предупреждение о длительности, не остановка вычисления).

## Эксперимент через Swagger

В Swagger откройте операции группы `supervisor-controller`:

1. **«Запустить эксперимент Python → C»**, POST `/api/experiments/start`.
   Нажмите Try it out: готовый пример уже подставлен. Execute немедленно вернёт
   **202** с `experimentId`; сборка образов и вычисления выполняются в фоне.
2. **«Статус эксперимента и отдельных моделей»**, GET `/{id}/status`.
   Подставьте experimentId и повторяйте Execute. Поля `status`, `stage`,
   `currentModel`, `currentOrder`, `modelStatuses` показывают выполнение без чтения логов.
3. После **COMPLETED** откройте **«Метаданные сохранённых результатов»**,
   GET `/{id}/results`. Здесь нет матрицы.
4. **«Скачать полный результат модели»**, GET `/{id}/models/{order}/result`:
   order=1 — Python, order=2 — C. Большую матрицу C скачивайте в файл.
5. Для сверки входов используйте **«Фактический HTTP-запрос к модели»**,
   GET `/{id}/models/{order}/request`. В запросе C энергия совпадает с результатом Python.
6. **«Журнал этапов эксперимента»**, GET `/{id}/logs`, и **«Сохранённые логи
   вычислительной модели»**, GET `/{id}/models/{order}/logs`, доступны после удаления контейнеров.
7. **«Отменить эксперимент»**, POST `/{id}/cancel`: CANCELLING означает принятую
   команду; дождитесь CANCELLED через статус. Дальнейшие модели не запускаются.

Все сокращённые пути выше имеют префикс `/api/experiments`.
При FAILED сведения об ошибке находятся в `errorMessage` эксперимента и модели;
следующий шаг получает SKIPPED. Полученные ранее результаты остаются доступны.
Недоступный ещё результат даёт 409; неизвестный эксперимент — 404.
Дополнительно есть GET `/{id}/configuration`, GET `/{id}/models/{order}/publications`
и GET `/api/experiments/example` с готовой конфигурацией.

## Готовый JSON

Тот же файл: [src/main/resources/examples/python-c.json](src/main/resources/examples/python-c.json).
Это реальные входы. C получает `E_start` и `E_end` только из результата Python;
не добавляйте их в статические parameters C — такой конфликт отклоняется с 400,
даже если значение случайно совпало. Прочие выходы Python автоматически не сливаются.

```json
{
  "experimentName": "Python → C: real HTTP calculation",
  "models": [
    {
      "modelId": "python",
      "name": "shtorka_model",
      "order": 1,
      "version": "1.0",
      "language": "PYTHON",
      "modelPath": "model_01",
      "parameters": {
        "E_input": 130,
        "h_y_1": 0.8,
        "h_y_2": 0.8,
        "h_x_1": 0.8,
        "h_x_2": 0.1
      }
    },
    {
      "modelId": "c",
      "name": "difract_model",
      "order": 2,
      "version": "1.0",
      "language": "C",
      "modelPath": "model_02",
      "parameters": {
        "c_x": 0,
        "c_y": 3950000000,
        "c_z": 0,
        "s_x": 0,
        "s_y": 0,
        "s_z": 0,
        "omega": 0,
        "kappa": 0,
        "phi": 0,
        "xSampleSize": 600000,
        "ySampleSize": 600000,
        "zSampleSize": 600000,
        "d_x": 0,
        "d_y": 55000000,
        "d_z": 0,
        "theta": 0,
        "beta": 0,
        "gammaValue": 0,
        "sU": 5000,
        "sB": 5000,
        "sR": 5000,
        "sL": 5000,
        "t": 1
      }
    }
  ]
}
```

## Проверка через curl

Из корня проекта при запущенном Java:

```bash
curl --fail-with-body -sS -H 'Content-Type: application/json'   --data-binary @src/main/resources/examples/python-c.json   http://localhost:18090/api/experiments/start
```

Из ответа возьмите experimentId и подставьте его вместо 123:

```bash
ID=123
curl -fsS "http://localhost:18090/api/experiments/$ID/status"
```

Повторяйте запрос статуса до COMPLETED, FAILED либо CANCELLED. При COMPLETED:

```bash
curl -fsS "http://localhost:18090/api/experiments/$ID/results"
curl -fS "http://localhost:18090/api/experiments/$ID/models/1/result" -o /tmp/python-result.json
curl -fS "http://localhost:18090/api/experiments/$ID/models/2/result" -o /tmp/c-result.json
curl -fsS "http://localhost:18090/api/experiments/$ID/models/2/request"
```

`RUNNING` охватывает стадии REGISTERED, PREPARING, BUILDING, STARTING_CONTAINER,
WAITING_API, SUBMITTING, MODEL_ACCEPTED/RUNNING/FINALIZING, SAVING_RESULT.
У моделей статусы PENDING, RUNNING, CANCELLING, SUCCEEDED, FAILED, CANCELLED, SKIPPED;
эксперимент завершается только после сохранения последнего результата и очистки.
HTTP cancel вызывается при отмене активного расчёта, затем при необходимости Docker
stop/kill. CANCELLED подтверждается после остановки и удаления контейнера.

## Сохранённые данные и совместимость

Для каждого experimentId отдельно сохраняются configuration.json, status.json,
events.log и models/<order>/: parameters.json, request.json, model-status.json,
result.json, metadata.json, logs.jsonl, publications.jsonl, build.log,
container.json, docker-commands.log, cleanup.json и диагностическая копия файлов
обёртки wrapper-files/. Пустая история публикаций нормальна: модели их не генерируют.
После удаления контейнеров HTTP-операции читают сохранённые данные с диска.
Статические параметры по умолчанию сохранены, теперь выбираются по language,
а не по порядковому номеру; ручной parameters имеет приоритет, кроме явно связанных энергий.

Сохранены исходные путь запуска, поля запроса и ответа, ExperimentStatus и Swagger.
Изменения контракта: успешная регистрация теперь 202 вместо синхронного 200;
неверная конфигурация — 400, второй активный эксперимент — 409. Ошибки вычисления
после регистрации выдаются через статус, а не ответом исходного POST.
modelPath поддерживает существующие model_01 для PYTHON и model_02 для C;
version и modelId остаются метаданными конфигурации. Ранее не реализованные
JAVA/CPP/CSHARP явно отклоняются. Одномодельные Python/C-запуски также поддерживаются;
для отдельной C можно задавать статические E_start/E_end. Связь действует только
при непосредственном переходе PYTHON → C.

Один активный эксперимент допускается на экземпляр Java. Запускайте один экземпляр.
СУБД, повторов и возобновления нет. При штатном закрытии сервер запрашивает отмену.
После аварийного перезапуска незавершённая история получает FAILED/INTERRUPTED;
после SIGKILL возможны осиротевшие контейнеры — перед новым запуском проверьте их
по label `skif.supervisor.experiment`. Чужие контейнеры автоматически не удаляются.
Если очистку контейнера подтвердить не удалось, активный слот сохраняется, новый
эксперимент отклоняется: устраните проблему Docker и проверьте оставшиеся контейнеры.
Большой JSON C проверяется в памяти; журналы пока не рассчитаны на неограниченный объём.

## Автоматическая сквозная проверка

При работающем Java-сервере с чистым отдельным каталогом данных:

```bash
python3 scripts/test-supervisor-integration.py http://localhost:18090 test-results/java-http-check
```

Каталог доказательств должен быть новым. Проверяются Swagger/OpenAPI, один POST
всей цепочки, промежуточные статусы, фактическая передача энергий, ошибка реального
Python, отказ параллельного запуска и отмена реального длительного C-расчёта.
Это запуск через Java, не подмена интеграции отдельными smoke-тестами обёрток.
Скрипт рассчитан на успешную проверку; при его неожиданном прерывании посмотрите
сохранённую registration.json и отмените соответствующий эксперимент через API.

[Фактический отчёт интеграции](docs/SUPERVISOR-VERIFICATION.md).
[Самостоятельная проверка обёрток](models/http_wrapper/README.md).
[Старый README (история, не актуальная инструкция запуска)](docs/legacy-supervisor-readme.md).
