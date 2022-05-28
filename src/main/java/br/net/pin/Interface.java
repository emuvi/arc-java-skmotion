package br.net.pin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

  private final ImageIcon iconLogo = new ImageIcon(getClass().getResource("skmotion.png"));
  private final ImageIcon iconBlue = new ImageIcon(getClass().getResource("blue.png"));
  private final ImageIcon iconGreen = new ImageIcon(getClass().getResource("green.png"));
  private final ImageIcon iconRed = new ImageIcon(getClass().getResource("red.png"));
  private final ImageIcon iconYellow = new ImageIcon(getClass().getResource("yellow.png"));

  private final JFrame frame = new JFrame("SkMotion");

  private final JButton buttonMonitor = new JButton("Monitor:");
  private final DefaultComboBoxModel<String> modelMonitor = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboMonitor = new JComboBox<>(modelMonitor);
  private final JLabel labelResolution = new JLabel("Resolution:");
  private final DefaultComboBoxModel<String> modelResolution = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboResolution = new JComboBox<String>(modelResolution);
  private final JLabel labelSensitivity = new JLabel("Sensitivity:");
  private final SpinnerNumberModel modelSensitivity = new SpinnerNumberModel(0.01, 0.0, 1.0, 0.001);
  private final JSpinner spinnerSensitivity = new JSpinner(modelSensitivity);
  private final JLabel labelResilience = new JLabel("Resilience:");
  private final SpinnerNumberModel modelResilience = new SpinnerNumberModel(10, 0, 1_000_000, 1);
  private final JSpinner spinnerResilience = new JSpinner(modelResilience);
  private final JLabel labelDestiny = new JLabel("Destiny:");
  private final JTextField textDestiny = new JTextField();
  private final JLabel labelStatus = new JLabel("Waiting...");
  private final JButton buttonAction = new JButton("Start");

  private volatile RecMotion recMotion = null;

  public Interface() {
    setupComponents();
    setupLayout();
  }

  private void setupComponents() {
    frame.setIconImage(iconLogo.getImage());
    buttonMonitor.setHorizontalAlignment(SwingConstants.RIGHT);
    buttonMonitor.addActionListener((ev) -> {
      try {
        showMonitorIndicator();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage());
      }
    });
    labelResolution.setHorizontalAlignment(SwingConstants.RIGHT);
    labelSensitivity.setHorizontalAlignment(SwingConstants.RIGHT);
    labelResilience.setHorizontalAlignment(SwingConstants.RIGHT);
    labelDestiny.setHorizontalAlignment(SwingConstants.RIGHT);
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
    pane.add(buttonMonitor, like);
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
    pane.add(labelSensitivity, like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(spinnerSensitivity, like);
    like.gridx = 0;
    like.gridy = 3;
    like.weightx = 1.0;
    pane.add(labelResilience, like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(spinnerResilience, like);
    like.gridx = 0;
    like.gridy = 4;
    like.weightx = 1.0;
    pane.add(labelDestiny, like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(textDestiny, like);
    like.gridx = 0;
    like.gridy = 5;
    like.weightx = 1.0;
    pane.add(Box.createHorizontalGlue(), like);
    like.gridx = 1;
    like.weightx = 3.0;
    pane.add(labelStatus, like);
    like.gridx = 0;
    like.gridy = 6;
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
    loadResilience(setup);
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
    modelResolution.addElement("1066x600");
    modelResolution.addElement("1280x720");
    modelResolution.addElement("1600x900");
    modelResolution.addElement("1920x1080");
    var resolution = setup.getProperty("resolution");
    comboResolution.setSelectedItem(resolution != null ? resolution : "1066x600");
  }

  private void loadSensitivity(Properties setup) {
    var sensitivity = setup.getProperty("sensitivity");
    modelSensitivity.setValue(sensitivity != null ? Double.parseDouble(sensitivity) : 0.01);
  }

  private void loadResilience(Properties setup) {
    var resilience = setup.getProperty("resilience");
    modelResilience.setValue(resilience != null ? Integer.parseInt(resilience) : 18);
  }

  private void loadDestiny(Properties setup) {
    var destiny = setup.getProperty("destiny");
    textDestiny.setText(destiny != null ? destiny : "records");
  }

  private void save() throws Exception {
    var file = new FileOutputStream("skmotion.ini");
    var setup = new Properties();
    setup.setProperty("monitor", String.valueOf(comboMonitor.getSelectedItem()));
    setup.setProperty("resolution", String.valueOf(comboResolution.getSelectedItem()));
    setup.setProperty("sensitivity", String.valueOf(modelSensitivity.getValue()));
    setup.setProperty("resilience", String.valueOf(modelResilience.getValue()));
    setup.setProperty("destiny", textDestiny.getText());
    setup.store(file, "SkMotion");
  }

  private void showMonitorIndicator() throws Exception {
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
    var indicator = new JFrame("Monitor Indicator");
    indicator.setIconImage(iconLogo.getImage());
    indicator.setUndecorated(true);
    indicator.setAlwaysOnTop(true);
    indicator.setLocation(area.x, area.y);
    indicator.setSize(area.width, area.height);
    indicator.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    indicator.setBackground(Color.BLACK);
    indicator.setOpacity(0.54f);
    indicator.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        indicator.setVisible(false);
        indicator.dispose();
      }
    });
    indicator.setVisible(true);
  }

  public void show() throws Exception {
    load();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(330, frame.getMinimumSize().height);
    frame.setVisible(true);
    initRecorderStatus();
  }

  private void initRecorderStatus() throws Exception {
    new Thread("Recorder Status") {
      @Override
      public void run() {
        var lastSaved = 0;
        var lastDropped = 0;
        while (frame.isDisplayable()) {
          try {
            Thread.sleep(500);
            if (recMotion != null && !recMotion.isStopped()) {
              var dropped = recMotion.getDroppedFrames();
              var saved = recMotion.getSavedFrames();
              if (saved != lastSaved) {
                lastSaved = saved;
                var differs = String.format("%.6f", recMotion.getSavedDiffers());
                SwingUtilities.invokeLater(() -> {
                  labelStatus.setIcon(iconGreen);
                  labelStatus.setText("Differs: " + differs);
                });
              } else if (dropped != lastDropped) {
                lastDropped = dropped;
                var differs = String.format("%.6f", recMotion.getDroppedDiffers());
                SwingUtilities.invokeLater(() -> {
                  labelStatus.setIcon(iconRed);
                  labelStatus.setText("Differs: " + differs);
                });
              }
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    }.start();
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
      var resilience = (Integer) modelResilience.getValue();
      var destiny = new File(textDestiny.getText());
      recMotion = new RecMotion(area, size, destiny, sensitivity, resilience);
      recMotion.start();
      labelStatus.setText("Starting...");
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
