using UnityEngine;
using UnityEngine.UI;

public class CameraOrbitZoom : MonoBehaviour
{
    /*
     Настройка камеры, вращение, приближение, смещение по Z  и так-же ограничение столкновений с объектами.
     */
    // Настройки камеры
    public Transform target;
    public float rotationSpeed = 5f;
    public float zoomSpeed = 5f;
    public float panSpeed = 0.1f;
    public float zSpeed = 0.1f;
    public float minZoomDistance = 2f;
    public float maxZoomDistance = 15f;
    public float minZPosition = -10f;
    public float maxZPosition = 10f;
    public Collider[] importantColliders;
    public float raycastBackStep = 0.5f;
    public float backRayLength = 2f; // Длина луча позади камеры
    public Button buttonReset;
    public float resetDuration = 1f;

    private Vector3 offset;
    private Vector3 originalOffset;
    private Vector3 originalTargetPosition;
    private bool isRotating = false;
    private bool isPanning = false;
    private Vector3 lastMousePosition;
    private Vector3 lastValidPosition;
    private bool isColliding = false;
    private bool isBackColliding = false;
    private float collisionTimer = 0f;
    private const float COLLISION_COOLDOWN = 0.5f;
    private bool isResetting = false;
    private float resetProgress = 0f;

    void Start()
    {
        if (target == null)
        {
            Debug.LogError("Target not assigned!");
            return;
        }

        originalOffset = transform.position - target.position;
        originalTargetPosition = target.position;
        offset = originalOffset;
        lastValidPosition = transform.position;

        if (buttonReset != null)
        {
            buttonReset.onClick.AddListener(StartCameraReset);
        }
    }

    void Update()
    {
        if (target == null) return;

        if (isResetting)
        {
            HandleCameraReset();
            return;
        }

        HandleInput();
        HandleRotation();
        HandlePanning();
        HandleZoom();
        CheckCollisionWithRaycasts();
        UpdateCameraPosition();
    }

   

    
    void StartCameraReset()
    {
        isResetting = true;
        resetProgress = 0f;
    }
    void HandleCameraReset()
    {
        // Увеличиваем прогресс медленнее (делим на большее число)
        resetProgress += Time.deltaTime / (resetDuration * 2f); // В 2 раза медленнее

        // Используем SmoothStep для более плавного движения
        float smoothProgress = Mathf.SmoothStep(0, 1, resetProgress);

        // Плавная интерполяция с учетом smoothProgress
        target.position = Vector3.Lerp(target.position, originalTargetPosition, smoothProgress);
        offset = Vector3.Lerp(offset, originalOffset, smoothProgress);

        transform.position = target.position + offset;
        transform.LookAt(target);

        if (resetProgress >= 1f)
        {
            isResetting = false;
            lastValidPosition = transform.position;
        }
    }
    void HandleInput()
    {
        if (Input.GetMouseButtonDown(2))
        {
            if (Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.RightShift))
            {
                isPanning = true;
                lastMousePosition = Input.mousePosition;
            }
            else
            {
                isRotating = true;
            }
        }

        if (Input.GetMouseButtonUp(2))
        {
            isRotating = false;
            isPanning = false;
        }
    }

    void HandleRotation()
    {
        if (!isRotating) return;

        float mouseX = Input.GetAxis("Mouse X") * rotationSpeed;
        float mouseY = Input.GetAxis("Mouse Y") * rotationSpeed;

        // Вращение по горизонтали (всегда разрешено)
        transform.RotateAround(target.position, Vector3.up, mouseX);

        // Вычисляем текущий угол наклона камеры (по оси X)
        float currentPitchAngle = transform.eulerAngles.x;
        // Нормализуем угол в диапазон [-180, 180]
        if (currentPitchAngle > 180) currentPitchAngle -= 360;

        // Вычисляем новый угол после вращения
        float newPitchAngle = currentPitchAngle - mouseY;

        // Ограничиваем угол в диапазоне [-80, 80]
        if (newPitchAngle > -80f && newPitchAngle < 80f)
        {
            // Если в пределах допустимого - применяем вращение
            transform.RotateAround(target.position, transform.right, -mouseY);
        }
        else
        {
            // Если выходим за границы - фиксируем на граничном значении
            float clampedAngle = Mathf.Clamp(newPitchAngle, -80f, 80f);
            Vector3 currentEuler = transform.eulerAngles;
            transform.eulerAngles = new Vector3(clampedAngle, currentEuler.y, currentEuler.z);
        }

        // Обновляем offset
        offset = transform.position - target.position;
    }

    void HandlePanning()
    {
        if (!isPanning) return;

        Vector3 delta = Input.mousePosition - lastMousePosition;
        float zMovement = -delta.y * zSpeed;

        float newZ = Mathf.Clamp(
            target.position.z + zMovement,
            minZPosition,
            maxZPosition
        );

        target.position = new Vector3(
            target.position.x,
            target.position.y,
            newZ
        );

        lastMousePosition = Input.mousePosition;
    }

    void HandleZoom()
    {
        float scroll = Input.GetAxis("Mouse ScrollWheel");
        if (scroll != 0)
        {
            // Если есть коллизия сзади и мы пытаемся отдалиться - блокируем зум
            if (isBackColliding && scroll < 0) return;

            float desiredDistance = offset.magnitude - scroll * zoomSpeed;
            desiredDistance = Mathf.Clamp(desiredDistance, minZoomDistance, maxZoomDistance);
            offset = offset.normalized * desiredDistance;
        }
    }

    void CheckCollisionWithRaycasts()
    {
        bool wasColliding = isColliding || isBackColliding;
        isColliding = false;
        isBackColliding = false;
        float closestDistance = float.MaxValue;
        Vector3 closestPoint = Vector3.zero;

        // Основной луч от камеры к цели
        Vector3 direction = (target.position - transform.position).normalized;
        float distance = Vector3.Distance(transform.position, target.position);
        Ray ray = new Ray(transform.position, direction);

        // Луч позади камеры (противоположный направлению взгляда)
        Ray backRay = new Ray(transform.position, -transform.forward);

        // Проверка коллизий с важными коллайдерами
        foreach (Collider collider in importantColliders)
        {
            if (collider == null) continue;

            // Проверка основного луча
            RaycastHit hit;
            if (collider.Raycast(ray, out hit, distance))
            {
                isColliding = true;
                if (hit.distance < closestDistance)
                {
                    closestDistance = hit.distance;
                    closestPoint = hit.point;
                }
            }

            // Проверка луча позади камеры
            if (collider.Raycast(backRay, out hit, backRayLength))
            {
                isBackColliding = true;
                if (hit.distance < closestDistance)
                {
                    closestDistance = hit.distance;
                    closestPoint = hit.point;
                }
            }
        }

        // Обработка коллизий
        if (isColliding || isBackColliding)
        {
            Vector3 escapeDirection = (transform.position - closestPoint).normalized;
            lastValidPosition = closestPoint + escapeDirection * raycastBackStep;
        }
        else if (wasColliding)
        {
            collisionTimer = COLLISION_COOLDOWN;
        }
        else if (collisionTimer > 0)
        {
            collisionTimer -= Time.deltaTime;
        }
    }
    void UpdateCameraPosition()
    {
        Vector3 desiredPosition = target.position + offset;

        if (isColliding)
        {
            // Плавное перемещение к скорректированной позиции
            transform.position = Vector3.Lerp(transform.position, lastValidPosition, Time.deltaTime * 10f);
        }
        else if (collisionTimer <= 0)
        {
            // Обычное движение
            lastValidPosition = desiredPosition;
            transform.position = desiredPosition;
        }

        transform.LookAt(target);
    }

    void OnDrawGizmosSelected()
    {
        if (target != null)
        {
            Gizmos.color = isColliding ? Color.red : Color.green;
            Gizmos.DrawLine(transform.position, target.position);

            if (isResetting)
            {
                Gizmos.color = Color.blue;
                Gizmos.DrawWireSphere(originalTargetPosition, 0.5f);
            }
        }
    }
}