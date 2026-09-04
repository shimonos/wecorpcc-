package com.wecorpcc;

import net.runelite.api.World;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Timer;

import net.runelite.client.callback.ClientThread;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import net.runelite.api.Client;
import net.runelite.api.Player;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class CorpMassLobbyPanel extends PluginPanel {
    private final JPanel massListPanel =
            new JPanel();

    private final Client client;
    private final ClientThread clientThread;
    private static final String LOBBY_API = "https://wecorpcc.onrender.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final Gson gson;

    private int advertisedMemberCount = 0;

    private long emptySince = -1L;
    private long advertisedSince = -1L;

    private final Timer lobbyRefreshTimer;


    private final JButton advertiseMassButton =
            new JButton("Advertise My Mass");
    private int advertisedWorld = -1;
    private String advertisedRule = null;
    private String ownerToken = null;

    public CorpMassLobbyPanel(
            Client client,
            ClientThread clientThread,
            OkHttpClient httpClient,
            Gson gson)
    {
        super();

        this.client = client;
        this.clientThread = clientThread;
        this.httpClient = httpClient;
        this.gson = gson;
        lobbyRefreshTimer = new Timer(
                5000,
                event ->
                {
                    refreshAdvertisedMass();
                    fetchSharedMasses();
                }
        );

        lobbyRefreshTimer.start();

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

        JLabel titleLabel =
                new JLabel(
                        "Corp Mass Lobby",
                        SwingConstants.CENTER
                );

        titleLabel.setForeground(
                new Color(0, 153, 255)
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

        JLabel subtitleLabel =
                new JLabel(
                        "Live advertised Corp masses",
                        SwingConstants.CENTER
                );

        subtitleLabel.setForeground(
                new Color(180, 180, 180)
        );

        subtitleLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        topPanel.add(titleLabel);

        topPanel.add(
                Box.createVerticalStrut(4)
        );

        topPanel.add(subtitleLabel);

        topPanel.add(
                Box.createVerticalStrut(8)
        );

        massListPanel.setLayout(
                new BoxLayout(
                        massListPanel,
                        BoxLayout.Y_AXIS
                )
        );

        massListPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        JLabel emptyLabel =
                new JLabel(
                        "No live masses right now",
                        SwingConstants.CENTER
                );

        emptyLabel.setForeground(
                new Color(180, 180, 180)
        );

        emptyLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        massListPanel.add(emptyLabel);

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

        advertiseMassButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        advertiseMassButton.addActionListener(
                event ->
                {
                    Object[] options =
                            {
                                    "FFA",
                                    "Split"
                            };

                    int choice =
                            javax.swing.JOptionPane.showOptionDialog(
                                    this,
                                    "Choose the loot rule for your mass:",
                                    "Advertise Corp Mass",
                                    javax.swing.JOptionPane.DEFAULT_OPTION,
                                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    options,
                                    options[0]
                            );

                    if (choice == 0) {
                        advertiseMass("FFA");
                    } else if (choice == 1) {
                        advertiseMass("SPLIT");
                    }
                }
        );

        bottomPanel.add(
                Box.createVerticalStrut(8)
        );

        bottomPanel.add(
                advertiseMassButton
        );

        bottomPanel.add(
                Box.createVerticalStrut(8)
        );

        JButton guideButton =
                new JButton("Guide");

        guideButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        guideButton.addActionListener(
                event ->
                        javax.swing.JOptionPane.showMessageDialog(
                                this,
                                "Corp Mass Lobby Guide\n\n" +
                                        "• Advertise only if you want your mass to be public.\n" +
                                        "• Choose FFA or Split before advertising.\n" +
                                        "• Private Corp teams are never shown automatically.\n" +
                                        "• Split masses should agree on loot rules before starting.\n" +
                                        "• Keep evidence of the agreement and important drops.",
                                "Corp Mass Lobby Guide",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE
                        )
        );

        bottomPanel.add(
                guideButton
        );

        JPanel contentPanel =
                new JPanel();

        contentPanel.setLayout(
                new BoxLayout(
                        contentPanel,
                        BoxLayout.Y_AXIS
                )
        );

        contentPanel.setBackground(
                ColorScheme.DARK_GRAY_COLOR
        );

        contentPanel.add(
                massListPanel
        );

        contentPanel.add(
                Box.createVerticalStrut(10)
        );

        contentPanel.add(
                bottomPanel
        );

        contentPanel.add(
                Box.createVerticalGlue()
        );

        add(
                topPanel,
                BorderLayout.NORTH
        );

        add(
                contentPanel,
                BorderLayout.CENTER
        );
    }

    private int getCorpMemberCount() {
        int count = 0;

        for (Player player : client.getPlayers()) {
            if (player == null) {
                continue;
            }

            if (player.getLocalLocation() != null) {
                count++;
            }
        }

        return count;
    }

    private void advertiseMass(String rule) {
        clientThread.invokeLater(() -> {
            Player localPlayer = client.getLocalPlayer();

            if (localPlayer == null ||
                    localPlayer.getWorldLocation() == null ||
                    localPlayer.getWorldLocation().getRegionID() != 11844) {
                showLobbyMessage(
                        "You must be at Corp to advertise a mass.",
                        javax.swing.JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (advertisedWorld != -1 && ownerToken != null) {
                replaceAdvertisedMass(rule);
                return;
            }

            int world = client.getWorld();
            int members = getCorpMemberCount();

            String body = gson.toJson(
                    new AdvertiseRequest(
                            world,
                            members,
                            rule
                    )
            );

            Request request = new Request.Builder()
                    .url(LOBBY_API + "/masses")
                    .post(
                            RequestBody.create(
                                    JSON,
                                    body
                            )
                    )
                    .build();

            httpClient.newCall(request).enqueue(
                    new okhttp3.Callback() {
                        @Override
                        public void onFailure(
                                okhttp3.Call call,
                                java.io.IOException e) {
                            showLobbyMessage(
                                    "Could not advertise mass.\n\n" +
                                            e.getMessage(),
                                    javax.swing.JOptionPane.ERROR_MESSAGE
                            );
                        }

                        @Override
                        public void onResponse(
                                okhttp3.Call call,
                                Response response)
                                throws java.io.IOException {

                            try (Response res = response) {
                                String json =
                                        res.body() != null
                                                ? res.body().string()
                                                : "";

                                if (!res.isSuccessful()) {
                                    ServerError error =
                                            parseServerError(json);

                                    showLobbyMessage(
                                            error.error != null
                                                    ? error.error
                                                    : "Server returned " +
                                                    res.code(),
                                            javax.swing.JOptionPane.WARNING_MESSAGE
                                    );
                                    return;
                                }

                                AdvertiseResponse result =
                                        gson.fromJson(
                                                json,
                                                AdvertiseResponse.class
                                        );

                                if (result == null ||
                                        result.ownerToken == null) {
                                    showLobbyMessage(
                                            "Server did not return an advertisement token.",
                                            javax.swing.JOptionPane.ERROR_MESSAGE
                                    );
                                    return;
                                }

                                advertisedWorld = result.world;
                                advertisedRule = result.rule;
                                advertisedMemberCount = result.members;
                                advertisedSince = result.startedAt;
                                emptySince = -1L;
                                ownerToken = result.ownerToken;

                                fetchSharedMasses();
                            }
                        }
                    }
            );
        });
    }
    private void replaceAdvertisedMass(String newRule) {
        int oldWorld = advertisedWorld;
        String oldOwnerToken = ownerToken;

        String body = gson.toJson(
                new DeleteRequest(oldOwnerToken)
        );

        Request request = new Request.Builder()
                .url(LOBBY_API + "/masses/" + oldWorld)
                .delete(RequestBody.create(JSON, body))
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(
                    okhttp3.Call call,
                    java.io.IOException e) {
                showLobbyMessage(
                        "Could not replace your current mass.\n\n" +
                                e.getMessage(),
                        javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }

            @Override
            public void onResponse(
                    okhttp3.Call call,
                    Response response)
                    throws java.io.IOException {

                try (Response res = response) {
                    if (!res.isSuccessful() &&
                            res.code() != 404) {
                        showLobbyMessage(
                                "Could not replace your current mass.\n\n" +
                                        "Server returned " + res.code(),
                                javax.swing.JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                    clearLocalAdvertisement();

                    javax.swing.SwingUtilities.invokeLater(
                            () -> advertiseMass(newRule)
                    );
                }
            }
        });
    }
        private void refreshAdvertisedMass() {
            if (advertisedWorld == -1 || ownerToken == null) {
                return;
            }

            clientThread.invokeLater(() -> {
                if (advertisedWorld == -1 || ownerToken == null) {
                    return;
                }

                if (client.getWorld() != advertisedWorld) {
                    return;
                }

                Player localPlayer = client.getLocalPlayer();

                if (localPlayer == null ||
                        localPlayer.getWorldLocation() == null ||
                        localPlayer.getWorldLocation().getRegionID() != 11844) {
                    return;
                }

                int members = getCorpMemberCount();
                advertisedMemberCount = members;

                String body = gson.toJson(
                        new HeartbeatRequest(
                                members,
                                ownerToken
                        )
                );

                Request request = new Request.Builder()
                        .url(
                                LOBBY_API +
                                        "/masses/" +
                                        advertisedWorld
                        )
                        .put(
                                RequestBody.create(
                                        JSON,
                                        body
                                )
                        )
                        .build();

                httpClient.newCall(request).enqueue(
                        new okhttp3.Callback() {
                            @Override
                            public void onFailure(
                                    okhttp3.Call call,
                                    java.io.IOException e) {
                                System.out.println(
                                        "WECORP MASS LOBBY | heartbeat failed: " +
                                                e.getMessage()
                                );
                            }

                            @Override
                            public void onResponse(
                                    okhttp3.Call call,
                                    Response response)
                                    throws java.io.IOException {

                                try (Response res = response) {
                                    if (res.code() == 404) {
                                        clearLocalAdvertisement();
                                        fetchSharedMasses();
                                    } else if (!res.isSuccessful()) {
                                        System.out.println(
                                                "WECORP MASS LOBBY | heartbeat returned " +
                                                        res.code()
                                        );
                                    }
                                }
                            }
                        }
                );
            });
        }

    private void clearLocalAdvertisement() {
        advertisedWorld = -1;
        advertisedRule = null;
        advertisedMemberCount = 0;
        ownerToken = null;
        emptySince = -1L;
        advertisedSince = -1L;
    }

    private void addMassCard(
            int world,
            int members,
            String rule) {
        JPanel massCard =
                new JPanel();

        massCard.setLayout(
                new BoxLayout(
                        massCard,
                        BoxLayout.Y_AXIS
                )
        );

        // Slightly different background from the main Lobby panel
        massCard.setBackground(
                new Color(28, 32, 40)
        );

        // Border + inside spacing for each advertisement
        massCard.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(60, 70, 85)
                        ),
                        BorderFactory.createEmptyBorder(
                                10,
                                8,
                                10,
                                8
                        )
                )
        );

        massCard.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        massCard.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        90
                )
        );

        JPanel infoRow =
                new JPanel();

        infoRow.setLayout(
                new BoxLayout(
                        infoRow,
                        BoxLayout.X_AXIS
                )
        );

        infoRow.setOpaque(false);

        infoRow.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel worldLabel =
                new JLabel(
                        "🌐  W" + world
                );

        worldLabel.setForeground(
                Color.WHITE
        );

        JLabel membersLabel =
                new JLabel(
                        "👤  " + members + " members"
                );

        membersLabel.setForeground(
                Color.WHITE
        );

        JLabel ruleLabel =
                new JLabel(
                        rule
                );

        ruleLabel.setForeground(
                Color.WHITE
        );

        infoRow.add(
                worldLabel
        );

        infoRow.add(
                Box.createHorizontalStrut(14)
        );

        infoRow.add(
                membersLabel
        );

        infoRow.add(
                Box.createHorizontalStrut(14)
        );

        infoRow.add(
                ruleLabel
        );

        JButton hopButton =
                new JButton(
                        "➤  Hop to W" + world
                );

        hopButton.setBackground(
                new Color(45, 105, 190)
        );

        hopButton.setForeground(
                Color.WHITE
        );

        hopButton.setFocusPainted(false);

        hopButton.setOpaque(true);

        hopButton.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(80, 140, 220)
                        ),
                        BorderFactory.createEmptyBorder(
                                5,
                                12,
                                5,
                                12
                        )
                )
        );

        hopButton.addActionListener(
                event ->
                {
                    for (World targetWorld : client.getWorldList()) {
                        if (targetWorld.getId() == world) {
                            client.hopToWorld(targetWorld);
                            return;
                        }
                    }

                    System.out.println(
                            "WECORP MASS LOBBY | world not found: " +
                                    world
                    );
                }
        );

        hopButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        massCard.add(
                infoRow
        );

        massCard.add(
                Box.createVerticalStrut(8)
        );

        massCard.add(
                hopButton
        );

        massListPanel.add(
                massCard
        );

    }



    private void renderSharedMasses(List<SharedMass> masses) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            massListPanel.removeAll();

            if (masses == null || masses.isEmpty()) {
                JLabel empty = new JLabel("No live masses right now", SwingConstants.CENTER);
                empty.setForeground(new Color(180, 180, 180));
                empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                massListPanel.add(empty);
            } else {
                for (SharedMass mass : masses) {
                    if (mass == null) continue;
                    addMassCard(mass.world, mass.members, mass.rule);
                    massListPanel.add(Box.createVerticalStrut(6));
                }
            }

            massListPanel.revalidate();
            massListPanel.repaint();
        });
    }

    private void fetchSharedMasses() {
        Request request = new Request.Builder().url(LOBBY_API + "/masses").get().build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                System.out.println("WECORP MASS LOBBY | fetch failed: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws java.io.IOException {
                try (Response res = response) {
                    if (!res.isSuccessful()) {
                        System.out.println("WECORP MASS LOBBY | server returned " + res.code());
                        return;
                    }

                    String body = res.body() != null ? res.body().string() : "[]";
                    Type type = new TypeToken<ArrayList<SharedMass>>() { }.getType();
                    List<SharedMass> masses = gson.fromJson(body, type);
                    renderSharedMasses(masses);
                }
            }
        });
    }

    private ServerError parseServerError(String body) {
        try {
            ServerError error = gson.fromJson(body, ServerError.class);
            return error != null ? error : new ServerError();
        } catch (Exception ignored) {
            return new ServerError();
        }
    }

    private void showLobbyMessage(String message, int type) {
        javax.swing.SwingUtilities.invokeLater(() ->
                javax.swing.JOptionPane.showMessageDialog(
                        this, message, "Corp Mass Lobby", type));
    }

    private static class AdvertiseRequest {
        private final int world;
        private final int members;
        private final String rule;
        private AdvertiseRequest(int world, int members, String rule) {
            this.world = world;
            this.members = members;
            this.rule = rule;
        }
    }

    private static class HeartbeatRequest {
        private final int members;
        private final String ownerToken;

        private HeartbeatRequest(int members, String ownerToken) {
            this.members = members;
            this.ownerToken = ownerToken;
        }
    }

    private static class DeleteRequest {
        private final String ownerToken;

        private DeleteRequest(String ownerToken) {
            this.ownerToken = ownerToken;
        }
    }

    private static class AdvertiseResponse {
        private int world;
        private int members;
        private String rule;
        private long startedAt;
        private String ownerToken;
    }

    private static class SharedMass {
        private int world;
        private int members;
        private String rule;
    }

    private static class ServerError {
        private String error;
    }
}
