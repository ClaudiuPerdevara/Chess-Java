package com.chess.gui;

import javax.sound.sampled.*;
import java.io.File;

public class AudioManager
{
    private static int currentVolume = 50;

    public static void setVolume(int volume)
    {
        currentVolume = volume;
    }

    public static void playSound(String fileName)
    {
        if (currentVolume == 0) return;

        try
        {
            File soundFile = new File("art/" + fileName);
            if (soundFile.exists())
            {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
                {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float volumeInDb = 20f * (float) Math.log10(currentVolume / 100f);

                    if (volumeInDb < gainControl.getMinimum()) volumeInDb = gainControl.getMinimum();
                    gainControl.setValue(volumeInDb);
                }

                clip.start();

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP)
                    {
                        clip.close();
                    }
                });

            }
            else
            {
                System.out.println("Nu am gasit sunetul la: " + soundFile.getAbsolutePath());
            }
        }
        catch (Exception e)
        {
            System.out.println("Eroare redare audio: " + e.getMessage());
        }
    }
}