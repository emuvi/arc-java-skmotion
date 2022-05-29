package br.net.pin.skmotion;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarculaLaf;

public class SkMotion {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(new FlatDarculaLaf());
        new Interface().show();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
