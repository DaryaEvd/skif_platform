# Unity: отдельный вычислительный эксперимент — 2026-10-29

## Изучено и реализовано

AGENTS.md в проекте, родительских каталогах и models/model_03 не найдены.
Изучены текущие README, отчёты, Java-контроллер/DTO/сервис/общий исполнитель,
Python/C /api/v1 и четыре Unity-компонента. Подтверждены прежние ограничения:
NewMoveComponent.GetState возвращал константу, UnityHttpServer подтверждал
постановку команды в очередь, MoveObj.OnStopGameClicked только останавливал время.

Добавлены UnityModelApi.cs и UnityServerMode.cs. Явный MODEL_SERVER_MODE=1 или
-modelApi включает отдельный серверный режим; без него проект сохраняет
интерактивный путь. Canvas и его ссылки не удалялись. В серверном режиме
отключены обработчики UI-команд и остальные движения; Rigidbody становятся
kinematic, чтобы исходная сцена не сдвигалась до запуска. Это сценарий
кинематического Phi, а не моделирование столкновений: прежняя ветвь Phi
также не использовала RayTarget.IsColliding.

UnityHttpServer сохраняет /api/scanner. В серверном режиме его POST дают 409,
а /api/v1 обслуживается через ту же очередь главного потока. Чтение HTTP тела
выполняется фоновым потоком без обращения к сцене; Dispatch, чтение Transform,
изменение состояния, измерения и сериализация согласованного снимка — в главном.
Legacy GET scanner/state также переведён в очередь, GetState теперь отражает
реальное idle/running/paused или серверное состояние. Старый host-prefix
0.0.0.0 заменён на wildcard: реальный запрос через localhost иначе получал
400 Invalid host. Префиксы маршрутов не менялись.

Общий контракт /api/v1: readiness; POST runs; GET runs/{runId}; cancel;
publications; result/metadata; result; logs. Один расчёт на контейнер,
идемпотентный повтор runId/JSON, конфликт при изменении или новом runId.
Readiness проверяет активный MoveObj/Phi Transform и рабочее хранилище сцены.
Состояния ACCEPTED, RUNNING, FINALIZING, SUCCEEDED/FAILED/CANCELLED; принятие
команды не равно окончанию. Отмена реально выключает флаги движения;
терминальное состояние не перезаписывается поздним Update. Сервер остаётся
доступен после расчёта.

### Вычисления и ограничения

angleDeg → PresrelativeMovePhi/relativeMovePhi; speedDegPerSec →
PrespeedPhi/speedPhi. Два поля скорости Phi изменены int → float; передача
нового сценария не использует старые UI-методы с приведением к int.
0.01 < angleDeg ≤ 360; 0 < speedDegPerSec ≤ 36, конечные положительные значения
float32 в расчётной части. У границ следует учитывать разрешение float32.
Отсечение к целым не выполняется. Угол меньше/равный существующему допуску
не принимается, чтобы не объявлять нулевое движение завершённым.

Положительная формула шага сохранена: sign(остатка) * min(speed * Time.deltaTime,
abs(остатка)), Transform.Rotate в Space.Self, уменьшение остатка; критерий
сброса флага 0.01° сохранён. StepServerPhi живёт в MoveObj и вызывается только
новым серверным сценарием. Отдельный guard запрещает ≥180° за кадр, когда
однозначное восстановление приращения кватерниона невозможно; нормальный
ограниченный Time.deltaTime и скорость ≤36°/с до этого порога не доходят.
Отрицательная/неожиданная внеосевая ротация считается ошибкой. Метод не заменяет
вычисление результатом angle/speed и не меняет формулы других осей.

Накопление — сумма **измеренных** приращений inverse(before)*after с учётом
эквивалентности q/-q и знака X. Завершение проверяет положительный накопленный
поворот, остаток ≤0.01°, ориентацию initial * rotationX(angle) и отсутствие
других движений. Затем фиксируются bytes результата и SHA-256/метаданные,
после чего SUCCEEDED. Начальная и конечная ориентации — локальные единичные
кватернионы x,y,z,w, углы в градусах, скорость в градусах/секунду модельного времени.

modelElapsedSeconds — сумма реально использованных Time.deltaTime, включая
дополнительный кадр сброса флага существующей ветви. realElapsedSeconds — Stopwatch.
Публикации имеют seq, publishedAt UTC и фактические значения/локальную ориентацию;
обычные снимки не чаще 4 Гц, плюс начальный и терминальный. Нет процентов или
фиктивных промежуточных данных. Фиксированный шаг не вводился; кадровый расчёт
не объявляется детерминированным.

### Сборка и супервизор

Версия ProjectVersion.txt и реально использованный Editor: **6000.5.5f1
(d16e074b49fd)**. На машине найдены Editor и Linux Dedicated Server модуль.
Assets/Editor/BuildModelApi.cs собирает ту же SampleScene как Linux x86_64
StandaloneBuildSubtarget.Server и создаёт манифест хешей. Полный выход:
models/model_03/Builds/ModelApi/. Исходные C# действительно включены в новую
Assembly-CSharp.dll; старый docker-build/mybuild не использовался.

Dockerfile.api копирует целую готовую сборку. Java Editor не запускает: новый
UnityModelExecutor использует существующий общий HTTP-клиент/lifecycle,
проверяет DedicatedServer subtarget, версию и SHA-256 исходников/бинарников,
затем обычный docker build. Бинарник запускается foreground с -batchmode
-nographics -modelApi. **В конечном контейнере нет DISPLAY и Xvfb.** Нужные
нативные библиотеки, включая зависимости AppUI, устанавливаются образом.

Сохранены существующие Java API/Swagger и Python → C. Добавлены enum UNITY,
отдельный пример /api/experiments/example/unity и второй именованный пример
в POST /api/experiments/start. Смешанные Unity/Python/C конфигурации отклоняются.
Unity не имеет скрытых параметров по умолчанию. Результаты проверяются по
схеме, значениям, кватернионам, SHA-256/размеру и соответствию заданным параметрам.
Публикации сохраняются общим исполнителем в publications.jsonl и выдаются
существующим /api/experiments/{id}/models/{order}/publications после удаления среды.

## Фактически выполнено

Все пути подтверждений далее относительно supervisor. Логи/результаты находятся
в test-results/unity-integration, не только в /tmp. Данные не удалялись.

| Проверка | Фактический результат | Подтверждение |
|---|---|---|
| Editor 6000.5.5f1, BuildModelApi.Linux, Server subtarget | Exit 0, новая Linux Dedicated Server сборка | editor-server-build.log; Build Finished, Result: Success; Builds/ModelApi/model-api-build.json |
| docker build -f models/model_03/Dockerfile.api -t skif-model-unity-api:test . | Exit 0 | docker-build-server.log |
| ./gradlew test bootJar --offline | Exit 0, 21 тест, 0 ошибок | build/test-results/test/TEST-*.xml |
| python3 scripts/test-unity-docker.py test-results/unity-integration/docker-server-http | Exit 0, 3 свежих контейнера | docker-server-http/PASS; отдельные test.log, inspect.json, docker.log, runs-snapshot/ |
| python3 scripts/test-unity-supervisor.py http://127.0.0.1:18090 test-results/unity-integration/java-server-http | Exit 0 | java-server-http/PASS; java-server-http.log |
| python3 scripts/test-supervisor-integration.py http://127.0.0.1:18090 test-results/unity-integration/python-c-regression | Exit 0, реальные Python и C, ошибка Python/пропуск C, отмена C, ранняя отмена | python-c-regression/PASS; python-c-regression.log |

Editor запускался -batchmode -nographics -quit -projectPath <absolute model_03>
-executeMethod BuildModelApi.Linux. Это новая сборка, не статическая проверка C#.
Был также успешный нативный запуск новой C#-версии на хосте, но выводы о конечном
серверном артефакте основаны на Dedicated Server Docker/Java-проверках ниже.

### Прямой Unity API в Docker без визуального интерфейса

Фактически проверены readiness до команды и неизменность исходной ориентации,
RUNNING, SUCCEEDED, числа/дробные параметры, промежуточные значения, неверные
границы параметров (422), повтор POST во время и после выполнения без повторного
движения, изменённый запрос/новый runId (409), запрет legacy POST (409),
пагинация публикаций, их монотонные seq, SHA-256/размер/повторная загрузка результата.
После отмены проверены CANCELLED, неподвижный согласованный снимок спустя время,
сохранение журналов и отсутствие доступного итогового успешного результата.

| Свежий контейнер | Результат | Накопленный поворот | Остаток | Публикации |
|---|---|---|---|---|
| 15.5° при 7.75°/с | SUCCEEDED | 15.49999895128924° | 0.000001048710759° | 9 |
| 360° при 36°/с | SUCCEEDED | 359.99969727401583° | 0.00030272598417° | 40 |
| Отмена 180.5° при 0.5°/с | CANCELLED | 0.216912306919° на момент отмены | движение не продолжалось | 3 |

Полный оборот проверяет именно накопление: конечный кватернион близок к -initial,
что описывает ту же ориентацию, но накопленное движение около 360°, не около нуля.
Модельное время полного оборота 10.0189488 с, реальное 10.0094598 с. Это измерения
одного прогона, не обещание таких же времён или чисел для последующих кадровых запусков.

### Unity через Java/Swagger

Java реально запущена:

```
java -jar build/libs/supervisor-0.0.1-SNAPSHOT.jar --server.port=18090 \
 --supervisor.paths.experiments-root=/home/onegigaflops/diploma_dev/skif_platform/development/supervisor/test-results/unity-integration/experiments
```

Журнал java-server.log. Swagger UI /swagger-ui/index.html и /v3/api-docs отвечали
200; в OpenAPI проверены два примера. Скрипт отправлял один POST через существующий
/api/experiments/start и опрашивал API статуса; получал публикации ещё при MODEL_RUNNING.
Визуальное нажатие Execute в браузере не автоматизировалось.

Успех: **experimentId 1793245712941**, COMPLETED, модель SUCCEEDED.
Параметры 15.5° и 7.75°/с не потеряли дробную часть. Численный результат:

```json
{
  "angleDeg": 15.5,
  "speedDegPerSec": 7.75,
  "initialLocalOrientation": {"x": 0.3916949, "y": -4.58016558e-8, "z": 3.95440019e-8, "w": 0.9200952},
  "finalLocalOrientation": {"x": 0.5121931, "y": -4.00507751e-8, "z": 4.535922e-8, "w": 0.8588703},
  "accumulatedRotationDeg": 15.500038765208206,
  "remainingAngleDeg": -0.0000387652082061,
  "toleranceDeg": 0.01,
  "orientationErrorDeg": 0.0000398263597854,
  "modelElapsedSeconds": 2.0207500979304314,
  "realElapsedSeconds": 1.9900683
}
```

Получены 9 публикаций. После удаления контейнера повторно получены результат
по стабильной ссылке из /results, публикации и журнал из Java API.
Артефакты: java-server-http/success/ и experiments/1793245712941/.

Отмена: **experimentId 1793245712942**, CANCELLED, 5 сохранённых публикаций.
HTTP cancel остановил активное движение, затем среда удалена. Статус/публикации/
логи доступны через супервизор, запрос успешного результата даёт 409.
Артефакты: java-server-http/cancel/ и experiments/1793245712942/.

Реальный Java API дополнительно отклонил неверный angleDeg и смешанный эксперимент
с 400: invalid-unity.json, mixed-unity.json. Модульные тесты проверили параметры,
отказ отсутствующего/неверного артефакта, численный формат результата и запрет
смешивания. Java не запускала Editor ни в одной операции эксперимента.

### Python → C: регрессия

На том же окончательном Java jar выполнены:

- 1793245712943 — COMPLETED, обе модели SUCCEEDED, реальная энергия Python
  E_start=E_end=31.515151515151487 точно передана C;
- 1793245712944 — FAILED при ошибке Python, C SKIPPED;
- 1793245712945 — отмена реального C, Python-результат сохранён;
- 1793245712946 — ранняя отмена, C не запускалась.

Подтверждения в python-c-regression/; формулы и исходники Python/C не изменялись.
Машинная сводка — test-results/unity-integration/summary.json.

## Обнаруженные неудачные попытки и исправления

1. Первая новая обычная Player-сборка на хосте дала Invalid host (HTTP 400):
   исправлен префикс HttpListener. Подтверждение native-http traceback, затем
   успешный native-final-http/result.json и 9 публикаций.
2. Обычный Linux Player в Docker падал exit 139 до C# API. Сначала найдена
   недостающая GTK-зависимость AppUI; её установка устранила ошибку загрузки
   библиотеки, но не нативный crash Player при отсутствии window backend.
   Логи сохранены в docker-http/ и docker-gtk-http/.
3. Исследовался вариант виртуального X11 (образ построен, не принят как конечный
   и не выдаётся за успешный запуск). Конечное решение — штатный Dedicated Server
   subtarget с установленным модулем. В конечном Dockerfile нет Xvfb или DISPLAY.
4. Первая Java-попытка с обычным Player (1793244842874) корректно завершилась
   FAILED/CONTAINER_EXITED; статус и диагностика сохранены. Успешные доказательства
   относятся к последующей Dedicated Server сборке, не к этой попытке.

Факты о режиме/поддерживаемых платформах сверялись с официальными
[системными требованиями Unity 6.5](https://docs.unity.com/en-us/engine/6000.5/manual/get-started/install-and-upgrade/getting-started-installing-unity/system-requirements).
Причина конкретного нативного crash обычного Player полностью не локализована;
он не объявляется исправленным одной установкой GTK. Конечный Server-артефакт
проверен экспериментально.

## Ограничения и сохранность

- Это только конечный кинематический поворот Phi; столкновения/дифракция/остальные
  оси не являются предметом сценария. Источник — исходная сцена нового контейнера.
- Float32, кадровый Time.deltaTime, нефиксированный шаг; детерминированность не заявлена.
- Интерактивные ветви сохранены условным серверным флагом, но полный визуальный UI
  сценарий в Editor вручную не проверялся. Проект успешно компилируется и собирается.
- Нужен Editor 6000.5.5f1 с Linux Dedicated Server модулем для пересборки; готовые
  бинарники/Library/Logs исключены из Git. В этом рабочем каталоге свежий артефакт
  оставлен, полный состав файлов описан в README.
- API хранит публикации/логи и в памяти, и на диске; очень долгие медленные движения
  потребуют ограничения объёма/дисковой пагинации. Автоматического тайм-аута расчёта нет.
- Принудительный Docker fallback после отказа Unity HTTP cancel отдельно не
  инъецировался; обычная отмена реально проверена напрямую и через Java.
- Исходные 46 Docker container ID сохранены. Состояние уже существовавшего
  unity_model_container (84358f3bf3a1) изменилось с exited на running во время работ;
  агент не выполнял start/stop/rm для него и оставил его работающим. Собственные
  контейнеры имели уникальные имена/метки и удалены после диагностики. Снимки —
  docker-before.txt/docker-after.txt. Volumes и ранее сохранённые данные не удалялись.
