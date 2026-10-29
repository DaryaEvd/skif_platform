using UnityEngine;
using UnityEngine.UI;

public class RayTarget : MonoBehaviour
{
    /* Программа для считывания столкновения коллизий 
     и ввод панели ограничений в 3см с данными о положений объектов
     */
    public bool IsColliding = false;
    public GameObject panelError;
    public MoveObj moveObj;

    public Button ButtonSpeed;
    public int speedState = 0;  // 0 = нормальная скорость, 1 = ускорение x2, 2 = ускорение x4
    public Text speedTime;
    private Coroutine speedTextCoroutine;


    public Text textKappa;
    public Text textOmega;
    public Text textDetector;
    public Text textTheta;


    void Start()
    {
        if (UnityServerMode.Enabled) return;
        panelError.SetActive(false);
        ButtonSpeed.onClick.AddListener(OnButtonClickSpeed);
        speedTime.gameObject.SetActive(false);

    }
    private void OnCollisionEnter(Collision collision)
    {
        // Пропускаем коллизии с объектами, у которых тег "Platform"
        if (collision.gameObject.CompareTag("Platform"))
        {
            return;
        }
        IsColliding = true;
        if (UnityServerMode.Enabled) return;
        panelError.SetActive(true);
        textKappa.text = "Kappa Absolute: " + (int)Mathf.DeltaAngle(0, moveObj.OmegaObj.transform.localEulerAngles.z);
        textOmega.text = "Omega Absolute: " + (int)Mathf.DeltaAngle(0, moveObj.KappaObj.transform.localEulerAngles.y);
        textDetector.text = "Detector Absolute: " + (int)(moveObj.DetectorObj.transform.localPosition.z * 100);
        textTheta.text = "Theta Absolute: " + (int)Mathf.DeltaAngle(0, moveObj.ThetaObj.transform.localEulerAngles.y);
    }
    private void OnCollisionExit(Collision collision)
    {
        // Пропускаем коллизии с объектами, у которых тег "Platform"
        if (collision.gameObject.CompareTag("Platform"))
        {
            return;
        }
        IsColliding = false;
        if (UnityServerMode.Enabled) return;
        panelError.SetActive(false);

    }

    void OnButtonClickSpeed()
    {
        if (UnityServerMode.Enabled) return;
        // Циклическое переключение между скоростями
        speedState = (speedState +1) % 3;  // Теперь 3 состояния: 0, 1, 2

        if (speedState == 1)
        {
            Time.timeScale = 1;  // Нормальная скорость
        }
        else if (speedState == 2)
        {
            Time.timeScale = 2;  // Скорость x2
        }
        else
        {
            Time.timeScale = 4;  // Скорость x4
        }

        // Показываем текущую скорость
        ShowSpeedText();
    }

    // Делаем метод публичным
    public void ShowSpeedText()
    {
        // Останавливаем предыдущую корутину, если она была
        if (speedTextCoroutine != null)
        {
            StopCoroutine(speedTextCoroutine);
        }

        // Устанавливаем текст в зависимости от текущей скорости
        string speedText = "";
        switch (speedState)
        {
            case 0:
                speedText = "Normal Speed (x1)";
                break;
            case 1:
                speedText = "Fast Speed (x2)";
                break;
            case 2:
                speedText = "Ultra Speed (x4)";
                break;
        }

        speedTime.text = speedText;
        speedTime.gameObject.SetActive(true);

        // Запускаем корутину для скрытия текста через 2 секунды
        speedTextCoroutine = StartCoroutine(HideSpeedTextAfterDelay(2f));
    }

    System.Collections.IEnumerator HideSpeedTextAfterDelay(float delay)
    {
        yield return new WaitForSeconds(delay);
        speedTime.gameObject.SetActive(false);
        speedTextCoroutine = null;
    }

    public float GetCurrentTimeScale()
    {
        switch (speedState)
        {
            case 0: return 1f;
            case 1: return 2f;
            case 2: return 4f;
            default: return 1f;
        }
    }
}

