using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ExitGame : MonoBehaviour
{
    public void CloseGame()
    {
        // Для мобильных устройств (Android/iOS) - просто сворачиваем приложение
        // Для редактора - останавливаем play mode
        // Для standalone сборки (Windows/Mac/Linux) - закрываем приложение

#if UNITY_EDITOR
            UnityEditor.EditorApplication.isPlaying = false;
#elif UNITY_WEBGL
            // Для WebGL показываем сообщение (закрыть вкладку нельзя программно)
            Screen.fullScreen = false;
            Application.ExternalEval("alert('Игра завершена. Закройте вкладку браузера.');");
#else
        Application.Quit();
#endif
    }
}
