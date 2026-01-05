
## SWAGGER  
Можно гонять на 
`http://localhost:8080/swagger-ui/index.html` 

## ВАЖНОЕ ПРО docker-java-api  
Напишу тут, чтобы не забыть.  
Апишка вот тут: `https://github.com/docker-java/docker-java/blob/main/docs/getting_started.md`.  
Было дофига проблем при подключении этой либы, но самое основное это:  
1. Если работать через Idea, то НИКОГДА не подключать библиотеки через `project settings -> libraries`, т.к. возникают проблемы с загрузкой библиотек, если подгружать разные версии библиотеки.  
Как лучше делать? Добавлять зависимости через систему сборки (gradle/maven).  
2. Если работать на linux, и возникает ошибка `HttpHostConnectException: Connect to localhost:2375 [localhost/127.0.0.1] failed: Connection refused`, то ответ как это сделать вот [тут](https://gist.github.com/styblope/dc55e0ad2a9848f2cc3307d4819d819f). Если на винде, и такая же ошибка, то (вроде как, сама не проверяла), ответ вот [тут](https://stackoverflow.com/a/63460193).


## ПРОВЕРКА РАБОТЫ ЗАПУСКА 2Х МОДЕЛЕЙ

### САМИ МОДЕЛЬКИ
Код моделек находится [вот здесь](models)  
1я моделька Димы - `model_01` (шторка) - на питоне   
2я модельки Ильи - `model_02` (дифрактометр) - на Си   

### КАК ЗАПУСКАТЬ  

1. В файле [application.properties](src/main/resources/application.properties)
надо поменять пути на свои локальные
`supervisor.paths.models-root-dir-path`, `supervisor.paths.start-json-dir-path`, `supervisor.paths.end-json-dir-path`, `supervisor.paths.inter-model-json-dir-path`

`supervisor.paths.models-root-dir-path` - это директория с кодом моделей  
`supervisor.paths.start-json-dir-path` - это директория с входными параметрами моделей (создаёт сам супервизор)  
`supervisor.paths.end-json-dir-path` - это директория с выходными параметрами моделей (создаёт сам супервизор)  
`supervisor.paths.inter-model-json-dir-path` - это директория с вспомогательными файлами (пока что сюда просто скопированы доп.файлы для model_02)  

2. Находясь в корне проекта (`development/supervisor`) пишем в терминале  
```gradle build```  
Потом  
```gradle bootJar```  

Для запуска жарника идем в ` build/libs/`:
```
cd build/libs
```  
Далее запускаем 
```
java -jar supervisor-0.0.1-SNAPSHOT.jar 
```   

3. Открываем `Postman`, делаем `Post` запрос на `localhost:8080/api/experiments/start` :
```
{
  "experimenId": "1",
  "experimentName": "calculations",
  "models": [
    {
      "modelId": "1",
      "name": "shtorka_model",
      "order": 1,
      "version": "1.0.0",
      "language": "PYTHON",
      "modelPath": "model_01",
      "parametersName": [
        "E_input",
        "h_y_1", "h_y_2", "h_x_1", "h_x_2"
        ]
    },
    {
      "modelId": "2",
      "name": "difract_model",
      "order": 2,
      "version": "1.2.0",
      "language": "C",
      "modelPath": "model_02",
      "parametersName": [
        "c_x", "c_y", "c_z",
        "s_x", "s_y", "s_z",
        "omega", "kappa", "phi",
        "xSampleSize", "ySampleSize", "zSampleSize",
        "d_x", "d_y", "d_z",
        "theta", "beta", "gammaValue",
        "sU", "sB", "sR", "sL",
        "E_start", "E_end", "t"
        ]   
    }
  ]
}
```   

1я модель (которая на питоне) работает в контейнере несколько секунд, т.к. рассчёты там пока совсем небольшие, 2я модель (которая на си) - работает ~7мин.  


## TODOS  
- [] Подумать над тем, как нормально обращаться с доп.файлами (кроме стартовых/итоговых), если они используются моделькой. Как их прокидывать. Пока вручную скопированы просто в директорию локальную.  
- [] Подумать над тем, как прокидывать пути в сам код моделек (чтоб мне как разрабу супервизора не лезть в код моделек, заменяя пути)   
- [] В `String createDockerfileContent(ModelRequest model)` - там сделать универсально чтоб добавлялось имя команды (при увеличении кол-ва моделей)
- [] Пофиксить id-шники  
- [] `.dockerignore` когда-то не был пойман docker-java-api, надо будет с этим разобраться и добавить. Тк, н-р, во 2й модели удалила директорию жирную, которая тянулась из microsoft visual studio (фу).