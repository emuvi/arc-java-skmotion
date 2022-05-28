package br.net.pin;

import java.awt.RenderingHints;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarculaLaf;

public class SkMotion {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(new FlatDarculaLaf());
        UIManager.getLookAndFeelDefaults().put(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        new Interface().show();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
