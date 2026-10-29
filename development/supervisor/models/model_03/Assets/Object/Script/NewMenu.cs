using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class NewMenu : MonoBehaviour
{
    /*
     Основанной функционал управления меню, ввод пользователя данных. 
    Показать скрыть элементы. Выбор функций toggle, сравнить какой выбран toggle
     */
    public GameObject panelShow; // Показать панель
    public Button ButtonShow; // Кнопка для переключения панели
    public NewMoveComponent newMoveComponent; // Установка значения
    public MoveObj moveObj; // Ссылка на компонент NewMenu

    public Button ButtonAll; // Кнопка для переключения панели All
    public Button ButtonPhi; // Кнопка для переключения панели Phi
    public Button ButtonOmega; // Кнопка для переключения панели Omega
    public Button ButtonKappa; // Кнопка для переключения панели Kappa
    public Button ButtonTheta; // Кнопка для переключения панели Theta
    public Button ButtonDetector; // Кнопка для переключения панели Detectors

    public GameObject panelAll; // Показать панель All
    public GameObject panelPhi; // Показать панель Phi
    public GameObject panelOmega; // Показать панель Omega
    public GameObject panelKappa; // Показать панель Kappa
    public GameObject panelTheta; // Показать панель Theta
    public GameObject panelDetector; // Показать панель Detector

    public GameObject[] inputFieldChange; // Массив для хранения Input
    public GameObject[] textChange; // Массив для хранения Text

    public Toggle[] toggleHomes; // Массив для хранения ссылок на Toggle
    public Toggle[] toggleSpeed; // Ссылка на toggleSpeed
    public Toggle[] toggleReset; // Ссылка на toggleReset
    public Toggle[] toggleStop; // Ссылка на toggleStop
    public Toggle[] toggleRelativeMove; // Ссылка на toggle Relative Move
    public Toggle[] toggleAbsoluteMove; // Ссылка на toggle Absolute Move

    public bool IstoggleSpeed = false;
    public bool IstoggleRelativeMove = false;
    public bool IstoggleAbsoluteMove = false;
    public bool IstoggleSpeedPhi = false;


    public Button ButtonStart; // Кнопка для переключения панели Start
    public Button ButtonStop; // Кнопка для переключения панели Stop
    public Button Button3D; // Кнопка для переключения панели 3D


    public Toggle[] selectedToggleArray; // Переменная для хранения выбранного массива Toggle

    void Start()
    {
        ButtonShow.onClick.AddListener(OnButtonClickShow);
        ButtonAll.onClick.AddListener(() => ShowPanel(panelAll, false, false));
        ButtonPhi.onClick.AddListener(() => ShowPanel(panelPhi, false, false));
        ButtonOmega.onClick.AddListener(() => ShowPanel(panelOmega, false, false));
        ButtonKappa.onClick.AddListener(() => ShowPanel(panelKappa, false, false));
        ButtonTheta.onClick.AddListener(() => ShowPanel(panelTheta, false, false));
        ButtonDetector.onClick.AddListener(() => ShowPanel(panelDetector, false, false));

        // Подписка на событие onValueChanged для каждого Toggle в массиве toggleSpeed
        foreach (Toggle toggle in toggleSpeed)
        {
            toggle.onValueChanged.AddListener(OnToggleSpeedValueChanged);
        }

        // Подписка на событие onValueChanged для каждого Toggle в массиве toggleReset
        foreach (Toggle toggle in toggleReset)
        {
            toggle.onValueChanged.AddListener(OnToggleResetValueChanged);
        }

        // Подписка на событие onValueChanged для каждого Toggle в массиве toggleHome
        foreach (Toggle toggle in toggleHomes)
        {
            toggle.onValueChanged.AddListener(OnToggleHomeValueChanged);
        }

        // Подписка на событие onValueChanged для каждого Toggle в массиве toggleStop
        foreach (Toggle toggle in toggleStop)
        {
            toggle.onValueChanged.AddListener(OnToggleStopValueChanged);
        }

        // Подписка на событие onValueChanged для каждого Toggle в массиве toggleStop
        foreach (Toggle toggle in toggleRelativeMove)
        {
            toggle.onValueChanged.AddListener(OnToggleRelativeMoveValueChanged);
        }

        // Подписка на событие onValueChanged для каждого Toggle в массиве toggleStop
        foreach (Toggle toggle in toggleAbsoluteMove)
        {
            toggle.onValueChanged.AddListener(OnToggleAbsoluteMoveValueChanged);
        }
    }

    void OnButtonClickShow()
    {
        panelShow.SetActive(!panelShow.activeSelf);
    }

    void ShowPanel(GameObject panelToShow, bool showInputFields, bool showTextchange)
    {
        // Проверяем, активна ли панель
        if (panelToShow.activeSelf)
        {
            // Если панель уже активна, скрываем её
            panelToShow.SetActive(false);
        }
        else
        {
            // Скрываем все панели
            panelAll.SetActive(false);
            panelPhi.SetActive(false);
            panelOmega.SetActive(false);
            panelKappa.SetActive(false);
            panelTheta.SetActive(false);
            panelDetector.SetActive(false);

            // Показываем только выбранную панель
            panelToShow.SetActive(true);
        }

        // Управляем видимостью InputField в зависимости от состояния showInputFields
        ToggleInputFields(showInputFields);

        ShowText(showTextchange);
    }

    void ToggleInputFields(bool show)
    {
        foreach (GameObject inputField in inputFieldChange)
        {
            inputField.SetActive(show);
        }
    }

    void ShowText(bool showTextchange)
    {
        foreach (GameObject textChangeNew in textChange)
        {
            textChangeNew.SetActive(showTextchange);
        }
    }



    // Метод, который будет вызван при изменении состояния любого Toggle из toggleSpeed
    void OnToggleSpeedValueChanged(bool isOn)
    {
        if (isOn)
        {
           
            selectedToggleArray = toggleSpeed; // Сохраняем ссылку на выбранный массив
            
            // Отключить все остальные Toggle в toggleSpeed
            foreach (Toggle toggle in toggleSpeed)
            {
                IstoggleRelativeMove = false;
                IstoggleAbsoluteMove = false;
                IstoggleSpeedPhi = false;
                if (toggle.isOn && toggle != UnityEngine.EventSystems.EventSystem.current.currentSelectedGameObject.GetComponent<Toggle>())
                {
                    toggle.isOn = false;   
                }

                if (toggle.isOn && toggle.name == "SpeedPhi")
                {
                    newMoveComponent.uiInputFieldPhi.text = moveObj.speedPhi.ToString(); // Устанавливаем текст
                    IstoggleSpeed = true;
                }
                if (toggle.isOn && toggle.name == "SpeedOmega")
                {
                    newMoveComponent.uiInputFieldOmega.text = moveObj.speedOmega.ToString(); // Устанавливаем текст
                    IstoggleSpeed = true;
                }

                if (toggle.isOn && toggle.name == "SpeedKappa")
                {
                    newMoveComponent.uiInputFieldKappa.text = moveObj.speedKappa.ToString(); // Устанавливаем текст
                    IstoggleSpeed = true;
                }

                if (toggle.isOn && toggle.name == "SpeedTheta")
                {
                    newMoveComponent.uiInputFieldTheta.text = moveObj.speedTheta.ToString(); // Устанавливаем текст
                    IstoggleSpeed = true;
                }

                if (toggle.isOn && toggle.name == "SpeedDetector")
                {
                    newMoveComponent.uiInputFieldDetector.text = moveObj.speedDetector.ToString(); // Устанавливаем текст
                    IstoggleSpeed = true;
                }
                if (toggle.isOn && toggle.name == "SpeedAll")
                {
                    newMoveComponent.uiInputFieldAll.text = moveObj.speedAll.ToString(); // Устанавливаем текст
                    IstoggleSpeed = true;

                }
                if (toggle.isOn && toggle.name == "SpeedAllPhi")
                {
                    newMoveComponent.uiInputFieldAll.text = moveObj.speedAllPhi.ToString(); // Устанавливаем текст
                    IstoggleSpeedPhi = true;
                }
            }

            // Показываем InputField и Text, если выбран Toggle Speed
            ToggleInputFields(true);
            ShowText(true);

            // Отключить все Toggle в toggleHomes, toggleReset, toggleStop, toggleRelativeMove, toggleAbsoluteMove
            DisableAllToggles(toggleHomes);
            DisableAllToggles(toggleReset);
            DisableAllToggles(toggleStop);
            DisableAllToggles(toggleRelativeMove);
            DisableAllToggles(toggleAbsoluteMove);


          
        }

    }

    
   

    // Метод, который будет вызван при изменении состояния любого Toggle из toggleReset
    void OnToggleResetValueChanged(bool isOn)
    {
        if (isOn)
        {
            selectedToggleArray = toggleReset; // Сохраняем ссылку на выбранный массив
            
            // Отключить все остальные Toggle в toggleReset
            foreach (Toggle toggle in toggleReset)
            {
                IstoggleRelativeMove = false;
                IstoggleSpeed = false;
                if (toggle.isOn && toggle != UnityEngine.EventSystems.EventSystem.current.currentSelectedGameObject.GetComponent<Toggle>())
                {
                    toggle.isOn = false;
                }
            }

            // Если Toggle Speed отключен, скрываем InputField и Text
            ToggleInputFields(false);
            ShowText(false);

            // Отключить все Toggle в toggleHomes и toggleSpeed
            DisableAllToggles(toggleHomes);
            DisableAllToggles(toggleSpeed);
            DisableAllToggles(toggleStop);
            DisableAllToggles(toggleRelativeMove);
            DisableAllToggles(toggleAbsoluteMove);
        }
    }

    // Метод, который будет вызван при изменении состояния любого Toggle из toggleHome
    void OnToggleHomeValueChanged(bool isOn)
    {
        if (isOn)
        {
            selectedToggleArray = toggleHomes; // Сохраняем ссылку на выбранный массив
       
            // Отключить все остальные Toggle в toggleHome
            foreach (Toggle toggle in toggleHomes)
            {
                IstoggleRelativeMove = false;
                IstoggleSpeed = false;
                if (toggle.isOn && toggle != UnityEngine.EventSystems.EventSystem.current.currentSelectedGameObject.GetComponent<Toggle>())
                {
                    toggle.isOn = false;
                }
            }

            // Если Toggle Speed отключен, скрываем InputField и Text
            ToggleInputFields(false);
            ShowText(false);

            // Отключить все Toggle в toggleSpeed и toggleReset
            DisableAllToggles(toggleSpeed);
            DisableAllToggles(toggleReset);
            DisableAllToggles(toggleStop);
            DisableAllToggles(toggleRelativeMove);
            DisableAllToggles(toggleAbsoluteMove);
        }
    }

    // Метод, который будет вызван при изменении состояния любого Toggle из toggleHome
    void OnToggleStopValueChanged(bool isOn)
    {
        if (isOn)
        {
            selectedToggleArray = toggleStop; // Сохраняем ссылку на выбранный массив

            // Отключить все остальные Toggle в toggleHome
            foreach (Toggle toggle in toggleStop)
            {
                IstoggleRelativeMove = false;
                IstoggleSpeed = false;
                if (toggle.isOn && toggle != UnityEngine.EventSystems.EventSystem.current.currentSelectedGameObject.GetComponent<Toggle>())
                {
                    toggle.isOn = false;
                }
            }

            // Если Toggle Speed отключен, скрываем InputField и Text
            ToggleInputFields(false);
            ShowText(false);

            // Отключить все Toggle в toggleSpeed и toggleReset
            DisableAllToggles(toggleSpeed);
            DisableAllToggles(toggleReset);
            DisableAllToggles(toggleHomes);
            DisableAllToggles(toggleRelativeMove);
            DisableAllToggles(toggleAbsoluteMove);
        }
    }

    // Метод, который будет вызван при изменении состояния любого Toggle из toggleHome
    void OnToggleRelativeMoveValueChanged(bool isOn)
    {
        if (isOn)
        {
            selectedToggleArray = toggleRelativeMove; // Сохраняем ссылку на выбранный массив
         
            // Отключить все остальные Toggle в toggleHome
            foreach (Toggle toggle in toggleRelativeMove)
            {
                IstoggleSpeed = false;
                IstoggleAbsoluteMove = false;
                IstoggleSpeedPhi = false;
                if (toggle.isOn && toggle != UnityEngine.EventSystems.EventSystem.current.currentSelectedGameObject.GetComponent<Toggle>())
                {
                    toggle.isOn = false;
                }
                if (toggle.isOn && toggle.name == "RelativeMovePhi")
                {
                   
                    newMoveComponent.uiInputFieldPhi.text = moveObj.PresrelativeMovePhi.ToString(); // Устанавливаем текст
                    IstoggleRelativeMove = true;
                }

                if (toggle.isOn && toggle.name == "RelativeMoveOmega")
                {

                    newMoveComponent.uiInputFieldOmega.text = moveObj.PresrelativeMoveOmega.ToString(); // Устанавливаем текст
                    IstoggleRelativeMove = true;
                }

                if (toggle.isOn && toggle.name == "RelativeMoveKappa")
                {

                    newMoveComponent.uiInputFieldKappa.text = moveObj.PresrelativeMoveKappa.ToString(); // Устанавливаем текст
                    IstoggleRelativeMove = true;
                }

                if (toggle.isOn && toggle.name == "RelativeMoveTheta")
                {

                    newMoveComponent.uiInputFieldTheta.text = moveObj.PresrelativeMoveTheta.ToString(); // Устанавливаем текст
                    IstoggleRelativeMove = true;
                }

                if (toggle.isOn && toggle.name == "RelativeMoveDetector")
                {

                    newMoveComponent.uiInputFieldDetector.text = moveObj.PresrelativeMoveDetector.ToString(); // Устанавливаем текст
                    IstoggleRelativeMove = true;
                }
            }

            // Показываем InputField и Text, если выбран Toggle Speed
            ToggleInputFields(true);
            ShowText(true);

            // Отключить все Toggle в toggleSpeed и toggleReset
            DisableAllToggles(toggleSpeed);
            DisableAllToggles(toggleReset);
            DisableAllToggles(toggleHomes);
            DisableAllToggles(toggleStop);
            DisableAllToggles(toggleAbsoluteMove);
        }


    }
    // Метод, который будет вызван при изменении состояния любого Toggle из toggleHome
    void OnToggleAbsoluteMoveValueChanged(bool isOn)
    {
        if (isOn)
        {
            selectedToggleArray = toggleAbsoluteMove; // Сохраняем ссылку на выбранный массив

            // Отключить все остальные Toggle в toggleHome
            foreach (Toggle toggle in toggleAbsoluteMove)
            {
                IstoggleRelativeMove = false;
                IstoggleSpeed = false;
                IstoggleSpeedPhi = false;
                if (toggle.isOn && toggle != UnityEngine.EventSystems.EventSystem.current.currentSelectedGameObject.GetComponent<Toggle>())
                {
                    
                    toggle.isOn = false;
                }

                if (toggle.isOn && toggle.name == "AbsoluteMovePhi")
                {
                    newMoveComponent.uiInputFieldPhi.text = moveObj.absoluteMovePhi.ToString(); // Устанавливаем текст
                    IstoggleAbsoluteMove = true;
                }

                if (toggle.isOn && toggle.name == "AbsoluteMoveOmega")
                {
                    newMoveComponent.uiInputFieldOmega.text = moveObj.absoluteMoveOmega.ToString(); // Устанавливаем текст
                    IstoggleAbsoluteMove = true;
                }

                if (toggle.isOn && toggle.name == "AbsoluteMoveKappa")
                {
                    newMoveComponent.uiInputFieldKappa.text = moveObj.absoluteMoveKappa.ToString(); // Устанавливаем текст
                    IstoggleAbsoluteMove = true;
                }

                if (toggle.isOn && toggle.name == "AbsoluteMoveTheta")
                {
                    newMoveComponent.uiInputFieldTheta.text = moveObj.absoluteMoveTheta.ToString(); // Устанавливаем текст
                    IstoggleAbsoluteMove = true;
                }

                if (toggle.isOn && toggle.name == "AbsoluteMoveDetector")
                {
                    newMoveComponent.uiInputFieldDetector.text = moveObj.absoluteMoveDetector.ToString(); // Устанавливаем текст
                    IstoggleAbsoluteMove = true;
                }

            }
            // Показываем InputField и Text, если выбран Toggle Speed
            ToggleInputFields(true);
            ShowText(true);

            // Отключить все Toggle в toggleSpeed и toggleReset
            DisableAllToggles(toggleSpeed);
            DisableAllToggles(toggleReset);
            DisableAllToggles(toggleHomes);
            DisableAllToggles(toggleStop);
            DisableAllToggles(toggleRelativeMove);
        }
    }

    public Toggle[] GetSelectedToggleArray()
    {
        return selectedToggleArray;
    }

    // Метод для отключения всех Toggle в данном массиве
    void DisableAllToggles(Toggle[] toggles)
    {
        foreach (Toggle toggle in toggles)
        {
            toggle.isOn = false;
        }
    }

}
