package ru.nsu.fit.evdokimova.supervisor.utils;

import java.time.Duration;

public class Constants {
    public static String START_JSONS_PATH = "/home/darya/skif_platform/development/supervisor/start_json_files/";
    public static String END_JSONS_PATH = "/home/darya/skif_platform/development/supervisor/end_json_files";

    public static String INPUT_PATH_HOST = "/home/darya/skif_platform/development/supervisor/start_json_files";
    public static String OUTPUT_PATH_HOST = "/home/darya/skif_platform/development/supervisor/end_json_files";

    public static String INPUT_PATH_CONTAINER = "/app/input";
    public static String OUTPUT_PATH_CONTAINER = "/app/output";

    public static Duration CONTAINER_TIMEOUT = Duration.ofMinutes(10);
}
