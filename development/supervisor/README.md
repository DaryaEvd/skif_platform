
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
Код моделек находится [вот здесь](development/supervisor/models)  
1я моделька Ильи - `model_01`  
2я модельки Димы - `model_02`

### КАК ЗАПУСКАТЬ
Находясь в корне проекта (`development/supervisor`) пишем в терминале
```gradle build```
```gradle bootJar```

Для запуска жарника идем в ` build/libs/`:
```
cd build/libs
```
Далее запускаем 
```
java -jar supervisor-0.0.1-SNAPSHOT.jar 
```

Открываем `Postman`, делаем `Post` запрос:
```
{
  "experimenId": "1",
  "experimentName": "calculations",
  "models": [
    {
      "modelId": "1",
      "name": "difract_model",
      "order": 1,
      "version": "1.2.0",
      "language": "C",
      "modelPath": "model_01",
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
