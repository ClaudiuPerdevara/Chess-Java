package com.chess.gui;

import javax.swing.*;
import java.awt.*;

public class LobbyPanel extends JPanel
{
    private ChessWindow windowController;
    private JLabel statusLabel;
    private Timer hostPollingTimer;

    public LobbyPanel(ChessWindow window)
    {
        this.windowController = window;
        setLayout(new GridBagLayout());

        JPanel lobbyBox = new JPanel(new BorderLayout())
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(35, 38, 42));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
        };
        lobbyBox.setPreferredSize(new Dimension(500, 350));
        lobbyBox.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 15, 30));

        JLabel titleLabel = new JLabel("MULTIPLAYER");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 30, 40));

        statusLabel = new JLabel("Selectează o opțiune pentru a începe.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        statusLabel.setForeground(new Color(180, 180, 180));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JButton hostBtn = createBtn("Host Game", new Color(70, 130, 180));
        hostBtn.addActionListener(e -> {
            statusLabel.setText("Se creează serverul...");
            statusLabel.setForeground(new Color(255, 165, 0));

            SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
            {
                @Override
                protected String doInBackground()
                {
                    return NetworkManager.hostGame();
                }
                @Override
                protected void done()
                {
                    try
                    {
                        String result = get();
                        if (result.equals("FIREBASE_ERROR"))
                        {
                            statusLabel.setText("Eroare Bază de date: Verifică regulile (Test Mode)");
                            statusLabel.setForeground(Color.RED);
                        }
                        else if (result.equals("ERROR"))
                        {
                            statusLabel.setText("Eroare de conexiune la internet.");
                            statusLabel.setForeground(Color.RED);
                        }
                        else
                        {
                            String[] parts = result.split("-");
                            String code = parts[0];
                            boolean isWhite = Boolean.parseBoolean(parts[1]);

                            statusLabel.setText("Codul tău: " + code + " | Aștept player...");
                            statusLabel.setForeground(new Color(50, 205, 50));

                            if(hostPollingTimer != null) hostPollingTimer.stop();
                            hostPollingTimer = new Timer(1500, event -> {
                                if (NetworkManager.checkGameStarted(code))
                                {
                                    hostPollingTimer.stop();
                                    windowController.getGameBoard().startOnlineGame(code, isWhite);
                                    windowController.switchScreen("GAME");
                                }
                            });
                            hostPollingTimer.start();
                        }
                    }
                    catch (Exception ex)
                    {
                        statusLabel.setText("Eroare de sistem: " + ex.toString());
                        statusLabel.setForeground(Color.RED);
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();
        });

        JButton joinBtn = createBtn("Join Game", new Color(50, 205, 50));
        joinBtn.addActionListener(e -> {
            String code = JOptionPane.showInputDialog(this, "Introdu codul de 4 cifre:");
            if (code != null && code.length() == 4)
            {
                statusLabel.setText("Se conectează...");
                statusLabel.setForeground(new Color(255, 165, 0));

                SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
                {
                    @Override
                    protected String doInBackground()
                    {
                        return NetworkManager.joinGame(code);
                    }
                    @Override
                    protected void done()
                    {
                        try
                        {
                            String joinResult = get();
                            if (joinResult.equals("FIREBASE_ERROR"))
                            {
                                statusLabel.setText("Eroare Bază de date: Verifică regulile (Test Mode)");
                                statusLabel.setForeground(Color.RED);
                            }
                            else if (joinResult.equals("NOT_FOUND") || joinResult.equals("ERROR"))
                            {
                                statusLabel.setText("Cod invalid sau meci început.");
                                statusLabel.setForeground(Color.RED);
                            }
                            else
                            {
                                boolean amIWhite = joinResult.equals("true");
                                windowController.getGameBoard().startOnlineGame(code, amIWhite);
                                windowController.switchScreen("GAME");
                            }
                        }
                        catch (Exception ex)
                        {
                            statusLabel.setText("Eroare de sistem: " + ex.toString());
                            statusLabel.setForeground(Color.RED);
                            ex.printStackTrace();
                        }
                    }
                };
                worker.execute();
            }
        });

        JButton backBtn = createBtn("Back to Menu", new Color(60, 60, 60));
        backBtn.addActionListener(e -> {
            if(hostPollingTimer != null) hostPollingTimer.stop();
            windowController.switchScreen("MENU");
        });

        contentPanel.add(hostBtn);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(joinBtn);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        contentPanel.add(backBtn);

        lobbyBox.add(headerPanel, BorderLayout.NORTH);
        lobbyBox.add(contentPanel, BorderLayout.CENTER);
        add(lobbyBox);
    }

    private JButton createBtn(String text, Color bg)
    {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(220, 45));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth(), height = getHeight();
        g2.setPaint(new GradientPaint(0, 0, new Color(15, 17, 20), 0, height, new Color(25, 30, 27)));
        g2.fillRect(0, 0, width, height);
        g2.setColor(new Color(255, 255, 255, 6));
        int tileSize = 140;
        for (int r = 0; r <= height / tileSize; r++)
        {
            for (int c = 0; c <= width / tileSize; c++)
            {
                if ((r + c) % 2 == 0) g2.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
            }
        }
    }
}