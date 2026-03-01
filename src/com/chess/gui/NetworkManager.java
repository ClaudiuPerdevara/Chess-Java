package com.chess.gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetworkManager
{
    private static final String DB_URL = "https://chessapp-89fb8-default-rtdb.europe-west1.firebasedatabase.app/games/";

    public static void sendGameAbandoned(String gameCode)
    {
        try
        {
            String jsonInputString = "{\"status\": \"abandoned\", \"lastMove\": \"none\", \"moveCount\": 0}";
            sendPutRequest(gameCode, jsonInputString);
        }
        catch (Exception e) {}
    }

    public static String hostGame()
    {
        try
        {
            int code = 1000 + (int)(Math.random() * 8999);
            String gameCode = String.valueOf(code);
            boolean hostIsWhite = Math.random() < 0.5;

            String jsonInputString = "{\"status\": \"waiting\", \"lastMove\": \"none\", \"moveCount\": 0, \"hostIsWhite\": " + hostIsWhite + "}";
            boolean success = sendPutRequest(gameCode, jsonInputString);

            if (!success) return "FIREBASE_ERROR";
            return gameCode + "-" + hostIsWhite;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public static String joinGame(String gameCode)
    {
        try
        {
            String response = sendGetRequest(gameCode);
            if (response == null) return "NOT_FOUND";
            if (response.equals("FIREBASE_ERROR")) return "FIREBASE_ERROR";

            if (response.contains("\"waiting\""))
            {
                boolean hostIsWhite = response.contains("\"hostIsWhite\":true") || response.contains("\"hostIsWhite\": true");

                String jsonInputString = "{\"status\": \"playing\", \"lastMove\": \"none\", \"moveCount\": 0, \"hostIsWhite\": " + hostIsWhite + "}";
                boolean success = sendPutRequest(gameCode, jsonInputString);

                if (!success) return "FIREBASE_ERROR";
                return hostIsWhite ? "false" : "true";
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "ERROR";
    }

    public static boolean checkGameStarted(String gameCode)
    {
        try
        {
            String response = sendGetRequest(gameCode);
            return response != null && response.contains("\"playing\"");
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static void sendMove(String gameCode, int from, int to, int moveCount)
    {
        try
        {
            String moveStr = from + "-" + to;
            String response = sendGetRequest(gameCode);
            boolean hostIsWhite = response != null && (response.contains("\"hostIsWhite\":true") || response.contains("\"hostIsWhite\": true"));

            String jsonInputString = "{\"status\": \"playing\", \"lastMove\": \"" + moveStr + "\", \"moveCount\": " + moveCount + ", \"hostIsWhite\": " + hostIsWhite + "}";
            sendPutRequest(gameCode, jsonInputString);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String[] getGameState(String gameCode)
    {
        try
        {
            String response = sendGetRequest(gameCode);
            if (response != null && !response.equals("null") && !response.equals("FIREBASE_ERROR"))
            {
                String status = extractValue(response, "status");
                String lastMove = extractValue(response, "lastMove");
                String moveCount = extractValue(response, "moveCount");
                return new String[]{status, lastMove, moveCount};
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static String extractValue(String json, String key)
    {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return "0";
        startIndex += searchKey.length();
        int endIndex;
        if (json.charAt(startIndex) == '"')
        {
            startIndex++;
            endIndex = json.indexOf("\"", startIndex);
        }
        else
        {
            endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = json.indexOf("}", startIndex);
        }
        return json.substring(startIndex, endIndex).trim();
    }

    private static boolean sendPutRequest(String path, String jsonInputString) throws Exception
    {
        URL url = new URL(DB_URL + path + ".json");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try(OutputStream os = con.getOutputStream())
        {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        int code = con.getResponseCode();
        con.disconnect();
        return code >= 200 && code < 300;
    }

    private static String sendGetRequest(String path) throws Exception
    {
        URL url = new URL(DB_URL + path + ".json");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestMethod("GET");

        int code = con.getResponseCode();
        if (code == 401 || code == 403) return "FIREBASE_ERROR";

        if (code == 200)
        {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)))
            {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) response.append(responseLine.trim());
                if (response.toString().equals("null")) return null;
                return response.toString();
            }
        }
        return null;
    }
}