# Интеграция Java-супервизора с HTTP-моделями — 2026-10-29

## Сохранённые API и Swagger

Проверены все src/main контроллеры, DTO, сервисы, application.properties,
build.gradle и src/test. В текущем checkout существовал
`SupervisorController` с POST `/api/experiments/start`, `ExperimentResponse`,
`RequestExperimentFromClient`, `ModelRequest`, `ExperimentStatus`, Spring Boot Web
и springdoc 2.7.0. Путь Swagger UI `/swagger-ui/index.html` сохранён, JSON OpenAPI
`/v3/api-docs` сохранён. Существующий маршрут запуска не заменялся параллельным API.

Поиск `@*Mapping`, ссылок на ExperimentStatus и тестов в этой версии checkout
не обнаружил дополнительных маршрутов чтения статуса или обновления этого DTO
во время выполнения. Поэтому существующий DTO подключён к ExperimentService,
а недостающие GET/cancel добавлены в тот же контроллер и пространство
`/api/experiments`. Этот вывод относится к проверенным исходникам, а не утверждает,
что статусного API не было в других версиях проекта.

Сохранены поля запроса experimentId/experimentName/models, полей моделей и ответа
experimentId/message/modelsCount/startTime. Идентификатор назначает сервер.
Существующие Java/Python/C DTO enum-значения не удалены; неподдержанные исполнители
явно отклоняются до регистрации. Поддержаны также отдельные Python/C-эксперименты.

## Изменение контракта

- POST /api/experiments/start теперь отвечает 202 после регистрации, а не
  синхронным 200 после всей цепочки. Формат успешного ответа сохранён.
- Неверная конфигурация, включая статические энергии при связи Python → C: 400.
- Второй активный эксперимент: 409. Неизвестный experimentId: 404.
- Ошибки расчёта после регистрации отражаются в ExperimentStatus/errorMessage,
  а не в ответе первоначального POST. Ошибки регистрации возвращают JSON error.
- Параметры по умолчанию выбираются по language, не по order. При связи Python → C
  передаются только E_start и E_end; остальные поля результата не сливаются.
  Явные E_start/E_end в parameters C конфликтуют даже при совпадении значения.
- modelPath ограничен существующими model_01/PYTHON и model_02/C: Dockerfile.api
  выбирается из проекта, а не генерируется по произвольному языку.
- ExperimentStatus расширен stage/errorMessage, ModelStatus — modelId/runId/stage/
  resultAvailable. Прежние поля currentModel/currentOrder/totalModels/status/
  lastUpdated/modelStatuses и ошибки отдельных моделей сохранены.

Добавленные операции в существующем namespace:

| Метод и суффикс после /api/experiments | Назначение |
|---|---|
| GET /example | Готовая конфигурация, та же, что в Swagger Try it out |
| GET /{id}/status | Живой статус эксперимента и каждой модели, без матрицы |
| POST /{id}/cancel | Асинхронная отмена; 202 CANCELLING либо 200 для терминального |
| GET /{id}/configuration | Сохранённая конфигурация |
| GET /{id}/results | Краткие метаданные и стабильные ссылки в супервизор |
| GET /{id}/models/{order}/result | Полный сохранённый результат, поток файла |
| GET /{id}/models/{order}/request | Фактический HTTP-запрос к модели |
| GET /{id}/logs | Журнал этапов с пагинацией |
| GET /{id}/models/{order}/logs | Сохранённый NDJSON журнала модели |
| GET /{id}/models/{order}/publications | Сохранённый NDJSON промежуточных публикаций |

Все операции отражены в Swagger с русскими названиями. В OpenAPI запуска указан
фактический HTTP 202 и готовый пример JSON. Ссылки metadata.downloadUrl/resultUrl
ведут в супервизор и не требуют существования контейнера.

## Реализация

ExperimentService регистрирует неизменяемую копию конфигурации и запускает один
фоновый worker. Синхронизация регистрации/статусов/отмены не допускает второй
активный эксперимент. Существующие ModelExecutorRegistry, PythonModelExecutor,
CModelExecutor и AbstractDockerModelExecutor теперь выполняют HTTP-жизненный цикл.
Внутренний Java-интерфейс ModelExecutor изменён с Path/file на JsonNode/context;
это внутренний рефакторинг, не новый внешний API.

Исполнитель использует Docker CLI для сборки существующих Dockerfile.api с корнем
supervisor в качестве build context. Образ запускается по ID успешной сборки,
с --restart=no и --init, на свободном localhost-порту. DockerClient-конфигурация
оставлена, но новый запуск CLI не использует её тайм-ауты: CLI наследует Docker
настройки окружения. Это требует Docker CLI на хосте Java.

Вход передаётся только HTTP POST. Статус, результат, метаданные, логи и публикации
получаются через /api/v1 моделей. Проверяются идентификатор, SHA-256, размер,
обязательные конечные числовые поля и реальная форма C-матрицы 988×988, error_rate.
Сохранение JSON атомарное. Следующая модель запускается только после сохранения
результата и очистки предыдущей среды. Завершение контейнера не считается успехом.
Docker cp /runs используется только как диагностическая копия, не как источник
входа или успешного результата для цепочки.

Отмена фиксируется до допуска следующей модели. Активному API посылается cancel;
после ожидания при необходимости выполняется Docker stop/kill. CANCELLED выставляется
после подтверждения остановки/удаления, а завершённые модели сохраняют SUCCEEDED.
При неподтверждённой очистке активный слот удерживается. Повторов расчёта нет.
Превышение expected-seconds создаёт warning.json и предупреждение в журнале,
но не останавливает вычисление.

Новые файлы: service/runtime/ExperimentFiles.java, ModelRunContext.java,
configuration/OpenApiConfig.java, resources/examples/python-c.json,
scripts/test-supervisor-integration.py и этот отчёт. Обновлены существующие сервис,
исполнители, контроллер, DTO статуса, сериализация modelPath, defaults/factory,
GeneratorId, MainPaths, application.properties и соответствующие тесты.
Убрана двойная регистрация MainPaths через @Component и @EnableConfigurationProperties:
оставлена существующая @Component. Пути конфигурации используют абсолютный user.dir,
чтобы Spring PathEditor не трактовал '.' как ресурс ServletContext.
README обновлён, прежний текст сохранён в docs/legacy-supervisor-readme.md.
/experiment-data исключён из Git. Математические исходники моделей, их API-обёртка
и Unity в этом этапе не редактировались (в рабочем дереве остаются изменения моделей
от прежних этапов).

## Фактические проверки

### Сборка и модульные проверки

Команда `./gradlew test bootJar --offline`: exit 0, **17 тестов**, failures=0,
errors=0. Подтверждения: build/test-results/test/TEST-*.xml и
build/reports/tests/test/index.html. Проверены прежний маршрут/поля ответа,
400, статус/cancel, один активный запуск, ошибка Python без C, отмена,
сохранение SUCCEEDED у Python, явное сопоставление энергий, конфликты и стабильные
ссылки сохранённых результатов.

Промежуточные сборки выявили ошибку Spring PathEditor для относительного '.',
двойную регистрацию MainPaths и URI-сериализацию Path. Исправления проверены
последующими успешными тестами и реальным запуском. Модульные mock-исполнители
не выдаются за проверку Docker-интеграции.

### Запуск Java и реальные HTTP-цепочки

Первый запуск был из корня supervisor с отдельным каталогом данных:

```
java -jar build/libs/supervisor-0.0.1-SNAPSHOT.jar --server.port=18090 \
  --supervisor.paths.project-root=/home/onegigaflops/diploma_dev/skif_platform/development/supervisor \
  --supervisor.paths.experiments-root=/home/onegigaflops/diploma_dev/skif_platform/development/supervisor/test-results/supervisor-integration/experiments
```

Затем проверена **точная команда запуска README**, без переопределения путей:

```
java -jar build/libs/supervisor-0.0.1-SNAPSHOT.jar --server.port=18090
```

Проверка: `python3 scripts/test-supervisor-integration.py http://127.0.0.1:18090
 test-results/supervisor-integration/verified-http` (одна строка), exit **0**.
Журнал — test-results/supervisor-integration/verified-integration.log.
Окончательный серверный журнал — java-verified.log в том же каталоге.
После проверки Java остановлена штатным SIGINT; экспериментальные данные оставлены.

| Сценарий окончательной проверки | experimentId | Фактически |
|---|---|---|
| Одна регистрация всей цепочки | 1793220871824 | 202 за ~0.017 с, затем COMPLETED, обе модели SUCCEEDED |
| Реальная ошибка Python | 1793220871825 | FAILED с RESULT_INVALID; C SKIPPED, её рабочий каталог/сборка/запрос не создавались |
| Отмена реально работающей C | 1793220871826 | Python SUCCEEDED, C CANCELLED, эксперимент CANCELLED; Python-результат сохранён |
| Отмена на первой модели | 1793220871827 | CANCELLED; C SKIPPED и не запускалась |

Для каждого сценария есть registration.json, registration-seconds.txt, запрос,
status.json и status-history.json в verified-http/<scenario>/.
Во время выполнения опрашивался **Java API статуса**, не логи; наблюдались BUILDING,
STARTING_CONTAINER, WAITING_API, MODEL_RUNNING/FINALIZING, SAVING_RESULT,
CANCELLING и терминальные состояния. Каждый короткий этап не обязан попасть
в дискретный опрос клиента. Статус не содержит матрицу.

Swagger проверен HTTP-запросами: /swagger-ui/index.html = 200, /v3/api-docs = 200,
проверены готовый пример и response 202. Файлы — verified-http/swagger-ui.html
и openapi.json. Визуальный браузерный тест нажатия Execute не выполнялся;
те же операции выполнялись HTTP-клиентом по опубликованному контракту.

Реальный Python вернул:

```json
{"E_start":31.515151515151487,"E_end":31.515151515151487}
```

Оба значения точно совпали с parameters фактического POST к C. Дополнительно
проверено совпадение с настоящим внутренним input/start.json C из диагностической
копии /runs. Конфигурация эксперимента не содержала статической энергии C.
Выход Python не заменялся подготовленным значением. Полный C-результат получен
через Java HTTP API: матрица 988×988, error_rate, max_dA=9.47391685530333,
min_dA=3.3739916564834846; 1 956 211 байт.

Итоги и HTTP-артефакты: verified-http/success/{python-result.json,c-result.json,
c-request.json,results.json,events.json,python-logs.jsonl,c-logs.jsonl}.
Рабочие данные окончательного запуска: experiment-data/<experimentId>/models/.
Метаданные не включают img; в результатах обе ссылки ведут в Java API.

Доказательство активной отмены: у C-модели model-status.json содержит CANCELLED,
а её logs.jsonl — `Process started pid=…` и `Process exited code=-15`.
Следовательно, остановлен настоящий дочерний вычислительный процесс. После этого
контейнер удалён, cleanup.json содержит removed=true. Принудительный kill как
fallback в этом сценарии не потребовался; намеренный отказ HTTP cancel отдельно
не имитировался.

Дополнительно проверены HTTP 400 при статических энергиях и 409 при второй
регистрации в активном эксперименте. После завершения контейнеров с label этого
эксперимента нет, а повторный GET полного C-результата возвращает тот же JSON.
После перезапуска Java доступен статус и Python-результат прежнего эксперимента:
test-results/supervisor-integration/restart-history-check.json.

Предыдущие промежуточные сквозные прогоны сохранены отдельно: http/,
default-paths-http/, final-http/. Они не заменяют verified-http окончательной
проверки; wrapper-only smoke-тесты в качестве доказательства интеграции не использовались.
Машинная сводка окончательного прогона — test-results/supervisor-integration/verified-summary.json.
Снимки исходных/итоговых Docker-контейнеров — docker-before.txt/docker-after.txt.
Все исходные контейнеры сохранены, чужие контейнеры не останавливались.

## Оставшиеся ограничения

- Один экземпляр Java на хосте; межпроцессной блокировки нескольких супервизоров нет.
- Нет СУБД и возобновления. При аварийном перезапуске незавершённая история
  получает FAILED/INTERRUPTED; после SIGKILL возможны осиротевшие контейнеры.
  Автоматического захвата/перезапуска чужих сред нет.
- Java запускается на Linux-хосте. Старый Dockerfile самого супервизора не адаптирован
  под Docker CLI/localhost-порты и не проверялся как способ запуска этой версии.
- API_UNREACHABLE, CONTAINER_EXITED, MODEL_HTTP, MODEL_FAILED, RESULT_INVALID и BUILD_FAILED
  разделены в ошибках; авария Docker daemon, разрыв API и отказ диска отдельно
  в реальном сквозном прогоне не инъецировались.
- Диагностическое копирование /runs выполняется best effort. При недоступном API
  могут отсутствовать последние ещё не полученные страницы логов; уже записанные
  данные не удаляются. Размер большой матрицы и хеш проверяются, но JSON пока
  разбирается в памяти. Экспериментальный журнал читается с диска целиком для пагинации.
- Нынешние модели не публикуют промежуточные значения; журнал пустой, без фиктивного прогресса.
- Unity не изменялась и не запускалась.
