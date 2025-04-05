
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
