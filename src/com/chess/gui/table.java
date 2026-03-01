package com.chess.gui;

import javax.imageio.ImageIO;
import java.io.File;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class table
{
    private JFrame gameFrame;
    private String[] pozitii = new String[64];
    private int sourceTile = -1;
    private boolean wKingMoved = false, bKingMoved = false;
    private boolean wRookLMoved = false, wRookRMoved = false;
    private boolean bRookLMoved = false, bRookRMoved = false;
    private int enPassantTarget = -1;
    private JLabel turnLabel;
    private boolean isWhiteTurn = true;
    private int dificultateSelectata = 0;
    private java.util.List<Integer> validMoves = new java.util.ArrayList<>();
    private java.util.Map<String, Image> imageCache = new java.util.HashMap<>();
    private java.util.List<gameState> istoricJoc = new java.util.ArrayList<>();
    private int lastMoveFrom = -1, lastMoveTo = -1;
    private BoardPanel boardPanel;

    private JLabel timerAlbLabel;
    private JPanel whiteCapturedPanel;
    private JPanel blackCapturedPanel;
    private EvalBar evalBar;

    private boolean isMultiplayer = false;
    private boolean amIWhite = true;
    private String gameCode = "";
    private int networkMoveCount = 0;
    private Timer networkTimer;

    private JPanel boardWithCoords;
    private JPanel bottomLettersPanel;
    private JPanel leftNumbersPanel;

    private javax.swing.JTextArea historyTextArea;
    private java.util.List<String> listaMutari = new java.util.ArrayList<>();

    private Timer playerTimer;
    private int playerTimeRemaining = 600;

    private final int[] knightEval = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    private final int[] pawnEvalBlack = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10,-20,-20, 10, 10,  5,
            5, -5,-10,  0,  0,-10, -5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5,  5, 10, 25, 25, 10,  5,  5,
            10, 10, 20, 30, 30, 20, 10, 10,
            50, 50, 50, 50, 50, 50, 50, 50,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    private int getPieceValue(String piece)
    {
        if(piece.isEmpty()) return 0;
        char type = piece.charAt(piece.length()-1);
        switch(type)
        {
            case 'P': return 100;
            case 'N': return 320;
            case 'B': return 330;
            case 'R': return 500;
            case 'Q': return 900;
            case 'K': return 20000;
            default: return 0;
        }
    }

    public int evaluateBoard()
    {
        int score = 0;
        for (int i = 0; i < 64; i++)
        {
            if (!pozitii[i].isEmpty())
            {
                int materialValue = getPieceValue(pozitii[i]);
                boolean isWhite = pozitii[i].startsWith("white");
                char type = pozitii[i].charAt(pozitii[i].length() - 1);

                int posBonus = 0;
                if (type == 'N')
                {
                    posBonus = knightEval[i];
                }
                else if (type == 'P')
                {
                    posBonus = isWhite ? pawnEvalBlack[63 - i] : pawnEvalBlack[i];
                }
                else if (type != 'R' && type != 'Q' && type != 'K')
                {
                    int row = i / 8; int col = i % 8;
                    int distFromCenter = Math.abs(row - 3) + Math.abs(col - 3);
                    posBonus = (6 - distFromCenter) * 3;
                }

                int finalValue = materialValue + posBonus;
                score += (isWhite ? -finalValue : finalValue);
            }
        }
        return score;
    }

    private int minimax(int depth, boolean isMaximizing, int alpha, int beta)
    {
        if(depth == 0) return evaluateBoard();

        java.util.List<Move> moves = getAllLegalMoves(!isMaximizing);

        if(moves.isEmpty())
        {
            if (isKingInCheck(!isMaximizing))
            {
                return isMaximizing ? -1000000 - depth : 1000000 + depth;
            }
            else
            {
                return 0;
            }
        }

        if(isMaximizing)
        {
            int maxEval = -2000000;
            for(Move move : moves)
            {
                String tDest = pozitii[move.to];
                String tSource = pozitii[move.from];

                pozitii[move.to] = tSource;
                pozitii[move.from] = "";

                int eval = minimax(depth - 1, false, alpha, beta);

                pozitii[move.from] = tSource;
                pozitii[move.to] = tDest;

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if(beta <= alpha) break;
            }
            return maxEval;
        }
        else
        {
            int minEval = 2000000;
            for (Move move : moves)
            {
                String tDest = pozitii[move.to];
                String tSource = pozitii[move.from];

                pozitii[move.to] = tSource;
                pozitii[move.from] = "";

                int eval = minimax(depth - 1, true, alpha, beta);

                pozitii[move.from] = tSource;
                pozitii[move.to] = tDest;

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if(beta <= alpha) break;
            }
            return minEval;
        }
    }

    private void updateTurnLabel()
    {
        if (isWhiteTurn)
        {
            turnLabel.setText("WHITE TO MOVE");
            turnLabel.setForeground(Color.WHITE);
        }
        else
        {
            turnLabel.setText("BLACK TO MOVE");
            turnLabel.setForeground(Color.GRAY);
        }
    }

    private void calculateValidMoves(int source)
    {
        validMoves.clear();
        boolean isWhite = pozitii[source].startsWith("white");

        for(int i = 0; i < 64; i++)
        {
            if(isValidMove(source, i))
            {
                String tempDest = pozitii[i];
                String tempSource = pozitii[source];

                pozitii[i] = tempSource;
                pozitii[source] = "";

                if(!isKingInCheck(isWhite))
                {
                    validMoves.add(i);
                }

                pozitii[source] = tempSource;
                pozitii[i] = tempDest;
            }
        }
    }

    public void startOnlineGame(String code, boolean iAmWhite)
    {
        this.isMultiplayer = true;
        this.gameCode = code;
        this.amIWhite = iAmWhite;
        this.networkMoveCount = 0;

        refreshBoardOrientation();
        resetBoard();
        startNetworkPoller();
    }

    public void startSinglePlayerGame()
    {
        if(networkTimer != null) networkTimer.stop();
        if (isMultiplayer && gameCode != null && !gameCode.isEmpty())
        {
            NetworkManager.sendGameAbandoned(gameCode);
        }
        this.isMultiplayer = false;
        this.gameCode = "";
        this.amIWhite = true;

        resetBoard();
        refreshBoardOrientation();
    }

    private void startNetworkPoller()
    {
        if (networkTimer != null) networkTimer.stop();
        networkTimer = new Timer(1000, e -> {
            if (!isMultiplayer || gameCode.isEmpty()) return;

            if (isWhiteTurn == amIWhite) return;

            SwingWorker<String[], Void> worker = new SwingWorker<String[], Void>()
            {
                @Override
                protected String[] doInBackground()
                {
                    return NetworkManager.getGameState(gameCode);
                }
                @Override
                protected void done()
                {
                    if (!isMultiplayer) return;
                    try
                    {
                        String[] state = get();
                        if (state != null)
                        {
                            String status = state[0];
                            String lastMove = state[1];
                            int remoteMoveCount = Integer.parseInt(state[2]);

                            if (status.equals("abandoned"))
                            {
                                isMultiplayer = false;
                                networkTimer.stop();
                                if(playerTimer != null) playerTimer.stop();

                                showCustomMessage("VICTORIE", "Adversarul a părăsit meciul!");

                                startSinglePlayerGame();

                                Container parent = getGamePanel().getParent();
                                while(parent != null && !(parent.getLayout() instanceof CardLayout))
                                {
                                    parent = parent.getParent();
                                }
                                if(parent != null)
                                {
                                    ((CardLayout)parent.getLayout()).show(parent, "MENU");
                                }
                                return;
                            }

                            if (status.equals("playing") && remoteMoveCount > networkMoveCount)
                            {
                                networkMoveCount = remoteMoveCount;
                                String[] parts = lastMove.split("-");
                                int from = Integer.parseInt(parts[0]);
                                int to = Integer.parseInt(parts[1]);
                                executeMove(from, to, false);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();
        });
        networkTimer.start();
    }
    private void resetBoard()
    {
        initPozitii();
        isWhiteTurn = true;
        sourceTile = -1;
        lastMoveFrom = -1;
        lastMoveTo = -1;
        istoricJoc.clear();
        listaMutari.clear();

        if (whiteCapturedPanel != null) whiteCapturedPanel.removeAll();
        if (blackCapturedPanel != null) blackCapturedPanel.removeAll();
        if (historyTextArea != null) historyTextArea.setText("");


        updateHistory();
        updateTurnLabel();
        boardPanel.drawBoard();
        playerTimeRemaining = 600;
        if(playerTimer != null) playerTimer.restart();
    }

    private void checkForCheckmate()
    {
        boolean hasSafeMoves = false;
        int aparitii=1;
        for (gameState state : istoricJoc)
        {
            if (java.util.Arrays.equals(state.clonaPozitii, this.pozitii))
            {
                aparitii++;
            }
        }

        if (aparitii >= 3)
        {
            if(playerTimer != null) playerTimer.stop();
            showCustomMessage("REMIZĂ", "Remiză prin repetiție.");
            return;
        }

        for (int i = 0; i < 64; i++)
        {
            if (!pozitii[i].isEmpty())
            {
                boolean isPieceWhite = pozitii[i].startsWith("white");
                if (isPieceWhite == isWhiteTurn)
                {
                    calculateValidMoves(i);
                    if (!validMoves.isEmpty())
                    {
                        hasSafeMoves = true;
                        break;
                    }
                }
            }
        }
        validMoves.clear();

        if (!hasSafeMoves)
        {
            if(playerTimer != null) playerTimer.stop();

            if (isKingInCheck(isWhiteTurn))
            {
                String winner = isWhiteTurn ? "Negrul" : "Albul";
                JOptionPane.showMessageDialog(gameFrame, "ȘAH MAT! " + winner + " a câștigat!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog(gameFrame, "REMIZĂ! (Piesele sunt blocate)", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private boolean isKingInCheck(boolean isWhite)
    {
        int kingPos = -1;
        String kingString = isWhite ? "white/wK" : "black/bK";

        for(int i = 0; i < 64; i++)
        {
            if(pozitii[i].equals(kingString))
            {
                kingPos = i;
                break;
            }
        }

        if(kingPos == -1) return false;

        for(int i = 0; i < 64; i++)
        {
            if(!pozitii[i].isEmpty())
            {
                boolean isEnemy = isWhite ? pozitii[i].startsWith("black") : pozitii[i].startsWith("white");
                if(isEnemy)
                {
                    if(isValidMove(i, kingPos))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private class TilePanel extends JPanel
    {
        private int tileId;

        TilePanel(int tileId)
        {
            this.setLayout(new GridBagLayout());
            this.tileId = tileId;
            assignTileColor(tileId);
            assignTilePiece(tileId);

            this.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e)
                {
                    if(SwingUtilities.isLeftMouseButton(e))
                    {

                        if (isMultiplayer)
                        {
                            if (isWhiteTurn != amIWhite) return;
                            if (sourceTile == -1 && !pozitii[tileId].isEmpty())
                            {
                                boolean isPieceWhite = pozitii[tileId].startsWith("white");
                                if (isPieceWhite != amIWhite) return;
                            }
                        }

                        if(sourceTile == -1)
                        {
                            if(!pozitii[tileId].isEmpty())
                            {
                                boolean isSelectedWhite = pozitii[tileId].startsWith("white");
                                if(isSelectedWhite == isWhiteTurn)
                                {
                                    sourceTile = tileId;
                                    calculateValidMoves(sourceTile);
                                    boardPanel.drawBoard();
                                }
                            }
                        }
                        else
                        {
                            if(validMoves.contains(tileId))
                            {
                                executeMove(sourceTile, tileId, true);
                            }
                            else
                            {
                                System.out.println("Mutare invalida");
                            }
                        }
                    }
                    else if(SwingUtilities.isRightMouseButton(e))
                    {
                        sourceTile = -1;
                        validMoves.clear();
                        boardPanel.drawBoard();
                    }
                }
            });
        }

        private void assignTileColor(int i)
        {
            int row = tileId / 8;
            int col = tileId % 8;
            boolean isLightSquare = (row + col) % 2 == 0;

            Color lightColor = new Color(225, 225, 225);
            Color darkColor = new Color(136, 136, 136);
            Color selectedColor = new Color(175, 175, 175);
            Color lastMoveColor = new Color(190, 190, 190, 200);

            if (tileId == sourceTile)
            {
                setBackground(selectedColor);
            }
            else if (tileId == lastMoveFrom || tileId == lastMoveTo)
            {
                setBackground(lastMoveColor);
            }
            else if (isLightSquare)
            {
                setBackground(lightColor);
            }
            else
            {
                setBackground(darkColor);
            }
        }

        private void assignTilePiece(int i)
        {
            this.removeAll();
            String piecePath = pozitii[tileId];
            if(!piecePath.isEmpty())
            {
                try
                {
                    Image scaledImage=imageCache.get(piecePath);
                    if(scaledImage==null)
                    {
                        BufferedImage image = ImageIO.read(new File("art/" + piecePath + ".png"));
                        scaledImage = image.getScaledInstance(90, 90, Image.SCALE_SMOOTH);
                        imageCache.put(piecePath,scaledImage);
                    }
                    add(new JLabel(new ImageIcon(scaledImage)));
                }
                catch (IOException e)
                {
                    System.out.println("Nu am gasit piesa la: art/" + piecePath + ".png");
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            String currentPiece = pozitii[tileId];
            if (currentPiece.endsWith("K"))
            {
                boolean isWhiteKing = currentPiece.startsWith("white");
                if (isKingInCheck(isWhiteKing))
                {
                    g2.setColor(new Color(255, 0, 0, 150));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }

            if(validMoves.contains(tileId))
            {
                g2.setColor(Color.darkGray);
                if (pozitii[tileId].isEmpty())
                {
                    int radius = 15;
                    g2.fillOval(getWidth() / 2 - radius, getHeight() / 2 - radius, radius * 2, radius * 2);
                }
                else
                {
                    g2.setStroke(new BasicStroke(6));
                    g2.drawOval(5, 5, getWidth() - 10, getHeight() - 10);
                }
            }
        }
    }

    private void initPozitii()
    {
        int i;
        for(i = 0; i < 64; i++) pozitii[i] = "";

        for(i = 8; i <= 15; i++) pozitii[i] = "black/bP";
        for(i = 48; i <= 55; i++) pozitii[i] = "white/wP";
        pozitii[0] = "black/bR"; pozitii[7] = "black/bR";
        pozitii[56] = "white/wR"; pozitii[63] = "white/wR";

        pozitii[1] = "black/bN"; pozitii[6] = "black/bN";
        pozitii[57] = "white/wN"; pozitii[62] = "white/wN";

        pozitii[2] = "black/bB"; pozitii[5] = "black/bB";
        pozitii[58] = "white/wB"; pozitii[61] = "white/wB";

        pozitii[3] = "black/bQ";
        pozitii[59] = "white/wQ";

        pozitii[4] = "black/bK";
        pozitii[60] = "white/wK";
    }

    private boolean isValidMove(int source, int dest)
    {
        String piece = pozitii[source];
        String targetPiece = pozitii[dest];

        if(!targetPiece.isEmpty())
        {
            boolean isSourceWhite = piece.startsWith("white");
            boolean isDestWhite = targetPiece.startsWith("white");
            if (isSourceWhite == isDestWhite)
            {
                return false;
            }
        }

        int startRow = source / 8, startCol = source % 8;
        int destRow = dest / 8, destCol = dest % 8;
        int rowDif = Math.abs(destRow - startRow);
        int colDif = Math.abs(destCol - startCol);

        if(piece.endsWith("N"))
        {
            return (rowDif == 2 && colDif == 1) || (rowDif == 1 && colDif == 2);
        }
        if(piece.endsWith("K"))
        {
            if(rowDif <= 1 && colDif <= 1) return true;
            if(rowDif == 0 && colDif == 2)
            {
                boolean isWhite = piece.startsWith("white");
                if(isWhite && wKingMoved) return false;
                if(!isWhite && bKingMoved) return false;

                if(dest == source + 2)
                {
                    if (isWhite && (wRookRMoved || !pozitii[61].isEmpty() || !pozitii[62].isEmpty())) return false;
                    if (!isWhite && (bRookRMoved || !pozitii[5].isEmpty() || !pozitii[6].isEmpty())) return false;
                    return true;
                }
                if (dest == source - 2)
                {
                    if (isWhite && (wRookLMoved || !pozitii[57].isEmpty() || !pozitii[58].isEmpty() || !pozitii[59].isEmpty())) return false;
                    if (!isWhite && (bRookLMoved || !pozitii[1].isEmpty() || !pozitii[2].isEmpty() || !pozitii[3].isEmpty())) return false;
                    return true;
                }
            }
            return false;
        }
        if(piece.endsWith("R"))
        {
            if(!((rowDif == 0 && colDif > 0) || (rowDif > 0 && colDif == 0))) return false;
            int rowStep = 0; if(destRow > startRow) rowStep = 1; else if(destRow < startRow) rowStep = -1;
            int colStep = 0; if(destCol > startCol) colStep = 1; else if(destCol < startCol) colStep = -1;
            int r = startRow + rowStep;
            int c = startCol + colStep;
            while(r != destRow || c != destCol)
            {
                if(!pozitii[r * 8 + c].isEmpty()) return false;
                r += rowStep; c += colStep;
            }
            return true;
        }
        if(piece.endsWith("B"))
        {
            if(!(rowDif == colDif && rowDif > 0)) return false;
            int rowStep = (destRow > startRow) ? 1 : -1;
            int colStep = (destCol > startCol) ? 1 : -1;
            int r = startRow + rowStep;
            int c = startCol + colStep;
            while(r != destRow || c != destCol)
            {
                if(!pozitii[8 * r + c].isEmpty()) return false;
                r += rowStep; c += colStep;
            }
            return true;
        }
        if(piece.endsWith("Q"))
        {
            if(!(((rowDif == 0 && colDif > 0) || (rowDif > 0 && colDif == 0)) || ((rowDif == colDif && rowDif > 0)))) return false;
            int rowStep = 0; if(destRow > startRow) rowStep = 1; else if(destRow < startRow) rowStep = -1;
            int colStep = 0; if(destCol > startCol) colStep = 1; else if(destCol < startCol) colStep = -1;
            int r = startRow + rowStep;
            int c = startCol + colStep;
            while(r != destRow || c != destCol)
            {
                if (!pozitii[r * 8 + c].isEmpty()) return false;
                r += rowStep; c += colStep;
            }
            return true;
        }
        if(piece.endsWith("P"))
        {
            boolean isWhite = piece.startsWith("white");
            int direction = isWhite ? -1 : 1;
            int startRowDoubleJump = isWhite ? 6 : 1;
            if(colDif == 0 && (destRow - startRow == direction))
            {
                return targetPiece.isEmpty();
            }
            if(colDif == 0 && startRowDoubleJump == startRow && (destRow - startRow == 2 * direction))
            {
                int middleRow = startRow + direction;
                return targetPiece.isEmpty() && pozitii[middleRow * 8 + startCol].isEmpty();
            }
            if(colDif == 1 && (destRow - startRow == direction))
            {
                if (!targetPiece.isEmpty()) return true;
                if (dest == enPassantTarget)
                {
                    if(isWhite && destRow==2) return true;
                    if(!isWhite && destRow==5) return true;
                }
            }
            return false;
        }
        return true;
    }

    public table()
    {
        this.gameFrame = new JFrame("Chess");

        initPozitii();

        JPanel mainContainer = new JPanel(new BorderLayout(30, 0))
        {
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
        };
        mainContainer.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(330, 0));
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        Dimension fixedBoxSize = new Dimension(240, 140);

        JLabel capTitle1 = new JLabel("Black pieces captured");
        capTitle1.setForeground(new Color(150, 150, 150));
        capTitle1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        capTitle1.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(capTitle1);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        whiteCapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        whiteCapturedPanel.setBackground(new Color(50, 55, 60));
        whiteCapturedPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));
        whiteCapturedPanel.setPreferredSize(fixedBoxSize);
        whiteCapturedPanel.setMinimumSize(fixedBoxSize);
        whiteCapturedPanel.setMaximumSize(fixedBoxSize);
        whiteCapturedPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(whiteCapturedPanel);

        leftPanel.add(Box.createRigidArea(new Dimension(0, 50)));

        JLabel capTitle2 = new JLabel("Your lost pieces");
        capTitle2.setForeground(new Color(150, 150, 150));
        capTitle2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        capTitle2.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(capTitle2);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        blackCapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        blackCapturedPanel.setBackground(new Color(50, 55, 60));
        blackCapturedPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));
        blackCapturedPanel.setPreferredSize(fixedBoxSize);
        blackCapturedPanel.setMinimumSize(fixedBoxSize);
        blackCapturedPanel.setMaximumSize(fixedBoxSize);
        blackCapturedPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(blackCapturedPanel);

        leftPanel.add(Box.createVerticalGlue());

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel boardWithBarPanel = new JPanel(new BorderLayout(15, 0));
        boardWithBarPanel.setOpaque(false);

        this.evalBar = new EvalBar();
        boardWithBarPanel.add(this.evalBar, BorderLayout.WEST);

        boardWithCoords = new JPanel(new BorderLayout());
        boardWithCoords.setOpaque(false);
        boardWithCoords.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 4, true));

        this.boardPanel = new BoardPanel(this);
        this.boardPanel.setPreferredSize(new Dimension(720, 720));

        bottomLettersPanel = new JPanel(new GridLayout(1, 8));
        bottomLettersPanel.setOpaque(true);
        bottomLettersPanel.setBackground(new Color(30, 32, 35));
        bottomLettersPanel.setPreferredSize(new Dimension(720, 25));

        leftNumbersPanel = new JPanel(new GridLayout(8, 1));
        leftNumbersPanel.setOpaque(true);
        leftNumbersPanel.setBackground(new Color(30, 32, 35));
        leftNumbersPanel.setPreferredSize(new Dimension(25, 720));

        setupCoordinatesUI(true);

        boardWithCoords.add(this.boardPanel, BorderLayout.CENTER);
        boardWithCoords.add(bottomLettersPanel, BorderLayout.SOUTH);
        boardWithCoords.add(leftNumbersPanel, BorderLayout.WEST);

        boardWithBarPanel.add(boardWithCoords, BorderLayout.CENTER);
        centerWrapper.add(boardWithBarPanel);

        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        JLabel whitePlayerLabel = new JLabel("YOUR TIME");
        whitePlayerLabel.setForeground(new Color(180, 180, 180));
        whitePlayerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        whitePlayerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(whitePlayerLabel);

        timerAlbLabel = new JLabel("10:00");
        timerAlbLabel.setForeground(Color.WHITE);
        timerAlbLabel.setFont(new Font("Consolas", Font.BOLD, 54));
        timerAlbLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(timerAlbLabel);

        playerTimer = new Timer(1000, e -> {
            if (isWhiteTurn && playerTimeRemaining > 0)
            {
                playerTimeRemaining--;
                int m = playerTimeRemaining / 60;
                int s = playerTimeRemaining % 60;
                timerAlbLabel.setText(String.format("%02d:%02d", m, s));

                if (playerTimeRemaining == 0)
                {
                    playerTimer.stop();
                    JOptionPane.showMessageDialog(getGamePanel(), "Timpul a expirat! Ai pierdut meciul.", "Time Out", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        playerTimer.start();

        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        turnLabel = new JLabel("WHITE TO MOVE");
        turnLabel.setForeground(new Color(50, 205, 50));
        turnLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(turnLabel);

        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        historyTextArea = new javax.swing.JTextArea();
        historyTextArea.setEditable(false);
        historyTextArea.setBackground(new Color(50, 52, 55, 200));
        historyTextArea.setForeground(new Color(220, 220, 220));
        historyTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(historyTextArea);
        scrollPane.setPreferredSize(new Dimension(240, 180));
        scrollPane.setMaximumSize(new Dimension(240, 180));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        rightPanel.add(scrollPane);

        rightPanel.add(Box.createVerticalGlue());

        JLabel diffTitle = new JLabel("DIFFICULTY");
        diffTitle.setForeground(new Color(150, 150, 150));
        diffTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        diffTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(diffTitle);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(25);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(50, 205, 50));
        progressBar.setMaximumSize(new Dimension(200, 25));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(progressBar);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        String[] diffs = {"Stupid", "Decent", "Hard", "Impossible"};
        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < diffs.length; i++)
        {
            final int level = i;
            JRadioButton rb = new JRadioButton("  " + diffs[i]);
            rb.setForeground(Color.WHITE);
            rb.setOpaque(false);
            rb.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            rb.setAlignmentX(Component.CENTER_ALIGNMENT);
            rb.setFocusPainted(false);

            if(i == 0) rb.setSelected(true);

            rb.addActionListener(e -> {
                dificultateSelectata = level;
                int progressValue = (level + 1) * 25;
                progressBar.setValue(progressValue);

                switch (level)
                {
                    case 0: progressBar.setForeground(new Color(50, 205, 50)); break;
                    case 1: progressBar.setForeground(new Color(255, 165, 0)); break;
                    case 2: progressBar.setForeground(new Color(220, 20, 60)); break;
                    case 3: progressBar.setForeground(new Color(138, 43, 226)); break;
                }

                if(level == 3) progressBar.setString("GOOD LUCK!");
                else progressBar.setString(null);
            });

            group.add(rb);
            rightPanel.add(rb);
            rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton undoButton = new JButton("Undo Move");
        undoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        undoButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        undoButton.setBackground(new Color(70, 130, 180));
        undoButton.setForeground(Color.WHITE);
        undoButton.setFocusPainted(false);
        undoButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Dimension btnSize = new Dimension(180, 40);
        undoButton.setPreferredSize(btnSize);
        undoButton.setMaximumSize(btnSize);
        undoButton.addActionListener(e -> undoMove());
        rightPanel.add(undoButton);

        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        JButton menuBtn = new JButton("Main Menu");
        menuBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        menuBtn.setBackground(new Color(60, 60, 60));
        menuBtn.setForeground(Color.WHITE);
        menuBtn.setFocusPainted(false);
        menuBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        menuBtn.setPreferredSize(btnSize);
        menuBtn.setMaximumSize(btnSize);
        menuBtn.addActionListener(e -> {
            Container parent = mainContainer.getParent();
            while(parent != null && !(parent.getLayout() instanceof CardLayout))
            {
                parent = parent.getParent();
            }
            if(parent != null)
            {
                ((CardLayout)parent.getLayout()).show(parent, "MENU");
            }
        });
        rightPanel.add(menuBtn);

        mainContainer.add(leftPanel, BorderLayout.WEST);
        mainContainer.add(centerWrapper, BorderLayout.CENTER);
        mainContainer.add(rightPanel, BorderLayout.EAST);

        this.gameFrame.getContentPane().removeAll();
        this.gameFrame.getContentPane().add(mainContainer);
    }

    private class Move {
        int from, to;
        Move(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    private java.util.List<Move> getAllLegalMoves(boolean isWhite) {
        java.util.List<Move> moves = new java.util.ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (!pozitii[i].isEmpty() && pozitii[i].startsWith(isWhite ? "white" : "black")) {
                calculateValidMoves(i);
                for (int dest : validMoves) {
                    moves.add(new Move(i, dest));
                }
            }
        }
        validMoves.clear();
        return moves;
    }

    private void makeAIMove() {
        java.util.List<Move> allMoves = getAllLegalMoves(false);
        if(allMoves.isEmpty()) return;

        turnLabel.setText("PC IS THINKING...");
        turnLabel.setForeground(new Color(255, 165, 0)); // Portocaliu

        SwingWorker<Move, Void> worker = new SwingWorker<Move, Void>()
        {
            @Override
            protected Move doInBackground() throws Exception {
                Move bestMove = null;
                int bestValue = -2000000;
                int depth = 1;

                if(dificultateSelectata == 0)
                {
                    Thread.sleep(600);
                    int randomIndex = (int)(Math.random() * allMoves.size());
                    bestMove = allMoves.get(randomIndex);
                }
                else
                {
                    if(dificultateSelectata == 1) depth = 3;
                    if(dificultateSelectata == 2) depth = 4;
                    if(dificultateSelectata == 3) depth = 5;

                    for(Move move : allMoves)
                    {
                        String tDest = pozitii[move.to];
                        String tSource = pozitii[move.from];

                        pozitii[move.to] = tSource;
                        pozitii[move.from] = "";

                        int boardValue = minimax(depth - 1, false, -2000000, 2000000);

                        pozitii[move.from] = tSource;
                        pozitii[move.to] = tDest;

                        if(boardValue >= bestValue)
                        {
                            bestValue = boardValue;
                            bestMove = move;
                        }
                    }
                }
                return bestMove;
            }

            @Override
            protected void done() {
                try {
                    Move result = get();
                    if(result != null) {
                        executeMove(result.from, result.to,false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    // isLocal ne spune dacă ai dat TU click sau a venit mutarea de pe Net/PC
    private void executeMove(int from, int to, boolean isLocal) {
        String pieceMoved = pozitii[from];

        String mutareAnotata = getChessNotation(from, to, pozitii[from]);
        listaMutari.add(mutareAnotata);
        updateHistory();

        istoricJoc.add(new gameState(pozitii,isWhiteTurn,wKingMoved,bKingMoved,wRookLMoved,wRookRMoved,bRookLMoved,bRookRMoved,enPassantTarget));

        if (pieceMoved.endsWith("K") && Math.abs(to - from) == 2) {
            if (to == from + 2) {
                pozitii[to - 1] = pozitii[to + 1];
                pozitii[to + 1] = "";
            } else if (to == from - 2) {
                pozitii[to + 1] = pozitii[to - 2];
                pozitii[to - 2] = "";
            }
        }

        if (pieceMoved.endsWith("P") && to == enPassantTarget) {
            int direction = pieceMoved.startsWith("white") ? -1 : 1;
            pozitii[to - direction * 8] = "";
        }

        if (pieceMoved.equals("white/wK")) wKingMoved = true;
        if (pieceMoved.equals("black/bK")) bKingMoved = true;
        if (from == 56) wRookLMoved = true;
        if (from == 63) wRookRMoved = true;
        if (from == 0) bRookLMoved = true;
        if (from == 7) bRookRMoved = true;

        if (pieceMoved.endsWith("P") && Math.abs((to / 8) - (from / 8)) == 2) {
            int direction = pieceMoved.startsWith("white") ? -1 : 1;
            enPassantTarget = from + direction * 8;
        } else {
            enPassantTarget = -1;
        }
        boolean isCapture = !pozitii[to].isEmpty();

        pozitii[to] = pozitii[from];
        pozitii[from] = "";

        if (isCapture) AudioManager.playSound("capture.wav");
        else AudioManager.playSound("move.wav");

        if (pieceMoved.endsWith("P")) {
            int targetRow = to / 8;
            if (targetRow == 0 || targetRow == 7) {
                String color = pieceMoved.startsWith("white") ? "white/w" : "black/b";
                String newPiece = isMultiplayer ? "Q" : showCustomPromotionDialog();
                pozitii[to] = color + newPiece;
            }
        }

        lastMoveFrom = from;
        lastMoveTo = to;
        sourceTile = -1;
        isWhiteTurn = !isWhiteTurn;
        updateTurnLabel();
        validMoves.clear();
        checkForCheckmate();
        boardPanel.drawBoard();

        // ROUTARE DUPĂ MUTARE: Trimitem pe net, SAU lăsăm PC-ul să joace
        if (isLocal && isMultiplayer) {
            networkMoveCount++;
            NetworkManager.sendMove(gameCode, from, to, networkMoveCount);
        } else if (isLocal && !isMultiplayer && !isWhiteTurn) {
            makeAIMove(); // Offline, deci e rândul PC-ului
        }
    }

    private class gameState
    {
        String clonaPozitii[];
        boolean isWhiteTurn,wK,bK,wRL,wRR,bRL,bRR;
        int enPassant;

        gameState(String[] p ,boolean turn, boolean wk,boolean bk, boolean wrl,boolean wrr, boolean brl,boolean brr, int ep)
        {
            this.clonaPozitii=p.clone();
            this.isWhiteTurn=turn;
            this.wK=wk;
            this.bK=bk;
            this.wRL=wrl;
            this.wRR=wrr;
            this.bRL=brl;
            this.bRR=brr;
            this.enPassant=ep;
        }
    }

    private void undoMove()
    {

        if(isMultiplayer) return;
        if (!isWhiteTurn) return;

        if(istoricJoc.size()>=2)
        {
            istoricJoc.remove(istoricJoc.size()-1);
            gameState trecut=istoricJoc.remove(istoricJoc.size()-1);
            listaMutari.remove(listaMutari.size() - 1);
            listaMutari.remove(listaMutari.size() - 1);
            this.pozitii=trecut.clonaPozitii.clone();
            this.isWhiteTurn=trecut.isWhiteTurn;
            this.wKingMoved=trecut.wK;
            this.bKingMoved= trecut.bK;
            this.wRookLMoved=trecut.wRL;
            this.wRookRMoved=trecut.wRR;
            this.bRookLMoved=trecut.bRL;
            this.bRookRMoved=trecut.bRR;
            this.enPassantTarget=trecut.enPassant;

            sourceTile=-1;
            lastMoveTo=-1;
            lastMoveFrom=-1;
            validMoves.clear();

            updateHistory();
            updateTurnLabel();
            boardPanel.drawBoard();
        }
    }

    private boolean checkPat()
    {
        int n=istoricJoc.size();
        if(n>=8)
        {
            gameState inUrmaCu4=istoricJoc.get(n-4);
            gameState inUrmaCu8=istoricJoc.get(n-8);
            boolean ok1=java.util.Arrays.equals(inUrmaCu4.clonaPozitii, this.pozitii);
            boolean ok2=java.util.Arrays.equals(inUrmaCu8.clonaPozitii, this.pozitii);
            if(ok1 && ok2) return true;
        }
        return false;
    }

    public void updateCapturedPieces()
    {
        String[] fullSet = {"Q", "R", "R", "B", "B", "N", "N", "P", "P", "P", "P", "P", "P", "P", "P"};

        java.util.List<String> currentWhite=new java.util.ArrayList<>();
        java.util.List<String> currentBlack=new java.util.ArrayList<>();

        for (String p : pozitii)
        {
            if (!p.isEmpty() && !p.endsWith("K"))
            {
                if(p.startsWith("white"))
                    currentWhite.add(p.substring(7));
                else
                    currentBlack.add(p.substring(7));
            }
        }

        java.util.List<String> deadWhite=new java.util.ArrayList<>(java.util.Arrays.asList(fullSet));
        for(String p : currentWhite) deadWhite.remove(p);

        java.util.List<String> deadBlack = new java.util.ArrayList<>(java.util.Arrays.asList(fullSet));
        for(String p : currentBlack) deadBlack.remove(p);

        drawMiniatures(whiteCapturedPanel, deadBlack, "black/b");
        drawMiniatures(blackCapturedPanel, deadWhite, "white/w");
    }

    private void drawMiniatures(JPanel panel, java.util.List<String> pieces, String prefix)
    {
        if (panel == null) return;
        panel.removeAll();
        pieces.sort((p1, p2) -> Integer.compare(getPieceValue(prefix + p2), getPieceValue(prefix + p1)));

        for(String p : pieces)
        {
            String fullPath=prefix + p;
            if(imageCache.containsKey(fullPath))
            {
                Image img=imageCache.get(fullPath);
                Image miniImg=img.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                panel.add(new JLabel(new ImageIcon(miniImg)));
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private String getChessNotation(int from, int to,String piece)
    {
        String coloane="abcdefgh";
        int fromCol=from%8;
        int fromRow=8-(from/8);
        int toCol=to%8;
        int toRow=8-(to/8);

        String p="";
        if(!piece.isEmpty() && !piece.endsWith("P"))
        {
            p=piece.substring(piece.length()-1);
        }
        return p+coloane.charAt(fromCol)+fromRow+"-"+coloane.charAt(toCol)+toRow;
    }

    private void updateHistory()
    {
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<listaMutari.size();i++)
        {
            if (i % 2 == 0)
            {
                sb.append((i / 2 + 1)).append(". ").append(listaMutari.get(i)).append(" \t");
            }
            else
            {
                sb.append(listaMutari.get(i)).append("\n");
            }
        }
        if(historyTextArea!=null) historyTextArea.setText(sb.toString());
    }

    public EvalBar getEvalBar()
    {
        return this.evalBar;
    }

    public JPanel createTile(int id)
    {
        int mappedId = amIWhite ? id : (63 - id);
        return new TilePanel(mappedId);
    }

    public JPanel getGamePanel()
    {
        return this.gameFrame.getContentPane().getComponent(0) instanceof JPanel ? (JPanel) this.gameFrame.getContentPane().getComponent(0) : new JPanel();
    }

    private void showCustomMessage(String title, String message)
    {
        JDialog dialog=new JDialog(gameFrame,title,true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0,0,0,0));

        JPanel panel = new JPanel(new BorderLayout(10, 20))
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(35, 38, 42, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(100, 100, 100));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };

        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(new Color(255, 80, 80));
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLbl, BorderLayout.NORTH);

        JLabel msgLbl = new JLabel(message);
        msgLbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        msgLbl.setForeground(Color.WHITE);
        msgLbl.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(msgLbl, BorderLayout.CENTER);

        JButton okBtn = new JButton("OK");
        okBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        okBtn.setBackground(new Color(70, 130, 180));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFocusPainted(false);
        okBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okBtn.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(okBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(gameFrame.getContentPane());
        dialog.setVisible(true);
    }

    private String showCustomPromotionDialog()
    {
        JDialog dialog = new JDialog(gameFrame, "Promotion", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0,0,0,0));
        final String[] result = {"Q"};

        JPanel panel = new JPanel(new BorderLayout(10, 20))
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(35, 38, 42, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.setOpaque(false);

        JLabel titleLbl = new JLabel("Choose a piece:");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(new Color(50, 205, 50));
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLbl, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        btnPanel.setOpaque(false);

        String[] pieces = {"Queen (Q)", "Rook (R)", "Bishop (B)", "Knight (N)"};
        String[] codes = {"Q", "R", "B", "N"};

        for (int i = 0; i < pieces.length; i++)
        {
            JButton btn = new JButton(pieces[i]);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setBackground(new Color(60, 60, 60));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            final String code = codes[i];
            btn.addActionListener(e -> {
                result[0] = code;
                dialog.dispose();
            });
            btnPanel.add(btn);
        }

        panel.add(btnPanel, BorderLayout.CENTER);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(gameFrame.getContentPane());
        dialog.setVisible(true);

        return result[0];
    }

    private void refreshBoardOrientation()
    {
        if (boardWithCoords != null)
        {
            boardWithCoords.remove(boardPanel);
            boardPanel = new BoardPanel(this);
            boardPanel.setPreferredSize(new Dimension(720, 720));
            boardWithCoords.add(boardPanel, BorderLayout.CENTER);

            setupCoordinatesUI(amIWhite);

            boardWithCoords.revalidate();
            boardWithCoords.repaint();
        }
    }

    private void setupCoordinatesUI(boolean isWhite)
    {
        if(bottomLettersPanel == null || leftNumbersPanel == null) return;

        bottomLettersPanel.removeAll();
        String[] cols = isWhite ? new String[]{"A", "B", "C", "D", "E", "F", "G", "H"} : new String[]{"H", "G", "F", "E", "D", "C", "B", "A"};
        for(String c : cols)
        {
            JLabel lbl = new JLabel(c, SwingConstants.CENTER);
            lbl.setForeground(new Color(150, 150, 150));
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            bottomLettersPanel.add(lbl);
        }

        leftNumbersPanel.removeAll();
        String[] rows = isWhite ? new String[]{"8", "7", "6", "5", "4", "3", "2", "1"} : new String[]{"1", "2", "3", "4", "5", "6", "7", "8"};
        for(String r : rows)
        {
            JLabel lbl = new JLabel(r, SwingConstants.CENTER);
            lbl.setForeground(new Color(150, 150, 150));
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            leftNumbersPanel.add(lbl);
        }

        bottomLettersPanel.revalidate(); bottomLettersPanel.repaint();
        leftNumbersPanel.revalidate(); leftNumbersPanel.repaint();
    }
}