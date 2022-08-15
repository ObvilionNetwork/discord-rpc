package ru.obvilion.discordrpc.api;

public enum DiscordBuild {
    CANARY("//canary.discord.com/api"),

    PTB("//ptb.discord.com/api"),

    STABLE("//discord.com/api"),

    ANY;

    private final String endpoint;

    DiscordBuild(String endpoint) {
        this.endpoint = endpoint;
    }

    DiscordBuild() {
        this(null);
    }

    public static DiscordBuild from(String endpoint){
        for (DiscordBuild value : values()) {
            if (value.endpoint != null && value.endpoint.equals(endpoint)) {
                return value;
            }
        }

        return ANY;
    }
}
