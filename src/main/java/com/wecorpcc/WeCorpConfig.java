package com.wecorpcc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("wecorpcc")
public interface WeCorpConfig extends Config
{
    @ConfigItem(
            keyName = "pluginMode",
            name = "Plugin Mode",
            description = "Choose Boosting, Solo, or Mass mode",
            position = 0
    )
    default PluginMode pluginMode()
    {
        return PluginMode.BOOSTING;
    }

    @ConfigItem(
            keyName = "showPlayerList",
            name = "Show Player List",
            description = "Show the list of players currently tracked at Corp",
            position = 1
    )
    default boolean showPlayerList()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showSpecs",
            name = "Show Spec Counters",
            description = "Show DWH, BGS, Elder Maul, and Voidwaker activity in Boosting mode",
            position = 2
    )
    default boolean showSpecs()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showKillTimer",
            name = "Show Kill Time",
            description = "Display the current Corp kill duration",
            position = 3
    )
    default boolean showKillTimer()
    {
        return true;
    }

    @ConfigItem(
            keyName = "lowHpWarning",
            name = "Low HP Warning",
            description = "Mark watched players under 40% HP in red and send a chat warning",
            position = 4
    )
    default boolean lowHpWarning()
    {
        return true;
    }

    @ConfigItem(
            keyName = "lowHpNames",
            name = "Low HP Watch List",
            description = "Enter player names separated by commas. Leave empty to warn for everyone.",
            position = 5
    )
    default String lowHpNames()
    {
        return "";
    }

    @ConfigItem(
            keyName = "killTarget",
            name = "Kill Target",
            description = "Send a chat message when the trip reaches this number of kills. Set to 0 to disable.",
            position = 6
    )
    default int killTarget()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "autoReset",
            name = "Auto Reset After 15 Min",
            description = "Automatically reset Boosting mode after 15 minutes away from Corp",
            position = 7
    )
    default boolean autoReset()
    {
        return true;
    }
}