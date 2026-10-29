using System;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using Newtonsoft.Json.Linq;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

public static class BuildModelApi
{
    private static string Hash(string file)
    {
        using (var sha=SHA256.Create()) using(var stream=File.OpenRead(file))
            return BitConverter.ToString(sha.ComputeHash(stream)).Replace("-","").ToLowerInvariant();
    }
    public static void Linux()
    {
        string output=Environment.GetEnvironmentVariable("UNITY_MODEL_BUILD_DIR");
        if (string.IsNullOrEmpty(output)) output=Path.GetFullPath("Builds/ModelApi");
        Directory.CreateDirectory(output);
        string manifest=Path.Combine(output,"model-api-build.json");
        // Remove validity marker before attempting build; an unsuccessful build cannot reuse an old marker.
        if(File.Exists(manifest)) File.Delete(manifest);
        var report=BuildPipeline.BuildPlayer(new BuildPlayerOptions {
            scenes=new[] {"Assets/Scenes/SampleScene.unity"},
            locationPathName=Path.Combine(output,"model.x86_64"),
            target=BuildTarget.StandaloneLinux64,
            subtarget=(int)StandaloneBuildSubtarget.Server,
            options=BuildOptions.None
        });
        if (report.summary.result!=BuildResult.Succeeded) throw new Exception("Linux model build failed: "+report.summary.result);
        var sources=new JObject();
        foreach(string source in Directory.GetFiles("Assets/Object/Script","*.cs").Concat(new[]{"Assets/Scenes/SampleScene.unity","Assets/Editor/BuildModelApi.cs"}))
            sources[source.Replace('\\','/')]=Hash(source);
        var binaries=new JObject();
        foreach(string file in Directory.GetFiles(output,"*",SearchOption.AllDirectories))
            binaries[file.Substring(output.Length+1).Replace('\\','/')]=Hash(file);
        File.WriteAllText(manifest,new JObject {
            ["apiVersion"]="1",["scenario"]="phi-relative-v1",["unityVersion"]=Application.unityVersion,
            ["scene"]="Assets/Scenes/SampleScene.unity",["subtarget"]="DedicatedServer",["builtAt"]=DateTime.UtcNow.ToString("o"),
            ["sources"]=sources,["files"]=binaries
        }.ToString());
    }
}
