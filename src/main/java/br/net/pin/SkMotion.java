package br.net.pin;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SkMotion {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(() -> {
      try {
        JFrame.setDefaultLookAndFeelDecorated(true);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new Interface().show();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
