/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package iessineu.penguinrunner;

import java.io.File;

import javax.swing.SwingUtilities;

/**
 *
 * @author loren
 */
public class PenguinRunner {

    public static void main(String[] args) {

        File carpeta = new File("saves"); //cream carpeta guardats
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        /*
         * SwingUtilities.invokeLater fa que la finestra es creï
         * correctament dins el fil d'execució de Swing.
         */
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}
