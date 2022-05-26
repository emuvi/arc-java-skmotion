package br.net.pin;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Interface {

  private final JFrame frame = new JFrame("SkMotion");

  private final JLabel labelMonitor = new JLabel("Monitor:");
  private final DefaultComboBoxModel<String> modelMonitor = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboMonitor = new JComboBox<>(modelMonitor);
  private final JLabel labelResolution = new JLabel("Resolution:");
  private final DefaultComboBoxModel<String> modelResolution = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboResolution = new JComboBox<String>(modelResolution);
  private final JLabel labelDestiny = new JLabel("Destiny:");
  private final JTextField textDestiny = new JTextField(60);
  private final JButton buttonAction = new JButton("Start");

  private volatile RecMotion recMotion = null;

  public Interface() {
    setupComponents();
    setupLayout();
  }

  private void setupComponents() {
    labelMonitor.setHorizontalAlignment(SwingConstants.RIGHT);
    labelResolution.setHorizontalAlignment(SwingConstants.RIGHT);
    labelDestiny.setHorizontalAlignment(SwingConstants.RIGHT);
    buttonAction.addActionListener((ev) -> {
      try {
        doStartOrStop();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage());
      }
    });
  }

  private void setupLayout() {
    frame.setLocationRelativeTo(null);
    frame.setResizable(false);
    var pane = frame.getContentPane();
    pane.setLayout(new GridBagLayout());
    var like = new GridBagConstraints();
    like.fill = GridBagConstraints.HORIZONTAL;
    like.ipadx = 5;
    like.ipady = 5;
    like.insets = new Insets(3, 3, 3, 3);
    like.gridx = 0;
    like.gridy = 0;
    like.weightx = 1.0;
    pane.add(labelMonitor, like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(comboMonitor, like);
    like.gridx = 0;
    like.gridy = 1;
    like.weightx = 1.0;
    pane.add(labelResolution, like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(comboResolution, like);
    like.gridx = 0;
    like.gridy = 2;
    like.weightx = 1.0;
    pane.add(labelDestiny, like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(textDestiny, like);
    like.gridx = 0;
    like.gridy = 3;
    like.weightx = 1.0;
    pane.add(Box.createHorizontalGlue(), like);
    like.gridx = 1;
    like.gridy = 3;
    like.weightx = 3.0;
    pane.add(buttonAction, like);
    frame.pack();
  }

  private void load() throws Exception {
    var setup = new Properties();
    var file = new File("skmotion.ini");
    if (file.exists()) {
      setup.load(new FileInputStream(file));
    }
    loadMonitors(setup);
    loadResolutions(setup);
    loadDestiny(setup);
  }

  private void loadMonitors(Properties setup) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (var display : ge.getScreenDevices()) {
      modelMonitor.addElement(display.getIDstring());
    }
    comboMonitor.setSelectedItem(setup.getProperty("monitor"));
  }

  private void loadResolutions(Properties setup) {
    modelResolution.addElement("640x480");
    modelResolution.addElement("800x600");
    modelResolution.addElement("960x720");
    modelResolution.addElement("1280x720");
    modelResolution.addElement("1600x900");
    modelResolution.addElement("1920x1080");
    comboResolution.setSelectedItem(setup.getProperty("resolution"));
  }

  private void loadDestiny(Properties setup) {
    textDestiny.setText(setup.getProperty("destiny"));
  }

  private void save() throws Exception {
    var file = new FileOutputStream("skmotion.ini");
    var setup = new Properties();
    setup.setProperty("monitor", String.valueOf(comboMonitor.getSelectedItem()));
    setup.setProperty("resolution", String.valueOf(comboResolution.getSelectedItem()));
    setup.setProperty("destiny", textDestiny.getText());
    setup.store(file, "SkMotion");
  }

  public void show() throws Exception {
    load();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(360, frame.getMinimumSize().height);
    frame.setVisible(true);
  }

  private void doStartOrStop() throws Exception {
    if (recMotion == null) {
      var monitor = String.valueOf(comboMonitor.getSelectedItem());
      var graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
      Rectangle area = null;
      for (var device : graphics.getScreenDevices()) {
        if (monitor == device.getIDstring()) {
          area = device.getDefaultConfiguration().getBounds();
          break;
        }
      }
      if (area == null) {
        throw new Exception("Monitor not found");
      }
      var resolution = String.valueOf(comboResolution.getSelectedItem());
      var parts = resolution.split("x");
      if (parts.length != 2) {
        throw new Exception("Resolution mal formed");
      }
      var width = Integer.parseInt(parts[0]);
      var height = Integer.parseInt(parts[1]);
      var size = new Dimension(width, height);
      recMotion = new RecMotion(area, size, new File(textDestiny.getText()));
      recMotion.start();
      buttonAction.setText("Stop");
      save();
    } else {
      recMotion.stop();
      buttonAction.setEnabled(false);
      new Thread("Waiting to stop") {
        @Override
        public void run() {
          try {
            var elapsed = recMotion.join();
            recMotion = null;
            SwingUtilities.invokeLater(() -> {
              buttonAction.setText("Start");
              buttonAction.setEnabled(true);
              JOptionPane.showMessageDialog(frame, "Recording finished in " + formatTime(elapsed));
            });
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }.start();
    }
  }

  private static String formatTime(long elapsed) {
    var sb = new StringBuilder();
    var seconds = elapsed / 1000;
    var minutes = seconds / 60;
    var hours = minutes / 60;
    if (hours > 0) {
      sb.append(hours).append("h ");
    }
    if (minutes > 0) {
      sb.append(minutes).append("m ");
    }
    sb.append(seconds).append("s");
    return sb.toString();
  }

}
