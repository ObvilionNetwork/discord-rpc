package ru.obvilion.discordrpc.api;

public class User {
    private final String name;
    private final String discriminator;
    private final String id;
    private final String avatar;

    public User(String name, String discriminator, String id, String avatar) {
        this.name = name;
        this.discriminator = discriminator;
        this.id = id;
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public long getIdLong() {
        return Long.parseLong(id);
    }

    public String getId() {
        return id;
    }

    public String getAvatarId() {
        return avatar;
    }

    public String getAvatarUrl() {
        return getAvatarId() == null ? null : "https://cdn.discord.com/avatars/" + getId() + "/" + getAvatarId()
            + (getAvatarId().startsWith("a_") ? ".gif" : ".png");
    }

    public String getDefaultAvatarId() {
        return DefaultAvatar.values()[Integer.parseInt(getDiscriminator()) % DefaultAvatar.values().length].toString();
    }

    public String getDefaultAvatarUrl() {
        return "https://discord.com/assets/" + getDefaultAvatarId() + ".png";
    }

    public String getEffectiveAvatarUrl() {
        return getAvatarUrl() == null ? getDefaultAvatarUrl() : getAvatarUrl();
    }
 
    public boolean isBot() {
        return false; //bots cannot use RPC
    }

    public String getAsMention() {
        return "<@" + id + '>';
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User))
            return false;

        User oUser = (User) o;

        return this == oUser || this.id.equals(oUser.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DiscordUser[" + getName() + '(' + id + ")]";
    }

    public enum DefaultAvatar {
        BLUE("6debd47ed13483642cf09e832ed0bc1b"),
        GREY("322c936a8c8be1b803cd94861bdfa868"),
        GREEN("dd4dbc0016779df1378e7812eabaa04d"),
        YELLOW("0e291f67c9274a1abdddeb3fd919cbaa"),
        RED("1cbd08c76f8af6dddce02c5138971129");

        private final String text;

        DefaultAvatar(String text)
        {
            this.text = text;
        }

        @Override
        public String toString()
        {
            return text;
        }
    }
}
