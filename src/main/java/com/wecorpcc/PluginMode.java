package com.wecorpcc;

public enum PluginMode
{
    BOOSTING("Boosting"),
    MASS("Mass"),
    SOLO("Solo");

    private final String displayName;

    PluginMode(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}