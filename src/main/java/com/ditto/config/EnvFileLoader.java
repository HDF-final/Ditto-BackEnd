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
        if (envFile == null) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String raw : lines) {
                applyLine(raw);
            }
        } catch (IOException ignored) {
            // .env 가 없으면 OS 환경 변수만 사용한다.
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
