package com.wecorpcc;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import net.runelite.api.events.HitsplatApplied;
import java.util.List;
import java.util.Map;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.events.ConfigChanged;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
        name = "WeCorpCC",
        description = "Corp Beast helper for WeCorpCC - tracks players, specs, KPH and more",
        tags = {"corp", "corporeal beast", "boosting", "wecorp"}
)
public class WeCorpPlugin extends Plugin {
    private static final int CORP_REGION = 11844;
    private static final int CORP_ID = 319;

    private boolean pendingSoloDwh;
    private boolean pendingSoloBgs;
    private boolean pendingSoloArclight;
    private int pendingSoloSpecGameCycle = 0;

    private boolean pendingMassBgs;
    private int pendingMassBgsGameCycle = 0;

    private static final int FANG_SPEC_ANIM = 11222;
    private static final int DWH_ANIM = 1378;
    private static final int BGS_ANIM = 7642;
    private static final int BGS_ANIM_2 = 7643;
    private static final int ELDER_MAUL_ANIM = 7516;
    private static final int VOIDWAKER_ANIM = 11275;

    private static final int CORP_RESPAWN_SECONDS = 30;
    private long lastAtCorpTime;
    private int lastSpecialEnergy = -1;
    private long killStartTime;
    private long tripStartTime;
    private long lastDropTime;
    private long corpDeathTime;
    private static final int ARCLIGHT_ID = 19675;
    private String lastDropMessage = "";

    private boolean corpAlive;
    // Locks one KC to one real Corp spawn/death cycle.
    private boolean corpKillProcessed;
    private boolean atCorp;

    private int totalKills;
    private long firstKillTime = 0;
    private int notAtCorpTicks;
    private int lastAnnouncedTarget;
    private int tickCounter;

    private final Set<String> customers = new HashSet<>();
    private final Set<String> hiddenPlayers = new HashSet<>();
    private final Set<String> lastKillAttendees = new HashSet<>();
    private final Set<String> lowHpWarned = new HashSet<>();
    private final Set<String> lowHpPlayers = new HashSet<>();
    private final Set<String> currentKillAttendees = new HashSet<>();

    private final Map<String, Long> lastSeenAtCorp = new HashMap<>();

    private final Map<String, Integer> dwhCount = new HashMap<>();
    private final Map<String, Integer> bgsCount = new HashMap<>();
    private final Map<String, Integer> maulCount = new HashMap<>();
    private final Map<String, Integer> voidwakerCount = new HashMap<>();
    private final Map<String,Integer> fangCount =
            new HashMap<>();
    private final Map<String, Integer> killCount = new HashMap<>();
    private final Map<String, Integer> massFangCount =
            new HashMap<>();

    private final Map<String, Integer> massVoidwakerCount =
            new HashMap<>();

    private final Map<String, Integer> massDwhCount =
            new HashMap<>();

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private WeCorpConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ClientToolbar clientToolbar;

    private WeCorpPanel panel;
    private MassPanel massPanel;
    private SoloPanel soloPanel;
    private ModePanel modePanel;

    private NavigationButton navButton;


    @Override
    protected void startUp()
    {
        panel = new WeCorpPanel();

        massPanel = new MassPanel();
        massPanel.setResetCallback(this::resetMassMode);

        soloPanel = new SoloPanel();

        soloPanel.setReadyCallback(() ->
                clientThread.invoke(() ->
                        client.addChatMessage(
                                ChatMessageType.GAMEMESSAGE,
                                "",
                                "<col=00ff00>WeCorpCC: Corp is ready to kill!</col>",
                                null
                        )
                )
        );

        modePanel = new ModePanel(
                panel,
                massPanel,
                soloPanel,
                mode ->
                {
                    configManager.setConfiguration(
                            "wecorpcc",
                            "pluginMode",
                            mode
                    );

                    applyPluginMode();
                }
        );

        panel.setPackageCompleteCallback(message ->
                clientThread.invoke(() ->
                        client.addChatMessage(
                                ChatMessageType.GAMEMESSAGE,
                                "",
                                "<col=ffff00>" + message + "</col>",
                                null
                        )
                )
        );

        panel.setResetCallback(this::fullReset);

        panel.setGiveKillCallback(name ->
        {
            String cleanName = removeYouSuffix(name);

            killCount.put(
                    cleanName,
                    killCount.getOrDefault(cleanName, 0) + 1
            );

            if (massPanel != null)
            {
                massPanel.setTotalKills(totalKills);
            }

            updatePlayerList();
        });

        BufferedImage icon =
                new BufferedImage(
                        16,
                        16,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                icon.createGraphics();

        try
        {
            graphics.setColor(
                    new Color(30, 30, 30)
            );

            graphics.fillRect(
                    0,
                    0,
                    16,
                    16
            );

            graphics.setColor(
                    Color.WHITE
            );

            graphics.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            12
                    )
            );

            graphics.drawString(
                    "W",
                    2,
                    13
            );
        }
        finally
        {
            graphics.dispose();
        }

        navButton =
                NavigationButton.builder()
                        .tooltip("WeCorpCC")
                        .icon(icon)
                        .priority(5)
                        .panel(modePanel)
                        .build();

        clientToolbar.addNavigation(
                navButton
        );

        applyPluginMode();

        updateWaitingStatus();

        lastSpecialEnergy =
                client.getVarpValue(300);
    }
    private void applyPluginMode()
    {
        PluginMode mode =
                config.pluginMode();

        if (mode == null)
        {
            mode = PluginMode.BOOSTING;
        }

        if (modePanel != null)
        {
            modePanel.showMode(mode);
        }
    }

    @Subscribe
    public void onConfigChanged(
            ConfigChanged event)
    {
        if (event == null ||
                !"wecorpcc".equals(
                        event.getGroup()
                ))
        {
            return;
        }

        if ("pluginMode".equals(
                event.getKey()
        ))
        {
            applyPluginMode();
        }
    }

    private void resetMassMode()
    {
        clearPendingMassBgs();

        /*
         * Clear Mass trip spec totals.
         */
        massFangCount.clear();
        massVoidwakerCount.clear();
        massDwhCount.clear();

        /*
         * Clear the Mass kill totals stored in the plugin.
         *
         * Without this, updatePlayerList() sends the previous
         * trip's KC back to the Mass panel after the next refresh.
         */
        killCount.clear();
        totalKills = 0;

        /*
         * Clear attendance and previous-drop information so the
         * new Mass begins completely fresh.
         */
        currentKillAttendees.clear();
        lastKillAttendees.clear();

        lastDropMessage = "";
        lastDropTime = 0;

        /*
         * Reset trip and kill timing.
         */
        tripStartTime = 0;
        killStartTime = 0;
        corpDeathTime = 0;
        corpAlive = false;

        if (massPanel != null)
        {
            massPanel.resetPanel();
            massPanel.setTotalKills(0);
        }

        updatePlayerList();
    }

    @Override
    protected void shutDown() {
        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
        }

        clearData();
        lastSpecialEnergy = -1;
        customers.clear();
        hiddenPlayers.clear();
        lastSeenAtCorp.clear();
        killCount.clear();
        lowHpPlayers.clear();
        lowHpWarned.clear();

    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }

        return removeYouSuffix(name)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String removeYouSuffix(String name) {
        if (name == null) {
            return "";
        }

        return name.replace(" (You)", "").trim();
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        Player localPlayer = client.getLocalPlayer();

        if (localPlayer == null) {
            return;
        }
        if (pendingSoloDwh ||
                pendingSoloBgs ||
                pendingSoloArclight)
        {
            int elapsedCycles =
                    client.getGameCycle() -
                            pendingSoloSpecGameCycle;

            if (elapsedCycles < 0 ||
                    elapsedCycles > 120)
            {
                clearPendingSoloSpec();
            }
        }

        if (pendingMassBgs)
        {
            int elapsedCycles =
                    client.getGameCycle() -
                            pendingMassBgsGameCycle;

            if (elapsedCycles < 0 ||
                    elapsedCycles > 120)
            {
                clearPendingMassBgs();
            }
        }

        WorldPoint location = localPlayer.getWorldLocation();

        boolean inCorpRegion =
                location != null && location.getRegionID() == CORP_REGION;

        boolean corpFound = false;

        for (NPC npc : client.getNpcs()) {
            if (npc != null && npc.getId() == CORP_ID) {
                corpFound = true;
                break;
            }
        }

        if (corpFound || inCorpRegion) {
            atCorp = true;
            notAtCorpTicks = 0;
        } else {
            /*
             * The local player has left the Corp region.
             * Reset specs immediately instead of waiting several ticks.
             */
            if (atCorp) {
                atCorp = false;

                clearData();
                updatePlayerList();

            }

            notAtCorpTicks = 0;
        }


        if (!atCorp) {
            handleOutsideCorp();
            return;
        }

        lastAtCorpTime = System.currentTimeMillis();

        if (tripStartTime == 0) {
            tripStartTime = System.currentTimeMillis();
        }

        if (!corpAlive && corpFound) {
            corpAlive = true;
            killStartTime = System.currentTimeMillis();
            corpDeathTime = 0;
        }

        String kphValue = calculateKph();

        long tripSeconds =
                (System.currentTimeMillis() - tripStartTime) / 1000;

        long minutes = tripSeconds / 60;
        long seconds = tripSeconds % 60;

        int playerCount = countVisiblePlayers();

        if (config.showKillTimer() && corpAlive && killStartTime > 0) {
            long killSeconds =
                    (System.currentTimeMillis() - killStartTime) / 1000;

            updateStatusPanel(playerCount, kphValue, killSeconds);
        } else if (!corpAlive && corpDeathTime > 0) {
            long remaining = getRespawnSecondsRemaining();

            panel.setStatus(
                    "<html><center>" +
                            "<b>At Corp</b><br>" +
                            "Respawn: " + remaining + "s<br>" +
                            "<font color='#0066CC'><b>Kills: " +
                            totalKills +
                            "</b></font><br>" +
                            "KPH: " + kphValue + "<br>" +
                            "Players: " + playerCount + "<br>" +
                            "Trip: " + minutes + "m " + seconds + "s" +
                            "</center></html>"
            );
        } else {
            panel.setStatus(
                    "<html><center>" +
                            "<b>At Corp</b><br>" +
                            "<font color='#0066CC'><b>Kills: " +
                            totalKills +
                            "</b></font><br>" +
                            "KPH: " + kphValue + "<br>" +
                            "Players: " + playerCount + "<br>" +
                            "Trip: " + minutes + "m " + seconds + "s" +
                            "</center></html>"
            );
        }

        int target = config.killTarget();

        if (target > 0 &&
                totalKills >= target &&
                lastAnnouncedTarget < target) {
            client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    "<col=0066CC>WeCorpCC: Target reached! " +
                            totalKills +
                            " kills.</col>",
                    null
            );

            lastAnnouncedTarget = target;
        }

        tickCounter++;

        if (tickCounter >= 10) {
            tickCounter = 0;
            updatePlayerList();
        }
    }

    private void handleOutsideCorp() {
        String kphValue = calculateKph();

        long tripSeconds =
                tripStartTime > 0
                        ? (System.currentTimeMillis() - tripStartTime) / 1000
                        : 0;

        long minutes = tripSeconds / 60;
        long seconds = tripSeconds % 60;

        if (!corpAlive && corpDeathTime > 0) {
            long remaining = getRespawnSecondsRemaining();

            panel.setStatus(
                    "<html><center>" +
                            "<b>Waiting for Corp...</b><br>" +
                            "Respawn: " + remaining + "s<br>" +
                            "<font color='#0066CC'><b>Kills: " +
                            totalKills +
                            "</b></font><br>" +
                            "KPH: " + kphValue + "<br>" +
                            "Players: 0<br>" +
                            "Trip: " + minutes + "m " + seconds + "s" +
                            "</center></html>"
            );
        } else {
            panel.setStatus(
                    "<html><center>" +
                            "<b>Waiting for Corp...</b><br>" +
                            "<font color='#0066CC'><b>Kills: " +
                            totalKills +
                            "</b></font><br>" +
                            "KPH: " + kphValue + "<br>" +
                            "Players: 0<br>" +
                            "Trip: " + minutes + "m " + seconds + "s" +
                            "</center></html>"
            );
        }

        if (config.autoReset() &&
                lastAtCorpTime > 0 &&
                System.currentTimeMillis() - lastAtCorpTime >
                        15 * 60 * 1000L) {
            fullReset();
            lastAtCorpTime = 0;
        }
    }

    private int countVisiblePlayers() {
        int playerCount = 0;
        Player localPlayer = client.getLocalPlayer();

        if (localPlayer != null) {
            playerCount++;
        }

        for (Player player : client.getPlayers()) {
            if (player != null &&
                    player.getName() != null &&
                    player != localPlayer) {
                playerCount++;
            }
        }

        return playerCount;
    }

    private long getRespawnSecondsRemaining() {
        long elapsed =
                (System.currentTimeMillis() - corpDeathTime) / 1000;

        return Math.max(0, CORP_RESPAWN_SECONDS - elapsed);
    }

    private String calculateKph() {
        if (tripStartTime <= 0 || totalKills <= 0) {
            return "0";
        }

        double hours =
                (System.currentTimeMillis() - tripStartTime) / 3_600_000.0;

        if (hours <= 0) {
            return "0";
        }

        return String.valueOf((int) (totalKills / hours));
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (!atCorp ||
                !(event.getActor() instanceof Player))
        {
            return;
        }

        Player player =
                (Player) event.getActor();

        String name =
                player.getName();

        if (name == null)
        {
            return;
        }

        int animation =
                player.getAnimation();
        if (animation == FANG_SPEC_ANIM)
        {
            fangCount.put(
                    name,
                    fangCount.getOrDefault(name, 0) + 1
            );

            massFangCount.put(
                    name,
                    massFangCount.getOrDefault(name, 0) + 1
            );

            if (config.pluginMode() == PluginMode.MASS &&
                    massPanel != null)
            {
                massPanel.addFangSpec();
            }
        }
        else if (animation == DWH_ANIM)
        {
            /*
             * Existing Boosting tracking.
             */
            massDwhCount.put(
                    name,
                    massDwhCount.getOrDefault(name, 0) + 1
            );
            dwhCount.put(
                    name,
                    dwhCount.getOrDefault(name, 0) + 1
            );

            /*
             * Optional Mass DWH counter.
             */
            if (config.pluginMode() == PluginMode.MASS &&
                    massPanel != null)
            {
                massPanel.addDwhSpec();
            }


            /*
             * Solo waits for a successful hitsplat.
             */
            if (config.pluginMode() == PluginMode.SOLO &&
                    soloPanel != null &&
                    player == client.getLocalPlayer())
            {
                pendingSoloDwh = true;
                pendingSoloBgs = false;
                pendingSoloArclight = false;

                pendingSoloSpecGameCycle =
                        client.getGameCycle();
            }
        }
        else if (animation == BGS_ANIM ||
                animation == BGS_ANIM_2)
        {
            /*
             * Existing Boosting tracking.
             */
            bgsCount.put(
                    name,
                    bgsCount.getOrDefault(name, 0) + 1
            );

            /*
             * Mass Mode tracks the local player's actual BGS damage.
             * Remote-player hitsplats cannot be safely assigned here.
             */
            if (config.pluginMode() == PluginMode.MASS &&
                    massPanel != null &&
                    player == client.getLocalPlayer())
            {
                pendingMassBgs = true;
                pendingMassBgsGameCycle =
                        client.getGameCycle();
            }

            /*
             * Solo waits for the actual BGS damage hitsplat.
             */
            if (config.pluginMode() == PluginMode.SOLO &&
                    soloPanel != null &&
                    player == client.getLocalPlayer())
            {
                pendingSoloBgs = true;
                pendingSoloDwh = false;
                pendingSoloArclight = false;

                pendingSoloSpecGameCycle =
                        client.getGameCycle();
            }
        }
        else if (animation == VOIDWAKER_ANIM)
        {
            /*
             * Existing Boosting tracking.
             */
            voidwakerCount.put(
                    name,
                    voidwakerCount.getOrDefault(name, 0) + 1
            );

            massVoidwakerCount.put(
                    name,
                    massVoidwakerCount.getOrDefault(name, 0) + 1
            );

            /*
             * Mass current-kill and trip Voidwaker counter.
             */
            if (config.pluginMode() == PluginMode.MASS &&
                    massPanel != null)
            {
                massPanel.addVoidwakerSpec();
            }
        }

        updatePlayerList();
    }
    private boolean isElderMaulEquipped()
    {
        net.runelite.api.ItemContainer equipment =
                client.getItemContainer(
                        net.runelite.api.InventoryID.EQUIPMENT
                );

        if (equipment == null)
        {
            return false;
        }

        net.runelite.api.Item weapon =
                equipment.getItem(
                        net.runelite.api.EquipmentInventorySlot.WEAPON
                                .getSlotIdx()
                );

        if (weapon == null)
        {
            return false;
        }

        /*
         * RuneLite's Special Attack Counter identifies Elder Maul from
         * the equipped weapon when special-attack energy decreases.
         * 21003 = Elder maul, 27100 = Elder maul (ornament).
         */
        int weaponId = weapon.getId();

        return weaponId == 21003 ||
                weaponId == 27100;
    }

    private boolean isArclightEquipped()
    {
        net.runelite.api.ItemContainer equipment =
                client.getItemContainer(
                        net.runelite.api.InventoryID.EQUIPMENT
                );

        if (equipment == null)
        {
            return false;
        }

        net.runelite.api.Item weapon =
                equipment.getItem(
                        net.runelite.api.EquipmentInventorySlot.WEAPON
                                .getSlotIdx()
                );

        return weapon != null &&
                weapon.getId() == ARCLIGHT_ID;
    }
    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (!(event.getActor() instanceof NPC) ||
                event.getHitsplat() == null)
        {
            return;
        }

        NPC npc = (NPC) event.getActor();

        if (npc.getId() != CORP_ID ||
                !event.getHitsplat().isMine())
        {
            return;
        }

        int damage =
                event.getHitsplat().getAmount();

        /*
         * Mass Mode: count the local player's real BGS damage only.
         */
        if (config.pluginMode() == PluginMode.MASS &&
                massPanel != null &&
                pendingMassBgs)
        {
            int elapsedCycles =
                    client.getGameCycle() -
                            pendingMassBgsGameCycle;

            clearPendingMassBgs();

            if (elapsedCycles >= 0 &&
                    elapsedCycles <= 120 &&
                    damage > 0)
            {
                massPanel.addBgsDamage(damage);
            }

            return;
        }

        /*
         * Solo Mode logic remains unchanged.
         */
        if (config.pluginMode() != PluginMode.SOLO ||
                soloPanel == null)
        {
            return;
        }

        int elapsedCycles =
                client.getGameCycle() -
                        pendingSoloSpecGameCycle;

        if (elapsedCycles < 0 ||
                elapsedCycles > 120)
        {
            clearPendingSoloSpec();
            return;
        }

        if (pendingSoloDwh)
        {
            clearPendingSoloSpec();

            if (damage > 0)
            {
                soloPanel.addDwhSpec();
            }

            return;
        }

        if (pendingSoloBgs)
        {
            clearPendingSoloSpec();

            if (damage > 0)
            {
                soloPanel.addBgsDamage(damage);
            }

            return;
        }

        if (pendingSoloArclight)
        {
            clearPendingSoloSpec();

            if (damage > 0)
            {
                soloPanel.addArclightHit();
            }
        }
    }

    private void clearPendingSoloSpec()
    {
        pendingSoloDwh = false;
        pendingSoloBgs = false;
        pendingSoloArclight = false;
        pendingSoloSpecGameCycle = 0;
    }

    private void clearPendingMassBgs()
    {
        pendingMassBgs = false;
        pendingMassBgsGameCycle = 0;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE &&
                event.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        String message = event.getMessage();

        if (message == null)
        {
            return;
        }

        String cleanMessage = message
                .replaceAll("<[^>]*>", "")
                .trim();

        String lowerMessage =
                cleanMessage.toLowerCase();

        /*
         * Only process Corporeal Beast drop messages.
         */
        if (!lowerMessage.contains("received a drop") ||
                !lowerMessage.contains("corporeal beast"))
        {
            return;
        }

        /*
         * Prevent processing the same Corp drop message twice.
         */
        if (cleanMessage.equals(lastDropMessage) &&
                System.currentTimeMillis() - lastDropTime < 3000)
        {
            return;
        }

        lastDropMessage = cleanMessage;
        lastDropTime = System.currentTimeMillis();

        int index =
                lowerMessage.indexOf(" received a drop");

        if (index <= 0)
        {
            return;
        }

        String receiverName =
                cleanMessage
                        .substring(0, index)
                        .trim();

        String normalizedReceiverName =
                normalizeName(receiverName);

        /*
         * Save everyone who attended this kill before clearing
         * the current-kill attendance set.
         */
        lastKillAttendees.clear();
        lastKillAttendees.addAll(
                currentKillAttendees
        );

        currentKillAttendees.clear();

        /*
         * Give the receiver +1 KC.
         */
        boolean found = false;

        for (String existingName :
                new HashSet<>(killCount.keySet()))
        {
            if (normalizeName(existingName)
                    .equals(normalizedReceiverName))
            {
                killCount.put(
                        existingName,
                        killCount.getOrDefault(
                                existingName,
                                0
                        ) + 1
                );

                found = true;
                break;
            }
        }

        if (!found)
        {
            String cleanReceiverName =
                    removeYouSuffix(receiverName);

            killCount.put(
                    cleanReceiverName,
                    1
            );
        }

        /*
         * Existing Boosting customer package tracking.
         */
        panel.recordCustomerKill(
                receiverName
        );

        totalKills++;

        /*
         * Mass Mode total KC and valuable-drop tracking.
         */
        if (massPanel != null)
        {
            massPanel.setTotalKills(
                    totalKills
            );

            if (lowerMessage.contains("elysian sigil"))
            {
                massPanel.addElysianSigil(
                        receiverName,
                        new HashSet<>(
                                lastKillAttendees
                        )
                );
            }
            else if (lowerMessage.contains("spectral sigil"))
            {
                massPanel.addSpectralSigil(
                        receiverName,
                        new HashSet<>(
                                lastKillAttendees
                        )
                );
            }
            else if (lowerMessage.contains("arcane sigil"))
            {
                massPanel.addArcaneSigil(
                        receiverName,
                        new HashSet<>(
                                lastKillAttendees
                        )
                );
            }
            else if (lowerMessage.contains("onyx"))
            {
                massPanel.addOnyx();
            }

            /*
             * Prepare the top counters for the next Corp kill.
             * Player trip totals remain saved.
             */
            massPanel.resetCurrentKillSpecs();
        }

        /*
         * Corp has died.
         *
         * Start the respawn countdown and reset the current kill timer.
         */
        corpAlive = false;
        corpDeathTime =
                System.currentTimeMillis();

        killStartTime = 0;

        clearPendingSoloSpec();
        clearPendingMassBgs();

        if (soloPanel != null)
        {
            soloPanel.resetProgress();
        }

        /*
         * Reset Boosting DWH, BGS, Elder Maul and Voidwaker
         * information for the next Corp kill.
         */
        clearData();

        updatePlayerList();
    }
    private void updatePlayerList()
    {
        Map<String, String> playerInfo =
                new LinkedHashMap<>();

        Map<String, MassPanel.PlayerMassData> massPlayerInfo =
                new LinkedHashMap<>();

        lowHpPlayers.clear();

        if (atCorp)
        {
            long now =
                    System.currentTimeMillis();

            Player localPlayer =
                    client.getLocalPlayer();

            if (localPlayer != null &&
                    localPlayer.getName() != null)
            {
                String name =
                        localPlayer.getName();

                String normalizedName =
                        normalizeName(name);

                lastSeenAtCorp.put(
                        normalizedName,
                        now
                );

                /*
                 * Boosting panel:
                 * Keep the existing hidden-player behavior.
                 */
                if (!hiddenPlayers.contains(normalizedName))
                {
                    playerInfo.put(
                            name + " (You)",
                            getSpecText(name)
                    );
                }

                /*
                 * Mass panel:
                 * MassPanel manages its own removed-player list.
                 */
                massPlayerInfo.put(
                        name + " (You)",
                        new MassPanel.PlayerMassData(
                                name + " (You)",
                                getKillCountForName(name),
                                massFangCount.getOrDefault(name, 0),
                                massVoidwakerCount.getOrDefault(name, 0),
                                massDwhCount.getOrDefault(name, 0)
                        )
                );

                checkLowHp(
                        localPlayer,
                        name
                );
            }

            for (Player player :
                    client.getPlayers())
            {
                if (player == null ||
                        player.getName() == null ||
                        player == localPlayer)
                {
                    continue;
                }

                String name =
                        player.getName();

                String normalizedName =
                        normalizeName(name);

                lastSeenAtCorp.put(
                        normalizedName,
                        now
                );

                if (!hiddenPlayers.contains(normalizedName))
                {
                    playerInfo.put(
                            name,
                            getSpecText(name)
                    );
                }

                massPlayerInfo.put(
                        name,
                        new MassPanel.PlayerMassData(
                                name,
                                getKillCountForName(name),
                                massFangCount.getOrDefault(name, 0),
                                massVoidwakerCount.getOrDefault(name, 0),
                                massDwhCount.getOrDefault(name, 0)
                        )
                );

                checkLowHp(
                        player,
                        name
                );
            }
        }

        /*
         * Update Boosting Mode exactly as before.
         */
        panel.updatePlayers(
                playerInfo,
                lowHpPlayers,
                lastKillAttendees,
                killCount,
                hiddenPlayers
        );

        /*
         * Update Mass Mode separately.
         */
        if (massPanel != null)
        {
            massPanel.setMassActive(atCorp);
            massPanel.updatePlayers(
                    massPlayerInfo
            );
        }
    }

    private int getKillCountForName(String name)
    {
        String normalizedName = normalizeName(name);

        for (Map.Entry<String, Integer> entry : killCount.entrySet())
        {
            if (normalizeName(entry.getKey()).equals(normalizedName))
            {
                return entry.getValue();
            }
        }

        return 0;
    }

    private String getSpecText(String name)
    {
        List<String> parts = new ArrayList<>();

        if (config.showSpecs())
        {
            int dwh = dwhCount.getOrDefault(name, 0);
            int bgs = bgsCount.getOrDefault(name, 0);
            int maul = maulCount.getOrDefault(name, 0);
            int voidwaker =
                    voidwakerCount.getOrDefault(name, 0);

            if (dwh > 0)
            {
                parts.add("DWH:" + dwh);
            }

            if (bgs > 0)
            {
                parts.add("BGS:" + bgs);
            }

            if (maul > 0)
            {
                parts.add("Maul:" + maul);
            }

            if (voidwaker > 0)
            {
                parts.add("VW:" + voidwaker);
            }
        }

        return parts.isEmpty()
                ? ""
                : String.join(" ", parts);
    }
    private void updateStatusPanel(
            int playerCount,
            String kphValue,
            long killSeconds)
    {
        long tripSeconds = tripStartTime > 0
                ? (System.currentTimeMillis() - tripStartTime) / 1000
                : 0;

        long minutes = tripSeconds / 60;
        long seconds = tripSeconds % 60;

        panel.setStatus(
                "<html><center>" +
                        "<b>At Corp</b><br>" +
                        "<font color='#0066CC'><b>Kills: " +
                        totalKills +
                        "</b></font><br>" +
                        "KPH: " + kphValue + "<br>" +
                        "Trip: " + minutes + "m " + seconds + "s<br>" +
                        "Kill: " + killSeconds + "s<br>" +
                        "Players: " + playerCount +
                        "</center></html>"
        );
    }

    private void clearData()
    {
        dwhCount.clear();
        bgsCount.clear();
        maulCount.clear();
        voidwakerCount.clear();

        if (panel != null)
        {
            panel.clearDisplayedSpecs();
        }
    }

    private void fullReset()
    {
        clearPendingSoloSpec();
        clearPendingMassBgs();
        firstKillTime = 0;
        if (totalKills > 0 || tripStartTime > 0)
        {
            long tripSeconds = tripStartTime > 0
                    ? (System.currentTimeMillis() - tripStartTime) / 1000
                    : 0;

            long minutes = tripSeconds / 60;
            long seconds = tripSeconds % 60;

            String kphValue = calculateKph();

            final String summaryTitle =
                    "<col=0066CC>WeCorpCC Session Summary</col>";

            final String summaryDetails =
                    "Kills: " + totalKills +
                            " | KPH: " + kphValue +
                            " | Time: " +
                            minutes + "m " + seconds + "s";

            clientThread.invoke(() ->
            {
                client.addChatMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "",
                        summaryTitle,
                        null
                );

                client.addChatMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "",
                        summaryDetails,
                        null
                );
            });
        }

        clearData();

        killCount.clear();
        lastKillAttendees.clear();
        lowHpWarned.clear();
        lowHpPlayers.clear();
        currentKillAttendees.clear();
        customers.clear();
        hiddenPlayers.clear();
        lastSeenAtCorp.clear();

        totalKills = 0;
        tripStartTime = 0;
        killStartTime = 0;
        corpDeathTime = 0;
        corpAlive = false;
        atCorp = false;
        lastAnnouncedTarget = 0;
        lastAtCorpTime = 0;
        notAtCorpTicks = 0;
        tickCounter = 0;
        lastDropMessage = "";
        lastDropTime = 0;

        panel.resetPanel();
        updateWaitingStatus();
        updatePlayerList();

    }

    private void updateWaitingStatus()
    {
        panel.setStatus(
                "<html><center>" +
                        "<b>Waiting for Corp...</b><br>" +
                        "<font color='#0066CC'><b>Kills: 0</b></font><br>" +
                        "KPH: 0<br>" +
                        "Trip: 0m 0s<br>" +
                        "Kill: 0s<br>" +
                        "Players: 0" +
                        "</center></html>"
        );
    }

    private void checkLowHp(Player player, String name)
    {
        String normalizedName = normalizeName(name);

        if (!config.lowHpWarning())
        {
            lowHpPlayers.remove(normalizedName);
            lowHpWarned.remove(normalizedName);
            return;
        }

        String configuredNames = config.lowHpNames();

        String watchList =
                configuredNames == null
                        ? ""
                        : configuredNames.trim();

        if (!watchList.isEmpty())
        {
            boolean inList = false;

            for (String watchedName : watchList.split(","))
            {
                if (watchedName.trim().equalsIgnoreCase(name))
                {
                    inList = true;
                    break;
                }
            }

            if (!inList)
            {
                lowHpPlayers.remove(normalizedName);
                lowHpWarned.remove(normalizedName);
                return;
            }
        }

        int healthRatio = player.getHealthRatio();
        int healthScale = player.getHealthScale();

        if (healthScale <= 0 || healthRatio < 0)
        {
            return;
        }

        double percentage =
                (double) healthRatio / healthScale * 100.0;

        if (percentage <= 40.0)
        {
            lowHpPlayers.add(normalizedName);

            if (!lowHpWarned.contains(normalizedName))
            {
                client.addChatMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "",
                        name + " is low HP!",
                        null
                );

                lowHpWarned.add(normalizedName);
            }
        }
        else
        {
            lowHpPlayers.remove(normalizedName);
            lowHpWarned.remove(normalizedName);
        }
    }
    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (event.getVarpId() != 300)
        {
            return;
        }

        int currentSpecialEnergy =
                client.getVarpValue(300);

        if (lastSpecialEnergy < 0)
        {
            lastSpecialEnergy = currentSpecialEnergy;
            return;
        }

        boolean specialEnergyDropped =
                currentSpecialEnergy < lastSpecialEnergy;

        lastSpecialEnergy = currentSpecialEnergy;

        if (!specialEnergyDropped)
        {
            return;
        }

        if (!atCorp)
        {
            return;
        }

        /*
         * Reliable local Elder Maul special detection:
         * RuneLite itself detects a special attack from the SA-energy
         * decrease, then checks the equipped weapon. This avoids relying
         * on the obsolete/incorrect 7516 animation.
         */
        if (config.pluginMode() == PluginMode.BOOSTING &&
                isElderMaulEquipped())
        {
            Player localPlayer = client.getLocalPlayer();

            if (localPlayer != null &&
                    localPlayer.getName() != null &&
                    localPlayer.getInteracting() instanceof NPC &&
                    ((NPC) localPlayer.getInteracting()).getId() == CORP_ID)
            {
                String name = localPlayer.getName();

                maulCount.put(
                        name,
                        maulCount.getOrDefault(name, 0) + 1
                );

                updatePlayerList();
            }

            return;
        }

        /*
         * Existing Solo Arclight handling.
         */
        if (config.pluginMode() != PluginMode.SOLO ||
                soloPanel == null ||
                !isArclightEquipped())
        {
            return;
        }

        pendingSoloArclight = true;
        pendingSoloDwh = false;
        pendingSoloBgs = false;

        pendingSoloSpecGameCycle =
                client.getGameCycle();

    }
    @Provides
    WeCorpConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WeCorpConfig.class);
    }
}