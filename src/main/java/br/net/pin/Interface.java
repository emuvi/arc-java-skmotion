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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Interface {

  private final ImageIcon iconBlue = new ImageIcon(getClass().getResource("blue.png"));
  private final ImageIcon iconGreen = new ImageIcon(getClass().getResource("green.png"));
  private final ImageIcon iconRed = new ImageIcon(getClass().getResource("red.png"));
  private final ImageIcon iconYellow = new ImageIcon(getClass().getResource("yellow.png"));

  private final JFrame frame = new JFrame("SkMotion");

  private final JLabel labelMonitor = new JLabel("Monitor:");
  private final DefaultComboBoxModel<String> modelMonitor = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboMonitor = new JComboBox<>(modelMonitor);
  private final JLabel labelResolution = new JLabel("Resolution:");
  private final DefaultComboBoxModel<String> modelResolution = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboResolution = new JComboBox<String>(modelResolution);
  private final JLabel labelDestiny = new JLabel("Destiny:");
  private final JTextField textDestiny = new JTextField(60);
  private final JLabel labelSensitivity = new JLabel("Sensitivity:");
  private final SpinnerNumberModel modelSensitivity = new SpinnerNumberModel(0.01f, 0.0f, 1.0f, 0.001f);
  private final JSpinner spinnerSensitivity = new JSpinner(modelSensitivity);
  private final JLabel labelStatus = new JLabel("Waiting...");
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
    labelSensitivity.setHorizontalAlignment(SwingConstants.RIGHT);
    labelStatus.setVerticalTextPosition(SwingConstants.CENTER);
    labelStatus.setIcon(iconBlue);
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
    pane.add(labelSensitivity, like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(spinnerSensitivity, like);
    like.gridx = 0;
    like.gridy = 4;
    like.weightx = 1.0;
    pane.add(Box.createHorizontalGlue(), like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(labelStatus, like);
    like.gridx = 0;
    like.gridy = 5;
    like.weightx = 1.0;
    pane.add(Box.createHorizontalGlue(), like);
    like.gridx = 1;
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
    loadSensitivity(setup);
    loadDestiny(setup);
  }

  private void loadMonitors(Properties setup) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (var display : ge.getScreenDevices()) {
      modelMonitor.addElement(display.getIDstring());
    }
    var monitor = setup.getProperty("monitor");
    comboMonitor.setSelectedItem(monitor != null ? monitor : modelMonitor.getElementAt(0));
  }

  private void loadResolutions(Properties setup) {
    modelResolution.addElement("640x480");
    modelResolution.addElement("800x600");
    modelResolution.addElement("960x720");
    modelResolution.addElement("1280x720");
    modelResolution.addElement("1600x900");
    modelResolution.addElement("1920x1080");
    var resolution = setup.getProperty("resolution");
    comboResolution.setSelectedItem(resolution != null ? resolution : modelResolution.getElementAt(0));
  }

  private void loadSensitivity(Properties setup) {
    var sensitivity = setup.getProperty("sensitivity");
    modelSensitivity.setValue(sensitivity != null ? Double.parseDouble(sensitivity) : 0.01);
  }

  private void loadDestiny(Properties setup) {
    var destiny = setup.getProperty("destiny");
    textDestiny.setText(destiny != null ? destiny : "recorder.mp4");
  }

  private void save() throws Exception {
    var file = new FileOutputStream("skmotion.ini");
    var setup = new Properties();
    setup.setProperty("monitor", String.valueOf(comboMonitor.getSelectedItem()));
    setup.setProperty("resolution", String.valueOf(comboResolution.getSelectedItem()));
    setup.setProperty("sensitivity", String.valueOf(modelSensitivity.getValue()));
    setup.setProperty("destiny", textDestiny.getText());
    setup.store(file, "SkMotion");
  }

  public void show() throws Exception {
    load();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(330, frame.getMinimumSize().height);
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
      var sensitivity = ((Double) modelSensitivity.getValue()).floatValue();
      recMotion = new RecMotion(area, size, new File(textDestiny.getText()), sensitivity);
      recMotion.start();
      buttonAction.setText("Stop");
      save();
    } else {
      recMotion.stop();
      buttonAction.setEnabled(false);
      buttonAction.setText("Closing");
      labelStatus.setIcon(iconYellow);
      labelStatus.setText("Saving...");
      new Thread("Waiting to stop") {
        @Override
        public void run() {
          try {
            var elapsed = recMotion.join();
            var saved = recMotion.getSavedFrames();
            var dropped = recMotion.getDroppedFrames();
            recMotion = null;
            SwingUtilities.invokeLater(() -> {
              labelStatus.setIcon(iconBlue);
              labelStatus.setText("Waiting...");
              buttonAction.setText("Start");
              buttonAction.setEnabled(true);
              var message = new StringBuilder("Recorded for: ");
              message.append(formatTime(elapsed));
              message.append("\n");
              message.append("Frames saved: ");
              message.append(saved);
              message.append("\n");
              message.append("Frames dropped: ");
              message.append(dropped);
              JOptionPane.showMessageDialog(frame, message.toString());
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
