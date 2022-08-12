package ru.obvilion.discordrpc.util;

public class OsUtil {
    public static final Os CURRENT_OS = parseOs(System.getProperty("os.name"));

    public static boolean isWindows() {
        return CURRENT_OS.equals(Os.WINDOWS);
    }

    public static Os parseOs(String name) {
        if (name.contains("win")) {
            return Os.WINDOWS;
        }

        if (name.contains("mac")) {
            return Os.MAC;
        }

        if (name.contains("nix") || name.contains("nux") || name.contains("aix")) {
            return Os.LINUX;
        }

        if (name.contains("sunos")) {
            return Os.SUNOS;
        }

        return Os.OTHER;
    }

    public enum Os {
        OTHER,
        WINDOWS,
        LINUX,
        MAC,
        SUNOS,
    }
}
