using System;
using UnityEngine;

// Explicit opt-in: interactive scenes retain their original behaviour.
public static class UnityServerMode
{
    public static bool Enabled => Environment.GetEnvironmentVariable("MODEL_SERVER_MODE") == "1" ||
        Array.IndexOf(Environment.GetCommandLineArgs(), "-modelApi") >= 0;

    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
    private static void PrepareScene()
    {
        if (!Enabled) return;
        Time.timeScale = 1;
        Application.runInBackground = true;
        foreach (var motion in UnityEngine.Object.FindObjectsByType<MoveObj>(FindObjectsInactive.Include, FindObjectsSortMode.None))
            motion.StopServerMotion();
        // This scenario is the existing kinematic Phi rotation, not a dynamic-body simulation.
        // Freeze passive rigidbodies so the initial scene cannot drift before POST /runs.
        foreach (var body in UnityEngine.Object.FindObjectsByType<Rigidbody>(FindObjectsInactive.Include, FindObjectsSortMode.None))
            body.isKinematic = true;
        foreach (var canvas in UnityEngine.Object.FindObjectsByType<Canvas>(FindObjectsInactive.Include, FindObjectsSortMode.None))
        {
            var group = canvas.GetComponent<CanvasGroup>();
            if (group == null) group = canvas.gameObject.AddComponent<CanvasGroup>();
            group.interactable = false;
            group.blocksRaycasts = false;
        }
    }
}
