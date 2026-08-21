package com.wecorpcc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class MassPanel extends PluginPanel
{
    private static final long INACTIVE_AFTER_MS = 90_000L;
    private static final long LEFT_AFTER_MS = 12 * 60 * 1000L;

    private static final Color ACTIVE_COLOR = new Color(50, 205, 50);
    private static final Color AWAY_COLOR = new Color(145, 145, 145);
    private static final Color TITLE_COLOR = new Color(0, 153, 255);
    private static final Color SECONDARY_TEXT_COLOR = new Color(180, 180, 180);

    private final JLabel titleLabel =
            new JLabel("WeCorpCC Mass", SwingConstants.CENTER);

    private final JLabel statusLabel =
            new JLabel("○ WAITING FOR CORP", SwingConstants.CENTER);

    private final JLabel playersValue = new JLabel("0");
    private final JLabel killsValue = new JLabel("0");
    private final JLabel tripValue = new JLabel("0m 0s");

    private final JLabel fangValue = new JLabel("0");
    private final JLabel voidwakerValue = new JLabel("0");
    private final JLabel dwhValue = new JLabel("0");
    private final JLabel bgsValue = new JLabel("0");

    private final JLabel onyxValue = new JLabel("0");
    private final JLabel arcaneValue = new JLabel("0");
    private final JLabel spectralValue = new JLabel("0");
    private final JLabel elysianValue = new JLabel("0");

    private final JPanel playerListPanel = new JPanel();
    private final JScrollPane playerScrollPane =
            new JScrollPane(playerListPanel);

    private final JButton resetButton = new JButton("Reset Mass");
    private final JButton guideButton = new JButton("Guide");
    private final JButton supportButton = new JButton("Support RuneLite");

    private final Map<String, PlayerMassData> players =
            new LinkedHashMap<>();

    private final Set<String> removedPlayers =
            new HashSet<>();

    private Runnable resetCallback;

    private final Timer refreshTimer;

    private boolean showDwhCounter = true;
    private boolean overviewCollapsed;
    private boolean currentKillCollapsed;
    private boolean dropsCollapsed;
    private boolean activeCollapsed;
    private boolean inactiveCollapsed;

    private long massStartTime;
    private int playersSeen;
    private int totalKills;

    private int currentFangSpecs;
    private int currentVoidwakerSpecs;
    private int currentDwhSpecs;
    private int currentBgsDamage;

    private int tripFangSpecs;
    private int tripVoidwakerSpecs;
    private int tripDwhSpecs;
    private int tripBgsDamage;

    private int onyxCount;
    private int arcaneCount;
    private int spectralCount;
    private int elysianCount;

    public MassPanel()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        setupTitle();

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(3));
        topPanel.add(statusLabel);
        topPanel.add(Box.createVerticalStrut(7));

        topPanel.add(createCollapsibleSection(
                "OVERVIEW",
                createOverviewPanel(),
                SectionType.OVERVIEW
        ));

        topPanel.add(Box.createVerticalStrut(7));

        topPanel.add(createCollapsibleSection(
                "CURRENT KILL SPECS",
                createCurrentKillPanel(),
                SectionType.CURRENT_KILL
        ));

        topPanel.add(Box.createVerticalStrut(7));

        topPanel.add(createCollapsibleSection(
                "SPECIAL DROPS",
                createDropsPanel(),
                SectionType.DROPS
        ));

        topPanel.add(Box.createVerticalStrut(7));
        topPanel.add(createDivider());

        playerListPanel.setLayout(
                new BoxLayout(playerListPanel, BoxLayout.Y_AXIS)
        );
        playerListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        playerScrollPane.setBorder(null);
        playerScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        playerScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        playerScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        playerScrollPane.getViewport().setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(
                new BoxLayout(bottomPanel, BoxLayout.Y_AXIS)
        );
        bottomPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        bottomPanel.add(createDivider());
        bottomPanel.add(Box.createVerticalStrut(6));
        bottomPanel.add(createButtonPanel());

        setupButtons();

        add(topPanel, BorderLayout.NORTH);
        add(playerScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshTimer = new Timer(
                1000,
                event ->
                {
                    refreshTripTime();
                    refreshPlayerDisplay();
                }
        );
        refreshTimer.start();

        refreshAll();
        refreshPlayerDisplay();
    }

    private void setupTitle()
    {
        titleLabel.setForeground(TITLE_COLOR);
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.BOLD, 18f)
        );

        statusLabel.setForeground(AWAY_COLOR);
        statusLabel.setFont(
                statusLabel.getFont().deriveFont(Font.BOLD, 11f)
        );
    }
    private JPanel createCurrentKillPanel()
    {
        JPanel panel = createInfoPanel(65);

        addStatRow(panel, 0, "Fang Specs", new Color(255, 190, 60), fangValue);
        addStatRow(panel, 1, "Voidwaker Specs", new Color(80, 200, 230), voidwakerValue);

        return panel;
    }

    private JPanel createDropsPanel()
    {
        JPanel panel = createInfoPanel(105);

        addStatRow(panel, 0, "Onyx", new Color(210, 210, 210), onyxValue);
        addStatRow(panel, 1, "Arcane", new Color(80, 170, 255), arcaneValue);
        addStatRow(panel, 2, "Spectral", new Color(170, 120, 255), spectralValue);
        addStatRow(panel, 3, "Elysian", new Color(255, 220, 90), elysianValue);

        return panel;
    }

    private JPanel createInfoPanel(int height)
    {
        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                ColorScheme.MEDIUM_GRAY_COLOR
                        ),
                        BorderFactory.createEmptyBorder(7, 7, 7, 7)
                )
        );
        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, height)
        );
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        return panel;
    }
    private JPanel createOverviewPanel()
    {
        JPanel panel = createInfoPanel(82);

        addStatRow(
                panel,
                0,
                "Players",
                Color.WHITE,
                playersValue
        );

        addStatRow(
                panel,
                1,
                "Corp Kills",
                new Color(0, 153, 255),
                killsValue
        );

        addStatRow(
                panel,
                2,
                "Trip Time",
                new Color(190, 140, 255),
                tripValue
        );

        return panel;
    }
    private JPanel createCollapsibleSection(
            String title,
            JPanel contentPanel,
            SectionType sectionType)
    {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                BorderFactory.createEmptyBorder(6, 7, 6, 7)
        ));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel headerLabel = new JLabel();
        headerLabel.setForeground(SECONDARY_TEXT_COLOR);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 11f));
        header.add(headerLabel, BorderLayout.WEST);

        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Runnable refreshSection = () ->
        {
            boolean collapsed = isTopSectionCollapsed(sectionType);
            headerLabel.setText((collapsed ? "▶ " : "▼ ") + title);
            contentPanel.setVisible(!collapsed);
            wrapper.revalidate();
            wrapper.repaint();
        };

        MouseAdapter listener = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if (!SwingUtilities.isLeftMouseButton(event))
                {
                    return;
                }

                toggleTopSection(sectionType);
                refreshSection.run();
            }
        };

        header.addMouseListener(listener);
        headerLabel.addMouseListener(listener);

        wrapper.add(header);
        wrapper.add(Box.createVerticalStrut(3));
        wrapper.add(contentPanel);

        refreshSection.run();
        return wrapper;
    }

    private boolean isTopSectionCollapsed(SectionType sectionType)
    {
        switch (sectionType)
        {
            case OVERVIEW:
                return overviewCollapsed;
            case CURRENT_KILL:
                return currentKillCollapsed;
            case DROPS:
            default:
                return dropsCollapsed;
        }
    }

    private void toggleTopSection(SectionType sectionType)
    {
        switch (sectionType)
        {
            case OVERVIEW:
                overviewCollapsed = !overviewCollapsed;
                break;
            case CURRENT_KILL:
                currentKillCollapsed = !currentKillCollapsed;
                break;
            case DROPS:
            default:
                dropsCollapsed = !dropsCollapsed;
                break;
        }
    }

    private void addStatRow(
            JPanel panel,
            int row,
            String text,
            Color color,
            JLabel valueLabel)
    {
        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(dot.getFont().deriveFont(Font.BOLD, 13f));

        JLabel name = new JLabel(text);
        name.setForeground(SECONDARY_TEXT_COLOR);
        name.setFont(name.getFont().deriveFont(Font.PLAIN, 12f));

        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(
                valueLabel.getFont().deriveFont(Font.BOLD, 12f)
        );
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        GridBagConstraints c0 = new GridBagConstraints();
        c0.gridx = 0;
        c0.gridy = row;
        c0.anchor = GridBagConstraints.WEST;
        c0.insets = new Insets(1, 0, 1, 5);
        panel.add(dot, c0);

        GridBagConstraints c1 = new GridBagConstraints();
        c1.gridx = 1;
        c1.gridy = row;
        c1.weightx = 1.0;
        c1.fill = GridBagConstraints.HORIZONTAL;
        c1.anchor = GridBagConstraints.WEST;
        c1.insets = new Insets(1, 0, 1, 5);
        panel.add(name, c1);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 2;
        c2.gridy = row;
        c2.anchor = GridBagConstraints.EAST;
        c2.insets = new Insets(1, 5, 1, 0);
        panel.add(valueLabel, c2);
    }

    private void setupButtons()
    {
        resetButton.setFocusable(false);
        guideButton.setFocusable(false);
        supportButton.setFocusable(false);

        resetButton.addActionListener(event -> confirmReset());
        guideButton.addActionListener(event -> showGuide());
        supportButton.addActionListener(
                event -> LinkBrowser.browse(
                        "https://www.patreon.com/runelite"
                )
        );
    }

    private JPanel createButtonPanel()
    {
        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 6, 0)
        );

        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.add(resetButton);
        panel.add(guideButton);
        panel.add(supportButton);

        return panel;
    }

    private JPanel createDivider()
    {
        JPanel divider = new JPanel();

        divider.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        divider.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 1)
        );
        divider.setPreferredSize(new Dimension(1, 1));

        return divider;
    }

    public void setResetCallback(Runnable resetCallback)
    {
        this.resetCallback = resetCallback;
    }

    public void setShowDwhCounter(boolean showDwhCounter)
    {
        this.showDwhCounter = showDwhCounter;
        dwhValue.setVisible(showDwhCounter);
        refreshPlayerDisplay();
    }

    public void setMassActive(boolean active)
    {
        if (active)
        {
            if (massStartTime == 0)
            {
                massStartTime = System.currentTimeMillis();
            }

            statusLabel.setText("● MASS ACTIVE");
            statusLabel.setForeground(ACTIVE_COLOR);
        }
        else
        {
            statusLabel.setText("○ WAITING FOR CORP");
            statusLabel.setForeground(AWAY_COLOR);
        }
    }

    public void updatePlayers(
            Map<String, PlayerMassData> currentPlayers)
    {
        SwingUtilities.invokeLater(() ->
        {
            long now = System.currentTimeMillis();
            Set<String> visibleNames = new HashSet<>();

            if (currentPlayers != null)
            {
                for (Map.Entry<String, PlayerMassData> entry :
                        currentPlayers.entrySet())
                {
                    String displayName = entry.getKey();
                    PlayerMassData incoming = entry.getValue();

                    if (displayName == null ||
                            displayName.trim().isEmpty() ||
                            incoming == null)
                    {
                        continue;
                    }

                    String normalized = normalizeName(displayName);

                    if (removedPlayers.contains(normalized))
                    {
                        continue;
                    }

                    visibleNames.add(normalized);

                    boolean firstSeen =
                            !players.containsKey(normalized);

                    PlayerMassData stored =
                            players.getOrDefault(
                                    normalized,
                                    new PlayerMassData(displayName)
                            );

                    stored.displayName = displayName;
                    stored.kills = Math.max(0, incoming.kills);
                    stored.tripFangSpecs = Math.max(0, incoming.tripFangSpecs);
                    stored.tripVoidwakerSpecs = Math.max(0, incoming.tripVoidwakerSpecs);
                    stored.tripDwhSpecs = Math.max(0, incoming.tripDwhSpecs);
                    stored.lastSeen = now;
                    stored.currentlyVisible = true;

                    players.put(normalized, stored);

                    if (firstSeen)
                    {
                        playersSeen++;
                    }
                }
            }

            for (Map.Entry<String, PlayerMassData> entry :
                    players.entrySet())
            {
                if (!visibleNames.contains(entry.getKey()))
                {
                    entry.getValue().currentlyVisible = false;
                }
            }

            refreshAll();
            refreshPlayerDisplay();
        });
    }

    public void setTotalKills(int totalKills)
    {
        this.totalKills = Math.max(0, totalKills);
        refreshAll();
    }

    public void addFangSpec()
    {
        currentFangSpecs++;
        tripFangSpecs++;
        refreshAll();
    }

    public void addVoidwakerSpec()
    {
        currentVoidwakerSpecs++;
        tripVoidwakerSpecs++;
        refreshAll();
    }

    public void addDwhSpec()
    {
        currentDwhSpecs++;
        tripDwhSpecs++;
        refreshAll();
    }

    public void addBgsDamage(int damage)
    {
        int safeDamage = Math.max(0, damage);
        currentBgsDamage += safeDamage;
        tripBgsDamage += safeDamage;
        refreshAll();
    }

    public void resetCurrentKillSpecs()
    {
        currentFangSpecs = 0;
        currentVoidwakerSpecs = 0;
        currentDwhSpecs = 0;
        currentBgsDamage = 0;
        refreshAll();
    }

    public void addOnyx()
    {
        onyxCount++;
        refreshAll();
    }

    public void addArcaneSigil(
            String receiverName,
            Set<String> attendees)
    {
        arcaneCount++;
        showSigilAttendance(
                "ARCANE SIGIL",
                receiverName,
                attendees
        );
        refreshAll();
    }

    public void addSpectralSigil(
            String receiverName,
            Set<String> attendees)
    {
        spectralCount++;
        showSigilAttendance(
                "SPECTRAL SIGIL",
                receiverName,
                attendees
        );
        refreshAll();
    }

    public void addElysianSigil(
            String receiverName,
            Set<String> attendees)
    {
        elysianCount++;
        showSigilAttendance(
                "ELYSIAN SIGIL",
                receiverName,
                attendees
        );
        refreshAll();
    }

    private void showSigilAttendance(
            String sigilName,
            String receiverName,
            Set<String> attendees)
    {
        List<String> names = new ArrayList<>();

        if (attendees != null)
        {
            for (String name : attendees)
            {
                if (name != null && !name.trim().isEmpty())
                {
                    names.add(removeYouSuffix(name));
                }
            }
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);

        String winner =
                receiverName == null ||
                        receiverName.trim().isEmpty()
                        ? "Unknown"
                        : receiverName.trim();

        String attendeeText =
                names.isEmpty()
                        ? "No attendees recorded."
                        : String.join(", ", names);

        JOptionPane.showMessageDialog(
                this,
                sigilName + " RECEIVED\n\n" +
                        "Winner: " + winner + "\n\n" +
                        "Kill attendees:\n" +
                        attendeeText,
                "WeCorpCC Sigil Drop",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void refreshAll()
    {
        Runnable update = () ->
        {
            int currentPlayers = 0;

            for (PlayerMassData player : players.values())
            {
                if (player.currentlyVisible)
                {
                    currentPlayers++;
                }
            }

            playersValue.setText(String.valueOf(currentPlayers));
            killsValue.setText(String.valueOf(totalKills));

            fangValue.setText(String.valueOf(currentFangSpecs));
            voidwakerValue.setText(String.valueOf(currentVoidwakerSpecs));
            dwhValue.setText(String.valueOf(currentDwhSpecs));
            bgsValue.setText(String.valueOf(currentBgsDamage));

            onyxValue.setText(String.valueOf(onyxCount));
            arcaneValue.setText(String.valueOf(arcaneCount));
            spectralValue.setText(String.valueOf(spectralCount));
            elysianValue.setText(String.valueOf(elysianCount));

            refreshTripTime();
        };

        if (SwingUtilities.isEventDispatchThread())
        {
            update.run();
        }
        else
        {
            SwingUtilities.invokeLater(update);
        }
    }

    private void refreshTripTime()
    {
        if (massStartTime <= 0)
        {
            tripValue.setText("0m 0s");
            return;
        }

        long totalSeconds =
                (System.currentTimeMillis() - massStartTime) / 1000;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        tripValue.setText(
                minutes + "m " + seconds + "s"
        );
    }

    private void refreshPlayerDisplay()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::refreshPlayerDisplay);
            return;
        }

        playerListPanel.removeAll();

        List<PlayerMassData> active = new ArrayList<>();
        List<PlayerMassData> inactive = new ArrayList<>();

        long now = System.currentTimeMillis();

        for (Map.Entry<String, PlayerMassData> entry :
                players.entrySet())
        {
            if (removedPlayers.contains(entry.getKey()))
            {
                continue;
            }

            PlayerMassData player = entry.getValue();

            long awayDuration =
                    player.lastSeen <= 0
                            ? Long.MAX_VALUE
                            : now - player.lastSeen;

            if (!player.currentlyVisible &&
                    awayDuration >= INACTIVE_AFTER_MS)
            {
                inactive.add(player);
            }
            else
            {
                active.add(player);
            }
        }

        active.sort(
                Comparator.comparing(
                        player ->
                                player.displayName.toLowerCase()
                )
        );

        inactive.sort(
                Comparator.comparingLong(
                        (PlayerMassData player) ->
                                player.lastSeen
                ).reversed()
        );

        addCategory(
                "Active Members",
                active,
                false,
                activeCollapsed
        );

        addCategory(
                "Inactive Members",
                inactive,
                true,
                inactiveCollapsed
        );

        playerListPanel.revalidate();
        playerListPanel.repaint();
    }

    private void addCategory(
            String title,
            List<PlayerMassData> categoryPlayers,
            boolean inactiveCategory,
            boolean collapsed)
    {
        JPanel header = new JPanel(new BorderLayout());

        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0,
                                0,
                                1,
                                0,
                                ColorScheme.MEDIUM_GRAY_COLOR
                        ),
                        BorderFactory.createEmptyBorder(6, 7, 6, 7)
                )
        );
        header.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 32)
        );

        JLabel label = new JLabel(
                (collapsed ? "▶ " : "▼ ") +
                        title.toUpperCase() +
                        "  (" +
                        categoryPlayers.size() +
                        ")"
        );

        label.setForeground(
                inactiveCategory
                        ? AWAY_COLOR
                        : ACTIVE_COLOR
        );
        label.setFont(
                label.getFont().deriveFont(Font.BOLD, 11f)
        );

        header.add(label, BorderLayout.WEST);
        header.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        MouseAdapter collapseListener = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if (!SwingUtilities.isLeftMouseButton(event))
                {
                    return;
                }

                if (inactiveCategory)
                {
                    inactiveCollapsed = !inactiveCollapsed;
                }
                else
                {
                    activeCollapsed = !activeCollapsed;
                }

                refreshPlayerDisplay();
            }
        };

        header.addMouseListener(collapseListener);
        label.addMouseListener(collapseListener);

        playerListPanel.add(header);
        playerListPanel.add(Box.createVerticalStrut(3));

        if (collapsed)
        {
            playerListPanel.add(Box.createVerticalStrut(6));
            return;
        }

        if (categoryPlayers.isEmpty())
        {
            JLabel empty = new JLabel("No players");
            empty.setForeground(SECONDARY_TEXT_COLOR);
            empty.setBorder(
                    BorderFactory.createEmptyBorder(5, 10, 7, 2)
            );
            playerListPanel.add(empty);
        }
        else
        {
            for (PlayerMassData player : categoryPlayers)
            {
                playerListPanel.add(
                        createPlayerRow(player, inactiveCategory)
                );
                playerListPanel.add(Box.createVerticalStrut(2));
            }
        }

        playerListPanel.add(Box.createVerticalStrut(8));
    }
    private JPanel createPlayerRow(
            PlayerMassData player,
            boolean inactiveCategory)
    {
        JPanel row = new JPanel(
                new GridBagLayout()
        );

        row.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        row.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        5,
                        5,
                        3
                )
        );

        row.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        62
                )
        );

        boolean gray =
                inactiveCategory ||
                        !player.currentlyVisible;

        JLabel circle = new JLabel("●");

        circle.setForeground(
                gray
                        ? AWAY_COLOR
                        : ACTIVE_COLOR
        );

        circle.setFont(
                circle.getFont().deriveFont(
                        Font.BOLD,
                        14f
                )
        );

        GridBagConstraints c0 =
                new GridBagConstraints();

        c0.gridx = 0;
        c0.gridy = 0;
        c0.gridheight = 3;
        c0.anchor = GridBagConstraints.WEST;
        c0.insets = new Insets(
                0,
                0,
                0,
                5
        );

        row.add(circle, c0);

        JPanel infoPanel = new JPanel();

        infoPanel.setLayout(
                new BoxLayout(
                        infoPanel,
                        BoxLayout.Y_AXIS
                )
        );

        infoPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        JLabel name =
                new JLabel(player.displayName);

        name.setForeground(Color.WHITE);

        name.setFont(
                name.getFont().deriveFont(
                        Font.BOLD,
                        12f
                )
        );

        JLabel status =
                new JLabel(
                        buildPlayerStatus(player)
                );

        status.setForeground(
                gray
                        ? AWAY_COLOR
                        : SECONDARY_TEXT_COLOR
        );

        status.setFont(
                status.getFont().deriveFont(
                        Font.PLAIN,
                        9f
                )
        );

        /*
         * Keep Fang/Voidwaker on the left.
         * KC is moved to its own fixed right-side column below,
         * so it cannot be clipped by long player/stat text.
         */
        JLabel stats =
                new JLabel(
                        "F:" +
                                player.tripFangSpecs +
                                "  VW:" +
                                player.tripVoidwakerSpecs
                );

        stats.setForeground(
                SECONDARY_TEXT_COLOR
        );

        stats.setFont(
                stats.getFont().deriveFont(
                        Font.BOLD,
                        10f
                )
        );

        name.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        status.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        stats.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        infoPanel.add(name);
        infoPanel.add(status);
        infoPanel.add(
                Box.createVerticalStrut(2)
        );
        infoPanel.add(stats);

        GridBagConstraints c1 =
                new GridBagConstraints();

        c1.gridx = 1;
        c1.gridy = 0;
        c1.gridheight = 3;
        c1.weightx = 1.0;
        c1.fill = GridBagConstraints.HORIZONTAL;
        c1.anchor = GridBagConstraints.WEST;
        c1.insets = new Insets(0, 0, 0, 4);

        row.add(infoPanel, c1);

        /*
         * Dedicated KC column on the far right.
         */
        JLabel kc =
                new JLabel(
                        "KC " + player.kills,
                        SwingConstants.RIGHT
                );

        kc.setForeground(Color.WHITE);

        kc.setFont(
                kc.getFont().deriveFont(
                        Font.BOLD,
                        11f
                )
        );

        kc.setPreferredSize(
                new Dimension(42, 38)
        );

        kc.setMinimumSize(
                new Dimension(42, 38)
        );

        GridBagConstraints c2 =
                new GridBagConstraints();

        c2.gridx = 2;
        c2.gridy = 0;
        c2.gridheight = 3;
        c2.anchor = GridBagConstraints.EAST;
        c2.insets = new Insets(0, 2, 0, 0);

        row.add(kc, c2);

        installRemoveMenu(row, player);
        installRemoveMenu(circle, player);
        installRemoveMenu(infoPanel, player);
        installRemoveMenu(name, player);
        installRemoveMenu(status, player);
        installRemoveMenu(stats, player);
        installRemoveMenu(kc, player);

        return row;
    }

    private String buildPlayerStatus(PlayerMassData player)
    {
        if (player.currentlyVisible)
        {
            return "At Corp";
        }

        long awayMs =
                System.currentTimeMillis() -
                        player.lastSeen;

        if (awayMs >= LEFT_AFTER_MS)
        {
            return "LEFT";
        }

        return "AFK " + formatAfkTime(awayMs);
    }
    private String buildPlayerSpecText(
            PlayerMassData player)
    {
        return "Fang: " +
                player.tripFangSpecs +
                "  VW: " +
                player.tripVoidwakerSpecs;
    }
    private void installRemoveMenu(
            Component component,
            PlayerMassData player)
    {
        component.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                showPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                showPopup(event);
            }

            private void showPopup(MouseEvent event)
            {
                if (!event.isPopupTrigger())
                {
                    return;
                }

                JPopupMenu menu = new JPopupMenu();

                JMenuItem title =
                        new JMenuItem(player.displayName);

                title.setEnabled(false);

                JMenuItem remove =
                        new JMenuItem("Remove from Mass");

                remove.addActionListener(
                        action ->
                                removePlayer(
                                        player.displayName
                                )
                );

                menu.add(title);
                menu.addSeparator();
                menu.add(remove);

                menu.show(
                        event.getComponent(),
                        event.getX(),
                        event.getY()
                );
            }
        });
    }

    public void removePlayer(String playerName)
    {
        String normalized =
                normalizeName(playerName);

        removedPlayers.add(normalized);
        players.remove(normalized);

        refreshAll();
        refreshPlayerDisplay();
    }

    private void confirmReset()
    {
        int choice = JOptionPane.showConfirmDialog(
                this,
                buildMassSummary(),
                "Reset Mass?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION)
        {
            return;
        }

        if (resetCallback != null)
        {
            resetCallback.run();
        }

        resetPanel();
    }

    private String buildMassSummary()
    {
        return "Mass Summary\n\n" +
                "Trip Time: " + tripValue.getText() + "\n" +
                "Players Seen: " + playersSeen + "\n" +
                "Corp Kills: " + totalKills + "\n\n" +
                "Trip Specs\n" +
                "Fang: " + tripFangSpecs + "\n" +
                "Voidwaker: " + tripVoidwakerSpecs + "\n\n" +
                "Special Drops\n" +
                "Onyx: " + onyxCount + "\n" +
                "Arcane: " + arcaneCount + "\n" +
                "Spectral: " + spectralCount + "\n" +
                "Elysian: " + elysianCount + "\n\n" +
                "Reset this mass?";
    }

    public void resetPanel()
    {
        players.clear();
        removedPlayers.clear();

        massStartTime = 0;
        playersSeen = 0;
        totalKills = 0;

        currentFangSpecs = 0;
        currentVoidwakerSpecs = 0;
        currentDwhSpecs = 0;
        currentBgsDamage = 0;

        tripFangSpecs = 0;
        tripVoidwakerSpecs = 0;
        tripDwhSpecs = 0;
        tripBgsDamage = 0;

        onyxCount = 0;
        arcaneCount = 0;
        spectralCount = 0;
        elysianCount = 0;

        setMassActive(false);
        refreshAll();
        refreshPlayerDisplay();
    }

    private void showGuide()
    {
        String guide =
                "WeCorpCC Mass Guide\n\n" +
                        "Overview\n" +
                        "• Shows current players, Corp kills and trip time.\n\n" +
                        "Current Kill Specs\n" +
                        "• Tracks Fang and Voidwaker specs.\n" +
                        "• Current-kill counters reset after every Corp kill.\n\n" +
                        "Player Rows\n" +
                        "• Fang and Voidwaker totals remain for the full trip.\n" +
                        "• KC shows how many Corp kills each player received.\n" +
                        "• Green means active at Corp.\n" +
                        "• Gray means away from Corp.\n" +
                        "• After 90 seconds, the player moves to Inactive Members.\n" +
                        "• After 12 minutes away, the status changes to LEFT.\n\n" +
                        "Special Drops\n" +
                        "• Tracks Onyx, Arcane, Spectral and Elysian drops.\n\n" +
                        "Right-click a player to remove them from the Mass panel.";

        JOptionPane.showMessageDialog(
                this,
                guide,
                "WeCorpCC Mass Guide",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    private String formatAfkTime(long milliseconds)
    {
        long seconds =
                Math.max(0, milliseconds / 1000);

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes <= 0)
        {
            return remainingSeconds + "s";
        }

        return minutes +
                "m " +
                remainingSeconds +
                "s";
    }

    private String normalizeName(String name)
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

    private String removeYouSuffix(String name)
    {
        if (name == null)
        {
            return "";
        }

        return name
                .replace(" (You)", "")
                .trim();
    }

    private enum SectionType
    {
        OVERVIEW,
        CURRENT_KILL,
        DROPS
    }

    public static class PlayerMassData
    {
        private String displayName;
        private int kills;
        private int tripFangSpecs;
        private int tripVoidwakerSpecs;
        private int tripDwhSpecs;
        private long lastSeen;
        private boolean currentlyVisible;

        public PlayerMassData(String displayName)
        {
            this.displayName =
                    displayName == null
                            ? ""
                            : displayName;
        }

        public PlayerMassData(
                String displayName,
                int kills,
                int tripFangSpecs,
                int tripVoidwakerSpecs,
                int tripDwhSpecs)
        {
            this(displayName);

            this.kills = Math.max(0, kills);
            this.tripFangSpecs =
                    Math.max(0, tripFangSpecs);
            this.tripVoidwakerSpecs =
                    Math.max(0, tripVoidwakerSpecs);
            this.tripDwhSpecs =
                    Math.max(0, tripDwhSpecs);
        }
    }
}