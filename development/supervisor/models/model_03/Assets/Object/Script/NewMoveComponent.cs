using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro; // ����������� ������������ ���� TextMeshPro


public class NewMoveComponent : MonoBehaviour
{
    /* ����������� �� ������� ����� ������������ � ���������� ���� ��������, 
     ��� -�� ������ � ����������� � ����������� ��������
     */
    public NewMenu newMenu; // ������ �� ��������� NewMenu
    public MoveObj moveObj; // ������ �� ��������� MoveObj

    public Button[] ButtonSend; //������ ������ Send

    public Button[] Buttoncurentup; // ��������
    public Button[] Buttoncurentdown; // �������

    private string Texttoggel;

    public int allMove; // ��������� ��� �������� allMove
    public int phiMove; // ��������� ��� �������� phiMove
    public int omegaMove; // ��������� ��� �������� OmegaMove
    public int kappaMove; // ��������� ��� �������� KappaMove
    public int thetaMove; // ��������� ��� �������� ThetaMove
    public int detectorMove; // ��������� ��� �������� DetectorMove

    private int corentPhi;
    public TMP_Text uiTextPhi; // ����� ��� Phi �������
    public TMP_InputField uiInputFieldPhi; // ���� ����� ��� Phi �������

    public TMP_InputField uiInputFieldOmega; // ���� ����� ��� Omega �������
    public TMP_Text uiTextOmega; // ����� ��� Omega �������

    public TMP_InputField uiInputFieldKappa; // ���� ����� ��� Kappa �������
    public TMP_Text uiTextKappa; // ����� ��� Kappa �������

    public TMP_InputField uiInputFieldTheta; // ���� ����� ��� ���������� �������
    public TMP_Text uiTextTheta; // ����� ��� ���������� �������

    public TMP_InputField uiInputFieldDetector; // ���� ����� ��� ���������� �������
    public TMP_Text uiTextDetector; // ����� ��� ���������� �������

    public TMP_InputField uiInputFieldAll; // ���� ����� ��� ���������� �������
    public TMP_Text uiTextAll; // ����� ��� ���������� �������


    void Start()
    {


        // �������� �� ������� ������� ��� ������ ������
        foreach (Button button in ButtonSend)
        {
            button.onClick.AddListener(() => ButtonClicked(button));
        }

        // �������� �� ������� ������� ��� ������ ������
        foreach (Button button in Buttoncurentup)
        {
            button.onClick.AddListener(() => ButtonCurentUp(button));
        }

        // �������� �� ������� ������� ��� ������ ������
        foreach (Button button in Buttoncurentdown)
        {
            button.onClick.AddListener(() => ButtonCurentDown(button));
        }

        // �������� �� ������� ��������� ������ � InputFieldPhi
        uiInputFieldPhi.onValueChanged.AddListener(UpdateSpeedFromInputPhi);

        // �������� �� ������� ��������� ������ � InputFieldOmega
        uiInputFieldOmega.onValueChanged.AddListener(UpdateSpeedFromInputOmega);

        // �������� �� ������� ��������� ������ � InputFieldKappa
        uiInputFieldKappa.onValueChanged.AddListener(UpdateSpeedFromInputKappa);

        // �������� �� ������� ��������� ������ � InputFieldTheta
        uiInputFieldTheta.onValueChanged.AddListener(UpdateSpeedFromInputTheta);

        // �������� �� ������� ��������� ������ � InputFieldTheta
        uiInputFieldDetector.onValueChanged.AddListener(UpdateSpeedFromInputDetector);

        // �������� �� ������� ��������� ������ � InputFieldAll
        uiInputFieldAll.onValueChanged.AddListener(UpdateSpeedFromInputAll);
    }

    void Update()
    {

        // ���������, �������� �� newMenu
        if (newMenu != null)
        {
            Toggle[] selectedToggles = newMenu.GetSelectedToggleArray();

            // ���������, ���� �� ��������� Toggle
            if (selectedToggles != null)
            {
                foreach (Toggle toggle in selectedToggles)
                {
                    // ���������, ������� �� Toggle
                    if (toggle.isOn)
                    {
                        Texttoggel = toggle.name;
                    }
                }
            }
        }

    }

    void ButtonClicked(Button button)
    {

        if (button.name == "SendAll")
        {
            switch (Texttoggel)
            {
                case "HomeAll":
                    allMove = 1;
                    break;

                case "SpeedAllPhi":
                    allMove = 3;
                    break;

                case "SpeedAll":
                    allMove = 2;
                    break;

                case "ResetAll":
                    allMove = 4;
                    break;
            }
        }

        if (button.name == "SendPhi")
        {
            switch (Texttoggel)
            {
                case "HomePhi":
                    phiMove = 1;
                    break;

                case "StopPhi":
                    phiMove = 2;
                    break;

                case "RotatePhi":
                    phiMove = 3;
                    break;

                case "SpeedPhi":
                    phiMove = 4;
                    break;

                case "RelativeMovePhi":
                    phiMove = 5;
                    break;

                case "AbsoluteMovePhi":
                    phiMove = 6;
                    break;

                case "ResetPhi":
                    phiMove = 7;
                    break;

            }
        }

        if (button.name == "SendOmega")
        {
            switch (Texttoggel)
            {
                case "HomeOmega":
                    omegaMove = 1;
                    break;

                case "StopOmega":
                    omegaMove = 2;
                    break;

                case "SpeedOmega":
                    omegaMove = 3;
                    break;

                case "RelativeMoveOmega":
                    omegaMove = 4;
                    break;

                case "AbsoluteMoveOmega":
                    omegaMove = 5;
                    break;

                case "ResetOmega":
                    omegaMove = 6;
                    break;
            }
        }

        if (button.name == "SendKappa")
        {
            switch (Texttoggel)
            {
                case "HomeKappa":
                    kappaMove = 1;
                    break;

                case "StopKappa":
                    kappaMove = 2;
                    break;

                case "SpeedKappa":
                    kappaMove = 3;
                    break;

                case "RelativeMoveKappa":
                    kappaMove = 4;
                    break;

                case "AbsoluteMoveKappa":
                    kappaMove = 5;
                    break;

                case "ResetKappa":
                    kappaMove = 6;
                    break;
            }
        }

        if (button.name == "SendTheta")
        {
            switch (Texttoggel)
            {
                case "HomeTheta":
                    thetaMove = 1;
                    break;

                case "StopTheta":
                    thetaMove = 2;
                    break;

                case "SpeedTheta":
                    thetaMove = 3;
                    break;

                case "RelativeMoveTheta":
                    thetaMove = 4;
                    break;

                case "AbsoluteMoveTheta":
                    thetaMove = 5;
                    break;

                case "ResetTheta":
                    thetaMove = 6;
                    break;
            }
        }

        if (button.name == "SendDetector")
        {
            switch (Texttoggel)
            {
                case "HomeDetector":
                    detectorMove = 1;
                    break;

                case "StopDetector":
                    detectorMove = 2;
                    break;

                case "SpeedDetector":
                    detectorMove = 3;
                    break;

                case "RelativeMoveDetector":
                    detectorMove = 4;
                    break;

                case "AbsoluteMoveDetector":
                    detectorMove = 5;
                    break;

                case "ResetDetector":
                    detectorMove = 6;
                    break;
            }
        }
    }

    void ButtonCurentUp(Button button)
    {

        if (button.name == "UpAll")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentUpPhi(1);
        }

        if (button.name == "UpPhi")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentUpPhi(1);
        }

        if (button.name == "UpOmega")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentUpPhi(1);
        }

        if (button.name == "UpKappa")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentUpPhi(1);
        }

        if (button.name == "UpTheta")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentUpPhi(1);
        }

        if (button.name == "UpDetector")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentUpPhi(1);
        }
    }
    void ButtonCurentDown(Button button)
    {

        if (button.name == "DownAll")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentDownPhi(-1);
        }

        if (button.name == "DownPhi")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentDownPhi(-1);
        }

        if (button.name == "DownOmega")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentDownPhi(-1);
        }

        if (button.name == "DownKappa")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentDownPhi(-1);
        }

        if (button.name == "DownTheta")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentDownPhi(-1);
        }

        if (button.name == "DownDetector")
        {
            // �������� ������������� �������� 1 � CurentUp
            CurentDownPhi(-1);
        }
    }

    void CurentUpPhi(int upCurent)
    {
        string inputValuePhi = uiInputFieldPhi.text; // �������� ������� �������� �� ���������� ����
        string inputValueOmega = uiInputFieldOmega.text; // �������� ������� �������� �� ���������� ����
        string inputValueKappa = uiInputFieldKappa.text; // �������� ������� �������� �� ���������� ����
        string inputValueTheta = uiInputFieldTheta.text; // �������� ������� �������� �� ���������� ����
        string inputValueDetector = uiInputFieldDetector.text; // �������� ������� �������� �� ���������� ����
        string inputValueAll = uiInputFieldAll.text; // �������� ������� �������� �� ���������� ����

        if (float.TryParse(inputValuePhi, out float floatValuePhi)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedPhi" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValuePhi + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -36 || newValue > 36)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValuePhi = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldPhi.SetTextWithoutNotify(inputValuePhi);
                    moveObj.PrespeedPhi = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMovePhi" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValuePhi + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -360 || newValue > 360)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValuePhi = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldPhi.SetTextWithoutNotify(inputValuePhi);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMovePhi = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMovePhi" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValuePhi + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -360 || newValue > 360)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValuePhi = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldPhi.SetTextWithoutNotify(inputValuePhi);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMovePhi = (int)newValue;

                }
            }
        }

        if (float.TryParse(inputValueOmega, out float floatValueOmega)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedOmega" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueOmega + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueOmega = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldOmega.SetTextWithoutNotify(inputValueOmega);
                    moveObj.PrespeedOmega = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveOmega" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueOmega + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -72 || newValue > 72)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueOmega = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldOmega.SetTextWithoutNotify(inputValueOmega);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveOmega = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveOmega" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueOmega + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -72 || newValue > 72)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueOmega = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldOmega.SetTextWithoutNotify(inputValueOmega);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveOmega = (int)newValue;

                }
            }
        }

        if (float.TryParse(inputValueKappa, out float floatValueKappa)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedKappa" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueKappa + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueKappa = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldKappa.SetTextWithoutNotify(inputValueKappa);
                    moveObj.PrespeedKappa = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveKappa" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueKappa + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 99)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueKappa = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldKappa.SetTextWithoutNotify(inputValueKappa);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveKappa = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveKappa" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueKappa + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 99)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueKappa = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldKappa.SetTextWithoutNotify(inputValueKappa);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveKappa = (int)newValue;

                }
            }
        }

        if (float.TryParse(inputValueTheta, out float floatValueTheta)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedTheta" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueTheta + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueTheta = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldKappa.SetTextWithoutNotify(inputValueTheta);
                    moveObj.PrespeedTheta = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveTheta" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueTheta + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 90)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueTheta = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldTheta.SetTextWithoutNotify(inputValueTheta);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveTheta = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveTheta" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueTheta + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 90)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueTheta = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldTheta.SetTextWithoutNotify(inputValueTheta);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveTheta = (int)newValue;

                }
            }
        }

        if (float.TryParse(inputValueDetector, out float floatValueDetector)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedDetector" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueDetector + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueDetector = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldDetector.SetTextWithoutNotify(inputValueDetector);
                    moveObj.PrespeedDetector = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveDetector" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueDetector + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -100 || newValue > 100)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueDetector = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldDetector.SetTextWithoutNotify(inputValueDetector);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveDetector = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveDetector" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueDetector + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < 0 || newValue > 100)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueDetector = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldDetector.SetTextWithoutNotify(inputValueDetector);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveDetector = (int)newValue;

                }
            }
        }
        if (float.TryParse(inputValueAll, out float floatValueAll)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedAll" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueAll + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueAll = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldAll.SetTextWithoutNotify(inputValueAll);
                    moveObj.PrespeedAll = (int)newValue;

                }
            }
            else if (Texttoggel == "SpeedAllPhi" && newMenu.IstoggleSpeedPhi)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueAll + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -36 || newValue > 36)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueAll = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldAll.SetTextWithoutNotify(inputValueAll);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.speedAllPhi = (int)newValue;
                }
            }
        }
    }

    void CurentDownPhi(int upCurent)
    {
        string inputValuePhi = uiInputFieldPhi.text; // �������� ������� �������� �� ���������� ����
        string inputValueOmega = uiInputFieldOmega.text; // �������� ������� �������� �� ���������� ����
        string inputValueKappa = uiInputFieldKappa.text; // �������� ������� �������� �� ���������� ����
        string inputValueTheta = uiInputFieldTheta.text; // �������� ������� �������� �� ���������� ����
        string inputValueDetector = uiInputFieldDetector.text; // �������� ������� �������� �� ���������� ����
        string inputValueAll = uiInputFieldAll.text; // �������� ������� �������� �� ���������� ����

        if (float.TryParse(inputValuePhi, out float floatValuePhi)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedPhi" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValuePhi + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -36 || newValue > 36)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValuePhi = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldPhi.SetTextWithoutNotify(inputValuePhi);
                    moveObj.PrespeedPhi = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMovePhi" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValuePhi + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -360 || newValue > 360)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValuePhi = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldPhi.SetTextWithoutNotify(inputValuePhi);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMovePhi = (int)newValue;

                }
            }
            if (Texttoggel == "AbsoluteMovePhi" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValuePhi + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -360 || newValue > 360)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValuePhi = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldPhi.SetTextWithoutNotify(inputValuePhi);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMovePhi = (int)newValue;

                }
            }
        }
        if (float.TryParse(inputValueOmega, out float floatValueOmega)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedOmega" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueOmega + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueOmega = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldOmega.SetTextWithoutNotify(inputValueOmega);
                    moveObj.PrespeedOmega = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveOmega" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueOmega + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -72 || newValue > 72)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueOmega = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldOmega.SetTextWithoutNotify(inputValueOmega);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveOmega = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveOmega" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueOmega + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -72 || newValue > 72)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueOmega = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldOmega.SetTextWithoutNotify(inputValueOmega);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveOmega = (int)newValue;

                }
            }
        }

        if (float.TryParse(inputValueKappa, out float floatValueKappa)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedKappa" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueKappa + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueKappa = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldKappa.SetTextWithoutNotify(inputValueKappa);
                    moveObj.PrespeedKappa = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveKappa" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueKappa + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 99)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueKappa = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldKappa.SetTextWithoutNotify(inputValueKappa);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveKappa = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveKappa" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueKappa + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 99)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueKappa = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldKappa.SetTextWithoutNotify(inputValueKappa);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveKappa = (int)newValue;

                }
            }
        }

        if (float.TryParse(inputValueTheta, out float floatValueTheta)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedTheta" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueTheta + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueTheta = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldTheta.SetTextWithoutNotify(inputValueTheta);
                    moveObj.PrespeedTheta = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveTheta" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueTheta + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 90)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueTheta = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldTheta.SetTextWithoutNotify(inputValueTheta);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveTheta = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveTheta" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueTheta + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -90 || newValue > 90)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueTheta = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldTheta.SetTextWithoutNotify(inputValueTheta);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveTheta = (int)newValue;

                }
            }
        }

        if (float.TryParse(inputValueDetector, out float floatValueDetector)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedDetector" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueDetector + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueDetector = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldDetector.SetTextWithoutNotify(inputValueDetector);
                    moveObj.PrespeedDetector = (int)newValue;

                }
            }
            if (Texttoggel == "RelativeMoveDetector" && newMenu.IstoggleRelativeMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueDetector + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -100 || newValue > 100)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueDetector = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldDetector.SetTextWithoutNotify(inputValueDetector);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresrelativeMoveDetector = (int)newValue;


                }
            }
            if (Texttoggel == "AbsoluteMoveDetector" && newMenu.IstoggleAbsoluteMove)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueDetector + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < 0 || newValue > 100)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueDetector = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldDetector.SetTextWithoutNotify(inputValueDetector);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.PresabsoluteMoveDetector = (int)newValue;

                }
            }
        }
        if (float.TryParse(inputValueAll, out float floatValueAll)) // ������� ������������� inputValue � float
        {
            if (Texttoggel == "SpeedAll" && newMenu.IstoggleSpeed)
            {
                // ������ �� ����� ��������� ����������
                float newValue = floatValueAll + upCurent; // ��������

                // �������� �� ������������ ��������
                if (newValue < -18 || newValue > 18)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueAll = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldAll.SetTextWithoutNotify(inputValueAll);
                    moveObj.PrespeedAll = (int)newValue;

                }
            }
            else if (Texttoggel == "SpeedAllPhi" && newMenu.IstoggleSpeedPhi)
            {

                // ������ �� ����� ��������� ����������
                float newValue = floatValueAll + upCurent; // ��������
                // �������� �� ������������ ��������
                if (newValue < -36 || newValue > 36)
                {
                    newValue = 0;
                }
                else
                {
                    // ����������� ��������� ������� � ������
                    inputValueAll = newValue.ToString();
                    // ���� �����, �� ����� ������ �������� ��������� ����
                    uiInputFieldAll.SetTextWithoutNotify(inputValueAll);
                    // ��������� ��� float, �� � ����� ���������
                    moveObj.speedAllPhi = (int)newValue;


                }
            }
        }
    }

    void UpdateSpeedFromInputPhi(string inputValue)
    {
        if (Texttoggel == "SpeedPhi" && newMenu.IstoggleSpeed)
        {
            // ���� ������� ������ "-" ��� ������ ������, ������ ����������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }
            // ������� ������������� ������ � �����
            if (int.TryParse(inputValue, out int value))
            {

                // �������� �� ������������ ��������
                if (value < -36 || value > 36)
                {

                    // ������� ��������� ������ ��� ������������� ������ ��������
                    if (inputValue.Length > 0)
                    {
                        uiInputFieldPhi.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
                    }
                    else
                    {
                        uiInputFieldPhi.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
                    }
                    return; // ����� �� ������, ���� �������� �����������
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� ������ �����
                if (value == 0 && inputValue.Length > 1)
                {
                    HandleInvalidInput(inputValue);
                    return; // ����� �� ������, ���� �������� �����������
                }

                moveObj.PrespeedPhi = value; // ��������� �������� speedPhi
            }

            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }

        }
        if (Texttoggel == "RelativeMovePhi" && newMenu.IstoggleRelativeMove)
        {
            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -360 || intValue > 360)
                {
                    floatValue = floatValue + corentPhi;
                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresrelativeMovePhi = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldPhi.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
        if (Texttoggel == "AbsoluteMovePhi" && newMenu.IstoggleAbsoluteMove)
        {
            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -360 || intValue > 360)
                {

                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresabsoluteMovePhi = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldPhi.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
    }

    void UpdateSpeedFromInputOmega(string inputValue)
    {
        if (Texttoggel == "SpeedOmega" && newMenu.IstoggleSpeed)
        {

            // ���� ������� ������ "-" ��� ������ ������, ������ ����������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }
            // ������� ������������� ������ � �����
            if (int.TryParse(inputValue, out int value))
            {

                // �������� �� ������������ ��������
                if (value < -18 || value > 18)
                {

                    // ������� ��������� ������ ��� ������������� ������ ��������
                    if (inputValue.Length > 0)
                    {
                        uiInputFieldOmega.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
                    }
                    else
                    {
                        uiInputFieldOmega.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
                    }
                    return; // ����� �� ������, ���� �������� �����������
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� ������ �����
                if (value == 0 && inputValue.Length > 1)
                {
                    HandleInvalidInput(inputValue);
                    return; // ����� �� ������, ���� �������� �����������
                }

                moveObj.PrespeedOmega = value; // ��������� �������� speedPhi
            }

            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }

        }
        if (Texttoggel == "RelativeMoveOmega" && newMenu.IstoggleRelativeMove)
        {

            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -72 || intValue > 72)
                {
                    floatValue = floatValue + corentPhi;
                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresrelativeMoveOmega = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldOmega.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
        if (Texttoggel == "AbsoluteMoveOmega" && newMenu.IstoggleAbsoluteMove)
        {
            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -72 || intValue > 72)
                {

                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresabsoluteMoveOmega = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldOmega.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
    }

    void UpdateSpeedFromInputKappa(string inputValue)
    {
        if (Texttoggel == "SpeedKappa" && newMenu.IstoggleSpeed)
        {

            // ���� ������� ������ "-" ��� ������ ������, ������ ����������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }
            // ������� ������������� ������ � �����
            if (int.TryParse(inputValue, out int value))
            {

                // �������� �� ������������ ��������
                if (value < -18 || value > 18)
                {

                    // ������� ��������� ������ ��� ������������� ������ ��������
                    if (inputValue.Length > 0)
                    {
                        uiInputFieldKappa.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
                    }
                    else
                    {
                        uiInputFieldKappa.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
                    }
                    return; // ����� �� ������, ���� �������� �����������
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� ������ �����
                if (value == 0 && inputValue.Length > 1)
                {
                    HandleInvalidInput(inputValue);
                    return; // ����� �� ������, ���� �������� �����������
                }

                moveObj.PrespeedKappa = value; // ��������� �������� speedPhi
            }

            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }

        }
        if (Texttoggel == "RelativeMoveKappa" && newMenu.IstoggleRelativeMove)
        {

            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -90 || intValue > 99)
                {
                    floatValue = floatValue + corentPhi;
                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresrelativeMoveKappa = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldKappa.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
        if (Texttoggel == "AbsoluteMoveKappa" && newMenu.IstoggleAbsoluteMove)
        {
            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -90 || intValue > 99)
                {

                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresabsoluteMoveKappa = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldKappa.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
    }

    void UpdateSpeedFromInputTheta(string inputValue)
    {
        if (Texttoggel == "SpeedTheta" && newMenu.IstoggleSpeed)
        {

            // ���� ������� ������ "-" ��� ������ ������, ������ ����������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }
            // ������� ������������� ������ � �����
            if (int.TryParse(inputValue, out int value))
            {

                // �������� �� ������������ ��������
                if (value < -18 || value > 18)
                {

                    // ������� ��������� ������ ��� ������������� ������ ��������
                    if (inputValue.Length > 0)
                    {
                        uiInputFieldTheta.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
                    }
                    else
                    {
                        uiInputFieldTheta.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
                    }
                    return; // ����� �� ������, ���� �������� �����������
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� ������ �����
                if (value == 0 && inputValue.Length > 1)
                {
                    HandleInvalidInput(inputValue);
                    return; // ����� �� ������, ���� �������� �����������
                }

                moveObj.PrespeedTheta = value; // ��������� �������� speedPhi
            }

            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }

        }
        if (Texttoggel == "RelativeMoveTheta" && newMenu.IstoggleRelativeMove)
        {

            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -90 || intValue > 90)
                {
                    floatValue = floatValue + corentPhi;
                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresrelativeMoveTheta = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldTheta.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
        if (Texttoggel == "AbsoluteMoveTheta" && newMenu.IstoggleAbsoluteMove)
        {
            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -90 || intValue > 90)
                {

                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresabsoluteMoveTheta = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldTheta.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
    }

    void UpdateSpeedFromInputDetector(string inputValue)
    {
        if (Texttoggel == "SpeedDetector" && newMenu.IstoggleSpeed)
        {

            // ���� ������� ������ "-" ��� ������ ������, ������ ����������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }
            // ������� ������������� ������ � �����
            if (int.TryParse(inputValue, out int value))
            {

                // �������� �� ������������ ��������
                if (value < -18 || value > 18)
                {

                    // ������� ��������� ������ ��� ������������� ������ ��������
                    if (inputValue.Length > 0)
                    {
                        uiInputFieldDetector.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
                    }
                    else
                    {
                        uiInputFieldDetector.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
                    }
                    return; // ����� �� ������, ���� �������� �����������
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� ������ �����
                if (value == 0 && inputValue.Length > 1)
                {
                    HandleInvalidInput(inputValue);
                    return; // ����� �� ������, ���� �������� �����������
                }

                moveObj.PrespeedDetector = value; // ��������� �������� speedPhi
            }

            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }

        }
        if (Texttoggel == "RelativeMoveDetector" && newMenu.IstoggleRelativeMove)
        {

            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < -100 || intValue > 100)
                {
                    floatValue = floatValue + corentPhi;
                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresrelativeMoveDetector = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldDetector.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
        if (Texttoggel == "AbsoluteMoveDetector" && newMenu.IstoggleAbsoluteMove)
        {
            // ���� ������� ������ "-" ��� ������ ������
            if (inputValue == "-" || string.IsNullOrEmpty(inputValue))
            {
                return;
            }

            // ������� ������������� ������ � ����� (float)
            if (float.TryParse(inputValue, out float floatValue))
            {
                // ��������� �� ������ ��� �������� ���������
                int intValue = Mathf.RoundToInt(floatValue);

                // �������� �� ������������ �������� (-360..360)
                if (intValue < 0 || intValue > 100)
                {

                    HandleInvalidInput(inputValue);
                    return;
                }

                // �������������� ��������: ���� ������ 0, �� ��������� ���� "0..." 
                if (intValue == 0 && inputValue.Length > 1 && inputValue != "0")
                {
                    HandleInvalidInput(inputValue);
                    return;
                }

                // ��������� ��� float, �� � ����� ���������
                moveObj.PresabsoluteMoveDetector = intValue;

                // ��������� ����� � ���� (��� .0)
                uiInputFieldTheta.SetTextWithoutNotify(intValue.ToString());
            }
            else
            {
                Debug.LogWarning("������������ ����: " + inputValue);
                HandleInvalidInput(inputValue);
            }
        }
    }

    void UpdateSpeedFromInputAll(string inputValue)
    {

        if (int.TryParse(inputValue, out int value))
        {
            if (Texttoggel == "SpeedAll" && newMenu.IstoggleSpeed)
            {
                // ����������� ��� ����� ��������
                if (value < -18 || value > 18)
                {
                    HandleInvalidInput(inputValue);
                    return;
                }
                moveObj.speedAll = value;
            }
            else if (Texttoggel == "SpeedAllPhi" && newMenu.IstoggleSpeedPhi)
            {
                // ����������� ��� SpeedAllPhi
                if (value < -36 || value > 36)
                {
                    HandleInvalidInput(inputValue);
                    return;
                }
                moveObj.speedAllPhi = value;
            }
        }
        else
        {
            HandleInvalidInput(inputValue);
        }
    }


    private void HandleInvalidInput(string inputValue)
    {
        // ������� ��������� ������ ��� ������������� ������ ��������
        if (inputValue.Length > 0)
        {
            uiInputFieldPhi.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
            uiInputFieldOmega.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
            uiInputFieldKappa.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
            uiInputFieldTheta.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
            uiInputFieldDetector.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
            uiInputFieldAll.SetTextWithoutNotify(inputValue.Substring(0, inputValue.Length - 1));
        }
        else
        {
            uiInputFieldPhi.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
            uiInputFieldOmega.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
            uiInputFieldKappa.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
            uiInputFieldTheta.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
            uiInputFieldDetector.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������
            uiInputFieldAll.SetTextWithoutNotify(""); // ������� ����, ���� ��� ������

        }
    }


    // ===== ������� ��� ���������� ���� =====

    // ������ Home ��� ��������� ���. ���������� ������������ ����� MoveObj, ����� ������ UI/Start �������� ��� ���������.
    public void HomeAxis(string axis)
    {
        if (moveObj == null) return;
        axis = axis.ToLower();
        switch (axis)
        {
            case "all":
                allMove = 1; // HomeAll
                break;
            case "phi":
                phiMove = 1; // HomePhi
                break;
            case "omega":
                kappaMove = 1; // HomeOmega (legacy MoveObj naming)
                break;
            case "kappa":
                omegaMove = 1; // HomeKappa (legacy MoveObj naming)
                break;
            case "theta":
                thetaMove = 1; // HomeTheta
                break;
            case "detector":
                detectorMove = 1; // HomeDetector
                break;
            default:
                Debug.LogWarning($"HomeAxis: unknown axis '{axis}'");
                break;
        }
    }

    // ���������� ���. ������������� ��������������� Move?�����, ����� MoveObj ��������� ���������.
    public void StopAxis(string axis)
    {
        if (moveObj == null) return;
        axis = axis.ToLower();
        switch (axis)
        {
            case "all":
                allMove = 4; // ResetAll/StopAll �������� ����� UI, ���������� StopAllAxis ��������
                moveObj.StopAllAxis();
                break;
            case "phi":
                phiMove = 2; // StopPhi
                break;
            case "omega":
                kappaMove = 2; // StopOmega (legacy MoveObj naming)
                break;
            case "kappa":
                omegaMove = 2; // StopKappa (legacy MoveObj naming)
                break;
            case "theta":
                thetaMove = 2; // StopTheta
                break;
            case "detector":
                detectorMove = 2; // StopDetector
                break;
            default:
                Debug.LogWarning($"StopAxis: unknown axis '{axis}'");
                break;
        }
    }

    // ���������/���������� �������� ��� ���, �������������� ����������� �������� (Phi).
    // start=true � ������; start=false � ����������.
    public void RotateAxis(string axis, bool start)
    {
        if (moveObj == null) return;
        axis = axis.ToLower();
        if (axis == "phi")
        {
            phiMove = start ? 3 : 2;
        }
        else
        {
            Debug.LogWarning($"RotateAxis: axis '{axis}' does not support continuous rotate");
        }
    }

    // ���������� �������� ��� ���������� ��� � ���������� �� ���������, ���������� � UI.
    // ����� ������������� Move?����, ����� MoveObj �������� �������� Prespeed.
    public void SetSpeedAxis(string axis, int speed)
    {
        if (moveObj == null) return;
        axis = axis.ToLower();
        switch (axis)
        {
            case "all":
                moveObj.PrespeedAll = Mathf.Clamp(speed, -18, 18);
                allMove = 2; // SpeedAll
                break;
            case "phi":
                moveObj.PrespeedPhi = Mathf.Clamp(speed, -36, 36);
                phiMove = 4; // SpeedPhi
                break;
            case "omega":
                moveObj.PrespeedKappa = Mathf.Clamp(speed, -18, 18);
                kappaMove = 3; // SpeedOmega (legacy MoveObj naming)
                break;
            case "kappa":
                moveObj.PrespeedOmega = Mathf.Clamp(speed, -18, 18);
                omegaMove = 3; // SpeedKappa (legacy MoveObj naming)
                break;
            case "theta":
                moveObj.PrespeedTheta = Mathf.Clamp(speed, -18, 18);
                thetaMove = 3; // SpeedTheta
                break;
            case "detector":
                moveObj.PrespeedDetector = Mathf.Clamp(speed, -18, 18);
                detectorMove = 3; // SpeedDetector
                break;
            default:
                Debug.LogWarning($"SetSpeedAxis: unknown axis '{axis}'");
                break;
        }
    }

    // ������������� ����������� ��� ��������� ��� � ����������� ��������� ��������� � ��������� Presrelative + Move?�����.
    public void RelativeMoveAxis(string axis, float distance)
    {
        if (moveObj == null) return;
        axis = axis.ToLower();
        switch (axis)
        {
            case "phi":
                distance = Mathf.Clamp(distance, -360f, 360f);
                moveObj.PresrelativeMovePhi = (int)distance;
                phiMove = 5; // RelativeMovePhi
                break;
            case "kappa":
                distance = Mathf.Clamp(distance, -72f, 72f);
                moveObj.PresrelativeMoveOmega = (int)distance;
                omegaMove = 4; // RelativeMoveKappa (legacy MoveObj naming)
                break;
            case "omega":
                distance = Mathf.Clamp(distance, -90f, 99f);
                moveObj.PresrelativeMoveKappa = (int)distance;
                kappaMove = 4; // RelativeMoveOmega (legacy MoveObj naming)
                break;
            case "theta":
                distance = Mathf.Clamp(distance, -90f, 90f);
                moveObj.PresrelativeMoveTheta = (int)distance;
                thetaMove = 4; // RelativeMoveTheta
                break;
            case "detector":
                distance = Mathf.Clamp(distance, 0f, 100f);
                moveObj.PresrelativeMoveDetector = (int)distance;
                detectorMove = 4; // RelativeMoveDetector
                break;
            case "all":
                // apply to all with their respective clamps
                RelativeMoveAxis("phi", distance);
                RelativeMoveAxis("kappa", distance);
                RelativeMoveAxis("omega", distance);
                RelativeMoveAxis("theta", distance);
                RelativeMoveAxis("detector", distance);
                break;
            default:
                Debug.LogWarning($"RelativeMoveAxis: unknown axis '{axis}'");
                break;
        }
    }

    // ���������� ����������� ��� ��������� ��� � ����������� ��������� ��������� � ��������� Presabsolute + Move?�����.
    public void AbsoluteMoveAxis(string axis, float distance)
    {
        if (moveObj == null) return;
        axis = axis.ToLower();
        switch (axis)
        {
            case "phi":
                distance = Mathf.Clamp(distance, -360f, 360f);
                moveObj.PresabsoluteMovePhi = (int)distance;
                phiMove = 6; // AbsoluteMovePhi
                break;
            case "kappa":
                distance = Mathf.Clamp(distance, -72f, 72f);
                moveObj.PresabsoluteMoveOmega = (int)distance;
                omegaMove = 5; // AbsoluteMoveKappa (legacy MoveObj naming)
                break;
            case "omega":
                distance = Mathf.Clamp(distance, -90f, 99f);
                moveObj.PresabsoluteMoveKappa = (int)distance;
                kappaMove = 5; // AbsoluteMoveOmega (legacy MoveObj naming)
                break;
            case "theta":
                distance = Mathf.Clamp(distance, -90f, 90f);
                moveObj.PresabsoluteMoveTheta = (int)distance;
                thetaMove = 5; // AbsoluteMoveTheta
                break;
            case "detector":
                distance = Mathf.Clamp(distance, 0f, 100f);
                moveObj.PresabsoluteMoveDetector = (int)distance;
                detectorMove = 5; // AbsoluteMoveDetector
                break;
            case "all":
                AbsoluteMoveAxis("phi", distance);
                AbsoluteMoveAxis("kappa", distance);
                AbsoluteMoveAxis("omega", distance);
                AbsoluteMoveAxis("theta", distance);
                AbsoluteMoveAxis("detector", distance);
                break;
            default:
                Debug.LogWarning($"AbsoluteMoveAxis: unknown axis '{axis}'");
                break;
        }
    }

    public void ResetAxis(string axis)
    {
        if (moveObj == null) return;
        axis = axis.ToLower();
        if (axis == "all")
        {
            moveObj.ResetAllAxis();
        }
        else
        {
            // ��� ������ ���������� ��� ���������� Move?�����, ����� ������������� ������������ ������ UI, 
            //      ������� ��������� �� ��� ����� � �������� Reset ��� ������ ���.
            switch (axis)
            {
                case "phi":
                    phiMove = 7; // ResetPhi
                    break;
                case "omega":
                    kappaMove = 6; // ResetOmega (legacy MoveObj naming)
                    break;
                case "kappa":
                    omegaMove = 6; // ResetKappa (legacy MoveObj naming)
                    break;
                case "theta":
                    thetaMove = 6; // ResetTheta
                    break;
                case "detector":
                    detectorMove = 6; // ResetDetector
                    break;
                default:
                    Debug.LogWarning($"ResetAxis: unknown axis '{axis}'");
                    break;
            }
        }
    }

    public string GetState()
    {
        return "running";
    }
}
