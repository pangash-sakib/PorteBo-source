package com.sakib.portebo.model;

public class AppItem {
    public enum Tier {
        WHITELIST,
        GREYLIST,
        BLACKLIST
    }

    private String packageName;
    private String name;
    private Tier tier;
    private int quotaSeconds;
    private int usedSeconds;
    private boolean isProtected;

    public AppItem(String packageName, String name, Tier tier, int quotaSeconds, int usedSeconds) {
        this.packageName = packageName;
        this.name = name;
        this.tier = tier;
        this.quotaSeconds = quotaSeconds > 0 ? quotaSeconds : 300;
        this.usedSeconds = usedSeconds;
        this.isProtected = false;
    }

    public AppItem(String packageName, String name, Tier tier, int quotaSeconds, int usedSeconds, boolean isProtected) {
        this.packageName = packageName;
        this.name = name;
        this.tier = tier;
        this.quotaSeconds = quotaSeconds > 0 ? quotaSeconds : 300;
        this.usedSeconds = usedSeconds;
        this.isProtected = isProtected;
    }

    public String getPackageName() { return packageName; }
    public String getName() { return name; }
    public String getRole() { return isProtected ? "সিস্টেম অ্যাপ · সবসময় খোলা" : "পড়ার সময়ের অ্যাপ"; }
    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
    public int getQuotaSeconds() { return quotaSeconds; }
    public void setQuotaSeconds(int quotaSeconds) { this.quotaSeconds = Math.max(60, Math.min(10800, quotaSeconds)); this.usedSeconds = Math.min(this.usedSeconds, this.quotaSeconds); }
    public int getUsedSeconds() { return usedSeconds; }
    public void setUsedSeconds(int usedSeconds) { this.usedSeconds = usedSeconds; }
    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean isProtected) { this.isProtected = isProtected; }
}