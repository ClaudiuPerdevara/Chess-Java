package com.chess.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

public class SettingsPanel extends JPanel
{
    private ChessWindow windowController;

    public SettingsPanel(ChessWindow window)
    {
        this.windowController = window;
        setLayout(new GridBagLayout());

        JPanel settingsBox = new JPanel(new BorderLayout())
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(35, 38, 42));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
        };
        settingsBox.setPreferredSize(new Dimension(650, 400));
        settingsBox.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 15, 30));

        JLabel titleLabel = new JLabel("SETTINGS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);

        JButton closeButon = new JButton("X");
        closeButon.setFont(new Font("Segoe UI", Font.BOLD, 22));
        closeButon.setForeground(new Color(150, 150, 150));
        closeButon.setContentAreaFilled(false);
        closeButon.setBorderPainted(false);
        closeButon.setFocusPainted(false);
        closeButon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        closeButon.addActionListener(e -> windowController.switchScreen("MENU"));

        closeButon.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseEntered(java.awt.event.MouseEvent evt)
            {
                closeButon.setForeground(new Color(255, 80, 80));
            }
            public void mouseExited(java.awt.event.MouseEvent evt)
            {
                closeButon.setForeground(new Color(150, 150, 150));
            }
        });

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(closeButon, BorderLayout.EAST);

        JPanel separator = new JPanel();
        separator.setBackground(new Color(60, 60, 60));
        separator.setPreferredSize(new Dimension(590, 2));
        headerPanel.add(separator, BorderLayout.SOUTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(50, 40, 50, 40));

        JLabel volLabel = new JLabel("Master Volume");
        volLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        volLabel.setForeground(new Color(220, 220, 220));
        contentPanel.add(volLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JSlider volSlider = new JSlider(0, 100, 50);
        volSlider.setOpaque(false);
        volSlider.setUI(new ThickModernSliderUI(volSlider));
        volSlider.setCursor(new Cursor(Cursor.HAND_CURSOR));

        volSlider.addChangeListener(e -> {
            int currentVol = volSlider.getValue();
            AudioManager.setVolume(currentVol);
        });

        contentPanel.add(volSlider);

        contentPanel.add(Box.createVerticalGlue());

        settingsBox.add(headerPanel, BorderLayout.NORTH);
        settingsBox.add(contentPanel, BorderLayout.CENTER);

        add(settingsBox);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int width = getWidth();
        int height = getHeight();
        Color colorTop = new Color(15, 17, 20);
        Color colorBottom = new Color(25, 30, 27);
        GradientPaint baseGradient = new GradientPaint(0, 0, colorTop, 0, height, colorBottom);
        g2.setPaint(baseGradient);
        g2.fillRect(0, 0, width, height);

        g2.setColor(new Color(255, 255, 255, 6));
        int tileSize = 140;
        for (int row = 0; row <= height / tileSize; row++)
        {
            for (int col = 0; col <= width / tileSize; col++)
            {
                if ((row + col) % 2 == 0) g2.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
            }
        }
    }

    private static class ThickModernSliderUI extends BasicSliderUI
    {
        public ThickModernSliderUI(JSlider b)
        {
            super(b);
        }

        @Override
        public void paintTrack(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle trackBounds = trackRect;
            int trackThickness = 16;
            int cy = (trackBounds.height / 2) - (trackThickness / 2);

            g2d.setColor(new Color(60, 60, 65));
            g2d.fillRoundRect(trackBounds.x, trackBounds.y + cy, trackBounds.width, trackThickness, trackThickness, trackThickness);

            int fillRight = thumbRect.x + (thumbRect.width / 2);
            g2d.setColor(new Color(50, 205, 50));
            g2d.fillRoundRect(trackBounds.x, trackBounds.y + cy, fillRight - trackBounds.x, trackThickness, trackThickness, trackThickness);
        }

        @Override
        public void paintThumb(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = 28;
            int x = thumbRect.x;
            int y = thumbRect.y + (thumbRect.height / 2) - (size / 2);

            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillOval(x + 2, y + 2, size, size);

            g2d.setColor(Color.WHITE);
            g2d.fillOval(x, y, size, size);
        }
    }
}