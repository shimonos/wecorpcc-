package com.wecorpcc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JProgressBar;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

public class WeCorpPanel extends PluginPanel
{
    private static final long AWAY_GRACE_PERIOD_MS = 90_000L;



    private static final Color ACTIVE_COLOR =
            new Color(50, 205, 50);

    private static final Color LOW_HP_COLOR =
            new Color(220, 55, 55);

    private static final Color CUSTOMER_COLOR =
            new Color(255, 205, 0);

    private static final Color AWAY_COLOR =
            new Color(145, 145, 145);

    private static final Color TITLE_COLOR =
            new Color(0, 153, 255);

    private static final Color NORMAL_TEXT_COLOR =
            Color.WHITE;

    private static final Color SECONDARY_TEXT_COLOR =
            new Color(180, 180, 180);

    private static final Color KILLS_COLOR =
            new Color(0, 153, 255);

    private static final Color KPH_COLOR =
            new Color(80, 200, 120);

    private static final Color TRIP_COLOR =
            new Color(190, 140, 255);

    private static final Color KILL_TIMER_COLOR =
            new Color(255, 165, 70);

    private static final Color PLAYERS_COLOR =
            new Color(230, 230, 230);

    private final JLabel titleLabel =
            new JLabel(
                    "WeCorpCC v2",
                    SwingConstants.CENTER
            );

    private final JLabel statusLabel =
            new JLabel(
                    "",
                    SwingConstants.CENTER
            );

    private final JLabel locationValueLabel =
            new JLabel(
                    "Waiting for Corp...",
                    SwingConstants.CENTER
            );

    private final JLabel killsValueLabel =
            new JLabel("0");

    private final JLabel kphValueLabel =
            new JLabel("0");

    private final JLabel tripValueLabel =
            new JLabel("0m 0s");

    private final JLabel killValueLabel =
            new JLabel("0s");

    private final JLabel playersValueLabel =
            new JLabel("0");

    private final JPanel statsPanel =
            new JPanel();

    private final JPanel playerListPanel =
            new JPanel();

    /*
     * Only the player/category area scrolls.
     *
     * The upper statistics and lower buttons stay fixed.
     */
    private final JScrollPane playerScrollPane =
            new JScrollPane(playerListPanel);

    private final JButton resetTripButton =
            new JButton("Reset Trip");

    private final JButton showHiddenButton =
            new JButton("Hidden Players");

    private final JButton guideButton =
            new JButton("Guide");

    private final JButton donateButton =
            new JButton("Support");

    private final Map<String, String> currentPlayerInfo =
            new LinkedHashMap<>();

    private final Map<String, String> lastPlayerInfo =
            new LinkedHashMap<>();

    private final Set<String> currentLowHpPlayers =
            new HashSet<>();

    private final Set<String> currentLastKillAttendees =
            new HashSet<>();

    private Map<String, Integer> killCountReference =
            new HashMap<>();

    private Set<String> hiddenPlayersReference =
            new HashSet<>();

    private final Map<String, String> displayNames =
            new HashMap<>();

    private final Map<String, Long> lastSeenAtCorp =
            new HashMap<>();

    private final Map<String, CustomerPackage> customers =
            new HashMap<>();

    private Runnable resetCallback;
    private Consumer<String> giveKillCallback;
    private Consumer<String> packageCompleteCallback;

    private final Timer awayRefreshTimer;
    private boolean activePlayersCollapsed = false;
    private boolean customersCollapsed = false;
    private boolean awayPlayersCollapsed = false;

    public WeCorpPanel()
    {
        super();

        setLayout(new BorderLayout());

        setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        /*
         * Set up every component before adding it to the
         * visible panel.
         */
        setupTitle();
        setupStatsPanel();
        setupFallbackStatus();
        setupPlayerList();
        setupButtons();

        JPanel topPanel =
                new JPanel();

        topPanel.setLayout(
                new BoxLayout(
                        topPanel,
                        BoxLayout.Y_AXIS
                )
        );

        topPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        titleLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        locationValueLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        statsPanel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        statusLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        topPanel.add(titleLabel);

        topPanel.add(
                Box.createVerticalStrut(6)
        );

        topPanel.add(locationValueLabel);

        topPanel.add(
                Box.createVerticalStrut(6)
        );

        topPanel.add(statsPanel);

        topPanel.add(
                Box.createVerticalStrut(6)
        );

        topPanel.add(statusLabel);

        topPanel.add(
                Box.createVerticalStrut(6)
        );

        topPanel.add(createDivider());

        playerScrollPane.setBorder(null);

        playerScrollPane.setWheelScrollingEnabled(
                false
        );

        playerScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        playerScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        playerScrollPane
                .getVerticalScrollBar()
                .setUnitIncrement(18);

        playerScrollPane
                .getVerticalScrollBar()
                .setBlockIncrement(72);

        playerScrollPane.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        playerScrollPane
                .getViewport()
                .setBackground(
                        ColorScheme.DARK_GRAY_COLOR
                );

        JPanel bottomPanel =
                new JPanel();

        bottomPanel.setLayout(
                new BoxLayout(
                        bottomPanel,
                        BoxLayout.Y_AXIS
                )
        );

        bottomPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        bottomPanel.add(createDivider());

        bottomPanel.add(
                Box.createVerticalStrut(6)
        );

        bottomPanel.add(
                createButtonPanel()
        );

        add(
                topPanel,
                BorderLayout.NORTH
        );

        add(
                playerScrollPane,
                BorderLayout.CENTER
        );

        add(
                bottomPanel,
                BorderLayout.SOUTH
        );

        /*
         * Refreshes the AFK text and moves players into
         * Away / AFK after 90 seconds.
         */
        awayRefreshTimer =
                new Timer(
                        1000,
                        event ->
                                refreshPlayerDisplay()
                );

        awayRefreshTimer.start();
    }

    private void setupTitle()
    {
        titleLabel.setForeground(
                TITLE_COLOR
        );

        titleLabel.setFont(
                titleLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                18f
                        )
        );

        titleLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        titleLabel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        28
                )
        );
    }

    private void setupStatsPanel()
    {
        statsPanel.removeAll();

        statsPanel.setLayout(
                new GridBagLayout()
        );

        statsPanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        statsPanel.setOpaque(true);
        statsPanel.setVisible(true);

        statsPanel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        statsPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                ColorScheme.MEDIUM_GRAY_COLOR
                        ),
                        BorderFactory.createEmptyBorder(
                                7,
                                7,
                                7,
                                7
                        )
                )
        );

        statsPanel.setPreferredSize(
                new Dimension(
                        250,
                        125
                )
        );

        statsPanel.setMinimumSize(
                new Dimension(
                        180,
                        125
                )
        );

        statsPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        125
                )
        );

        addStatRow(
                0,
                "Kills",
                KILLS_COLOR,
                killsValueLabel
        );

        addStatRow(
                1,
                "KPH",
                KPH_COLOR,
                kphValueLabel
        );

        addStatRow(
                2,
                "Trip",
                TRIP_COLOR,
                tripValueLabel
        );

        addStatRow(
                3,
                "Kill Time",
                KILL_TIMER_COLOR,
                killValueLabel
        );

        addStatRow(
                4,
                "Players",
                PLAYERS_COLOR,
                playersValueLabel
        );

        locationValueLabel.setForeground(
                NORMAL_TEXT_COLOR
        );

        locationValueLabel.setFont(
                locationValueLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                11f
                        )
        );

        locationValueLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        locationValueLabel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        22
                )
        );
    }

    private void addStatRow(
            int rowIndex,
            String labelText,
            Color iconColor,
            JLabel valueLabel)
    {
        JLabel iconLabel =
                new JLabel("●");

        iconLabel.setForeground(
                iconColor
        );

        iconLabel.setFont(
                iconLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                13f
                        )
        );

        JLabel nameLabel =
                new JLabel(labelText);

        nameLabel.setForeground(
                SECONDARY_TEXT_COLOR
        );

        nameLabel.setFont(
                nameLabel
                        .getFont()
                        .deriveFont(
                                Font.PLAIN,
                                13f
                        )
        );

        valueLabel.setForeground(
                NORMAL_TEXT_COLOR
        );

        valueLabel.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        valueLabel.setFont(
                valueLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                13f
                        )
        );

        GridBagConstraints iconConstraints =
                new GridBagConstraints();

        iconConstraints.gridx = 0;
        iconConstraints.gridy = rowIndex;

        iconConstraints.anchor =
                GridBagConstraints.WEST;

        iconConstraints.insets =
                new Insets(
                        1,
                        0,
                        1,
                        5
                );

        statsPanel.add(
                iconLabel,
                iconConstraints
        );

        GridBagConstraints nameConstraints =
                new GridBagConstraints();

        nameConstraints.gridx = 1;
        nameConstraints.gridy = rowIndex;
        nameConstraints.weightx = 1.0;

        nameConstraints.fill =
                GridBagConstraints.HORIZONTAL;

        nameConstraints.anchor =
                GridBagConstraints.WEST;

        nameConstraints.insets =
                new Insets(
                        1,
                        0,
                        1,
                        5
                );

        statsPanel.add(
                nameLabel,
                nameConstraints
        );

        GridBagConstraints valueConstraints =
                new GridBagConstraints();

        valueConstraints.gridx = 2;
        valueConstraints.gridy = rowIndex;

        valueConstraints.anchor =
                GridBagConstraints.EAST;

        valueConstraints.insets =
                new Insets(
                        1,
                        5,
                        1,
                        0
                );

        statsPanel.add(
                valueLabel,
                valueConstraints
        );
    }

    private void setupFallbackStatus()
    {
        statusLabel.setForeground(
                NORMAL_TEXT_COLOR
        );

        statusLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        statusLabel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        40
                )
        );

        statusLabel.setVisible(false);
    }

    private void setupPlayerList()
    {
        playerListPanel.removeAll();

        playerListPanel.setLayout(
                new BoxLayout(
                        playerListPanel,
                        BoxLayout.Y_AXIS
                )
        );

        playerListPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        playerListPanel.setOpaque(true);

        playerListPanel.setAlignmentY(
                Component.TOP_ALIGNMENT
        );

        /*
         * Enables wheel scrolling while the cursor is over
         * the background of the player list.
         */
        enablePlayerListScrolling(
                playerListPanel
        );

        /*
         * Enables scrolling while the cursor is over the
         * viewport's unused space.
         */
        enablePlayerListScrolling(
                playerScrollPane.getViewport()
        );
    }

    private void setupButtons()
    {
        resetTripButton.setFocusable(false);
        showHiddenButton.setFocusable(false);
        guideButton.setFocusable(false);
        donateButton.setFocusable(false);

        resetTripButton.addActionListener(
                event ->
                        confirmFullReset()
        );

        showHiddenButton.addActionListener(
                event ->
                        showHiddenPlayersMenu()
        );

        guideButton.addActionListener(
                event ->
                        showGuideDialog()
        );

        donateButton.addActionListener(
                event ->
                        LinkBrowser.browse(
                                "https://www.patreon.com/runelite"
                        )
        );
    }

    private void enablePlayerListScrolling(
            Component component)
    {
        /*
         * Forward wheel movement from the player-list area to RuneLite's
         * OUTER plugin-panel scrollbar.
         *
         * This fixes the split behavior where Kills/KPH scrolled the whole
         * panel but player rows scrolled only the inner player list.
         *
         * No layout, colors, sizes, KC, progress bars or player logic are
         * changed here.
         */
        if (component instanceof javax.swing.JComponent)
        {
            javax.swing.JComponent swingComponent =
                    (javax.swing.JComponent) component;

            if (Boolean.TRUE.equals(
                    swingComponent.getClientProperty(
                            "wecorpcc.outerScrollInstalled"
                    )))
            {
                return;
            }

            swingComponent.putClientProperty(
                    "wecorpcc.outerScrollInstalled",
                    Boolean.TRUE
            );
        }

        component.addMouseWheelListener(
                event ->
                {
                    javax.swing.JScrollPane outerScrollPane =
                            findOuterScrollPane();

                    javax.swing.JScrollBar scrollBar =
                            outerScrollPane != null
                                    ? outerScrollPane.getVerticalScrollBar()
                                    : playerScrollPane.getVerticalScrollBar();

                    if (scrollBar == null)
                    {
                        return;
                    }

                    int direction =
                            event.getWheelRotation();

                    if (direction == 0)
                    {
                        return;
                    }

                    int increment =
                            Math.max(
                                    18,
                                    scrollBar.getUnitIncrement(direction)
                            );

                    int maximumValue =
                            Math.max(
                                    scrollBar.getMinimum(),
                                    scrollBar.getMaximum() -
                                            scrollBar.getVisibleAmount()
                            );

                    int newValue =
                            scrollBar.getValue() +
                                    (direction * increment * 3);

                    scrollBar.setValue(
                            Math.max(
                                    scrollBar.getMinimum(),
                                    Math.min(
                                            newValue,
                                            maximumValue
                                    )
                            )
                    );

                    event.consume();
                }
        );

        if (component instanceof Container)
        {
            Container container =
                    (Container) component;

            for (Component child :
                    container.getComponents())
            {
                enablePlayerListScrolling(
                        child
                );
            }
        }
    }

    private javax.swing.JScrollPane findOuterScrollPane()
    {
        /*
         * Start ABOVE the inner playerScrollPane so we never select it.
         */
        java.awt.Container parent =
                playerScrollPane.getParent();

        while (parent != null)
        {
            if (parent instanceof javax.swing.JScrollPane)
            {
                return (javax.swing.JScrollPane) parent;
            }

            parent = parent.getParent();
        }

        return null;
    }

    private JPanel createButtonPanel()
    {
        JPanel buttonPanel =
                new JPanel();

        buttonPanel.setLayout(
                new BoxLayout(
                        buttonPanel,
                        BoxLayout.Y_AXIS
                )
        );

        buttonPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        JPanel firstRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                6,
                                0
                        )
                );

        firstRow.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        firstRow.add(resetTripButton);
        firstRow.add(showHiddenButton);

        JPanel secondRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                6,
                                0
                        )
                );

        secondRow.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        secondRow.add(guideButton);
        secondRow.add(donateButton);

        buttonPanel.add(firstRow);

        buttonPanel.add(
                Box.createVerticalStrut(5)
        );

        buttonPanel.add(secondRow);

        buttonPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        80
                )
        );

        return buttonPanel;
    }

    private JPanel createDivider()
    {
        JPanel divider =
                new JPanel();

        divider.setBackground(
                ColorScheme.MEDIUM_GRAY_COLOR
        );

        divider.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        1
                )
        );

        divider.setPreferredSize(
                new Dimension(
                        1,
                        1
                )
        );

        return divider;
    }

    private void showGuideDialog()
    {
        String guideText =
                "WeCorpCC Plugin Guide\n\n" +

                        "PLAYER COLORS\n" +
                        "Green: Active at Corp\n" +
                        "Red: Low HP\n" +
                        "Yellow: Customer\n" +
                        "Gray: Away / AFK\n\n" +

                        "LOW HP WATCH LIST\n" +
                        "In the RuneLite plugin settings, enter the exact RuneScape names " +
                        "of the players you want to monitor, separated by commas.\n" +
                        "Only players in this list will trigger Low HP warnings in the chat box.\n" +
                        "If the list is left empty, the plugin will monitor all tracked players " +
                        "and notify you whenever any player falls below the Low HP threshold.\n\n" +

                        "PLAYER ROW\n" +
                        "Circle | Player Name | Specs | KC\n\n" +

                        "RIGHT-CLICK OPTIONS\n" +
                        "Add +1 Kill\n" +
                        "Remove -1 Kill\n" +
                        "Reset Player KC\n" +
                        "Set as Customer\n" +
                        "Remove Customer\n" +
                        "Set Package Size\n" +
                        "Add +1 Package Kill\n" +
                        "Reset Package\n" +
                        "Mark Complete\n" +
                        "Hide Player\n" +
                        "Unhide Player\n" +
                        "Copy Name\n\n" +

                        "AWAY / AFK\n" +
                        "A player shows AFK immediately after leaving Corp.\n" +
                        "They remain in Active Players during the 90-second grace period.\n" +
                        "After 90 seconds, they move into Away / AFK and become gray.\n" +
                        "The panel displays how long they have been away.\n\n" +

                        "SPECS\n" +
                        "DWH, BGS, Elder Maul and Voidwaker are tracked.\n" +
                        "Specs reset when Corp dies or when you leave Corp.\n\n" +

                        "CUSTOMERS\n" +
                        "Customers appear in their own yellow section.\n" +
                        "You can set package size and track package kills.\n" +
                        "When a customer receives the Corp drop, their KC and " +
                        "package progress update automatically.\n\n" +

                        "RESET TRIP\n" +
                        "Reset Trip clears the full session after confirmation.";

        javax.swing.JTextArea guideArea =
                new javax.swing.JTextArea(
                        guideText
                );

        guideArea.setEditable(false);
        guideArea.setLineWrap(true);
        guideArea.setWrapStyleWord(true);
        guideArea.setCaretPosition(0);

        guideArea.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        guideArea.setForeground(
                NORMAL_TEXT_COLOR
        );

        guideArea.setFont(
                guideArea
                        .getFont()
                        .deriveFont(
                                Font.PLAIN,
                                12f
                        )
        );

        guideArea.setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        JScrollPane guideScrollPane =
                new JScrollPane(
                        guideArea
                );

        guideScrollPane.setPreferredSize(
                new Dimension(
                        520,
                        650
                )
        );

        guideScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        guideScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        guideScrollPane.setWheelScrollingEnabled(
                true
        );

        guideScrollPane
                .getVerticalScrollBar()
                .setUnitIncrement(16);

        JOptionPane.showMessageDialog(
                this,
                guideScrollPane,
                "WeCorpCC Plugin Guide",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public void setResetCallback(
            Runnable resetCallback)
    {
        this.resetCallback =
                resetCallback;
    }

    public void setGiveKillCallback(
            Consumer<String> giveKillCallback)
    {
        this.giveKillCallback =
                giveKillCallback;
    }

    public void setStatus(String status)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (status == null ||
                    status.trim().isEmpty())
            {
                setCompactStatus(
                        "Waiting for Corp...",
                        "0",
                        "0",
                        "0m 0s",
                        "0s",
                        "0"
                );

                statusLabel.setText("");
                statusLabel.setVisible(false);
                return;
            }

            parseStatusHtml(status);
        });
    }

    /*
     * Called by WeCorpPlugin when the player receiving
     * the Corp drop has already been marked as a customer.
     */
    public void recordCustomerKill(
            String playerName)
    {
        SwingUtilities.invokeLater(() ->
        {
            String normalizedName =
                    normalizeName(playerName);

            CustomerPackage customerPackage =
                    customers.get(
                            normalizedName
                    );

            if (customerPackage == null)
            {
                return;
            }

            customerPackage.packageKills++;

            if (customerPackage.packageSize > 0 &&
                    customerPackage.packageKills >=
                            customerPackage.packageSize)
            {
                customerPackage.packageKills =
                        customerPackage.packageSize;

                if (!customerPackage.completed)
                {
                    customerPackage.completed =
                            true;

                    if (packageCompleteCallback != null)
                    {
                        packageCompleteCallback.accept(
                                "★ Package Complete! " +
                                        playerName +
                                        " has finished their " +
                                        customerPackage.packageSize +
                                        "/" +
                                        customerPackage.packageSize +
                                        " KC package."
                        );
                    }
                }
            }

            refreshPlayerDisplay();
        });
    }

    /*
     * Removes displayed spec text immediately after
     * Corp dies or when the local player leaves Corp.
     */
    public void clearDisplayedSpecs()
    {
        SwingUtilities.invokeLater(() ->
        {
            currentPlayerInfo.replaceAll(
                    (name, specs) -> ""
            );

            lastPlayerInfo.replaceAll(
                    (name, specs) -> ""
            );

            refreshPlayerDisplay();
        });
    }

    private void parseStatusHtml(
            String status)
    {
        String plainText =
                status
                        .replaceAll(
                                "(?i)<br\\s*/?>",
                                "\n"
                        )
                        .replaceAll(
                                "(?i)</?html>",
                                ""
                        )
                        .replaceAll(
                                "(?i)</?center>",
                                ""
                        )
                        .replaceAll(
                                "(?i)</?b>",
                                ""
                        )
                        .replaceAll(
                                "(?i)<font[^>]*>",
                                ""
                        )
                        .replaceAll(
                                "(?i)</font>",
                                ""
                        )
                        .replace(
                                "&nbsp;",
                                " "
                        )
                        .trim();

        String location =
                "At Corp";

        String kills =
                "0";

        String kph =
                "0";

        String trip =
                "0m 0s";

        String kill =
                "0s";

        String players =
                "0";

        List<String> extraLines =
                new ArrayList<>();

        for (String line :
                plainText.split("\\n"))
        {
            String cleanLine =
                    line.trim();

            if (cleanLine.isEmpty())
            {
                continue;
            }

            if (cleanLine.equalsIgnoreCase(
                    "At Corp"
            ) ||
                    cleanLine.equalsIgnoreCase(
                            "Waiting for Corp..."
                    ))
            {
                location =
                        cleanLine;
            }
            else if (cleanLine.startsWith(
                    "Kills:"
            ))
            {
                kills =
                        valueAfterColon(
                                cleanLine
                        );
            }
            else if (cleanLine.startsWith(
                    "KPH:"
            ))
            {
                kph =
                        valueAfterColon(
                                cleanLine
                        );
            }
            else if (cleanLine.startsWith(
                    "Trip:"
            ))
            {
                trip =
                        valueAfterColon(
                                cleanLine
                        );
            }
            else if (cleanLine.startsWith(
                    "Kill Time:"
            ))
            {
                kill =
                        valueAfterColon(
                                cleanLine
                        );
            }
            else if (cleanLine.startsWith(
                    "Kill:"
            ))
            {
                kill =
                        valueAfterColon(
                                cleanLine
                        );
            }
            else if (cleanLine.startsWith(
                    "Players:"
            ))
            {
                players =
                        valueAfterColon(
                                cleanLine
                        );
            }
            else
            {
                extraLines.add(
                        cleanLine
                );
            }
        }

        setCompactStatus(
                location,
                kills,
                kph,
                trip,
                kill,
                players
        );

        if (extraLines.isEmpty())
        {
            statusLabel.setText("");
            statusLabel.setVisible(false);
        }
        else
        {
            statusLabel.setText(
                    "<html><center>" +
                            String.join(
                                    "<br>",
                                    extraLines
                            ) +
                            "</center></html>"
            );

            statusLabel.setVisible(true);
        }

        revalidate();
        repaint();
    }

    private String valueAfterColon(
            String text)
    {
        int index =
                text.indexOf(':');

        if (index < 0 ||
                index + 1 >= text.length())
        {
            return "";
        }

        return text
                .substring(index + 1)
                .trim();
    }

    private void setCompactStatus(
            String location,
            String kills,
            String kph,
            String trip,
            String kill,
            String players)
    {
        locationValueLabel.setText(
                location == null ||
                        location.trim().isEmpty()
                        ? "Waiting for Corp..."
                        : location
        );

        killsValueLabel.setText(
                emptyToDefault(
                        kills,
                        "0"
                )
        );

        kphValueLabel.setText(
                emptyToDefault(
                        kph,
                        "0"
                )
        );

        tripValueLabel.setText(
                emptyToDefault(
                        trip,
                        "0m 0s"
                )
        );

        killValueLabel.setText(
                emptyToDefault(
                        kill,
                        "0s"
                )
        );

        playersValueLabel.setText(
                emptyToDefault(
                        players,
                        "0"
                )
        );
    }

    private String emptyToDefault(
            String value,
            String defaultValue)
    {
        if (value == null ||
                value.trim().isEmpty())
        {
            return defaultValue;
        }

        return value.trim();
    }

    public void updatePlayers(
            Map<String, String> playerInfo,
            Set<String> lowHpPlayers,
            Set<String> lastKillAttendees,
            Map<String, Integer> killCounts,
            Set<String> hiddenPlayers)
    {
        SwingUtilities.invokeLater(() ->
        {
            long now =
                    System.currentTimeMillis();

            currentPlayerInfo.clear();
            currentLowHpPlayers.clear();
            currentLastKillAttendees.clear();

            if (playerInfo != null)
            {
                for (Map.Entry<String, String> entry :
                        playerInfo.entrySet())
                {
                    String displayName =
                            entry.getKey();

                    if (displayName == null ||
                            displayName.trim().isEmpty())
                    {
                        continue;
                    }

                    String normalizedName =
                            normalizeName(
                                    displayName
                            );

                    String specText =
                            entry.getValue() == null
                                    ? ""
                                    : entry.getValue();

                    currentPlayerInfo.put(
                            displayName,
                            specText
                    );

                    lastPlayerInfo.put(
                            normalizedName,
                            specText
                    );

                    displayNames.put(
                            normalizedName,
                            displayName
                    );

                    lastSeenAtCorp.put(
                            normalizedName,
                            now
                    );
                }
            }

            if (lowHpPlayers != null)
            {
                for (String name :
                        lowHpPlayers)
                {
                    currentLowHpPlayers.add(
                            normalizeName(name)
                    );
                }
            }

            if (lastKillAttendees != null)
            {
                for (String name :
                        lastKillAttendees)
                {
                    currentLastKillAttendees.add(
                            normalizeName(name)
                    );
                }
            }

            if (killCounts != null)
            {
                killCountReference =
                        killCounts;
            }

            if (hiddenPlayers != null)
            {
                hiddenPlayersReference =
                        hiddenPlayers;
            }

            refreshPlayerDisplay();
        });
    }

    private void refreshPlayerDisplay()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(
                    this::refreshPlayerDisplay
            );

            return;
        }

        /*
         * Save the current scrollbar position before rebuilding.
         *
         * Without this, the one-second AFK refresh can make
         * scrolling feel broken or jump back toward the top.
         */
        javax.swing.JScrollBar verticalBar =
                playerScrollPane
                        .getVerticalScrollBar();

        int previousScrollValue =
                verticalBar.getValue();

        playerListPanel.removeAll();

        List<PlayerRowData> activePlayers =
                new ArrayList<>();

        List<PlayerRowData> customerPlayers =
                new ArrayList<>();

        List<PlayerRowData> awayPlayers =
                new ArrayList<>();

        long now =
                System.currentTimeMillis();

        Set<String> allKnownPlayers =
                new HashSet<>();

        allKnownPlayers.addAll(
                displayNames.keySet()
        );

        allKnownPlayers.addAll(
                lastSeenAtCorp.keySet()
        );

        allKnownPlayers.addAll(
                customers.keySet()
        );

        for (String normalizedName :
                allKnownPlayers)
        {
            if (normalizedName == null ||
                    normalizedName.isEmpty())
            {
                continue;
            }

            if (isHidden(normalizedName))
            {
                continue;
            }

            String displayName =
                    displayNames.getOrDefault(
                            normalizedName,
                            normalizedName
                    );

            String cleanDisplayName =
                    removeYouSuffix(
                            displayName
                    );

            long lastSeen =
                    lastSeenAtCorp.getOrDefault(
                            normalizedName,
                            0L
                    );

            long awayDuration =
                    lastSeen <= 0
                            ? Long.MAX_VALUE
                            : now - lastSeen;

            boolean currentlyVisible =
                    isCurrentlyVisible(
                            normalizedName
                    );

            boolean isTemporarilyAway =
                    !currentlyVisible &&
                            lastSeen > 0 &&
                            awayDuration <
                                    AWAY_GRACE_PERIOD_MS;

            boolean isAway =
                    !currentlyVisible &&
                            lastSeen > 0 &&
                            awayDuration >=
                                    AWAY_GRACE_PERIOD_MS;

            boolean isCustomer =
                    customers.containsKey(
                            normalizedName
                    );

            boolean isLowHp =
                    currentLowHpPlayers.contains(
                            normalizedName
                    );

            int playerKills =
                    getPlayerKillCount(
                            normalizedName
                    );

            String specText =
                    getLastSpecText(
                            normalizedName
                    );

            PlayerRowData rowData =
                    new PlayerRowData(
                            normalizedName,
                            cleanDisplayName,
                            specText,
                            playerKills,
                            isLowHp,
                            isTemporarilyAway,
                            isAway,
                            lastSeen
                    );

            if (isCustomer)
            {
                customerPlayers.add(
                        rowData
                );
            }
            else if (isAway)
            {
                awayPlayers.add(
                        rowData
                );
            }
            else
            {
                activePlayers.add(
                        rowData
                );
            }
        }

        activePlayers.sort(
                Comparator
                        .comparing(
                                (PlayerRowData row) ->
                                        !row.lowHp
                        )
                        .thenComparing(
                                row ->
                                        row.displayName
                                                .toLowerCase()
                        )
        );

        customerPlayers.sort(
                Comparator.comparing(
                        row ->
                                row.displayName
                                        .toLowerCase()
                )
        );

        awayPlayers.sort(
                Comparator
                        .comparingLong(
                                (PlayerRowData row) ->
                                        row.lastSeen
                        )
                        .reversed()
        );

        addCategory(
                "Active Players",
                activePlayers,
                PlayerCategory.ACTIVE
        );

        addCategory(
                "Customers",
                customerPlayers,
                PlayerCategory.CUSTOMER
        );

        addCategory(
                "Away / AFK",
                awayPlayers,
                PlayerCategory.AWAY
        );

        updateHiddenButtonText();

        playerListPanel.revalidate();
        playerListPanel.repaint();

        playerScrollPane
                .getViewport()
                .revalidate();

        playerScrollPane
                .getViewport()
                .repaint();

        /*
         * Restore the previous position after Swing recalculates
         * the new scrollbar range.
         */
        SwingUtilities.invokeLater(() ->
        {
            int maximumValue =
                    Math.max(
                            verticalBar.getMinimum(),
                            verticalBar.getMaximum() -
                                    verticalBar.getVisibleAmount()
                    );

            verticalBar.setValue(
                    Math.max(
                            verticalBar.getMinimum(),
                            Math.min(
                                    previousScrollValue,
                                    maximumValue
                            )
                    )
            );
        });
    }

    private void addCategory(
            String title,
            List<PlayerRowData> players,
            PlayerCategory category)
    {
        boolean collapsed =
                isCategoryCollapsed(category);

        JPanel headerPanel =
                new JPanel(
                        new BorderLayout()
                );

        headerPanel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        headerPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0,
                                0,
                                1,
                                0,
                                ColorScheme.MEDIUM_GRAY_COLOR
                        ),
                        BorderFactory.createEmptyBorder(
                                6,
                                7,
                                6,
                                7
                        )
                )
        );

        headerPanel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        32
                )
        );

        /*
         * Show a down arrow when expanded and a right arrow
         * when collapsed.
         */
        String arrow =
                collapsed
                        ? "▶ "
                        : "▼ ";

        JLabel categoryLabel =
                new JLabel(
                        arrow +
                                title.toUpperCase() +
                                "  (" +
                                players.size() +
                                ")"
                );

        categoryLabel.setForeground(
                getCategoryColor(category)
        );

        categoryLabel.setFont(
                categoryLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                11f
                        )
        );

        headerPanel.add(
                categoryLabel,
                BorderLayout.WEST
        );

        /*
         * Hand cursor shows users that the header is clickable.
         */
        headerPanel.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        categoryLabel.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        /*
         * Clicking either the header background or its text
         * collapses/expands the category.
         */
        MouseAdapter collapseListener =
                new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(
                            MouseEvent event)
                    {
                        if (!SwingUtilities.isLeftMouseButton(event))
                        {
                            return;
                        }

                        toggleCategoryCollapsed(
                                category
                        );
                    }
                };

        headerPanel.addMouseListener(
                collapseListener
        );

        categoryLabel.addMouseListener(
                collapseListener
        );

        /*
         * Mouse-wheel scrolling still works over the header.
         */
        enablePlayerListScrolling(
                headerPanel
        );

        playerListPanel.add(
                headerPanel
        );

        playerListPanel.add(
                Box.createVerticalStrut(3)
        );

        /*
         * Stop here when the category is collapsed.
         * The header remains visible, but its player rows are hidden.
         */
        if (collapsed)
        {
            playerListPanel.add(
                    Box.createVerticalStrut(5)
            );

            return;
        }

        if (players.isEmpty())
        {
            JLabel emptyLabel =
                    new JLabel(
                            "No players"
                    );

            emptyLabel.setForeground(
                    SECONDARY_TEXT_COLOR
            );

            emptyLabel.setBorder(
                    BorderFactory.createEmptyBorder(
                            5,
                            10,
                            7,
                            2
                    )
            );

            enablePlayerListScrolling(
                    emptyLabel
            );

            playerListPanel.add(
                    emptyLabel
            );
        }
        else
        {
            for (PlayerRowData player : players)
            {
                playerListPanel.add(
                        createPlayerRow(
                                player,
                                category
                        )
                );

                playerListPanel.add(
                        Box.createVerticalStrut(2)
                );
            }
        }

        playerListPanel.add(
                Box.createVerticalStrut(8)
        );
    }

    private boolean isCategoryCollapsed(
            PlayerCategory category)
    {
        switch (category)
        {
            case CUSTOMER:
                return customersCollapsed;

            case AWAY:
                return awayPlayersCollapsed;

            case ACTIVE:
            default:
                return activePlayersCollapsed;
        }
    }

    private void toggleCategoryCollapsed(
            PlayerCategory category)
    {
        switch (category)
        {
            case CUSTOMER:
                customersCollapsed =
                        !customersCollapsed;
                break;

            case AWAY:
                awayPlayersCollapsed =
                        !awayPlayersCollapsed;
                break;

            case ACTIVE:
            default:
                activePlayersCollapsed =
                        !activePlayersCollapsed;
                break;
        }

        refreshPlayerDisplay();
    }

    private JPanel createPlayerRow(
            PlayerRowData player,
            PlayerCategory category)
    {
        JPanel row =
                new JPanel(
                        new GridBagLayout()
                );

        row.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        row.setBorder(
                BorderFactory.createEmptyBorder(
                        4,
                        5,
                        4,
                        3
                )
        );

        /*
         * Preserve the polished panel design and colors.
         * Only the row layout changes:
         * - KC is under the player name
         * - customer progress is centered on a full row underneath
         */
        int rowHeight =
                category == PlayerCategory.CUSTOMER
                        ? 76
                        : 58;

        row.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        rowHeight
                )
        );

        Color circleColor =
                getPlayerCircleColor(
                        player,
                        category
                );

        /*
         * Column 1: status circle.
         */
        JLabel circleLabel =
                new JLabel("●");

        circleLabel.setForeground(
                circleColor
        );

        circleLabel.setFont(
                circleLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                14f
                        )
        );

        GridBagConstraints circleConstraints =
                new GridBagConstraints();

        circleConstraints.gridx = 0;
        circleConstraints.gridy = 0;
        circleConstraints.gridheight =
                category == PlayerCategory.CUSTOMER
                        ? 4
                        : 3;

        circleConstraints.anchor =
                GridBagConstraints.NORTHWEST;

        circleConstraints.insets =
                new Insets(
                        1,
                        0,
                        0,
                        5
                );

        row.add(
                circleLabel,
                circleConstraints
        );

        /*
         * Column 2: player name, KC, then status/details.
         *
         * KC is intentionally here instead of the far right so RuneLite
         * can never clip it on a narrow sidebar.
         */
        JPanel namePanel =
                new JPanel();

        namePanel.setLayout(
                new BoxLayout(
                        namePanel,
                        BoxLayout.Y_AXIS
                )
        );

        namePanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        JLabel nameLabel =
                new JLabel(
                        player.displayName
                );

        nameLabel.setForeground(
                NORMAL_TEXT_COLOR
        );

        nameLabel.setFont(
                nameLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                13f
                        )
        );

        JLabel kcLabel =
                new JLabel(
                        "KC " + player.kills
                );

        kcLabel.setForeground(
                category == PlayerCategory.CUSTOMER
                        ? CUSTOMER_COLOR
                        : NORMAL_TEXT_COLOR
        );

        kcLabel.setFont(
                kcLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        JLabel detailsLabel =
                new JLabel(
                        buildSecondaryText(
                                player,
                                category
                        )
                );

        detailsLabel.setForeground(
                category == PlayerCategory.AWAY
                        ? AWAY_COLOR
                        : SECONDARY_TEXT_COLOR
        );

        detailsLabel.setFont(
                detailsLabel
                        .getFont()
                        .deriveFont(
                                Font.PLAIN,
                                9f
                        )
        );

        nameLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        kcLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        detailsLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        namePanel.add(nameLabel);
        namePanel.add(kcLabel);
        namePanel.add(detailsLabel);

        GridBagConstraints nameConstraints =
                new GridBagConstraints();

        nameConstraints.gridx = 1;
        nameConstraints.gridy = 0;
        nameConstraints.gridheight = 3;
        nameConstraints.weightx = 1.0;

        nameConstraints.fill =
                GridBagConstraints.HORIZONTAL;

        nameConstraints.anchor =
                GridBagConstraints.NORTHWEST;

        nameConstraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        3
                );

        row.add(
                namePanel,
                nameConstraints
        );

        /*
         * Column 3: special attacks.
         * Keep the polished design's spec column and colors.
         */
        String specText =
                player.specText == null ||
                        player.specText.trim().isEmpty()
                        ? ""
                        : player.specText;

        JLabel specLabel =
                new JLabel(
                        specText,
                        SwingConstants.CENTER
                );

        specLabel.setForeground(
                SECONDARY_TEXT_COLOR
        );

        specLabel.setFont(
                specLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        specLabel.setPreferredSize(
                new Dimension(
                        52,
                        38
                )
        );

        specLabel.setMinimumSize(
                new Dimension(
                        42,
                        38
                )
        );

        specLabel.setMaximumSize(
                new Dimension(
                        58,
                        38
                )
        );

        GridBagConstraints specConstraints =
                new GridBagConstraints();

        specConstraints.gridx = 2;
        specConstraints.gridy = 0;
        specConstraints.gridheight = 3;

        specConstraints.anchor =
                GridBagConstraints.NORTHEAST;

        specConstraints.insets =
                new Insets(
                        0,
                        1,
                        0,
                        1
                );

        row.add(
                specLabel,
                specConstraints
        );

        /*
         * Customer progress bar:
         * own centered row across the usable player width.
         */
        JProgressBar packageProgressBar =
                new JProgressBar();

        packageProgressBar.setMinimum(0);
        packageProgressBar.setMaximum(1);
        packageProgressBar.setValue(0);

        packageProgressBar.setString("");
        packageProgressBar.setStringPainted(true);

        packageProgressBar.setBorderPainted(false);
        packageProgressBar.setFocusable(false);

        packageProgressBar.setPreferredSize(
                new Dimension(130, 17)
        );

        packageProgressBar.setMinimumSize(
                new Dimension(105, 17)
        );

        packageProgressBar.setMaximumSize(
                new Dimension(150, 17)
        );

        packageProgressBar.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        if (category == PlayerCategory.CUSTOMER)
        {
            CustomerPackage customerPackage =
                    customers.get(
                            player.normalizedName
                    );

            if (customerPackage != null &&
                    customerPackage.packageSize > 0)
            {
                int packageSize =
                        customerPackage.packageSize;

                int packageKills =
                        Math.min(
                                customerPackage.packageKills,
                                packageSize
                        );

                packageProgressBar.setMaximum(
                        packageSize
                );

                packageProgressBar.setValue(
                        packageKills
                );

                packageProgressBar.setString(
                        packageKills +
                                "/" +
                                packageSize
                );

                if (customerPackage.completed ||
                        packageKills >= packageSize)
                {
                    packageProgressBar.setForeground(
                            ACTIVE_COLOR
                    );
                }
                else
                {
                    packageProgressBar.setForeground(
                            CUSTOMER_COLOR
                    );
                }

                packageProgressBar.setVisible(true);
            }
            else
            {
                packageProgressBar.setVisible(false);
            }
        }
        else
        {
            packageProgressBar.setVisible(false);
        }

        GridBagConstraints progressConstraints =
                new GridBagConstraints();

        progressConstraints.gridx = 1;
        progressConstraints.gridy = 3;
        progressConstraints.gridwidth = 2;
        progressConstraints.weightx = 1.0;

        progressConstraints.anchor =
                GridBagConstraints.CENTER;

        progressConstraints.insets =
                new Insets(
                        4,
                        0,
                        0,
                        0
                );

        row.add(
                packageProgressBar,
                progressConstraints
        );

        /*
         * Right-click menu on every visible part.
         */
        installPlayerMenu(
                row,
                player.normalizedName
        );

        installPlayerMenu(
                circleLabel,
                player.normalizedName
        );

        installPlayerMenu(
                namePanel,
                player.normalizedName
        );

        installPlayerMenu(
                nameLabel,
                player.normalizedName
        );

        installPlayerMenu(
                kcLabel,
                player.normalizedName
        );

        installPlayerMenu(
                detailsLabel,
                player.normalizedName
        );

        installPlayerMenu(
                specLabel,
                player.normalizedName
        );

        installPlayerMenu(
                packageProgressBar,
                player.normalizedName
        );

        /*
         * Mouse wheel works over every part of this row.
         */
        enablePlayerListScrolling(
                row
        );

        return row;
    }

    private String buildSecondaryText(
            PlayerRowData player,
            PlayerCategory category)
    {
        if (category ==
                PlayerCategory.CUSTOMER)
        {
            CustomerPackage customerPackage =
                    customers.get(
                            player.normalizedName
                    );

            if (customerPackage == null)
            {
                return "Customer";
            }

            if (customerPackage.completed)
            {
                return "Package complete";
            }

            if (customerPackage.packageSize > 0)
            {
                int remaining =
                        Math.max(
                                0,
                                customerPackage.packageSize -
                                        customerPackage.packageKills
                        );

                return remaining +
                        " kills remaining";
            }

            return "Package size not set";
        }

        if (category ==
                PlayerCategory.AWAY)
        {
            long awayMilliseconds =
                    System.currentTimeMillis() -
                            player.lastSeen;

            return "AFK " +
                    formatAfkTime(
                            awayMilliseconds
                    );
        }

        /*
         * The player has left Corp but is still inside
         * the 90-second grace period.
         */
        if (player.temporarilyAway)
        {
            long awayMilliseconds =
                    System.currentTimeMillis() -
                            player.lastSeen;

            return "AFK " +
                    formatAfkTime(
                            awayMilliseconds
                    );
        }

        if (player.lowHp)
        {
            return "LOW HP";
        }

        if (currentLastKillAttendees.contains(
                player.normalizedName))
        {
            return "Attended last kill";
        }

        return "At Corp";
    }

    /*
     * Kept for compatibility with the earlier panel version.
     *
     * The current player rows use separate KC and package labels,
     * but this method remains available if another panel section
     * still calls it.
     */
    private String buildRightSideText(
            PlayerRowData player,
            PlayerCategory category)
    {
        if (category ==
                PlayerCategory.CUSTOMER)
        {
            CustomerPackage customerPackage =
                    customers.get(
                            player.normalizedName
                    );

            if (customerPackage != null &&
                    customerPackage.packageSize > 0)
            {
                return "<html><div style='text-align:right;'>" +
                        "<b>KC " +
                        player.kills +
                        "</b><br>" +
                        "<b>" +
                        customerPackage.packageKills +
                        "/" +
                        customerPackage.packageSize +
                        "</b>" +
                        "</div></html>";
            }
        }

        return "KC " +
                player.kills;
    }

    private Color getPlayerCircleColor(
            PlayerRowData player,
            PlayerCategory category)
    {
        if (category ==
                PlayerCategory.CUSTOMER)
        {
            return CUSTOMER_COLOR;
        }

        if (category ==
                PlayerCategory.AWAY)
        {
            return AWAY_COLOR;
        }

        if (player.lowHp)
        {
            return LOW_HP_COLOR;
        }

        return ACTIVE_COLOR;
    }

    private Color getCategoryColor(
            PlayerCategory category)
    {
        switch (category)
        {
            case CUSTOMER:
                return CUSTOMER_COLOR;

            case AWAY:
                return AWAY_COLOR;

            case ACTIVE:
            default:
                return ACTIVE_COLOR;
        }
    }

    private void installPlayerMenu(
            Component component,
            String normalizedName)
    {
        component.addMouseListener(
                new MouseAdapter()
                {
                    @Override
                    public void mousePressed(
                            MouseEvent event)
                    {
                        showPopupIfRequired(
                                event
                        );
                    }

                    @Override
                    public void mouseReleased(
                            MouseEvent event)
                    {
                        showPopupIfRequired(
                                event
                        );
                    }

                    private void showPopupIfRequired(
                            MouseEvent event)
                    {
                        if (!event.isPopupTrigger())
                        {
                            return;
                        }

                        JPopupMenu menu =
                                createPlayerPopupMenu(
                                        normalizedName
                                );

                        menu.show(
                                event.getComponent(),
                                event.getX(),
                                event.getY()
                        );
                    }
                }
        );
    }
    public void setPackageCompleteCallback(
            Consumer<String> packageCompleteCallback)
    {
        this.packageCompleteCallback =
                packageCompleteCallback;
    }
    private JPopupMenu createPlayerPopupMenu(
            String normalizedName)
    {
        JPopupMenu menu =
                new JPopupMenu();

        String displayName =
                getDisplayName(
                        normalizedName
                );

        boolean isCustomer =
                customers.containsKey(
                        normalizedName
                );

        boolean isHidden =
                isHidden(
                        normalizedName
                );

        JMenuItem playerTitle =
                new JMenuItem(
                        displayName
                );

        playerTitle.setEnabled(false);

        menu.add(playerTitle);
        menu.addSeparator();

        JMenuItem addKillItem =
                new JMenuItem(
                        "Add +1 Kill"
                );

        addKillItem.addActionListener(
                event ->
                        addPlayerKill(
                                normalizedName
                        )
        );

        JMenuItem removeKillItem =
                new JMenuItem(
                        "Remove -1 Kill"
                );

        removeKillItem.addActionListener(
                event ->
                        removePlayerKill(
                                normalizedName
                        )
        );

        JMenuItem resetKillItem =
                new JMenuItem(
                        "Reset Player KC"
                );

        resetKillItem.addActionListener(
                event ->
                        resetPlayerKillCount(
                                normalizedName
                        )
        );

        menu.add(addKillItem);
        menu.add(removeKillItem);
        menu.add(resetKillItem);
        menu.addSeparator();

        if (isCustomer)
        {
            JMenuItem removeCustomerItem =
                    new JMenuItem(
                            "Remove Customer"
                    );

            removeCustomerItem.addActionListener(
                    event ->
                            removeCustomer(
                                    normalizedName
                            )
            );

            menu.add(
                    removeCustomerItem
            );
        }
        else
        {
            JMenuItem setCustomerItem =
                    new JMenuItem(
                            "Set as Customer"
                    );

            setCustomerItem.addActionListener(
                    event ->
                            setCustomer(
                                    normalizedName
                            )
            );

            menu.add(
                    setCustomerItem
            );
        }

        JMenuItem packageSizeItem =
                new JMenuItem(
                        "Set Package Size"
                );

        packageSizeItem.addActionListener(
                event ->
                        setCustomerPackageSize(
                                normalizedName
                        )
        );

        JMenuItem addPackageKillItem =
                new JMenuItem(
                        "Add +1 Package Kill"
                );

        addPackageKillItem.addActionListener(
                event ->
                        addCustomerPackageKill(
                                normalizedName
                        )
        );

        JMenuItem resetPackageItem =
                new JMenuItem(
                        "Reset Package"
                );

        resetPackageItem.addActionListener(
                event ->
                        resetCustomerPackage(
                                normalizedName
                        )
        );

        JMenuItem completePackageItem =
                new JMenuItem(
                        "Mark Complete"
                );

        completePackageItem.addActionListener(
                event ->
                        markCustomerPackageComplete(
                                normalizedName
                        )
        );

        menu.add(packageSizeItem);
        menu.add(addPackageKillItem);
        menu.add(resetPackageItem);
        menu.add(completePackageItem);
        menu.addSeparator();

        if (isHidden)
        {
            JMenuItem unhideItem =
                    new JMenuItem(
                            "Unhide Player"
                    );

            unhideItem.addActionListener(
                    event ->
                            unhidePlayer(
                                    normalizedName
                            )
            );

            menu.add(unhideItem);
        }
        else
        {
            JMenuItem hideItem =
                    new JMenuItem(
                            "Hide Player"
                    );

            hideItem.addActionListener(
                    event ->
                            hidePlayer(
                                    normalizedName
                            )
            );

            menu.add(hideItem);
        }

        JMenuItem copyNameItem =
                new JMenuItem(
                        "Copy Name"
                );

        copyNameItem.addActionListener(
                event ->
                        copyPlayerName(
                                normalizedName
                        )
        );

        menu.add(copyNameItem);

        return menu;
    }
    private void addPlayerKill(
            String normalizedName)
    {
        String displayName =
                getDisplayName(
                        normalizedName
                );

        if (giveKillCallback != null)
        {
            giveKillCallback.accept(
                    displayName
            );

            return;
        }

        String matchingKey =
                findKillCountKey(
                        normalizedName
                );

        killCountReference.put(
                matchingKey,
                killCountReference.getOrDefault(
                        matchingKey,
                        0
                ) + 1
        );

        refreshPlayerDisplay();
    }
    private void resetCustomerPackage(String normalizedName)
    {
        CustomerPackage customerPackage =
                customers.get(normalizedName);

        if (customerPackage == null)
        {
            return;
        }

        customerPackage.packageKills = 0;
        customerPackage.completed = false;

        refreshPlayerDisplay();
    }
    private void removePlayerKill(
            String normalizedName)
    {
        String key =
                findKillCountKey(
                        normalizedName
                );

        int currentKills =
                killCountReference.getOrDefault(
                        key,
                        0
                );

        if (currentKills <= 1)
        {
            killCountReference.remove(
                    key
            );
        }
        else
        {
            killCountReference.put(
                    key,
                    currentKills - 1
            );
        }

        refreshPlayerDisplay();
    }

    private void resetPlayerKillCount(
            String normalizedName)
    {
        String displayName =
                getDisplayName(
                        normalizedName
                );

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "Reset KC for " +
                                displayName +
                                "?",
                        "Reset Player KC",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (result !=
                JOptionPane.YES_OPTION)
        {
            return;
        }

        String key =
                findKillCountKey(
                        normalizedName
                );

        killCountReference.remove(
                key
        );

        refreshPlayerDisplay();
    }

    private void setCustomer(
            String normalizedName)
    {
        customers.computeIfAbsent(
                normalizedName,
                name ->
                        new CustomerPackage()
        );

        refreshPlayerDisplay();
    }

    private void removeCustomer(
            String normalizedName)
    {
        String displayName =
                getDisplayName(
                        normalizedName
                );

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "Remove " +
                                displayName +
                                " as a customer?",
                        "Remove Customer",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (result !=
                JOptionPane.YES_OPTION)
        {
            return;
        }

        customers.remove(
                normalizedName
        );

        refreshPlayerDisplay();
    }

    private void setCustomerPackageSize(
            String normalizedName)
    {
        setCustomer(
                normalizedName
        );

        CustomerPackage customerPackage =
                customers.get(
                        normalizedName
                );

        String existingValue =
                customerPackage.packageSize > 0
                        ? String.valueOf(
                        customerPackage.packageSize
                )
                        : "";

        String input =
                JOptionPane.showInputDialog(
                        this,
                        "Enter package size for " +
                                getDisplayName(
                                        normalizedName
                                ) +
                                ":",
                        existingValue
                );

        if (input == null)
        {
            return;
        }

        input =
                input.trim();

        if (input.isEmpty())
        {
            return;
        }

        try
        {
            int packageSize =
                    Integer.parseInt(
                            input
                    );

            if (packageSize <= 0)
            {
                throw new NumberFormatException();
            }

            customerPackage.packageSize =
                    packageSize;

            customerPackage.completed =
                    customerPackage.packageKills >=
                            packageSize;

            refreshPlayerDisplay();
        }
        catch (NumberFormatException exception)
        {
            JOptionPane.showMessageDialog(
                    this,
                    "Package size must be a number greater than 0.",
                    "Invalid Package Size",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void addCustomerPackageKill(
            String normalizedName)
    {
        setCustomer(normalizedName);

        CustomerPackage customerPackage =
                customers.get(normalizedName);

        /*
         * Remember whether it was already complete so the
         * chat message is sent only once.
         */
        boolean wasAlreadyCompleted =
                customerPackage.completed;

        customerPackage.packageKills++;

        if (customerPackage.packageSize > 0 &&
                customerPackage.packageKills >=
                        customerPackage.packageSize)
        {
            customerPackage.packageKills =
                    customerPackage.packageSize;

            customerPackage.completed = true;

            if (!wasAlreadyCompleted &&
                    packageCompleteCallback != null)
            {
                String playerName =
                        getDisplayName(normalizedName);

                packageCompleteCallback.accept(
                        "\"[WeCorp] Package Complete! \" " +
                                playerName +
                                " has finished their " +
                                customerPackage.packageSize +
                                "/" +
                                customerPackage.packageSize +
                                " KC package."
                );
            }
        }

        refreshPlayerDisplay();
    }

    private void markCustomerPackageComplete(
            String normalizedName)
    {
        setCustomer(
                normalizedName
        );

        CustomerPackage customerPackage =
                customers.get(
                        normalizedName
                );

        if (customerPackage.packageSize > 0)
        {
            customerPackage.packageKills =
                    customerPackage.packageSize;
        }

        customerPackage.completed =
                true;

        refreshPlayerDisplay();
    }

    private void hidePlayer(
            String normalizedName)
    {
        hiddenPlayersReference.add(
                normalizedName
        );

        refreshPlayerDisplay();
    }

    private void unhidePlayer(
            String normalizedName)
    {
        removeNormalizedName(
                hiddenPlayersReference,
                normalizedName
        );

        refreshPlayerDisplay();
    }

    private void showHiddenPlayersMenu()
    {
        JPopupMenu hiddenMenu =
                new JPopupMenu();

        List<String> hiddenNames =
                getNormalizedHiddenPlayers();

        if (hiddenNames.isEmpty())
        {
            JMenuItem emptyItem =
                    new JMenuItem(
                            "No hidden players"
                    );

            emptyItem.setEnabled(false);

            hiddenMenu.add(
                    emptyItem
            );
        }
        else
        {
            hiddenNames.sort(
                    Comparator.comparing(
                            this::getDisplayName
                    )
            );

            for (String normalizedName :
                    hiddenNames)
            {
                JMenuItem playerItem =
                        new JMenuItem(
                                "Unhide " +
                                        getDisplayName(
                                                normalizedName
                                        )
                        );

                playerItem.addActionListener(
                        event ->
                                unhidePlayer(
                                        normalizedName
                                )
                );

                hiddenMenu.add(
                        playerItem
                );
            }

            hiddenMenu.addSeparator();

            JMenuItem unhideAllItem =
                    new JMenuItem(
                            "Unhide All"
                    );

            unhideAllItem.addActionListener(
                    event ->
                    {
                        hiddenPlayersReference.clear();

                        refreshPlayerDisplay();
                    }
            );

            hiddenMenu.add(
                    unhideAllItem
            );
        }

        hiddenMenu.show(
                showHiddenButton,
                0,
                showHiddenButton.getHeight()
        );
    }

    private void copyPlayerName(
            String normalizedName)
    {
        String displayName =
                getDisplayName(
                        normalizedName
                );

        try
        {
            StringSelection selection =
                    new StringSelection(
                            displayName
                    );

            Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(
                            selection,
                            selection
                    );
        }
        catch (IllegalStateException exception)
        {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not copy the player name.",
                    "Copy Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void confirmFullReset()
    {
        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "Reset the full WeCorpCC trip?\n" +
                                "This clears KC, customers, packages and hidden players.",
                        "Reset Trip",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (result ==
                JOptionPane.YES_OPTION &&
                resetCallback != null)
        {
            resetCallback.run();
        }
    }
    public void resetPanel()
    {
        SwingUtilities.invokeLater(() ->
        {
            currentPlayerInfo.clear();
            lastPlayerInfo.clear();
            currentLowHpPlayers.clear();
            currentLastKillAttendees.clear();

            displayNames.clear();
            lastSeenAtCorp.clear();
            customers.clear();

            killCountReference =
                    new HashMap<>();

            hiddenPlayersReference =
                    new HashSet<>();

            setCompactStatus(
                    "Waiting for Corp...",
                    "0",
                    "0",
                    "0m 0s",
                    "0s",
                    "0"
            );

            statusLabel.setText("");
            statusLabel.setVisible(false);

            refreshPlayerDisplay();
        });
    }

    private boolean isCurrentlyVisible(
            String normalizedName)
    {
        for (String displayName :
                currentPlayerInfo.keySet())
        {
            if (normalizeName(displayName)
                    .equals(normalizedName))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isHidden(
            String normalizedName)
    {
        for (String hiddenName :
                hiddenPlayersReference)
        {
            if (normalizeName(hiddenName)
                    .equals(normalizedName))
            {
                return true;
            }
        }

        return false;
    }

    private List<String> getNormalizedHiddenPlayers()
    {
        Set<String> normalizedNames =
                new HashSet<>();

        for (String hiddenName :
                hiddenPlayersReference)
        {
            String normalizedName =
                    normalizeName(hiddenName);

            if (!normalizedName.isEmpty())
            {
                normalizedNames.add(
                        normalizedName
                );
            }
        }

        return new ArrayList<>(
                normalizedNames
        );
    }

    private void removeNormalizedName(
            Set<String> names,
            String normalizedName)
    {
        names.removeIf(
                name ->
                        normalizeName(name)
                                .equals(normalizedName)
        );
    }

    private int getPlayerKillCount(
            String normalizedName)
    {
        for (Map.Entry<String, Integer> entry :
                killCountReference.entrySet())
        {
            if (normalizeName(entry.getKey())
                    .equals(normalizedName))
            {
                return entry.getValue() == null
                        ? 0
                        : entry.getValue();
            }
        }

        return 0;
    }

    private String findKillCountKey(
            String normalizedName)
    {
        for (String key :
                killCountReference.keySet())
        {
            if (normalizeName(key)
                    .equals(normalizedName))
            {
                return key;
            }
        }

        return getDisplayName(
                normalizedName
        );
    }

    private String getLastSpecText(
            String normalizedName)
    {
        return lastPlayerInfo.getOrDefault(
                normalizedName,
                ""
        );
    }

    private String getDisplayName(
            String normalizedName)
    {
        String displayName =
                displayNames.getOrDefault(
                        normalizedName,
                        normalizedName
                );

        return removeYouSuffix(
                displayName
        );
    }

    private String normalizeName(
            String name)
    {
        if (name == null)
        {
            return "";
        }

        return removeYouSuffix(name)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String removeYouSuffix(
            String name)
    {
        if (name == null)
        {
            return "";
        }

        return name
                .replace('\u00A0', ' ')
                .replace(" (You)", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String formatAfkTime(
            long milliseconds)
    {
        if (milliseconds < 0 ||
                milliseconds == Long.MAX_VALUE)
        {
            return "unknown";
        }

        long totalSeconds =
                milliseconds / 1000;

        long minutes =
                totalSeconds / 60;

        long seconds =
                totalSeconds % 60;

        if (minutes <= 0)
        {
            return seconds + "s";
        }

        if (seconds == 0)
        {
            return minutes == 1
                    ? "1 min"
                    : minutes + " mins";
        }

        return minutes +
                "m " +
                seconds +
                "s";
    }

    private void updateHiddenButtonText()
    {
        int hiddenCount =
                getNormalizedHiddenPlayers()
                        .size();

        showHiddenButton.setText(
                hiddenCount > 0
                        ? "Hidden Players (" +
                        hiddenCount +
                        ")"
                        : "Hidden Players"
        );
    }

    private enum PlayerCategory
    {
        ACTIVE,
        CUSTOMER,
        AWAY
    }

    private static class CustomerPackage
    {
        private int packageSize;
        private int packageKills;
        private boolean completed;
    }

    private static class PlayerRowData
    {
        private final String normalizedName;
        private final String displayName;
        private final String specText;
        private final int kills;
        private final boolean lowHp;
        private final boolean temporarilyAway;
        private final boolean away;
        private final long lastSeen;

        private PlayerRowData(
                String normalizedName,
                String displayName,
                String specText,
                int kills,
                boolean lowHp,
                boolean temporarilyAway,
                boolean away,
                long lastSeen)
        {
            this.normalizedName =
                    normalizedName;

            this.displayName =
                    displayName;

            this.specText =
                    specText;

            this.kills =
                    kills;

            this.lowHp =
                    lowHp;

            this.temporarilyAway =
                    temporarilyAway;

            this.away =
                    away;

            this.lastSeen =
                    lastSeen;
        }
    }
}