package com.ditto.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 프로젝트 루트 {@code .env} 를 읽어 아직 없는 키만 시스템 프로퍼티로 넣는다.
 * 이미 OS 환경 변수가 있으면 덮어쓰지 않는다. Spring 이 {@code ${ORACLE_USERNAME}} 을 해석하게 한다.
 */
public final class EnvFileLoader {

    private EnvFileLoader() {
    }

    public static void load() {
        Path envFile = resolveEnvFile();
        if (envFile != null) {
            try {
                List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
                for (String raw : lines) {
                    applyLine(raw);
                }
            } catch (IOException ignored) {
                // .env 가 없으면 OS 환경 변수만 사용한다.
            }
        }
        loadAwsCliCredentialsIfPresent();
    }

    private static void loadAwsCliCredentialsIfPresent() {
        if (System.getenv("AWS_ACCESS_KEY_ID") != null || System.getProperty("aws.accessKeyId") != null) {
            return;
        }
        try {
            Process process = new ProcessBuilder("aws", "configure", "export-credentials", "--format", "env")
                    .redirectErrorStream(true)
                    .start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("export ")) {
                        line = line.substring(7).trim();
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String k = line.substring(0, eq).trim();
                            String v = line.substring(eq + 1).trim();
                            System.setProperty(k, v);
                            if ("AWS_ACCESS_KEY_ID".equals(k)) System.setProperty("aws.accessKeyId", v);
                            if ("AWS_SECRET_ACCESS_KEY".equals(k)) System.setProperty("aws.secretAccessKey", v);
                            if ("AWS_SESSION_TOKEN".equals(k)) System.setProperty("aws.sessionToken", v);
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
            // 로컬 AWS CLI가 없거나 실패 시 기본 SDK 체인에 위임
        }
    }

    private static Path resolveEnvFile() {
        Path cwd = Path.of(System.getProperty("user.dir"), ".env");
        if (Files.isRegularFile(cwd)) {
            return cwd;
        }
        return null;
    }

    private static void applyLine(String raw) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String key = line.substring(0, eq).trim();
        String value = unquote(line.substring(eq + 1).trim());
        if (key.isEmpty()) {
            return;
        }
        if (System.getenv(key) != null) {
            return;
        }
        if (System.getProperty(key) != null) {
            return;
        }
        System.setProperty(key, value);
        if ("AWS_PROFILE".equalsIgnoreCase(key) && System.getProperty("aws.profile") == null) {
            System.setProperty("aws.profile", value);
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
