package com.chess.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainMenuPanel extends JPanel
{

    private ChessWindow windowController;
    private Image backgroundImage;

    public MainMenuPanel(ChessWindow window)
    {
        this.windowController=window;
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

        add(Box.createVerticalGlue());

        JLabel titleLabel=new JLabel("Chess");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 54));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);

        add(Box.createRigidArea(new Dimension(0,60)));

        JButton butonPlayPc=createMenuButton("Play VS PC");
        butonPlayPc.addActionListener(e->windowController.switchScreen("GAME"));
        add(butonPlayPc);

        add(Box.createRigidArea(new Dimension(0,20)));

        JButton butonPlayPlayer = createMenuButton("Play VS Player");
        butonPlayPlayer.addActionListener(e -> windowController.switchScreen("LOBBY"));
        add(butonPlayPlayer);

        add(Box.createRigidArea(new Dimension(0,20)));

        JButton butonSetari=createMenuButton("Settings");
        butonSetari.addActionListener(e -> windowController.switchScreen("SETTINGS"));
        add(butonSetari);
        add(Box.createRigidArea(new Dimension(0,20)));

        add(Box.createVerticalGlue());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        bottomPanel.setBackground(new Color(25, 25, 25));
        bottomPanel.setOpaque(false);
        bottomPanel.setMaximumSize(new Dimension(2000, 80));

        JButton butonGit=new JButton();
        try
        {
            Image gitImg= ImageIO.read(new File("art/github.png"));
            Image scaledGit=gitImg.getScaledInstance(40,40,Image.SCALE_SMOOTH);
            butonGit.setIcon(new ImageIcon(scaledGit));
        }
        catch (Exception e)
        {
            butonGit.setText("Git");
            butonGit.setBackground(Color.WHITE);
        }
        butonGit.setPreferredSize(new Dimension(50,50));
        butonGit.setContentAreaFilled(false);
        butonGit.setBorderPainted(false);
        butonGit.setFocusPainted(false);
        butonGit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        butonGit.setToolTipText("View project on GitHub");

        butonGit.addActionListener(e -> {
            try
            {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/ClaudiuPerdevara"));
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        });

        bottomPanel.add(butonGit);
        add(bottomPanel);

    }

    private JButton createMenuButton(String text)
    {
        JButton btn=new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBorder(new RoundedBorder(20));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 20));

        Color normalColor = new Color(50, 52, 55);
        Color hoverColor = new Color(80, 82, 85);

        btn.setBackground(new Color(50,52,55));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseEntered(java.awt.event.MouseEvent evt)
            {
                btn.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt)
            {
                btn.setBackground(normalColor);
                btn.setOpaque(true);
            }
        });


        Dimension size=new Dimension(300,60);
        btn.setPreferredSize(size);
        btn.setMaximumSize(size);
        return btn;
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

                if ((row + col) % 2 == 0)
                {
                    g2.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
                }
            }
        }

        java.awt.geom.Point2D center = new java.awt.geom.Point2D.Float(width / 2f, 120);
        float radius = 500f;
        float[] dist = {0.0f, 1.0f};

        Color[] colors = {new Color(50, 205, 50, 35), new Color(0, 0, 0, 0)};

        java.awt.RadialGradientPaint spotlight = new java.awt.RadialGradientPaint(center, radius, dist, colors);
        g2.setPaint(spotlight);
        g2.fillRect(0, 0, width, height);
    }
}
