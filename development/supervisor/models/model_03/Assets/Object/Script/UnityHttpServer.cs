using UnityEngine;
using System;
using System.Net;
using System.Text;
using System.Threading;
using System.Collections.Generic;

public class UnityHttpServer : MonoBehaviour
{
    // Константы HTTP кодов статуса — чтобы не использовать магические числа в коде
    private const int HTTP_OK = 200;
    private const int HTTP_BAD_REQUEST = 400;
    private const int HTTP_NOT_FOUND = 404;
    private const int HTTP_METHOD_NOT_ALLOWED = 405;
    private const int HTTP_INTERNAL_ERROR = 500;

    [SerializeField]
    private int port = 5030;

    [SerializeField]
    private NewMoveComponent scannerController;

    private HttpListener httpListener;
    private Thread serverThread;
    private bool isRunning = false;
    private Queue<Action> mainThreadQueue = new Queue<Action>();
    private object queueLock = new object();

    // Инициализация: запускаем HttpListener и стартуем фоновый поток для прослушивания запросов.
    private void Start()
    {
        if (scannerController == null)
        {
            scannerController = GetComponent<NewMoveComponent>();
            if (scannerController == null)
            {
                Debug.LogError("NewMoveComponent не найден! Назначьте вручную или добавьте на этот GameObject");
                return;
            }
        }

        try
        {
            httpListener = new HttpListener();
            httpListener.Prefixes.Add($"http://0.0.0.0:{port}/");
            httpListener.Start();
            isRunning = true;

            serverThread = new Thread(ListenForRequests)
            {
                IsBackground = true,
                Name = "UnityHttpServer"
            };
            serverThread.Start();

            Debug.Log($"HTTP сервер запущен на http://localhost:{port}");
        }
        catch (Exception ex)
        {
            Debug.LogError($"Ошибка запуска HTTP сервера: {ex.Message}");
        }
    }

    // Фоновый цикл: слушает входящие HTTP-запросы и делегирует их соответствующим обработчикам.
    private void ListenForRequests()
    {
        while (isRunning)
        {
            try
            {
                HttpListenerContext context = httpListener.GetContext();
                HttpListenerRequest request = context.Request;
                HttpListenerResponse response = context.Response;

                string method = request.HttpMethod;
                string path = request.Url.AbsolutePath.ToLower();

                try
                {
                    if (method == "GET")
                    {
                        HandleGetRequest(path, response);
                    }
                    else if (method == "POST")
                    {
                        HandlePostRequest(path, request, response);
                    }
                    else
                    {
                        SendResponse(response, HTTP_METHOD_NOT_ALLOWED, "Method not allowed");
                    }
                }
                catch (Exception ex)
                {
                    Debug.LogError($"Ошибка обработки запроса: {ex.Message}");
                    SendResponse(response, HTTP_INTERNAL_ERROR, $"{{\"error\": \"{ex.Message}\"}}");
                }
            }
            catch (HttpListenerException)
            {
                // Нормальное завершение при остановке сервера
                break;
            }
            catch (Exception ex)
            {
                if (isRunning)
                {
                    Debug.LogError($"Критическая ошибка сервера: {ex.Message}");
                }
            }
        }
    }

    // Обработка GET-запросов: health и state. Возвращает 404 для неизвестных путей.
    private void HandleGetRequest(string path, HttpListenerResponse response)
    {
        if (path == "/api/scanner/state")
        {
            string stateJson = GetScannerState();
            SendResponse(response, HTTP_OK, stateJson, "application/json");
        }
        else if (path == "/api/scanner/health")
        {
            SendResponse(response, HTTP_OK, "{\"status\": \"ok\"}", "application/json");
        }
        else
        {
            SendResponse(response, HTTP_NOT_FOUND, "{\"error\": \"Endpoint not found\"}");
        }
    }

    // Обработка POST-запросов: поддерживаются как глобальные команды (/api/scanner/start, /api/scanner/stop), 
    // так и per-axis маршруты /api/scanner/{axis}/{command} — например /api/scanner/phi/speed
    private void HandlePostRequest(string path, HttpListenerRequest request, HttpListenerResponse response)
    {
        string body = "";
        using (System.IO.StreamReader reader = new System.IO.StreamReader(request.InputStream, request.ContentEncoding))
        {
            body = reader.ReadToEnd();
        }

        // Динамические маршруты по оси: /api/scanner/{axis}/{command} — обработка команд для конкретных осей (phi, kappa, omega, theta, detector, all)

        // Новый динамический маршрут: /api/scanner/{axis}/{command} или глобальные команды: /api/scanner/{command} (без axis)
        string[] parts = path.Trim('/').Split('/');
        
        // parts: ["api","scanner","{axis}","{command}"] или ["api","scanner","{command}"]
        if (parts.Length >= 3 && parts[0] == "api" && parts[1] == "scanner")
        {
            // Обработка глобальных команд по пути /api/scanner/{command} (без параметра axis: start, stop)
            if (parts.Length == 3)
            {
                string globalCommand = parts[2].ToLower();
                switch (globalCommand)
                {
                    case "stop":
                        QueueMainThreadAction(() =>
                        {
                            // Останавливаем все оси через контроллер и симулируем нажатие UI-кнопки Stop
                            try { scannerController?.StopAxis("all"); } catch { }
                            var moG = scannerController?.moveObj;
                            if (moG != null && moG.stopGame != null)
                            {
                                try { moG.stopGame.onClick.Invoke(); } catch (Exception ex) { Debug.LogError("Failed to invoke stop button: " + ex.Message); }
                            }
                        });
                        SendResponse(response, HTTP_OK, "{\"message\": \"Stop requested\"}", "application/json");
                        return;

                    case "start":
                        QueueMainThreadAction(() =>
                        {
                            // Включаем все оси через контроллер и симулируем нажатие UI-кнопки Start
                            var moG = scannerController?.moveObj;
                            if (moG != null && moG.startGame != null)
                            {
                                try { moG.startGame.onClick.Invoke(); } catch (Exception ex) { Debug.LogError("Failed to invoke start button: " + ex.Message); }
                            }
                        });
                        SendResponse(response, HTTP_OK, "{\"message\": \"Start requested\"}", "application/json");
                        return;

                    default:
                        // не глобальная команда — продолжить обработку как per‑axis (с axis)
                        break;
                }
            }
        }
        
        // если запрос содержит axis+command — обработка будет ниже
        
        if (parts.Length >= 4 && parts[0] == "api" && parts[1] == "scanner")
        {
            string axis = parts[2].ToLower();
            string command = parts[3].ToLower();

            try
            {
                switch (command)
                {
                    // Home: без тела запроса
                    case "home":
                        QueueMainThreadAction(() => scannerController?.HomeAxis(axis));
                        SendResponse(response, HTTP_OK, "{\"message\": \"Home " + axis + " requested\"}", "application/json");
                        break;

                    // Reset: без тела запроса
                    case "reset":
                        QueueMainThreadAction(() => scannerController?.ResetAxis(axis));
                        SendResponse(response, HTTP_OK, "{\"message\": \"Reset " + axis + " requested\"}", "application/json");
                        break;

                    // Rotate: ожидает тело { "start": true|false } — включает/выключает вращение (обычно для phi)
                    case "rotate":
                        RotateRequest rotateReq = null;
                        try { rotateReq = JsonUtility.FromJson<RotateRequest>(body); } catch { }
                        if (rotateReq == null)
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, "{\"error\": \"Invalid body\"}");
                            break;
                        }
                        if (axis != "phi")
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, "{\"error\": \"Rotate command is only valid for phi axis\"}", "application/json");
                            break;
                        }
                        QueueMainThreadAction(() => scannerController?.RotateAxis(axis, rotateReq.start));
                        SendResponse(response, HTTP_OK, "{\"message\": \"Rotate " + axis + " " + (rotateReq.start ? "start" : "stop") + " requested\"}", "application/json");
                        break;

                    // Speed: ожидает тело { "value": int } — установка скорости для указанной оси
                    case "speed":
                        ValueRequest vreq = null;
                        try { vreq = JsonUtility.FromJson<ValueRequest>(body); } catch { }
                        if (vreq == null)
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, "{\"error\": \"Invalid body\"}");
                            break;
                        }

                        // Валидация диапазона для каждой оси. Возвращаем 400, если значение выходит за пределы допустимого.
                        if (!ValidateValueForCommand(axis, "speed", vreq.value, out string verr))
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, "{\"error\": \"" + verr + "\"}", "application/json");
                            break;
                        }

                        QueueMainThreadAction(() => scannerController?.SetSpeedAxis(axis, vreq.value));
                        SendResponse(response, HTTP_OK, "{\"message\": \"Set speed " + axis + " to " + vreq.value + ", \"axis\": \"" + axis + "\", \"speed\": " + vreq.value + "}", "application/json");
                        break;

                    // Relative-move: тело { "distance": int } — относительное смещение от текущей позиции
                    case "relative-move":
                        MoveData mre = null;
                        try { mre = JsonUtility.FromJson<MoveData>(body); } catch { }
                        if (mre == null)
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, "{\"error\": \"Invalid body\"}");
                            break;
                        }

                        if (!ValidateValueForCommand(axis, "relative-move", mre.distance, out string rerr))
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, $"{{\"error\": \"{rerr}\"}}", "application/json");
                            break;
                        }

                        QueueMainThreadAction(() => scannerController?.RelativeMoveAxis(axis, mre.distance));
                        SendResponse(response, HTTP_OK, "{\"message\": \"Relative move " + axis + " by " + mre.distance + ", \"axis\": \"" + axis + "\", \"distance\": " + mre.distance + "}", "application/json");
                        break;

                    // Absolute-move: тело { "distance": int } — абсолютная позиция для оси
                    case "absolute-move":
                        MoveData mare = null;
                        try { mare = JsonUtility.FromJson<MoveData>(body); } catch { }
                        if (mare == null)
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, "{\"error\": \"Invalid body\"}");
                            break;
                        }

                        if (!ValidateValueForCommand(axis, "absolute-move", mare.distance, out string aerr))
                        {
                            SendResponse(response, HTTP_BAD_REQUEST, $"{{\"error\": \"{aerr}\"}}", "application/json");
                            break;
                        }

                        QueueMainThreadAction(() => scannerController?.AbsoluteMoveAxis(axis, mare.distance));
                        SendResponse(response, HTTP_OK, "{\"message\": \"Absolute move " + axis + " to " + mare.distance + ", \"axis\": \"" + axis + "\", \"distance\": " + mare.distance + "}", "application/json");
                        break;

                    default:
                        SendResponse(response, HTTP_NOT_FOUND, "{\"error\": \"Unknown command\"}");
                        break;
                }
            }
            catch (Exception ex)
            {
                SendResponse(response, HTTP_INTERNAL_ERROR, $"{{\"error\": \"{ex.Message}\"}}", "application/json");
            }

            return;
        }

        SendResponse(response, HTTP_NOT_FOUND, "{\"error\": \"Endpoint not found\"}");
    }



    // Валидация значений для команд и осей.
    // Возвращает true, если значение допустимо. В случае ошибки возвращает false и заполняет 'error' понятным сообщением.
    private bool ValidateValueForCommand(string axis, string command, float value, out string error)
    {
        error = null;
        axis = (axis ?? "").ToLower();
        // Если axis == 'all', значение должно быть валидным для каждой оси отдельно.
        if (axis == "all")
        {
            string[] axes = new[] { "phi", "kappa", "omega", "theta", "detector" };
            foreach (var a in axes)
            {
                if (!ValidateValueForCommand(a, command, value, out error)) return false;
            }
            return true;
        }

        switch (command)
        {
            case "speed":
                switch (axis)
                {
                    case "phi":
                        if (value < -36 || value > 36) { error = $"Speed for phi must be between -36 and 36"; return false; }
                        return true;
                    case "omega":
                    case "kappa":
                    case "theta":
                    case "detector":
                        if (value < -18 || value > 18) { error = $"Speed for {axis} must be between -18 and 18"; return false; }
                        return true;
                    case "all":
                        // handled above
                        return true;
                    default:
                        error = $"Unknown axis '{axis}'";
                        return false;
                }

            case "relative-move":
                switch (axis)
                {
                    case "phi":
                        if (value < -360f || value > 360f) { error = "Relative move for phi must be between -360 and 360"; return false; }
                        return true;
                    case "kappa":
                        if (value < -72f || value > 72f) { error = "Relative move for kappa must be between -72 and 72"; return false; }
                        return true;
                    case "omega":
                        if (value < -90f || value > 99f) { error = "Relative move for omega must be between -90 and 99"; return false; }
                        return true;
                    case "theta":
                        if (value < -90f || value > 90f) { error = "Relative move for theta must be between -90 and 90"; return false; }
                        return true;
                    case "detector":
                        if (value < 0f || value > 100f) { error = "Relative move for detector must be between 0 and 100"; return false; }
                        return true;
                    default:
                        error = $"Unknown axis '{axis}'";
                        return false;
                }

            case "absolute-move":
                switch (axis)
                {
                    case "phi":
                        if (value < -360f || value > 360f) { error = "Absolute move for phi must be between -360 and 360"; return false; }
                        return true;
                    case "kappa":
                        if (value < -72f || value > 72f) { error = "Absolute move for kappa must be between -72 and 72"; return false; }
                        return true;
                    case "omega":
                        if (value < -90f || value > 99f) { error = "Absolute move for omega must be between -90 and 99"; return false; }
                        return true;
                    case "theta":
                        if (value < -90f || value > 90f) { error = "Absolute move for theta must be between -90 and 90"; return false; }
                        return true;
                    case "detector":
                        if (value < 0f || value > 100f) { error = "Absolute move for detector must be between 0 and 100"; return false; }
                        return true;
                    default:
                        error = $"Unknown axis '{axis}'";
                        return false;
                }

            default:
                // Валидация для неизвестных команд не требуется
                error = null;
                return true;
        }
    }

    // Возвращает текущее состояние сканера как JSON-строку, используя NewMoveComponent.GetState().
    private string GetScannerState()
    {
        string state = "unknown";
        if (scannerController != null)
        {
            state = scannerController.GetState();
        }
        return $"{{\"state\": \"{state}\"}}";
    }

    // Помещает действие в очередь для выполнения в главном Unity-потоке.
    // Сервер обрабатывает запросы в фоновом потоке, поэтому все взаимодействия с GameObject должны выполняться в главном потоке.
    private void QueueMainThreadAction(Action action)
    {
        lock (queueLock)
        {
            mainThreadQueue.Enqueue(action);
        }
    }

    // Выполняется каждый кадр в главном потоке Unity: обрабатываем очередь действий, пришедших от HTTP сервера.
    private void Update()
    {
        lock (queueLock)
        {
            while (mainThreadQueue.Count > 0)
            {
                Action action = mainThreadQueue.Dequeue();
                try
                {
                    action.Invoke();
                }
                catch (Exception ex)
                {
                    Debug.LogError($"Ошибка выполнения действия в главном потоке: {ex.Message}");
                }
            }
        }
    }

    // Отправляет HTTP-ответ клиенту. Включает код статуса, заголовок Content-Type и тело (UTF-8).
    private void SendResponse(HttpListenerResponse response, int statusCode, string body, string contentType = "application/json")
    {
        try
        {
            response.StatusCode = statusCode;
            response.ContentType = contentType;
            byte[] buffer = Encoding.UTF8.GetBytes(body);
            response.ContentLength64 = buffer.Length;
            response.OutputStream.Write(buffer, 0, buffer.Length);
            response.OutputStream.Close();
        }
        catch (Exception ex)
        {
            Debug.LogError($"Ошибка отправки ответа: {ex.Message}");
        }
    }

    // Останавливает сервер при уничтожении объекта (при закрытии сцены/игры). Закрывает HttpListener и поток.
    private void OnDestroy()
    {
        isRunning = false;
        if (httpListener != null)
        {
            httpListener.Stop();
            httpListener.Close();
        }
        if (serverThread != null && serverThread.IsAlive)
        {
            serverThread.Join(1000);
        }
        Debug.Log("HTTP сервер остановлен");
    }

    [System.Serializable]
    private class MoveData
    {
        public string axis;
        public float distance;
    }

    [System.Serializable]
    private class RotateRequest
    {
        public bool start;
    }

    [System.Serializable]
    private class ValueRequest
    {
        public int value;
    }
}
