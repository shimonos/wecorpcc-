package com.wecorpcc;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.plugins.specialcounter.SpecialCounterUpdate;

public class ModePanel extends PluginPanel
{
    private static final String BOOSTING_CARD = "BOOSTING";
    private static final String SOLO_CARD = "SOLO";
    private static final String MASS_CARD = "MASS";
    private static final String LOBBY_CARD = "LOBBY";

    private static final Color ACTIVE_COLOR =
            new Color(50, 205, 50);

    private static final Color INACTIVE_COLOR =
            new Color(180, 180, 180);

    private final CardLayout cardLayout =
            new CardLayout();

    private final JPanel cardPanel =
            new JPanel(cardLayout);

    private final Map<PluginMode, JButton> modeButtons =
            new EnumMap<>(PluginMode.class);

    private final Consumer<PluginMode> modeChangedCallback;

    private JButton lobbyButton;
    private boolean lobbyActive = false;

    private PluginMode currentMode =
            PluginMode.BOOSTING;

    public ModePanel(
            WeCorpPanel boostingPanel,
            MassPanel massPanel,
            SoloPanel soloPanel,
            CorpMassLobbyPanel lobbyPanel,
            Consumer<PluginMode> modeChangedCallback)
    {
        this.modeChangedCallback =
                modeChangedCallback;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel selectorPanel =
                createSelectorPanel();

        cardPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        cardPanel.add(
                boostingPanel,
                BOOSTING_CARD
        );


        cardPanel.add(
                soloPanel,
                SOLO_CARD
        );

        cardPanel.add(
                massPanel,
                MASS_CARD
        );
        cardPanel.add(
                lobbyPanel,
                LOBBY_CARD
        );

        add(selectorPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        showMode(PluginMode.BOOSTING);
    }

    private JPanel createSelectorPanel()
    {
        JPanel selectorPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                4,
                                5
                        )
                );

        selectorPanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        selectorPanel.setBorder(
                BorderFactory.createMatteBorder(
                        0,
                        0,
                        1,
                        0,
                        ColorScheme.MEDIUM_GRAY_COLOR
                )
        );

        selectorPanel.add(
                createModeButton(
                        PluginMode.BOOSTING,
                        "Boosting"
                )
        );

        selectorPanel.add(
                createModeButton(
                        PluginMode.SOLO,
                        "Solo"
                )
        );

        selectorPanel.add(
                createModeButton(
                        PluginMode.MASS,
                        "Mass"
                )
        );

        lobbyButton = new JButton("Lobby");

        lobbyButton.setFocusable(false);

        lobbyButton.setFont(
                lobbyButton.getFont().deriveFont(
                        Font.BOLD,
                        10f
                )
        );

        lobbyButton.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        7,
                        5,
                        7
                )
        );

        lobbyButton.addActionListener(event ->
        {
            lobbyActive = true;

            cardLayout.show(
                    cardPanel,
                    LOBBY_CARD
            );

            refreshModeButtons();

            revalidate();
            repaint();
        });

        selectorPanel.add(
                lobbyButton
        );
        return selectorPanel;
    }

    private JButton createModeButton(
            PluginMode mode,
            String label)
    {
        JButton button =
                new JButton(label);

        button.setFocusable(false);

        button.setFont(
                button.getFont().deriveFont(
                        Font.BOLD,
                        10f
                )
        );

        button.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        7,
                        5,
                        7
                )
        );

        button.addActionListener(event ->
        {
            if (modeChangedCallback != null)
            {
                modeChangedCallback.accept(mode);
            }
            else
            {
                showMode(mode);
            }
        });

        modeButtons.put(mode, button);

        return button;
    }

    public void showMode(PluginMode mode)
    {
        SwingUtilities.invokeLater(() ->
        {
            PluginMode safeMode =
                    mode == null
                            ? PluginMode.BOOSTING
                            : mode;

            currentMode = safeMode;
            lobbyActive = false;

            switch (safeMode)
            {
                case MASS:
                    cardLayout.show(
                            cardPanel,
                            MASS_CARD
                    );
                    break;

                case SOLO:
                    cardLayout.show(
                            cardPanel,
                            SOLO_CARD
                    );
                    break;

                case BOOSTING:
                default:
                    cardLayout.show(
                            cardPanel,
                            BOOSTING_CARD
                    );
                    break;
            }

            refreshModeButtons();

            revalidate();
            repaint();
        });
    }

    private void refreshModeButtons()
    {
        for (Map.Entry<PluginMode, JButton> entry :
                modeButtons.entrySet())
        {
            boolean active =
                    !lobbyActive &&
                            entry.getKey() == currentMode;
            JButton button =
                    entry.getValue();

            String prefix =
                    active
                            ? "● "
                            : "○ ";

            String label;

            switch (entry.getKey())
            {
                case SOLO:
                    label = "Solo";
                    break;

                case MASS:
                    label = "Mass";
                    break;

                case BOOSTING:
                default:
                    label = "Boosting";
                    break;
            }

            button.setText(prefix + label);

            button.setForeground(
                    active
                            ? ACTIVE_COLOR
                            : INACTIVE_COLOR
            );

            button.setBackground(
                    active
                            ? ColorScheme.MEDIUM_GRAY_COLOR
                            : ColorScheme.DARKER_GRAY_COLOR
            );
        }
        lobbyButton.setText(
                lobbyActive
                        ? "● Lobby"
                        : "○ Lobby"
        );

        lobbyButton.setForeground(
                lobbyActive
                        ? ACTIVE_COLOR
                        : INACTIVE_COLOR
        );

        lobbyButton.setBackground(
                lobbyActive
                        ? ColorScheme.MEDIUM_GRAY_COLOR
                        : ColorScheme.DARKER_GRAY_COLOR
        );
    }
}