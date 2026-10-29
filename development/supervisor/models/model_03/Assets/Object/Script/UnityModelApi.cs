using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using UnityEngine;

// UnityHttpServer queues Dispatch on the main thread. No worker reads any Unity state.
public sealed class UnityModelApi : MonoBehaviour
{
    public const double ToleranceDeg = 0.01;
    private MoveObj motion;
    private bool ready;
    private string readinessError;
    private string root, directory, runId, status;
    private JObject request, error, metadata, snapshot;
    private byte[] resultBytes;
    private readonly List<JObject> logs = new List<JObject>();
    private readonly List<JObject> publications = new List<JObject>();
    private string acceptedAt, startedAt, finishedAt;
    private double angle, speed, accumulated, modelElapsed, lastPublication;
    private Quaternion initial;
    private readonly Stopwatch clock = new Stopwatch();
    private static string Utc() => DateTime.UtcNow.ToString("o");
    private static byte[] Bytes(JToken value) => Encoding.UTF8.GetBytes(value.ToString(Formatting.None));
    private static bool Terminal(string value) => value == "SUCCEEDED" || value == "FAILED" || value == "CANCELLED";
    private static JObject Orientation(Quaternion q) => new JObject { ["x"]=q.x,["y"]=q.y,["z"]=q.z,["w"]=q.w };
    private void Write(string name, byte[] content)
    {
        string path = Path.Combine(directory,name), temp = path + ".tmp";
        File.WriteAllBytes(temp,content);
        if (File.Exists(path)) File.Delete(path);
        File.Move(temp,path);
    }
    public void Initialize(MoveObj component)
    {
        try
        {
            motion=component;
            if (motion == null || motion.phiObj == null || !motion.isActiveAndEnabled)
                throw new InvalidOperationException("Scene MoveObj and active Phi Transform are required");
            root=Environment.GetEnvironmentVariable("RUNS_ROOT") ?? Path.Combine(Application.persistentDataPath,"model-runs");
            Directory.CreateDirectory(root);
            motion.StopServerMotion();
            initial=motion.phiObj.localRotation;
            ready=true;
        }
        catch (Exception ex) { readinessError=ex.Message; ready=false; }
    }
    private JObject State() => new JObject {
        ["runId"]=runId,["status"]=status,["acceptedAt"]=acceptedAt,["startedAt"]=startedAt,
        ["finishedAt"]=finishedAt,["lastPublicationSeq"]=publications.Count,
        ["resultAvailable"]=status=="SUCCEEDED",["error"]=error?.DeepClone(),["snapshot"]=snapshot?.DeepClone()
    };
    private void SaveStatus() => Write("status.json",Bytes(State()));
    private void Log(string message)
    {
        var entry=new JObject {["seq"]=logs.Count+1,["timestamp"]=Utc(),["stream"]="model",["message"]=message};
        File.AppendAllText(Path.Combine(directory,"logs.jsonl"),entry.ToString(Formatting.None)+"\n");
        logs.Add(entry);
    }
    private JObject Snapshot() => new JObject {
        ["accumulatedRotationDeg"]=accumulated,["remainingAngleDeg"]=angle-accumulated,
        ["modelElapsedSeconds"]=modelElapsed,["realElapsedSeconds"]=clock.Elapsed.TotalSeconds,
        ["localOrientation"]=Orientation(motion.phiObj.localRotation)
    };
    private void Publish(bool force=false)
    {
        snapshot=Snapshot();
        if (!force && clock.Elapsed.TotalSeconds-lastPublication<0.25) return;
        var entry=new JObject {["seq"]=publications.Count+1,["publishedAt"]=Utc(),["values"]=snapshot.DeepClone()};
        File.AppendAllText(Path.Combine(directory,"publications.jsonl"),entry.ToString(Formatting.None)+"\n");
        publications.Add(entry);lastPublication=clock.Elapsed.TotalSeconds;
    }
    private void Transition(string next)
    {
        if (Terminal(status)) return;
        status=next;motion.ServerCalculationState=next.ToLowerInvariant();
        if (Terminal(next)) { clock.Stop();finishedAt=Utc(); }
        SaveStatus();
    }
    private static double SignedXDelta(Quaternion before,Quaternion after)
    {
        Quaternion d=Quaternion.Inverse(before)*after;
        if (d.w<0) d=new Quaternion(-d.x,-d.y,-d.z,-d.w);
        if (Math.Abs(d.y)>1e-4 || Math.Abs(d.z)>1e-4) throw new InvalidOperationException("Unexpected off-axis Phi rotation");
        return 2.0*Math.Atan2(d.x,d.w)*180.0/Math.PI;
    }
    private static double OrientationError(Quaternion expected,Quaternion actual)
    {
        Quaternion d=Quaternion.Inverse(expected)*actual;
        double v=Math.Sqrt((double)d.x*d.x+(double)d.y*d.y+(double)d.z*d.z);
        return 2*Math.Atan2(v,Math.Abs(d.w))*180/Math.PI;
    }
    private void Update()
    {
        if (status!="ACCEPTED" && status!="RUNNING") return;
        try
        {
            if (status=="ACCEPTED")
            {
                // Atomic configuration transfer, no UI command slot or integer conversion.
                motion.BeginServerPhi((float)angle,(float)speed);
                startedAt=Utc();modelElapsed=0;accumulated=0;clock.Restart();
                Log("Relative Phi started on the scene main thread");Transition("RUNNING");Publish(true);
                return;
            }
            Quaternion before=motion.phiObj.localRotation;
            float dt=Time.deltaTime;
            if (!float.IsFinite(dt) || dt<0) throw new InvalidOperationException("Invalid frame delta time");
            motion.StepServerPhi(dt);
            double actual=SignedXDelta(before,motion.phiObj.localRotation);
            if (actual < -1e-4) throw new InvalidOperationException("Phi moved opposite to requested direction");
            accumulated+=actual;modelElapsed+=dt;
            Publish();
            if (!motion.сhekRelativeMovePhi)
            {
                Transition("FINALIZING");
                double remaining=angle-accumulated;
                Quaternion expected=initial*Quaternion.AngleAxis((float)angle,Vector3.right);
                double orientationError=OrientationError(expected,motion.phiObj.localRotation);
                if (accumulated<=0 || Math.Abs(remaining)>ToleranceDeg || orientationError>ToleranceDeg || motion.HasMotion)
                    throw new InvalidOperationException("Phi completion verification failed: remaining="+remaining+", orientationError="+orientationError);
                motion.StopServerMotion();clock.Stop();Publish(true);
                var result=new JObject {
                    ["angleDeg"]=angle,["speedDegPerSec"]=speed,
                    ["initialLocalOrientation"]=Orientation(initial),["finalLocalOrientation"]=Orientation(motion.phiObj.localRotation),
                    ["accumulatedRotationDeg"]=accumulated,["remainingAngleDeg"]=remaining,["toleranceDeg"]=ToleranceDeg,
                    ["orientationErrorDeg"]=orientationError,["modelElapsedSeconds"]=modelElapsed,["realElapsedSeconds"]=clock.Elapsed.TotalSeconds
                };
                byte[] immutable=Bytes(result);
                Write("result.json",immutable);
                string hash;
                using (var sha=SHA256.Create()) hash=BitConverter.ToString(sha.ComputeHash(immutable)).Replace("-","").ToLowerInvariant();
                metadata=new JObject {["runId"]=runId,["schema"]="unity-phi-result/v1",["mediaType"]="application/json",
                    ["sizeBytes"]=immutable.Length,["sha256"]=hash,["downloadUrl"]="/api/v1/runs/"+runId+"/result",
                    ["units"]=new JObject {["angles"]="degree",["speed"]="degree per model second",["time"]="second",["orientation"]="local quaternion x,y,z,w"}};
                Write("metadata.json",Bytes(metadata));
                resultBytes=immutable;
                Log("Phi verified; immutable numerical result saved");Transition("SUCCEEDED");
            }
        }
        catch (Exception ex)
        {
            motion.StopServerMotion();clock.Stop();
            error=new JObject {["code"]="CALCULATION_ERROR",["message"]=ex.Message};
            Log(ex.Message);Publish(true);Transition("FAILED");
        }
    }
    private static void Reply(HttpListenerContext context,int code,JToken value)
    { ReplyBytes(context,code,Bytes(value)); }
    private static void ReplyBytes(HttpListenerContext context,int code,byte[] bytes)
    {
        try {
            context.Response.StatusCode=code;context.Response.ContentType="application/json";
            context.Response.ContentLength64=bytes.Length;context.Response.OutputStream.Write(bytes,0,bytes.Length);
            context.Response.Close();
        } catch (IOException) { } catch (HttpListenerException) { } catch (ObjectDisposedException) { }
    }
    private static void Reject(HttpListenerContext c,int status,string code,string message)
        => Reply(c,status,new JObject {["error"]=new JObject {["code"]=code,["message"]=message}});
    private static double Numeric(JToken token,string field,double minExclusive,double max)
    {
        if (token==null || (token.Type!=JTokenType.Integer && token.Type!=JTokenType.Float))
            throw new ArgumentException(field+" must be a number");
        double value=token.Value<double>();
        if (!double.IsFinite(value) || value<=minExclusive || value>max || !float.IsFinite((float)value) || (float)value<=0)
            throw new ArgumentException(field+" must be finite, > "+minExclusive+" and <= "+max+" (float32 model)");
        return value;
    }
    // Runs exclusively through UnityHttpServer's main-thread queue.
    public void Dispatch(HttpListenerContext context,string body)
    {
        try { DispatchCore(context,body); }
        catch (JsonException ex) { Reject(context,400,"INVALID_REQUEST",ex.Message); }
        catch (ArgumentException ex) { Reject(context,422,"INVALID_PARAMETERS",ex.Message); }
        catch (Exception ex) { Reject(context,500,"INTERNAL_ERROR",ex.Message); }
    }
    private void DispatchCore(HttpListenerContext c,string body)
    {
        string method=c.Request.HttpMethod,path=c.Request.Url.AbsolutePath;
        if (method=="GET" && path=="/api/v1/readiness")
        {
            Reply(c,ready && request==null?200:503,new JObject {["apiVersion"]="1",["modelId"]="model_03",
                ["ready"]=ready && request==null,["reason"]=request!=null?"CONTAINER_ALREADY_USED":readinessError,
                ["activeRunId"]=!Terminal(status)?runId:null,["sceneInitialLocalOrientation"]=ready?Orientation(initial):null,
                ["currentLocalOrientation"]=ready?Orientation(motion.phiObj.localRotation):null});return;
        }
        if (method=="POST" && path=="/api/v1/runs")
        {
            if (c.Request.ContentType==null || !c.Request.ContentType.StartsWith("application/json")) {Reject(c,415,"UNSUPPORTED_MEDIA_TYPE","Use application/json");return;}
            var candidate=JObject.Parse(body,new JsonLoadSettings {DuplicatePropertyNameHandling=DuplicatePropertyNameHandling.Error});
            if (candidate.Properties().Count()!=3 || candidate["runId"]?.Type!=JTokenType.String || candidate["experimentId"]?.Type!=JTokenType.String || !(candidate["parameters"] is JObject parameters))
                throw new ArgumentException("Expected runId, experimentId and parameters");
            foreach (string key in new[]{"runId","experimentId"})
                if (!Regex.IsMatch(candidate[key].Value<string>(),"^[A-Za-z0-9_-]{1,128}$")) throw new ArgumentException("Invalid "+key);
            if (request!=null)
            {
                if (candidate["runId"].Value<string>()!=runId) {Reject(c,409,"CONTAINER_ALREADY_USED","New runId requires a new container");return;}
                if (!JToken.DeepEquals(candidate,request)) {Reject(c,409,"RUN_ID_CONFLICT","Parameters and experimentId are immutable");return;}
                Reply(c,200,State());return;
            }
            if (!ready) {Reject(c,503,"MODEL_NOT_READY",readinessError??"Scene not ready");return;}
            if (parameters.Properties().Count()!=2 || parameters["angleDeg"]==null || parameters["speedDegPerSec"]==null)
                throw new ArgumentException("Only angleDeg and speedDegPerSec are supported");
            double a=Numeric(parameters["angleDeg"],"angleDeg",ToleranceDeg,360);
            double v=Numeric(parameters["speedDegPerSec"],"speedDegPerSec",0,36);
            if (OrientationError(initial,motion.phiObj.localRotation)>1e-4 || motion.HasMotion)
                throw new InvalidOperationException("Scene changed before calculation start");
            directory=Path.Combine(root,Guid.NewGuid().ToString("N"));Directory.CreateDirectory(directory);
            File.WriteAllText(Path.Combine(directory,"logs.jsonl"),"");File.WriteAllText(Path.Combine(directory,"publications.jsonl"),"");
            angle=a;speed=v;runId=candidate["runId"].Value<string>();request=(JObject)candidate.DeepClone();
            Write("request.json",Bytes(request));acceptedAt=Utc();status="ACCEPTED";motion.ServerCalculationState="accepted";SaveStatus();Log("Calculation accepted, scene has not moved yet");
            c.Response.Headers["Location"]="/api/v1/runs/"+runId;Reply(c,202,State());return;
        }
        Match route=Regex.Match(path,@"^/api/v1/runs/([A-Za-z0-9_-]+)(/cancel|/publications|/result/metadata|/result|/logs)?$");
        if (!route.Success) {Reject(c,404,"NOT_FOUND","Unknown endpoint");return;}
        if (request==null || route.Groups[1].Value!=runId) {Reject(c,404,"RUN_NOT_FOUND","Unknown runId");return;}
        string suffix=route.Groups[2].Value;
        if (suffix=="/cancel" && method=="POST")
        {
            if (!Terminal(status))
            {
                Transition("CANCELLING");motion.StopServerMotion();clock.Stop();
                Publish(true);Log("Motion stopped; cancellation confirmed on main thread");Transition("CANCELLED");
            }
            Reply(c,200,State());return;
        }
        if (method!="GET" || suffix=="/cancel") {Reject(c,405,"METHOD_NOT_ALLOWED","Unsupported method");return;}
        if (suffix=="") {Reply(c,200,State());return;}
        if (suffix=="/logs" || suffix=="/publications")
        {
            if (!long.TryParse(c.Request.QueryString["after"]??"0",out long after) || after<0 ||
                !int.TryParse(c.Request.QueryString["limit"]??"100",out int limit) || limit<1 || limit>1000)
                {Reject(c,400,"INVALID_REQUEST","after >= 0, limit 1..1000 required");return;}
            var items=suffix=="/logs"?logs:publications;
            var page=items.Where(x=>x["seq"].Value<long>()>after).Take(limit).Select(x=>x.DeepClone()).ToList();
            long next=page.Count>0?page[page.Count-1]["seq"].Value<long>():after;
            Reply(c,200,new JObject {["runId"]=runId,["items"]=new JArray(page),["nextAfter"]=next,
                ["hasMore"]=items.Count>next,["closed"]=Terminal(status)});return;
        }
        if (status!="SUCCEEDED") {Reject(c,409,Terminal(status)?"RESULT_UNAVAILABLE":"RESULT_NOT_READY","Result not available");return;}
        if (suffix=="/result/metadata") Reply(c,200,metadata.DeepClone());
        else ReplyBytes(c,200,resultBytes);
    }
    private void OnDestroy() { if (motion!=null && !Terminal(status)) motion.StopServerMotion(); }
}
