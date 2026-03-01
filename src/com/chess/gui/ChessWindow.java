package com.chess.gui;

import javax.swing.*;
import java.awt.*;

public class ChessWindow extends JFrame
{
    private CardLayout cardLayout;
    private JPanel mainDeck;
    private table gameBoard;

    public ChessWindow()
    {
        setTitle("Chess AI Master Pro");
        setSize(1500, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainDeck = new JPanel(cardLayout);

        MainMenuPanel mainMenu = new MainMenuPanel(this);
        gameBoard = new table();
        SettingsPanel settingsMenu = new SettingsPanel(this);
        LobbyPanel lobbyMenu = new LobbyPanel(this);

        mainDeck.add(mainMenu, "MENU");
        mainDeck.add(gameBoard.getGamePanel(), "GAME");
        mainDeck.add(settingsMenu, "SETTINGS");
        mainDeck.add(lobbyMenu, "LOBBY");

        add(mainDeck);
        cardLayout.show(mainDeck, "MENU");
    }

    public void switchScreen(String screenName)
    {
        cardLayout.show(mainDeck, screenName);

        if (screenName.equals("MENU"))
        {
            gameBoard.startSinglePlayerGame();
        }
    }

    public table getGameBoard()
    {
        return gameBoard;
    }
}