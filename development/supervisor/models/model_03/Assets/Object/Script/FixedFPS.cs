using UnityEngine;

public class FixedFPS : MonoBehaviour
{
    [SerializeField] private int targetFPS = 60;

    private void Start()
    {
        // Устанавливаем целевую частоту кадров
        Application.targetFrameRate = targetFPS;
    }
}