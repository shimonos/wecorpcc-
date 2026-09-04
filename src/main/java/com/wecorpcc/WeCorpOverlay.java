package com.wecorpcc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class WeCorpOverlay extends OverlayPanel
{
    private static final int CORP_REGION = 11844;
    private static final int CORP_ID = 319;

    private final Client client;
    private final NPCManager npcManager;
    private final WeCorpPlugin plugin;
    private final WeCorpConfig config;

    @Inject
    public WeCorpOverlay(
            Client client,
            NPCManager npcManager,
            WeCorpPlugin plugin,
            WeCorpConfig config)
    {
        this.client = client;
        this.npcManager = npcManager;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Player localPlayer = client.getLocalPlayer();

        if (localPlayer == null)
        {
            return null;
        }

        WorldPoint location = localPlayer.getWorldLocation();

        if (location == null ||
                location.getRegionID() != CORP_REGION)
        {
            return null;
        }

        NPC corp = findCorp();

        if (corp == null)
        {
            return null;
        }

        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text("WeCorpCC")
                        .build()
        );

        addCorpHp(corp);

        /*
         * Boosting gets the full overlay.
         * Solo and Mass show HP only.
         */
        if (config.pluginMode() == PluginMode.BOOSTING)
        {
            int dwhTotal = plugin.getOverlayDwhCount();
            int maulTotal = plugin.getOverlayMaulCount();

            Actor target = corp.getInteracting();

            String targetName = "-";

            if (target instanceof Player &&
                    ((Player) target).getName() != null)
            {
                targetName = ((Player) target).getName();
            }

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Target")
                            .right(targetName)
                            .build()
            );

            addLowestCustomerHp();

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("DWH / Maul total")
                            .right(String.valueOf(
                                    dwhTotal + maulTotal))
                            .build()
            );
        }

        return super.render(graphics);
    }

    private NPC findCorp()
    {
        for (NPC npc : client.getNpcs())
        {
            if (npc != null &&
                    npc.getId() == CORP_ID)
            {
                return npc;
            }
        }

        return null;
    }

    private void addLowestCustomerHp()
    {
        Player lowestCustomer = null;
        double lowestPercent = 101.0;

        for (Player player : client.getPlayers())
        {
            if (player == null ||
                    player.getName() == null)
            {
                continue;
            }

            if (!plugin.isOverlayCustomer(
                    player.getName()))
            {
                continue;
            }

            int healthRatio =
                    player.getHealthRatio();

            int healthScale =
                    player.getHealthScale();

            if (healthRatio < 0 ||
                    healthScale <= 0)
            {
                continue;
            }

            double percentage =
                    (double) healthRatio /
                            healthScale *
                            100.0;

            if (percentage < lowestPercent)
            {
                lowestPercent = percentage;
                lowestCustomer = player;
            }
        }

        if (lowestCustomer == null)
        {
            return;
        }

        int displayPercent =
                (int) Math.round(
                        lowestPercent
                );

        boolean lowHp =
                displayPercent < 40;

        boolean flashOn =
                (System.currentTimeMillis() / 500) % 2 == 0;

        Color hpColor =
                lowHp && flashOn
                        ? Color.RED
                        : Color.WHITE;

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Client")
                        .right(
                                lowestCustomer.getName()
                        )
                        .build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("HP")
                        .right(
                                displayPercent + "%"
                        )
                        .rightColor(hpColor)
                        .build()
        );
    }
    private void addCorpHp(NPC corp)
    {
        int healthRatio = corp.getHealthRatio();
        int healthScale = corp.getHealthScale();

        if (healthRatio < 0 ||
                healthScale <= 0)
        {
            return;
        }

        Integer maxHealth = npcManager.getHealth(CORP_ID);

        if (maxHealth == null ||
                maxHealth <= 0)
        {
            return;
        }

        int health = 0;

        if (healthRatio > 0)
        {
            int minHealth = 1;
            int maxPossibleHealth;

            if (healthScale > 1)
            {
                if (healthRatio > 1)
                {
                    minHealth =
                            (maxHealth * (healthRatio - 1)
                                    + healthScale - 2)
                                    / (healthScale - 1);
                }

                maxPossibleHealth =
                        (maxHealth * healthRatio - 1)
                                / (healthScale - 1);

                if (maxPossibleHealth > maxHealth)
                {
                    maxPossibleHealth = maxHealth;
                }
            }
            else
            {
                maxPossibleHealth = maxHealth;
            }

            health =
                    (minHealth + maxPossibleHealth + 1) / 2;
        }

        double hpPercent =
                (double) health / maxHealth * 100.0;

        Color barColor;

        if (hpPercent > 60.0)
        {
            barColor = Color.GREEN;
        }
        else if (hpPercent > 30.0)
        {
            barColor = Color.YELLOW;
        }
        else
        {
            barColor = Color.RED;
        }

        ProgressBarComponent hpBar =
                new ProgressBarComponent();

        hpBar.setMinimum(0);
        hpBar.setMaximum(maxHealth);
        hpBar.setValue(health);

        hpBar.setForegroundColor(barColor);

        hpBar.setBackgroundColor(
                new Color(60, 20, 20)
        );

        hpBar.setCenterLabel(
                health + " / " + maxHealth
        );

        hpBar.setLabelDisplayMode(
                ProgressBarComponent.LabelDisplayMode.TEXT_ONLY
        );

        panelComponent.getChildren().add(hpBar);
    }
}