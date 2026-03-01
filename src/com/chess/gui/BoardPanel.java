package com.chess.gui;

import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel
{
    private table gameController;

    public BoardPanel(table gameController)
    {
        super(new GridLayout(8, 8));
        this.gameController = gameController;
        drawBoard();
    }

    public void drawBoard()
    {
        this.removeAll();
        for(int i = 0; i < 64; i++)
        {
            this.add(gameController.createTile(i));
        }

        gameController.updateCapturedPieces();

        if (gameController.getEvalBar() != null)
        {
            gameController.getEvalBar().updateScore(gameController.evaluateBoard());
        }

        this.revalidate();
        this.repaint();
    }
}