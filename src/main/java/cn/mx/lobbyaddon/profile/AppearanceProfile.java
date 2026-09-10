package cn.mx.lobbyaddon.profile;

public final class AppearanceProfile {

    private String prefix;
    private String suffix;
    private String temporaryNick;
    private String temporaryPrefix;
    private String temporarySuffix;
    private long nickExpiresAt;
    private long prefixExpiresAt;
    private long suffixExpiresAt;

    public AppearanceProfile copy() {
        AppearanceProfile copy = new AppearanceProfile();
        copy.prefix = prefix;
        copy.suffix = suffix;
        copy.temporaryNick = temporaryNick;
        copy.temporaryPrefix = temporaryPrefix;
        copy.temporarySuffix = temporarySuffix;
        copy.nickExpiresAt = nickExpiresAt;
        copy.prefixExpiresAt = prefixExpiresAt;
        copy.suffixExpiresAt = suffixExpiresAt;
        return copy;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getTemporaryNick() {
        return temporaryNick;
    }

    public void setTemporaryNick(String temporaryNick, long expiresAt) {
        this.temporaryNick = temporaryNick;
        this.nickExpiresAt = expiresAt;
    }

    public String getTemporaryPrefix() {
        return temporaryPrefix;
    }

    public void setTemporaryPrefix(String temporaryPrefix, long expiresAt) {
        this.temporaryPrefix = temporaryPrefix;
        this.prefixExpiresAt = expiresAt;
    }

    public String getTemporarySuffix() {
        return temporarySuffix;
    }

    public void setTemporarySuffix(String temporarySuffix, long expiresAt) {
        this.temporarySuffix = temporarySuffix;
        this.suffixExpiresAt = expiresAt;
    }

    public long getNickExpiresAt() {
        return nickExpiresAt;
    }

    public long getPrefixExpiresAt() {
        return prefixExpiresAt;
    }

    public long getSuffixExpiresAt() {
        return suffixExpiresAt;
    }

    public String getEffectiveNick(long now) {
        return temporaryNick != null && nickExpiresAt > now ? temporaryNick : null;
    }

    public String getEffectivePrefix(long now) {
        return temporaryPrefix != null && prefixExpiresAt > now ? temporaryPrefix : prefix;
    }

    public String getEffectiveSuffix(long now) {
        return temporarySuffix != null && suffixExpiresAt > now ? temporarySuffix : suffix;
    }

    public boolean clearExpired(long now) {
        boolean changed = false;
        if (temporaryNick != null && nickExpiresAt <= now) {
            temporaryNick = null;
            nickExpiresAt = 0L;
            changed = true;
        }
        if (temporaryPrefix != null && prefixExpiresAt <= now) {
            temporaryPrefix = null;
            prefixExpiresAt = 0L;
            changed = true;
        }
        if (temporarySuffix != null && suffixExpiresAt <= now) {
            temporarySuffix = null;
            suffixExpiresAt = 0L;
            changed = true;
        }
        return changed;
    }

    public boolean isEmpty() {
        return prefix == null && suffix == null && temporaryNick == null
                && temporaryPrefix == null && temporarySuffix == null;
    }
}
