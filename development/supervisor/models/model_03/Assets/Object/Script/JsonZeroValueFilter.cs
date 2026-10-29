using UnityEngine;
using System.IO;

public class JsonZeroValueFilter : MonoBehaviour
{
    [System.Serializable]
    private class InputData
    {
        public float phi;
        public float omega;
        public float kappa;
        public float theta;
        public float detector;
    }

    void Start()
    {
        if (UnityServerMode.Enabled) return;
        // Автоматические пути к файлам
        string inputPath = Path.Combine(Application.streamingAssetsPath, "input.json");
        string outputPath = Path.Combine(Application.streamingAssetsPath, "output.json");

        // Проверка существования входного файла
        if (!File.Exists(inputPath))
        {
            Debug.LogError("Input file not found: " + inputPath);
            return;
        }

        // Чтение файла (особенность для Android)
        string jsonString;

        // Для других платформ
        jsonString = File.ReadAllText(inputPath);

        InputData data = JsonUtility.FromJson<InputData>(jsonString);

        // Формирование выходных данных
        string outputJson = "{";
        bool isFirst = true;

        if (data.phi != 0f) AddField(ref outputJson, "phi", data.phi, ref isFirst);
        if (data.omega != 0f) AddField(ref outputJson, "omega", data.omega, ref isFirst);
        if (data.kappa != 0f) AddField(ref outputJson, "kappa", data.kappa, ref isFirst);
        if (data.theta != 0f) AddField(ref outputJson, "theta", data.theta, ref isFirst);
        if (data.detector != 0f) AddField(ref outputJson, "detector", data.detector, ref isFirst);

        outputJson += "}";

        // Сохранение результата
        File.WriteAllText(outputPath, outputJson);
        Debug.Log("Output saved to: " + outputPath);
    }

    private void AddField(ref string json, string name, float value, ref bool isFirst)
    {
        if (!isFirst) json += ",";
        json += $"\"{name}\": {value}";
        isFirst = false;
    }
}