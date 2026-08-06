package com.wecorpcc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.GridLayout;
import javax.swing.JOptionPane;
import net.runelite.client.util.LinkBrowser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class SoloPanel extends PluginPanel
{
    private static final int DWH_TARGET = 4;
    private static final int BGS_TARGET = 200;
    private static final int ARCLIGHT_TARGET = 20;
    private Runnable readyCallback;
    private boolean readyMessageSent;

    private static final Color TITLE_COLOR =
            new Color(0, 153, 255);

    private static final Color COMPLETE_COLOR =
            new Color(50, 205, 50);

    private static final Color INCOMPLETE_COLOR =
            new Color(255, 190, 60);

    private static final Color SECONDARY_TEXT_COLOR =
            new Color(180, 180, 180);

    private final JLabel remainingLabel =
            new JLabel("", SwingConstants.CENTER);

    private final JButton guideButton =
            new JButton("Guide");

    private final JButton supportButton =
            new JButton("Support RuneLite");
    private final JLabel titleLabel =
            new JLabel("WeCorpCC Solo", SwingConstants.CENTER);

    private final JLabel modeLabel =
            new JLabel("SOLO PREPARATION", SwingConstants.CENTER);

    private final JLabel statusLabel =
            new JLabel("PREPARING", SwingConstants.CENTER);

    private final JLabel prepTimeLabel =
            new JLabel("0m 00s", SwingConstants.RIGHT);

    private final JLabel dwhValueLabel =
            new JLabel("0 / 4", SwingConstants.RIGHT);

    private final JLabel bgsValueLabel =
            new JLabel("0 / 200", SwingConstants.RIGHT);

    private final JLabel arclightValueLabel =
            new JLabel("0 / 20", SwingConstants.RIGHT);

    private final JProgressBar dwhProgress =
            new JProgressBar(0, DWH_TARGET);

    private final JProgressBar bgsProgress =
            new JProgressBar(0, BGS_TARGET);

    private final JProgressBar arclightProgress =
            new JProgressBar(0, ARCLIGHT_TARGET);

    private final JButton resetButton =
            new JButton("Reset Solo Prep");

    private final Timer timer;

    private int dwhCount;
    private int bgsDamage;
    private int arclightCount;

    private long prepStartTime;
    private boolean preparationStarted;

    public SoloPanel()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        setBorder(
                BorderFactory.createEmptyBorder(
                        8,
                        8,
                        8,
                        8
                )
        );

        JPanel mainPanel = new JPanel();

        mainPanel.setLayout(
                new BoxLayout(
                        mainPanel,
                        BoxLayout.Y_AXIS
                )
        );

        mainPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        setupTitle();
        setupModeLabel();
        setupStatusLabel();
        setupProgressBar(dwhProgress);
        setupProgressBar(bgsProgress);
        setupProgressBar(arclightProgress);
        setupResetButton();

        remainingLabel.setForeground(SECONDARY_TEXT_COLOR);

        remainingLabel.setFont(
                remainingLabel.getFont().deriveFont(
                        Font.PLAIN,
                        11f
                )
        );

        remainingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        remainingLabel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        65
                )
        );

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(modeLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createStatusPanel());
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(remainingLabel);
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createPreparationPanel());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createTimerPanel());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(resetButton);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(createBottomButtonPanel());

        add(mainPanel, BorderLayout.NORTH);

        timer = new Timer(
                1000,
                event -> refreshTimer()
        );

        timer.start();
        refreshDisplay();
    }

    private JPanel createBottomButtonPanel()
    {
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));

        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        guideButton.setFocusable(false);
        supportButton.setFocusable(false);

        guideButton.addActionListener(e -> showSoloGuide());

        supportButton.addActionListener(e ->
                LinkBrowser.browse("https://www.patreon.com/runelite"));

        panel.add(guideButton);
        panel.add(supportButton);

        return panel;
    }



    public void setReadyCallback(Runnable readyCallback)
    {
        this.readyCallback = readyCallback;
    }
    private void setupTitle()
    {
        titleLabel.setForeground(TITLE_COLOR);

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

    private void setupModeLabel()
    {
        modeLabel.setForeground(
                SECONDARY_TEXT_COLOR
        );

        modeLabel.setFont(
                modeLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                10f
                        )
        );

        modeLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        modeLabel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        20
                )
        );
    }

    private void setupStatusLabel()
    {
        statusLabel.setForeground(
                INCOMPLETE_COLOR
        );

        statusLabel.setFont(
                statusLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                15f
                        )
        );

        statusLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );
    }

    private void setupProgressBar(
            JProgressBar progressBar)
    {
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setFocusable(false);

        progressBar.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        progressBar.setForeground(
                INCOMPLETE_COLOR
        );

        progressBar.setPreferredSize(
                new Dimension(210, 18)
        );

        progressBar.setMinimumSize(
                new Dimension(120, 18)
        );

        progressBar.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        18
                )
        );
    }

    private void setupResetButton()
    {
        resetButton.setFocusable(false);

        resetButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        resetButton.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        28
                )
        );

        resetButton.addActionListener(
                event -> resetProgress()
        );
    }

    private JPanel createStatusPanel()
    {
        JPanel panel = new JPanel(
                new BorderLayout()
        );

        panel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                ColorScheme.MEDIUM_GRAY_COLOR
                        ),
                        BorderFactory.createEmptyBorder(
                                8,
                                8,
                                8,
                                8
                        )
                )
        );

        panel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        44
                )
        );

        panel.add(
                statusLabel,
                BorderLayout.CENTER
        );

        return panel;
    }

    private JPanel createPreparationPanel()
    {
        JPanel panel = new JPanel(
                new GridBagLayout()
        );

        panel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                ColorScheme.MEDIUM_GRAY_COLOR
                        ),
                        BorderFactory.createEmptyBorder(
                                8,
                                8,
                                8,
                                8
                        )
                )
        );

        addPreparationRow(
                panel,
                0,
                "DWH Specs",
                dwhValueLabel,
                dwhProgress
        );

        addPreparationRow(
                panel,
                1,
                "BGS Damage",
                bgsValueLabel,
                bgsProgress
        );

        addPreparationRow(
                panel,
                2,
                "Arclight Hits",
                arclightValueLabel,
                arclightProgress
        );

        return panel;
    }

    private void addPreparationRow(
            JPanel panel,
            int row,
            String name,
            JLabel valueLabel,
            JProgressBar progressBar)
    {
        JLabel nameLabel =
                new JLabel(name);

        nameLabel.setForeground(Color.WHITE);

        nameLabel.setFont(
                nameLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                12f
                        )
        );

        valueLabel.setForeground(
                SECONDARY_TEXT_COLOR
        );

        valueLabel.setFont(
                valueLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                11f
                        )
        );

        GridBagConstraints nameConstraints =
                new GridBagConstraints();

        nameConstraints.gridx = 0;
        nameConstraints.gridy = row * 2;
        nameConstraints.weightx = 1.0;
        nameConstraints.anchor =
                GridBagConstraints.WEST;

        nameConstraints.fill =
                GridBagConstraints.HORIZONTAL;

        nameConstraints.insets =
                new Insets(
                        row == 0 ? 0 : 8,
                        0,
                        3,
                        5
                );

        panel.add(
                nameLabel,
                nameConstraints
        );

        GridBagConstraints valueConstraints =
                new GridBagConstraints();

        valueConstraints.gridx = 1;
        valueConstraints.gridy = row * 2;
        valueConstraints.anchor =
                GridBagConstraints.EAST;

        valueConstraints.insets =
                new Insets(
                        row == 0 ? 0 : 8,
                        5,
                        3,
                        0
                );

        panel.add(
                valueLabel,
                valueConstraints
        );

        GridBagConstraints progressConstraints =
                new GridBagConstraints();

        progressConstraints.gridx = 0;
        progressConstraints.gridy = row * 2 + 1;
        progressConstraints.gridwidth = 2;
        progressConstraints.weightx = 1.0;

        progressConstraints.fill =
                GridBagConstraints.HORIZONTAL;

        progressConstraints.insets =
                new Insets(
                        0,
                        0,
                        0,
                        0
                );

        panel.add(
                progressBar,
                progressConstraints
        );
    }

    private JPanel createTimerPanel()
    {
        JPanel panel = new JPanel(
                new BorderLayout()
        );

        panel.setBackground(
                ColorScheme.DARKER_GRAY_COLOR
        );

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                ColorScheme.MEDIUM_GRAY_COLOR
                        ),
                        BorderFactory.createEmptyBorder(
                                7,
                                8,
                                7,
                                8
                        )
                )
        );

        JLabel label =
                new JLabel("Prep Time");

        label.setForeground(
                SECONDARY_TEXT_COLOR
        );

        label.setFont(
                label
                        .getFont()
                        .deriveFont(
                                Font.PLAIN,
                                12f
                        )
        );

        prepTimeLabel.setForeground(
                Color.WHITE
        );

        prepTimeLabel.setFont(
                prepTimeLabel
                        .getFont()
                        .deriveFont(
                                Font.BOLD,
                                12f
                        )
        );

        panel.add(label, BorderLayout.WEST);
        panel.add(prepTimeLabel, BorderLayout.EAST);

        panel.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        36
                )
        );

        return panel;
    }

    public void setDwhCount(int count)
    {
        startPreparationIfNeeded();

        dwhCount = Math.max(
                0,
                count
        );

        refreshDisplay();
    }

    public void setBgsDamage(int damage)
    {
        startPreparationIfNeeded();

        bgsDamage = Math.max(
                0,
                damage
        );

        refreshDisplay();
    }

    public void setArclightCount(int count)
    {
        startPreparationIfNeeded();

        arclightCount = Math.max(
                0,
                count
        );

        refreshDisplay();
    }

    public void addDwhSpec()
    {
        setDwhCount(
                dwhCount + 1
        );
    }

    public void addBgsDamage(int damage)
    {
        setBgsDamage(
                bgsDamage +
                        Math.max(0, damage)
        );
    }

    public void addArclightHit()
    {
        setArclightCount(
                arclightCount + 1
        );
    }

    public void resetProgress()
    {
        dwhCount = 0;
        bgsDamage = 0;
        arclightCount = 0;

        preparationStarted = false;
        prepStartTime = 0;

        refreshDisplay();
        refreshTimer();
        readyMessageSent = false;
    }

    private void startPreparationIfNeeded()
    {
        if (preparationStarted)
        {
            return;
        }

        preparationStarted = true;
        prepStartTime =
                System.currentTimeMillis();
    }

    private void refreshDisplay()
    {
        SwingUtilities.invokeLater(() ->
        {
            int shownDwh =
                    Math.min(
                            dwhCount,
                            DWH_TARGET
                    );

            int shownBgs =
                    Math.min(
                            bgsDamage,
                            BGS_TARGET
                    );

            int shownArclight =
                    Math.min(
                            arclightCount,
                            ARCLIGHT_TARGET
                    );

            dwhProgress.setValue(shownDwh);
            bgsProgress.setValue(shownBgs);
            arclightProgress.setValue(shownArclight);

            dwhValueLabel.setText(
                    shownDwh +
                            " / " +
                            DWH_TARGET
            );

            bgsValueLabel.setText(
                    shownBgs +
                            " / " +
                            BGS_TARGET
            );

            arclightValueLabel.setText(
                    shownArclight +
                            " / " +
                            ARCLIGHT_TARGET
            );

            updateProgressColor(
                    dwhProgress,
                    shownDwh >= DWH_TARGET
            );

            updateProgressColor(
                    bgsProgress,
                    shownBgs >= BGS_TARGET
            );

            updateProgressColor(
                    arclightProgress,
                    shownArclight >= ARCLIGHT_TARGET
            );

            boolean ready =
                    shownDwh >= DWH_TARGET &&
                            shownBgs >= BGS_TARGET &&
                            shownArclight >= ARCLIGHT_TARGET;
            if (ready && !readyMessageSent)
            {
                readyMessageSent = true;

                if (readyCallback != null)
                {
                    readyCallback.run();
                }
            }

            if (!ready)
            {
                readyMessageSent = false;
            }
            statusLabel.setText(
                    ready
                            ? "READY TO KILL"
                            : "PREPARING"
            );

            statusLabel.setForeground(
                    ready
                            ? COMPLETE_COLOR
                            : INCOMPLETE_COLOR
            );

            updateRemainingText();

            revalidate();
            repaint();
        });
    }

    private void updateRemainingText()
    {
        boolean ready =
                dwhCount >= DWH_TARGET &&
                        bgsDamage >= BGS_TARGET &&
                        arclightCount >= ARCLIGHT_TARGET;

        if (ready)
        {
            remainingLabel.setText(
                    "<html><center>" +
                            "<font color='#32CD32'><b>" +
                            "All defence reductions complete." +
                            "</b></font>" +
                            "</center></html>"
            );
            return;
        }

        int remainingDwh =
                Math.max(0, DWH_TARGET - dwhCount);

        int remainingBgs =
                Math.max(0, BGS_TARGET - bgsDamage);

        int remainingArclight =
                Math.max(0, ARCLIGHT_TARGET - arclightCount);

        StringBuilder text =
                new StringBuilder("<html><center>");

        if (remainingDwh > 0)
        {
            text.append("Need ")
                    .append(remainingDwh)
                    .append(" more DWH<br>");
        }

        if (remainingBgs > 0)
        {
            text.append("Need ")
                    .append(remainingBgs)
                    .append(" BGS damage<br>");
        }

        if (remainingArclight > 0)
        {
            text.append("Need ")
                    .append(remainingArclight)
                    .append(" more Arclight");
        }

        text.append("</center></html>");
        remainingLabel.setText(text.toString());
    }

    private void updateProgressColor(
            JProgressBar progressBar,
            boolean complete)
    {
        progressBar.setForeground(
                complete
                        ? COMPLETE_COLOR
                        : INCOMPLETE_COLOR
        );
    }

    private void refreshTimer()
    {
        if (!preparationStarted ||
                prepStartTime <= 0)
        {
            prepTimeLabel.setText(
                    "0m 00s"
            );

            return;
        }

        long totalSeconds =
                (System.currentTimeMillis() -
                        prepStartTime) / 1000;

        long minutes =
                totalSeconds / 60;

        long seconds =
                totalSeconds % 60;

        prepTimeLabel.setText(
                String.format(
                        "%dm %02ds",
                        minutes,
                        seconds
                )
        );
    }
    private void showSoloGuide()
    {
        String guide =
                "WeCorpCC Solo Guide\n\n" +
                        "DWH\n" +
                        "- 4 successful specs\n" +
                        "- Zero damage doesn't count\n\n" +

                        "BGS\n" +
                        "- Deal 200 total spec damage\n" +
                        "- Zero damage doesn't count\n\n" +

                        "Arclight\n" +
                        "- 20 successful specs\n" +
                        "- Normal attacks don't count\n" +
                        "- Zero damage doesn't count\n\n" +

                        "READY TO KILL when all goals are complete.";

        JOptionPane.showMessageDialog(
                this,
                guide,
                "Solo Guide",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}