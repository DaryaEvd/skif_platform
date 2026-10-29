using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro; // Импортируем пространство имен TextMeshPro

public class MoveObj : MonoBehaviour
{
    /* Программа для движения частей сканера, учета нажатие кнопки старт, 
       работа со значениями ввода пользователя.
       Так же стоп и старт программы  в остановки времени.
     */
    public NewMoveComponent newMoveComponent; // Ссылка на компонент NewMoveComponent
    public Button startGame;
    public Button stopGame;


    [Header("Настройки вращения Phi")]
    public Transform phiObj;
    public bool phiObjRotate = false; // Вращение PhiObj
    private bool phiObjHome = false; // Дом phiObjHome

    public float speedPhi = 36; // Скорость вращения  
    public float PrespeedPhi = 36; // Скорость вращения  

    public float relativeMovePhi = 0; //От текушего положения
    public float PresrelativeMovePhi = 0; //От текушего положения
    public bool сhekRelativeMovePhi = false; //От текушего положения


    public bool chekAbsoluteMovePhi = false; //От текушего абсалют
    public float absoluteMovePhi = 0; //От текушего абсалют
    public float PresabsoluteMovePhi = 0; //От текушего абсалют
    private float currentPhi = 0f; // Текущий угол

    [Header("Настройки вращения Kappa")]
    public Transform OmegaObj;
    private bool OmegaObjHome = false; // Дом OmegaObjHome

    public int speedOmega = 18; // Скорость вращения  
    public int PrespeedOmega = 18; // Скорость вращения  

    public float relativeMoveOmega = 0; //От текушего положения
    public float PresrelativeMoveOmega = 0; //От текушего положения
    public bool сhekRelativeMoveOmega = false; //От текушего положения


    public bool chekAbsoluteMoveOmega = false; //От текушего абсалют
    public float absoluteMoveOmega = 0; //От текушего абсалют
    public float PresabsoluteMoveOmega = 0; //От текушего абсалют
    private float currentOmega = 0f; // Текущий угол

    [Header("Настройки вращения Omega")]
    public Transform KappaObj;
    private bool KappaObjHome = false; // Дом OmegaObjHome

    public int speedKappa = 18; // Скорость вращения  
    public int PrespeedKappa = 18; // Скорость вращения  

    public float relativeMoveKappa = 0; //От текушего положения
    public float PresrelativeMoveKappa = 0; //От текушего положения
    public bool сhekRelativeMoveKappa = false; //От текушего положения


    public bool chekAbsoluteMoveKappa = false; //От текушего абсалют
    public float absoluteMoveKappa = 0; //От текушего абсалют
    public float PresabsoluteMoveKappa = 0; //От текушего абсалют
    private float currentKappa = 0f; // Текущий угол

    [Header("Настройки вращения Theta")]
    public Transform ThetaObj;
    private bool ThetaObjHome = false; // Дом OmegaObjHome

    public int speedTheta = 18; // Скорость вращения  
    public int PrespeedTheta = 18; // Скорость вращения  

    public float relativeMoveTheta = 0; //От текушего положения
    public float PresrelativeMoveTheta = 0; //От текушего положения
    public bool сhekRelativeMoveTheta = false; //От текушего положения


    public bool chekAbsoluteMoveTheta = false; //От текушего абсалют
    public float absoluteMoveTheta = 0; //От текушего абсалют
    public float PresabsoluteMoveTheta = 0; //От текушего абсалют
    private float currentTheta = 0f; // Текущий угол

    [Header("Настройки вращения Detector")]
    public Transform DetectorObj;
    private bool DetectorObjHome = false; // Дом OmegaObjHome

    public int speedDetector = 18; // Скорость вращения  
    public int PrespeedDetector = 18; // Скорость вращения  

    public float relativeMoveDetector = 0; //От текушего положения
    public float PresrelativeMoveDetector = 0; //От текушего положения
    public bool сhekRelativeMoveDetector = false; //От текушего положения


    public bool chekAbsoluteMoveDetector = false; //От текушего абсалют
    public float absoluteMoveDetector = 0; //От текушего абсалют
    public float PresabsoluteMoveDetector = 0; //От текушего абсалют
    private float currentDetector = 0f; // Текущий угол

    [Header("Настройки вращения All")]
    public int speedAll = 18; // Скорость вращения
    public int PrespeedAll = 18; // Скорость вращения
    public int speedAllPhi = 36;     // Скорость Phi в режиме SpeedAllPhi


    public RayTarget rayTarget;





    // Start is called before the first frame update
    void Start()
    {
        if (UnityServerMode.Enabled) return;
        // Подписка на события нажатия кнопок
        startGame.onClick.AddListener(OnStartGameClicked);
        stopGame.onClick.AddListener(OnStopGameClicked);
    }

    void OnStartGameClicked()
    {
        if (UnityServerMode.Enabled) return;
        rayTarget.ShowSpeedText(); // Вызываем публичный метод
        if (newMoveComponent != null)
        {
            switch (newMoveComponent.allMove)
            {
                case 1:
                    // Выполните команду для allMove == 1
                    newMoveComponent.allMove = 0;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    phiObjRotate = false;
                    phiObjHome = true;
                    сhekRelativeMovePhi = false;
                    relativeMovePhi = 0;
                    PresrelativeMovePhi = relativeMovePhi;

                    OmegaObjHome = true;
                    сhekRelativeMoveOmega = false;
                    relativeMoveOmega = 0;
                    PresrelativeMoveOmega = relativeMoveOmega;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentOmega = OmegaObj.localRotation.eulerAngles.z;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentPhi = phiObj.localRotation.eulerAngles.x;

                    KappaObjHome = true;
                    сhekRelativeMoveKappa = false;
                    relativeMoveKappa = 0;
                    PresrelativeMoveKappa = relativeMoveKappa;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentKappa = KappaObj.localRotation.eulerAngles.y;

                    ThetaObjHome = true;
                    сhekRelativeMoveTheta = false;
                    relativeMoveTheta = 0;
                    PresrelativeMoveTheta = relativeMoveTheta;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentTheta = ThetaObj.localRotation.eulerAngles.y;

                    DetectorObjHome = true;
                    сhekRelativeMoveDetector = false;
                    chekAbsoluteMoveDetector = false;
                    relativeMoveDetector = 0;
                    PresrelativeMoveDetector = relativeMoveDetector;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentDetector = DetectorObj.transform.localPosition.z;

                    break;

                case 2: // Start All (общая скорость)
                    newMoveComponent.allMove = 0;
                    speedOmega = speedAll;
                    speedKappa = speedAll;
                    speedTheta = speedAll;
                    speedDetector = speedAll;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 3: // SpeedAllPhi
                    newMoveComponent.allMove = 0;
                    Debug.Log("1");
                    speedPhi = speedAllPhi;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 4:
                    // Выполните команду для allMove == 4
                    newMoveComponent.allMove = 0;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    phiObjRotate = false;

                    DetectorObjHome = false;
                    сhekRelativeMoveDetector = false;
                    chekAbsoluteMoveDetector = false;
                    speedDetector = 0;
                    relativeMoveDetector = 0;
                    PresrelativeMoveDetector = relativeMoveDetector;
                    speedDetector = 18;

                    phiObjRotate = false;
                    phiObjHome = false;
                    сhekRelativeMovePhi = false;
                    chekAbsoluteMovePhi = false;
                    speedPhi = 0;
                    relativeMovePhi = 0;
                    PresrelativeMovePhi = relativeMovePhi;
                    speedPhi = 36;

                    OmegaObjHome = false;
                    сhekRelativeMoveOmega = false;
                    chekAbsoluteMoveOmega = false;
                    speedOmega = 0;
                    relativeMoveOmega = 0;
                    PresrelativeMoveOmega = relativeMoveOmega;
                    speedOmega = 18;

                    KappaObjHome = false;
                    сhekRelativeMoveKappa = false;
                    chekAbsoluteMoveKappa = false;
                    speedKappa = 0;
                    relativeMoveKappa = 0;
                    PresrelativeMoveKappa = relativeMoveKappa;
                    speedKappa = 18;

                    ThetaObjHome = false;
                    сhekRelativeMoveTheta = false;
                    chekAbsoluteMoveTheta = false;
                    speedTheta = 0;
                    relativeMoveTheta = 0;
                    PresrelativeMoveTheta = relativeMoveTheta;
                    speedTheta = 18;

                    break;

                case 0:
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;
            }

            switch (newMoveComponent.phiMove)
            {
                case 1:
                    // Выполните команду для allMove == 1
                    newMoveComponent.phiMove = 0;
                    phiObjRotate = false;
                    phiObjHome = true;
                    сhekRelativeMovePhi = false;
                    relativeMovePhi = 0;
                    PresrelativeMovePhi = relativeMovePhi;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentPhi = phiObj.localRotation.eulerAngles.x;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 2:
                    // Выполните команду для allMove == 2
                    newMoveComponent.phiMove = 0;
                    phiObjHome = false;
                    phiObjRotate = false;
                    сhekRelativeMovePhi = false;
                    chekAbsoluteMovePhi = false;
                    relativeMovePhi = 0;
                    PresrelativeMovePhi = relativeMovePhi;
                    // Синхронизируем текущий угол с реальным положением объекта
                    currentPhi = phiObj.localRotation.eulerAngles.x;

                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 3:
                    // Выполните команду для allMove == 3
                    newMoveComponent.phiMove = 0;
                    phiObjRotate = true;
                    сhekRelativeMovePhi = false;
                    phiObjHome = false;
                    chekAbsoluteMovePhi = false;
                    relativeMovePhi = 0;
                    PresrelativeMovePhi = relativeMovePhi;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentPhi = phiObj.localRotation.eulerAngles.x;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 4:
                    // Выполните команду для allMove == 4
                    newMoveComponent.phiMove = 0;
                    speedPhi = PrespeedPhi;
                    Time.timeScale = rayTarget.speedState;
                    break;

                case 5:
                    // Выполните команду для allMove == 4
                    newMoveComponent.phiMove = 0;
                    phiObjHome = false;
                    phiObjRotate = false;
                    chekAbsoluteMovePhi = false;
                    сhekRelativeMovePhi = true;
                    relativeMovePhi = PresrelativeMovePhi;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 6:
                    // Выполните команду для allMove == 4
                    newMoveComponent.phiMove = 0;
                    phiObjHome = false;
                    phiObjRotate = false;
                    сhekRelativeMovePhi = false;
                    chekAbsoluteMovePhi = true;
                    relativeMovePhi = 0;
                    absoluteMovePhi = PresabsoluteMovePhi;
                    PresrelativeMovePhi = relativeMovePhi;


                    // Синхронизируем текущий угол с реальным положением объекта
                    currentPhi = phiObj.localRotation.eulerAngles.x;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 7:
                    // Выполните команду для allMove == 4
                    newMoveComponent.phiMove = 0;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    phiObjRotate = false;
                    phiObjHome = false;
                    сhekRelativeMovePhi = false;
                    chekAbsoluteMovePhi = false;
                    speedPhi = 0;
                    relativeMovePhi = 0;
                    PresrelativeMovePhi = relativeMovePhi;
                    speedPhi = 36;
                    break;

                case 0:
                    ;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;
            }


            switch (newMoveComponent.omegaMove)
            {
                case 1:
                    // Выполните команду для allMove == 1
                    newMoveComponent.omegaMove = 0;
                    OmegaObjHome = true;
                    сhekRelativeMoveOmega = false;
                    relativeMoveOmega = 0;
                    PresrelativeMoveOmega = relativeMoveOmega;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentOmega = OmegaObj.localRotation.eulerAngles.z;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 2:
                    // Выполните команду для allMove == 2
                    newMoveComponent.omegaMove = 0;
                    OmegaObjHome = false;
                    сhekRelativeMoveOmega = false;
                    chekAbsoluteMoveOmega = false;
                    relativeMoveOmega = 0;
                    PresrelativeMoveOmega = relativeMoveOmega;
                    // Синхронизируем текущий угол с реальным положением объекта
                    currentOmega = phiObj.localRotation.eulerAngles.z;

                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;


                case 3:
                    // Выполните команду для allMove == 4
                    newMoveComponent.omegaMove = 0;
                    speedOmega = PrespeedOmega;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 4:
                    // Выполните команду для allMove == 4
                    newMoveComponent.omegaMove = 0;
                    OmegaObjHome = false;
                    chekAbsoluteMoveOmega = false;
                    сhekRelativeMoveOmega = true;
                    relativeMoveOmega = PresrelativeMoveOmega;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 5:
                    // Выполните команду для allMove == 4
                    newMoveComponent.omegaMove = 0;
                    OmegaObjHome = false;
                    сhekRelativeMoveOmega = false;
                    chekAbsoluteMoveOmega = true;
                    relativeMoveOmega = 0;
                    absoluteMoveOmega = PresabsoluteMoveOmega;
                    PresrelativeMoveOmega = relativeMoveOmega;


                    // Синхронизируем текущий угол с реальным положением объекта
                    currentOmega = OmegaObj.localRotation.eulerAngles.z;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 6:
                    // Выполните команду для allMove == 4
                    newMoveComponent.omegaMove = 0;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    OmegaObjHome = false;
                    сhekRelativeMoveOmega = false;
                    chekAbsoluteMoveOmega = false;
                    speedOmega = 0;
                    relativeMoveOmega = 0;
                    PresrelativeMoveOmega = relativeMoveOmega;
                    speedOmega = 18;
                    break;

                case 0:
                    ;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;
            }

            switch (newMoveComponent.kappaMove)
            {
                case 1:
                    // Выполните команду для allMove == 1
                    newMoveComponent.kappaMove = 0;
                    KappaObjHome = true;
                    сhekRelativeMoveKappa = false;
                    relativeMoveKappa = 0;
                    PresrelativeMoveKappa = relativeMoveKappa;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentKappa = KappaObj.localRotation.eulerAngles.y;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 2:
                    // Выполните команду для allMove == 2
                    newMoveComponent.kappaMove = 0;
                    KappaObjHome = false;
                    сhekRelativeMoveKappa = false;
                    chekAbsoluteMoveKappa = false;
                    relativeMoveKappa = 0;
                    PresrelativeMoveKappa = relativeMoveKappa;
                    // Синхронизируем текущий угол с реальным положением объекта
                    currentKappa = phiObj.localRotation.eulerAngles.y;

                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;


                case 3:
                    // Выполните команду для allMove == 4
                    newMoveComponent.kappaMove = 0;
                    speedKappa = PrespeedKappa;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 4:
                    // Выполните команду для allMove == 4
                    newMoveComponent.kappaMove = 0;
                    KappaObjHome = false;
                    chekAbsoluteMoveKappa = false;
                    сhekRelativeMoveKappa = true;
                    relativeMoveKappa = PresrelativeMoveKappa;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 5:
                    // Выполните команду для allMove == 4
                    newMoveComponent.kappaMove = 0;
                    KappaObjHome = false;
                    сhekRelativeMoveKappa = false;
                    chekAbsoluteMoveKappa = true;
                    relativeMoveKappa = 0;
                    absoluteMoveKappa = PresabsoluteMoveKappa;
                    PresrelativeMoveKappa = relativeMoveKappa;


                    // Синхронизируем текущий угол с реальным положением объекта
                    currentKappa = KappaObj.localRotation.eulerAngles.y;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 6:
                    // Выполните команду для allMove == 4
                    newMoveComponent.kappaMove = 0;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    KappaObjHome = false;
                    сhekRelativeMoveKappa = false;
                    chekAbsoluteMoveKappa = false;
                    speedKappa = 0;
                    relativeMoveKappa = 0;
                    PresrelativeMoveKappa = relativeMoveKappa;
                    speedKappa = 18;
                    break;

                case 0:
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;
            }


            switch (newMoveComponent.thetaMove)
            {
                case 1:
                    // Выполните команду для allMove == 1
                    newMoveComponent.thetaMove = 0;
                    ThetaObjHome = true;
                    сhekRelativeMoveTheta = false;
                    relativeMoveTheta = 0;
                    PresrelativeMoveTheta = relativeMoveTheta;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentTheta = ThetaObj.localRotation.eulerAngles.y;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 2:
                    // Выполните команду для allMove == 2
                    newMoveComponent.thetaMove = 0;
                    ThetaObjHome = false;
                    сhekRelativeMoveTheta = false;
                    chekAbsoluteMoveTheta = false;
                    relativeMoveTheta = 0;
                    PresrelativeMoveTheta = relativeMoveTheta;
                    // Синхронизируем текущий угол с реальным положением объекта
                    currentTheta = ThetaObj.localRotation.eulerAngles.y;

                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;


                case 3:
                    // Выполните команду для allMove == 4
                    newMoveComponent.thetaMove = 0;
                    speedTheta = PrespeedTheta;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 4:
                    // Выполните команду для allMove == 4
                    newMoveComponent.thetaMove = 0;
                    ThetaObjHome = false;
                    chekAbsoluteMoveTheta = false;
                    сhekRelativeMoveTheta = true;
                    relativeMoveTheta = PresrelativeMoveTheta;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 5:
                    // Выполните команду для allMove == 4
                    newMoveComponent.thetaMove = 0;
                    ThetaObjHome = false;
                    сhekRelativeMoveTheta = false;
                    chekAbsoluteMoveTheta = true;
                    relativeMoveTheta = 0;
                    absoluteMoveTheta = PresabsoluteMoveTheta;
                    PresrelativeMoveTheta = relativeMoveTheta;


                    // Синхронизируем текущий угол с реальным положением объекта
                    currentTheta = ThetaObj.localRotation.eulerAngles.y;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 6:
                    // Выполните команду для allMove == 4
                    newMoveComponent.thetaMove = 0;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    ThetaObjHome = false;
                    сhekRelativeMoveTheta = false;
                    chekAbsoluteMoveTheta = false;
                    speedTheta = 0;
                    relativeMoveTheta = 0;
                    PresrelativeMoveTheta = relativeMoveTheta;
                    speedTheta = 18;
                    break;

                case 0:
                    ;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;
            }


            switch (newMoveComponent.detectorMove)
            {
                case 1:
                    // Выполните команду для allMove == 1
                    newMoveComponent.detectorMove = 0;
                    DetectorObjHome = true;
                    сhekRelativeMoveDetector = false;
                    chekAbsoluteMoveDetector = false;
                    relativeMoveDetector = 0;
                    PresrelativeMoveDetector = relativeMoveDetector;

                    // Синхронизируем текущий угол с реальным положением объекта
                    currentDetector = DetectorObj.transform.localPosition.z;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 2:
                    // Выполните команду для allMove == 2
                    newMoveComponent.detectorMove = 0;
                    DetectorObjHome = false;
                    сhekRelativeMoveDetector = false;
                    chekAbsoluteMoveDetector = false;
                    relativeMoveDetector = 0;
                    PresrelativeMoveDetector = relativeMoveTheta;
                    // Синхронизируем текущий угол с реальным положением объекта
                    currentDetector = DetectorObj.localRotation.eulerAngles.y;

                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;


                case 3:
                    // Выполните команду для allMove == 4
                    newMoveComponent.detectorMove = 0;
                    speedDetector = PrespeedDetector;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 4:
                    // Выполните команду для allMove == 4
                    newMoveComponent.detectorMove = 0;
                    DetectorObjHome = false;
                    chekAbsoluteMoveDetector = false;
                    сhekRelativeMoveDetector = true;
                    relativeMoveDetector = PresrelativeMoveDetector;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 5:
                    // Выполните команду для allMove == 4
                    newMoveComponent.detectorMove = 0;
                    DetectorObjHome = false;
                    сhekRelativeMoveDetector = false;
                    chekAbsoluteMoveDetector = true;
                    relativeMoveDetector = 0;
                    absoluteMoveDetector = PresabsoluteMoveDetector;
                    PresrelativeMoveDetector = relativeMoveDetector;


                    // Синхронизируем текущий угол с реальным положением объекта
                    currentDetector = DetectorObj.localRotation.eulerAngles.y;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;

                case 6:
                    // Выполните команду для allMove == 4
                    newMoveComponent.detectorMove = 0;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    DetectorObjHome = false;
                    сhekRelativeMoveDetector = false;
                    chekAbsoluteMoveDetector = false;
                    speedDetector = 0;
                    relativeMoveDetector = 0;
                    PresrelativeMoveDetector = relativeMoveDetector;
                    speedDetector = 18;
                    break;

                case 0:
                    ;
                    // Заменяем Time.timeScale = rayTarget.speedState на:
                    if (rayTarget != null)
                    {
                        Time.timeScale = rayTarget.GetCurrentTimeScale(); // Теперь масштаб времени будет правильным
                    }
                    else
                    {
                        Time.timeScale = 1f; // На случай, если rayTarget не назначен
                    }
                    break;
            }

        }
        else
        {
            Debug.LogError("newMoveComponent не назначен!");
        }
    }

    void OnStopGameClicked()
    {
        if (UnityServerMode.Enabled) return;
        // Останавливаем игру, устанавливая timeScale в 0
        Time.timeScale = 0;
        // Логика для остановки игры или выполнения других действий
    }



    // Update is called once per frame
    void Update()
    {
        if (UnityServerMode.Enabled) return;
        // Функционал Phi
        if (phiObj != null)
        {


            if (phiObjRotate)
            {
                // Вращаем phiObj локально
                phiObj.transform.Rotate(Vector3.right, speedPhi * Time.deltaTime); // Вращаем вокруг оси X
            }

            if (сhekRelativeMovePhi && phiObj != null)
            {
                if (speedPhi > 0)
                {
                    // Если остался угол для поворота
                    if (Mathf.Abs(relativeMovePhi) > 0.01f)
                    {
                        // Определяем направление и шаг вращения
                        float direction = Mathf.Sign(relativeMovePhi);
                        float step = direction * Mathf.Min(speedPhi * Time.deltaTime, Mathf.Abs(relativeMovePhi));

                        // Вращаем объект
                        phiObj.transform.Rotate(step, 0, 0, Space.Self);

                        // Уменьшаем оставшийся угол
                        relativeMovePhi -= step;
                    }
                    else
                    {
                        // Если вращение завершено, сбрасываем флаг
                        сhekRelativeMovePhi = false;

                        // Обнуляем угол (чтобы не было микроскопических остатков)
                        relativeMovePhi = 0f;
                    }
                }
                else
                {
                    // Если остался угол для поворота
                    if (Mathf.Abs(relativeMovePhi) > 0.01f)
                    {
                        // Определяем направление и шаг вращения
                        float direction = Mathf.Sign(relativeMovePhi);
                        float step = direction * Mathf.Min(speedPhi * Time.deltaTime, Mathf.Abs(relativeMovePhi));

                        // Вращаем объект
                        phiObj.transform.Rotate(step, 0, 0, Space.Self);

                        // Уменьшаем оставшийся угол
                        relativeMovePhi += step;
                    }
                    else
                    {
                        // Если вращение завершено, сбрасываем флаг
                        сhekRelativeMovePhi = false;

                        // Обнуляем угол (чтобы не было микроскопических остатков)
                        relativeMovePhi = 0f;
                    }
                }

            }

            if (phiObjHome)
            {
                if (speedPhi >= 0)
                {
                    // Вращаем phiObj локально
                    phiObj.transform.Rotate(Vector3.right, -speedPhi * Time.deltaTime); // Вращаем вокруг оси X
                }
                else
                {
                    // Вращаем phiObj локально
                    phiObj.transform.Rotate(Vector3.right, speedPhi * Time.deltaTime); // Вращаем вокруг оси X
                }

                // Проверка, достигли ли мы почти угла 0 по оси X
                if (phiObj.transform.localEulerAngles.x <= 0.1f)
                {
                    phiObjHome = false; // Останавливаем возврат после достижения
                }
            }

            if (chekAbsoluteMovePhi && phiObj != null)
            {
                float spidPhicurent = Mathf.Abs(speedPhi);

                if (Mathf.Abs(currentPhi - absoluteMovePhi) > 0.1f) // Проверка на достижение цели
                {
                    // Вычисляем направление вращения
                    float direction = Mathf.Sign(absoluteMovePhi - currentPhi);

                    // Если задана максимальная скорость, используем ее знак
                    if (spidPhicurent != 0) direction = Mathf.Sign(spidPhicurent);

                    float step = Mathf.Abs(spidPhicurent) * Time.deltaTime;
                    currentPhi = Mathf.MoveTowards(currentPhi, absoluteMovePhi, step * direction);

                    phiObj.localRotation = Quaternion.Euler(currentPhi, 0, 0);
                }
            }
        }

        // Функционал Omega с ограничением вращения
        if (OmegaObj != null)
        {
            // --- ОТНОСИТЕЛЬНОЕ ПЕРЕМЕЩЕНИЕ ---
            if (сhekRelativeMoveOmega)
            {


                if (Mathf.Abs(relativeMoveOmega) > 0.01f)
                {

                    // Получаем текущий угол (-180 до 180)
                    float currentAngle = OmegaObj.transform.localEulerAngles.z;
                    if (currentAngle > 180) currentAngle -= 360;

                    // Определяем направление и шаг вращения
                    float direction = Mathf.Sign(relativeMoveOmega);
                    float step = direction * Mathf.Min(
                        Mathf.Abs(speedOmega) * Time.deltaTime,
                        Mathf.Abs(relativeMoveOmega)
                    );

                    // Проверяем границы вращения [-90, 90]
                    float newAngle = currentAngle + step;
                    if (newAngle > 72f)
                    {
                        step = 72f - currentAngle;
                        relativeMoveOmega = 0f;
                    }
                    else if (newAngle < -72f)
                    {
                        step = -72f - currentAngle;
                        relativeMoveOmega = 0f;
                    }

                    // Проверка столкновения перед применением вращения
                    if (rayTarget.IsColliding)
                    {
                        // Определяем разрешенное направление (обратное текущему углу)
                        float allowedDirection = (currentAngle > 0) ? -1f : 1f;

                        // Если пытаемся двигаться в запрещенном направлении — блокируем движение
                        if (Mathf.Sign(step) != allowedDirection)
                        {
                            сhekRelativeMoveOmega = false;
                            relativeMoveOmega = 0f;
                            return;
                        }
                    }

                    // Применяем вращение
                    OmegaObj.transform.Rotate(0, 0, step, Space.Self);
                    relativeMoveOmega -= step;
                    // Проверяем завершение вращения
                    if (Mathf.Abs(relativeMoveOmega) <= 0.01f)
                    {
                        сhekRelativeMoveOmega = false;
                        relativeMoveOmega = 0f;
                    }

                }
                else
                {
                    сhekRelativeMoveOmega = false;
                    relativeMoveOmega = 0f;
                }
            }


            // --- ВОЗВРАТ В НУЛЕВУЮ ПОЗИЦИЮ ---
            if (OmegaObjHome)
            {
                // Получаем текущий угол (-180 до 180)
                float currentAngle = OmegaObj.transform.localEulerAngles.z;
                if (currentAngle > 180) currentAngle -= 360;

                // Определяем направление к нулю
                float rotationDirection = currentAngle > 0 ? -1f : 1f;
                float rotationStep = rotationDirection * Mathf.Abs(speedOmega) * Time.deltaTime;

                // Применяем вращение с проверкой границ
                float newAngle = currentAngle + rotationStep;
                if ((rotationDirection < 0 && newAngle < 0) ||
                    (rotationDirection > 0 && newAngle > 0))
                {
                    rotationStep = -currentAngle;
                }

                OmegaObj.transform.Rotate(0, 0, rotationStep, Space.Self);

                // Проверка достижения нуля
                if (Mathf.Abs(OmegaObj.transform.localEulerAngles.z) < 0.1f)
                {
                    OmegaObj.transform.localRotation = Quaternion.Euler(
                        OmegaObj.transform.localEulerAngles.x,
                        OmegaObj.transform.localEulerAngles.y,
                        0f
                    );
                    OmegaObjHome = false;
                }
            }

            // --- АБСОЛЮТНОЕ ПЕРЕМЕЩЕНИЕ ---
            if (chekAbsoluteMoveOmega)
            {
                // Ограничиваем целевую позицию
                float targetOmega = Mathf.Clamp(absoluteMoveOmega, -72f, 72f);

                // Текущий угол (-180 до 180)
                float currentAngle = OmegaObj.transform.localEulerAngles.z;
                if (currentAngle > 180) currentAngle -= 360;

                if (Mathf.Abs(currentAngle - targetOmega) > 0.1f)
                {
                    float direction = Mathf.Sign(targetOmega - currentAngle);
                    float step = direction * Mathf.Min(
                        Mathf.Abs(speedOmega) * Time.deltaTime,
                        Mathf.Abs(targetOmega - currentAngle)
                    );

                    // Применяем вращение
                    OmegaObj.transform.localRotation = Quaternion.Euler(
                        60,
                        90,
                        Mathf.Clamp(currentAngle + step, -72f, 72f)
                    );

                    if (rayTarget.IsColliding)
                    {
                        // Определяем разрешенное направление (обратное текущему углу)
                        float allowedDirection = (currentAngle > 0) ? -1f : 1f;

                        // Если пытаемся двигаться в запрещенном направлении
                        if (Mathf.Sign(step) != allowedDirection)
                        {
                            chekAbsoluteMoveOmega = false;
                            return;
                        }
                    }
                }
                else
                {
                    chekAbsoluteMoveOmega = false;
                }
            }
        }

        // Функционал Kappa с ограничением вращения [-72, 72]
        if (KappaObj != null)
        {
            // --- ОТНОСИТЕЛЬНОЕ ПЕРЕМЕЩЕНИЕ ДЛЯ KAPPA ---
            if (сhekRelativeMoveKappa)
            {
                if (Mathf.Abs(relativeMoveKappa) > 0.01f)
                {
                    // Получаем текущий угол (-180 до 180)
                    float currentAngle = KappaObj.transform.localEulerAngles.y;
                    if (currentAngle > 180) currentAngle -= 360;

                    // Определяем направление вращения (1 или -1)
                    float direction = Mathf.Sign(relativeMoveKappa);
                    // Рассчитываем шаг вращения
                    float step = direction * Mathf.Min(
                        Mathf.Abs(speedKappa) * Time.deltaTime,
                        Mathf.Abs(relativeMoveKappa)
                    );

                    // Проверяем столкновение и направление движения
                    if (rayTarget.IsColliding)
                    {
                        // Определяем разрешенное направление (обратное текущему углу)
                        float allowedDirection = (currentAngle > 0) ? -1f : 1f;

                        // Если пытаемся двигаться в запрещенном направлении
                        if (Mathf.Sign(step) != allowedDirection)
                        {
                            сhekRelativeMoveKappa = false;
                            relativeMoveKappa = 0f;
                            return;
                        }
                    }


                    // Проверяем, не выйдем ли за пределы [-72, 72]
                    float newAngle = currentAngle + step;
                    if (newAngle > 99f)
                    {
                        step = 99f - currentAngle;
                        relativeMoveKappa = 0f;
                    }
                    else if (newAngle < -90f)
                    {
                        step = -90f - currentAngle;
                        relativeMoveKappa = 0f;
                    }

                    // Вращаем объект
                    KappaObj.transform.Rotate(0, step, 0, Space.Self);

                    // Обновляем оставшийся угол поворота
                    relativeMoveKappa -= step;

                    // Если поворот почти завершен
                    if (Mathf.Abs(relativeMoveKappa) <= 0.01f)
                    {
                        сhekRelativeMoveKappa = false;
                        relativeMoveKappa = 0f;
                    }
                }
                else
                {
                    // Если угол поворота слишком мал, завершаем движение
                    сhekRelativeMoveKappa = false;
                    relativeMoveKappa = 0f;
                }
            }

            // --- ВОЗВРАТ В НУЛЕВУЮ ПОЗИЦИЮ ---
            if (KappaObjHome)
            {
                // Получаем текущий угол (-180 до 180)
                float currentAngle = KappaObj.transform.localEulerAngles.y;
                if (currentAngle > 180) currentAngle -= 360;

                // Определяем направление к нулю
                float rotationDirection = currentAngle > 0 ? -1f : 1f;
                float rotationStep = rotationDirection * Mathf.Abs(speedKappa) * Time.deltaTime;

                // Применяем вращение с проверкой границ
                float newAngle = currentAngle + rotationStep;
                if ((rotationDirection < 0 && newAngle < 0) ||
                    (rotationDirection > 0 && newAngle > 0))
                {
                    rotationStep = -currentAngle;
                }

                KappaObj.transform.Rotate(0, rotationStep, 0, Space.Self);
                // Проверка достижения нуля
                if (Mathf.Abs(KappaObj.transform.localEulerAngles.y) < 0.1f)
                {
                    KappaObj.transform.localRotation = Quaternion.Euler(
                        0,
                        0,
                        KappaObj.transform.localEulerAngles.z
                    );
                    KappaObjHome = false;
                }
            }

            // --- АБСОЛЮТНОЕ ПЕРЕМЕЩЕНИЕ ---
            if (chekAbsoluteMoveKappa)
            {
                // Ограничиваем целевую позицию [-72, 72]
                float targetKappa = Mathf.Clamp(absoluteMoveKappa, -90f, 99f);

                // Текущий угол (-180 до 180)
                float currentAngle = KappaObj.transform.localEulerAngles.y;
                if (currentAngle > 180) currentAngle -= 360;

                if (Mathf.Abs(currentAngle - targetKappa) > 0.1f)
                {
                    float direction = Mathf.Sign(targetKappa - currentAngle);
                    float step = direction * Mathf.Min(
                        Mathf.Abs(speedKappa) * Time.deltaTime,
                        Mathf.Abs(targetKappa - currentAngle)
                    );

                    // Применяем вращение с ограничением
                    KappaObj.transform.localRotation = Quaternion.Euler(
                        0,
                        Mathf.Clamp(currentAngle + step, -90f, 99f),
                        0
                    );

                    if (rayTarget.IsColliding)
                    {
                        // Определяем разрешенное направление (обратное текущему углу)
                        float allowedDirection = (currentAngle > 0) ? -1f : 1f;

                        // Если пытаемся двигаться в запрещенном направлении
                        if (Mathf.Sign(step) != allowedDirection)
                        {
                            chekAbsoluteMoveKappa = false;
                            return;
                        }
                    }
                }
                else
                {
                    chekAbsoluteMoveKappa = false;
                }
            }
        }

        // Функционал Theta с ограничением вращения [-65, 90]
        if (ThetaObj != null)
        {
            // --- ОТНОСИТЕЛЬНОЕ ПЕРЕМЕЩЕНИЕ ---
            if (сhekRelativeMoveTheta)
            {
                if (Mathf.Abs(relativeMoveTheta) > 0.01f)
                {
                    // Получаем текущий угол (-180 до 180)
                    float currentAngle = ThetaObj.transform.localEulerAngles.y;
                    if (currentAngle > 180) currentAngle -= 360;

                    // Определяем направление и шаг вращения
                    float direction = Mathf.Sign(relativeMoveTheta);
                    float step = direction * Mathf.Min(
                        Mathf.Abs(speedTheta) * Time.deltaTime,
                        Mathf.Abs(relativeMoveTheta)
                    );
                    // Проверяем столкновение и направление движения
                    if (rayTarget.IsColliding)
                    {
                        // Определяем разрешенное направление (обратное текущему углу)
                        float allowedDirection = (currentAngle > 0) ? -1f : 1f;

                        // Если пытаемся двигаться в запрещенном направлении
                        if (Mathf.Sign(step) != allowedDirection)
                        {
                            // Если вращение завершено, сбрасываем флаг
                            сhekRelativeMoveTheta = false;

                            // Обнуляем угол (чтобы не было микроскопических остатков)
                            relativeMoveTheta = 0f;
                            return;
                        }
                    }
                    // Проверяем, не выйдем ли за пределы [-90, 90]
                    float newAngle = currentAngle + step;
                    if (newAngle > 90f)
                    {
                        step = 90f - currentAngle;
                        relativeMoveTheta = 0f;
                    }
                    else if (newAngle < -90f)
                    {
                        step = -90f - currentAngle;
                        relativeMoveTheta = 0f;
                    }
                    if (speedTheta > 0)
                    {

                        // Вращаем объект
                        ThetaObj.transform.Rotate(0, step, 0, Space.Self);

                        // Корректируем оставшееся расстояние
                        relativeMoveTheta -= step;
                    }
                    else
                    {

                        // Если остался угол для поворота
                        if (Mathf.Abs(relativeMoveTheta) > 0.01f)
                        {
                            // Определяем направление и шаг вращения
                            float directions = Mathf.Sign(relativeMoveTheta);
                            float steps = directions * Mathf.Min(speedTheta * Time.deltaTime, Mathf.Abs(relativeMoveTheta));

                            // Вращаем объект
                            ThetaObj.transform.Rotate(0, steps, 0, Space.Self);

                            // Уменьшаем оставшийся угол
                            relativeMoveTheta += steps;
                        }
                        else
                        {
                            // Если вращение завершено, сбрасываем флаг
                            сhekRelativeMoveTheta = false;

                            // Обнуляем угол (чтобы не было микроскопических остатков)
                            relativeMoveTheta = 0f;
                        }
                    }



                    if (Mathf.Abs(relativeMoveTheta) <= 0.01f)
                    {
                        сhekRelativeMoveTheta = false;
                        relativeMoveTheta = 0f;
                    }
                }
                else
                {
                    сhekRelativeMoveTheta = false;
                    relativeMoveTheta = 0f;
                }
            }

            // --- ВОЗВРАТ В НУЛЕВУЮ ПОЗИЦИЮ ---
            if (ThetaObjHome)
            {
                // Получаем текущий угол (-180 до 180)
                float currentAngle = ThetaObj.transform.localEulerAngles.y;
                if (currentAngle > 180) currentAngle -= 360;

                // Определяем направление к нулю
                float rotationDirection = currentAngle > 0 ? -1f : 1f;
                float rotationStep = rotationDirection * Mathf.Abs(speedTheta) * Time.deltaTime;

                // Применяем вращение с проверкой границ
                float newAngle = currentAngle + rotationStep;
                if ((rotationDirection < 0 && newAngle < 0) ||
                    (rotationDirection > 0 && newAngle > 0))
                {
                    rotationStep = -currentAngle;
                }

                ThetaObj.transform.Rotate(0, rotationStep, 0, Space.Self);

                // Проверка достижения нуля
                if (Mathf.Abs(ThetaObj.transform.localEulerAngles.y) < 0.1f)
                {
                    ThetaObj.transform.localRotation = Quaternion.Euler(
                        0,
                        0,
                        ThetaObj.transform.localEulerAngles.z
                    );
                    ThetaObjHome = false;
                }
            }

            // --- АБСОЛЮТНОЕ ПЕРЕМЕЩЕНИЕ ---
            if (chekAbsoluteMoveTheta)
            {
                // Ограничиваем целевую позицию [-72, 72]
                float targetTheta = Mathf.Clamp(absoluteMoveTheta, -90f, 90f);

                // Текущий угол (-180 до 180)
                float currentAngle = ThetaObj.transform.localEulerAngles.y;
                if (currentAngle > 180) currentAngle -= 360;

                if (Mathf.Abs(currentAngle - targetTheta) > 0.1f)
                {
                    float direction = Mathf.Sign(targetTheta - currentAngle);
                    float step = direction * Mathf.Min(
                        Mathf.Abs(speedTheta) * Time.deltaTime,
                        Mathf.Abs(targetTheta - currentAngle)
                    );

                    // Применяем вращение с ограничением
                    ThetaObj.transform.localRotation = Quaternion.Euler(
                        0,
                        Mathf.Clamp(currentAngle + step, -90f, 90f),
                        0
                    );

                    if (rayTarget.IsColliding)
                    {
                        // Определяем разрешенное направление (обратное текущему углу)
                        float allowedDirection = (currentAngle > 0) ? -1f : 1f;

                        // Если пытаемся двигаться в запрещенном направлении
                        if (Mathf.Sign(step) != allowedDirection)
                        {
                            chekAbsoluteMoveTheta = false;
                            return;
                        }
                    }
                }
                else
                {
                    chekAbsoluteMoveTheta = false;

                }
            }
        }

        // Функционал перемещения по Y
        if (DetectorObj != null)
        {
            float RelativeInputScale = 0.0029f; // 1 ввод = 0.0029 Unity-единиц
            float minPosition = 0f; // Минимальная позиция (Z не может быть < 0)
            float maxPosition = 100f * RelativeInputScale; // Макс. позиция (100 вводов = 0.29 в Unity)

            if (сhekRelativeMoveDetector)
            {
                // Текущая позиция в Unity-единицах
                float currentZ = DetectorObj.transform.localPosition.z;

                // Целевая позиция в Unity-единицах (с учетом ввода)
                float targetInput = relativeMoveDetector * RelativeInputScale;
                float targetZ = currentZ + targetInput;

                // Ограничиваем целевую позицию [0 ... maxPosition]
                targetZ = Mathf.Clamp(targetZ, minPosition, maxPosition);

                // Рассчитываем шаг с учетом скорости (но не дальше targetZ)
                float step = Mathf.Sign(targetInput) * speedDetector * RelativeInputScale * Time.deltaTime;
                float newZ = Mathf.MoveTowards(currentZ, targetZ, Mathf.Abs(step));

                // Применяем перемещение
                DetectorObj.transform.localPosition = new Vector3(
                    DetectorObj.transform.localPosition.x,
                    DetectorObj.transform.localPosition.y,
                    newZ
                );

                // Обновляем оставшееся расстояние (в единицах ввода)
                float remainingInput = (targetZ - newZ) / RelativeInputScale;
                relativeMoveDetector = Mathf.Sign(relativeMoveDetector) * Mathf.Abs(remainingInput);

                // Если достигли цели (или почти достигли)
                if (Mathf.Abs(newZ - targetZ) < 0.0001f || Mathf.Abs(relativeMoveDetector) < 0.001f)
                {
                    сhekRelativeMoveDetector = false;
                    relativeMoveDetector = 0f;
                }
            }

            // --- ВОЗВРАТ В НУЛЕВУЮ ПОЗИЦИЮ ---
            if (DetectorObjHome && DetectorObj != null)
            {
                if (DetectorObj.transform.localPosition.z > 0.0001f)
                {
                    DetectorObj.transform.Translate(0, RelativeInputScale * speedDetector * Time.deltaTime, 0, Space.Self);
                    if (DetectorObj.transform.localPosition.z <= 0.0001f)
                    {
                        DetectorObjHome = false;
                    }
                }
                else if (DetectorObj.transform.localPosition.z < 0.0001f)
                {
                    DetectorObj.transform.Translate(0, -RelativeInputScale * speedDetector * Time.deltaTime, 0, Space.Self);
                    if (DetectorObj.transform.localPosition.z >= 0.0001f)
                    {
                        DetectorObjHome = false;
                    }
                }

            }

            // --- АБСОЛЮТНОЕ ПЕРЕМЕЩЕНИЕ ---
            if (chekAbsoluteMoveDetector && DetectorObj != null)
            {
                // Константа масштабирования (1 ввод = 0.01 Unity)
                const float InputScale = 0.0029f;


                // Конвертируем цель в Unity-координаты
                float targetY = absoluteMoveDetector * InputScale;

                // Текущая позиция в Unity-координатах
                float currentY = DetectorObj.transform.localPosition.z;

                if (Mathf.Abs(currentY - targetY) > 0.001f) // Порог точности
                {
                    // Масштабируем скорость (speedDetector вводится как 1 = 0.01/сек)
                    float scaledSpeed = Mathf.Abs(speedDetector) * InputScale;

                    // Вычисляем новую позицию
                    float newY = Mathf.MoveTowards(
                        currentY,
                        targetY,
                        scaledSpeed * Time.deltaTime
                    );

                    // Применяем перемещение
                    DetectorObj.transform.localPosition = new Vector3(
                        DetectorObj.transform.localPosition.x,
                        DetectorObj.transform.localPosition.y,
                        newY

                    );

                    // Проверяем столкновение перед перемещением
                    if (rayTarget.IsColliding)
                    {
                        // Определяем направление движения (к цели)
                        float moveDirection = Mathf.Sign(newY - currentY);

                        // Если движемся вперед (в сторону коллизии)
                        if (moveDirection > 0)
                        {
                            // Блокируем движение
                            chekAbsoluteMoveDetector = false;
                            return;
                        }
                    }

                    // Сбрасываем PresabsoluteMoveDetector при достижении
                    if (PresabsoluteMoveDetector != 0 && Mathf.Approximately(newY, targetY))
                    {
                        PresabsoluteMoveDetector = 0f;
                    }
                }

            }
        }
    }

    public void ResetAllAxis()
    {
        // Сброс положения всех осей
        currentPhi = 0f;
        currentOmega = 0f;
        currentKappa = 0f;
        currentTheta = 0f;
        
        if (phiObj != null) phiObj.rotation = Quaternion.identity;
        if (OmegaObj != null) OmegaObj.rotation = Quaternion.identity;
        if (KappaObj != null) KappaObj.rotation = Quaternion.identity;
        if (ThetaObj != null) ThetaObj.rotation = Quaternion.identity;
        if (DetectorObj != null) DetectorObj.rotation = Quaternion.identity;
        
        Debug.Log("All axes reset to home position");
    }

    public void StopAllAxis()
    {
        // Остановка всех осей
        phiObjRotate = false;
        
        Debug.Log("All axes stopped");
    }


    // Called only on the Unity main thread by the explicit server scenario.
    public string ServerCalculationState = "idle";
    public bool HasMotion => phiObjRotate || phiObjHome || сhekRelativeMovePhi || chekAbsoluteMovePhi ||
        OmegaObjHome || сhekRelativeMoveOmega || chekAbsoluteMoveOmega ||
        KappaObjHome || сhekRelativeMoveKappa || chekAbsoluteMoveKappa ||
        ThetaObjHome || сhekRelativeMoveTheta || chekAbsoluteMoveTheta ||
        DetectorObjHome || сhekRelativeMoveDetector || chekAbsoluteMoveDetector;

    public void StopServerMotion()
    {
        phiObjRotate = phiObjHome = сhekRelativeMovePhi = chekAbsoluteMovePhi = false;
        OmegaObjHome = сhekRelativeMoveOmega = chekAbsoluteMoveOmega = false;
        KappaObjHome = сhekRelativeMoveKappa = chekAbsoluteMoveKappa = false;
        ThetaObjHome = сhekRelativeMoveTheta = chekAbsoluteMoveTheta = false;
        DetectorObjHome = сhekRelativeMoveDetector = chekAbsoluteMoveDetector = false;
        if (newMoveComponent != null)
            newMoveComponent.allMove = newMoveComponent.phiMove = newMoveComponent.omegaMove =
            newMoveComponent.kappaMove = newMoveComponent.thetaMove = newMoveComponent.detectorMove = 0;
    }

    public void BeginServerPhi(float angleDeg, float speedDegPerSec)
    {
        StopServerMotion();
        PresrelativeMovePhi = relativeMovePhi = angleDeg;
        PrespeedPhi = speedPhi = speedDegPerSec;
        сhekRelativeMovePhi = true;
    }

    public void StepServerPhi(float deltaTime)
    {
        // Same positive-speed relative-Phi formula and tolerance as the interactive branch.
        if (!сhekRelativeMovePhi) return;
        if (Mathf.Abs(relativeMovePhi) > 0.01f)
        {
            float step = Mathf.Sign(relativeMovePhi) * Mathf.Min(speedPhi * deltaTime, Mathf.Abs(relativeMovePhi));
            if (step >= 180f) throw new InvalidOperationException("Frame rotation is too large to unwrap safely");
            phiObj.Rotate(step, 0, 0, Space.Self);
            relativeMovePhi -= step;
        }
        else
        {
            сhekRelativeMovePhi = false;
            relativeMovePhi = 0;
        }
    }
}
