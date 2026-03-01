package com.chess.gui;

import javax.swing.*;
import java.awt.*;


public class EvalBar extends JPanel
{
    private double whitePercentage=0.5;

    public EvalBar()
    {
        this.setPreferredSize(new Dimension(30,720));
        this.setMaximumSize(new Dimension(30,720));

        this.setBorder(BorderFactory.createLineBorder(new Color(60,60,60),2));
    }

    public void updateScore(int score)
    {
        int adjScore=-score;
        double prob=0.5*(adjScore/2000.0);
        if(prob>1.0) prob=1.0;
        if(prob<0.0) prob=0.0;
        this.whitePercentage=prob;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, 0, w, h);

        int whiteHeight = (int) (h * whitePercentage);
        g.setColor(new Color(230, 230, 230));
        g.fillRect(0, h - whiteHeight, w, whiteHeight);
    }
}