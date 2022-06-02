package br.net.pin.skmotion;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Interface {

  private static final File appFolder = new File(System.getProperty("user.home"), "SkMotion");

  static {
    if (!appFolder.exists()) {
      appFolder.mkdirs();
    }
  }

  private final ImageIcon iconLogo = new ImageIcon(getClass().getResource("logo.png"));
  private final ImageIcon iconBlue = new ImageIcon(getClass().getResource("blue.png"));
  private final ImageIcon iconGreen = new ImageIcon(getClass().getResource("green.png"));
  private final ImageIcon iconRed = new ImageIcon(getClass().getResource("red.png"));
  private final ImageIcon iconYellow = new ImageIcon(getClass().getResource("yellow.png"));

  private final JFrame frame = new JFrame("SkMotion");

  private final JButton buttonScreen = new JButton("Screen");
  private final DefaultComboBoxModel<String> modelScreen = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboScreen = new JComboBox<>(modelScreen);
  private final JLabel labelResolution = new JLabel("Resolution:");
  private final DefaultComboBoxModel<String> modelResolution = new DefaultComboBoxModel<>();
  private final JComboBox<String> comboResolution = new JComboBox<String>(modelResolution);
  private final JLabel labelSensitivity = new JLabel("Sensitivity:");
  private final JTextField textSensitivity = new JTextField(6);
  private final JLabel labelResilience = new JLabel("Resilience:");
  private final JTextField textResilience = new JTextField(12);
  private final JButton buttonDestiny = new JButton("Destiny");
  private final JTextField textDestiny = new JTextField(64);
  private final JLabel labelStatus = new JLabel("Waiting to start...");
  private final JButton buttonAbout = new JButton("About");
  private final JButton buttonAction = new JButton("Start");

  private volatile JFrame indicator = null;
  private volatile RecMotion recMotion = null;

  public Interface() {
    initComponents();
    initLayout();
  }

  private void initComponents() {
    frame.setIconImage(iconLogo.getImage());
    buttonScreen.addActionListener((ev) -> {
      try {
        showScreenIndicator();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    });
    comboScreen.addActionListener((ev) -> {
      jumpOnPositionByScreen();
    });
    labelResolution.setHorizontalAlignment(SwingConstants.RIGHT);
    labelSensitivity.setHorizontalAlignment(SwingConstants.RIGHT);
    textSensitivity.setHorizontalAlignment(SwingConstants.RIGHT);
    labelResilience.setHorizontalAlignment(SwingConstants.RIGHT);
    textResilience.setHorizontalAlignment(SwingConstants.RIGHT);
    buttonDestiny.addActionListener((ev) -> {
      try {
        showDestiny();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    });
    labelStatus.setVerticalTextPosition(SwingConstants.CENTER);
    labelStatus.setIcon(iconBlue);
    buttonAbout.addActionListener((ev) -> {
      JOptionPane.showMessageDialog(frame, "SkMotion\n\n" +
          "SkMotion (Screen Motion) is an application for desktop that\n" +
          "records the frames of a screen only when there is motion on it.\n\n" +
          "This application was developed to record video logs of a screen.\n\n" +
          "Source code on https://github.com/emuvi/skmotion\n\n" +
          "Copyright (C) 2022  Éverton M. Vieira\n\n" +
          "Version: 0.1.1\n", "About", JOptionPane.INFORMATION_MESSAGE);
    });
    buttonAction.addActionListener((ev) -> {
      try {
        doStartOrStop();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    });
  }

  private void initLayout() {
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
    pane.add(buttonScreen, like);
    like.gridx = 1;
    pane.add(comboScreen, like);
    like.gridx = 0;
    like.gridy = 1;
    pane.add(labelResolution, like);
    like.gridx = 1;
    pane.add(comboResolution, like);
    like.gridx = 0;
    like.gridy = 2;
    pane.add(labelSensitivity, like);
    like.gridx = 1;
    pane.add(textSensitivity, like);
    like.gridx = 0;
    like.gridy = 3;
    pane.add(labelResilience, like);
    like.gridx = 1;
    pane.add(textResilience, like);
    like.gridx = 0;
    like.gridy = 4;
    pane.add(buttonDestiny, like);
    like.gridx = 1;
    pane.add(textDestiny, like);
    like.gridx = 0;
    like.gridy = 5;
    pane.add(Box.createRigidArea(new Dimension(120, 10)), like);
    like.gridx = 1;
    pane.add(labelStatus, like);
    like.gridx = 0;
    like.gridy = 6;
    pane.add(Box.createRigidArea(new Dimension(120, 10)), like);
    like.gridx = 1;
    pane.add(Box.createRigidArea(new Dimension(180, 10)), like);
    like.gridx = 0;
    like.gridy = 7;
    pane.add(buttonAbout, like);
    like.gridx = 1;
    pane.add(buttonAction, like);
    frame.pack();
  }

  private void load() throws Exception {
    var setup = new Properties();
    var file = new File(appFolder, "skmotion.ini");
    if (file.exists()) {
      setup.load(new FileInputStream(file));
    }
    loadScreens(setup);
    loadResolutions(setup);
    loadSensitivity(setup);
    loadResilience(setup);
    loadDestiny(setup);
  }

  private void loadScreens(Properties setup) {
    var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (var display : ge.getScreenDevices()) {
      modelScreen.addElement(display.getIDstring());
    }
    var screen = setup.getProperty("screen");
    comboScreen.setSelectedItem(screen != null ? screen : modelScreen.getElementAt(0));
    jumpOnPositionByScreen();
  }

  private void jumpOnPositionByScreen() {
    var jumpTo = comboScreen.getSelectedIndex();
    var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    var device = ge.getScreenDevices()[jumpTo];
    var bounds = device.getDefaultConfiguration().getBounds();
    var posX = bounds.x + (bounds.width / 2 - frame.getWidth() / 2);
    var posY = bounds.y + (bounds.height / 2 - frame.getHeight() / 2);
    frame.setLocation(posX, posY);
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
    textSensitivity.setText(sensitivity != null ? sensitivity : "0.0001");
  }

  private void loadResilience(Properties setup) {
    var resilience = setup.getProperty("resilience");
    textResilience.setText(resilience != null ? resilience : "18");
  }

  private void loadDestiny(Properties setup) {
    var destiny = setup.getProperty("destiny");
    var records = new File(appFolder, "records");
    textDestiny.setText(destiny != null ? destiny : records.getAbsolutePath());
  }

  private void save() throws Exception {
    var file = new FileOutputStream(new File(appFolder, "skmotion.ini"));
    var setup = new Properties();
    setup.setProperty("screen", String.valueOf(comboScreen.getSelectedItem()));
    setup.setProperty("resolution", String.valueOf(comboResolution.getSelectedItem()));
    setup.setProperty("sensitivity", String.valueOf(textSensitivity.getText()));
    setup.setProperty("resilience", String.valueOf(textResilience.getText()));
    setup.setProperty("destiny", textDestiny.getText());
    setup.store(file, "SkMotion");
  }

  private void closeIndicator() {
    if (indicator != null) {
      indicator.setVisible(false);
      indicator.dispose();
      indicator = null;
    }
  }

  private void showScreenIndicator() throws Exception {
    if (indicator != null) {
      closeIndicator();
      return;
    }
    var screen = String.valueOf(comboScreen.getSelectedItem());
    var graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Rectangle area = null;
    for (var device : graphics.getScreenDevices()) {
      if (screen == device.getIDstring()) {
        area = device.getDefaultConfiguration().getBounds();
        break;
      }
    }
    if (area == null) {
      throw new Exception("Screen not found");
    }
    indicator = new JFrame("Screen Indicator");
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
        closeIndicator();
      }
    });
    indicator.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        closeIndicator();
      }
    });
    indicator.setVisible(true);
  }

  public void showDestiny() throws Exception {
    var destiny = new File(textDestiny.getText());
    Files.createDirectories(destiny.toPath());
    Desktop.getDesktop().open(destiny);
  }

  public void show() throws Exception {
    load();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(360, frame.getMinimumSize().height + 30);
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
                var differs = String.format("%.9f", recMotion.getSavedDiffers());
                SwingUtilities.invokeLater(() -> {
                  labelStatus.setIcon(iconGreen);
                  labelStatus.setText("Motion save: " + differs);
                });
              } else if (dropped != lastDropped) {
                lastDropped = dropped;
                var differs = String.format("%.9f", recMotion.getDroppedDiffers());
                SwingUtilities.invokeLater(() -> {
                  labelStatus.setIcon(iconRed);
                  labelStatus.setText("Motion drop: " + differs);
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
      var screen = String.valueOf(comboScreen.getSelectedItem());
      var graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
      Rectangle area = null;
      for (var device : graphics.getScreenDevices()) {
        if (screen == device.getIDstring()) {
          area = device.getDefaultConfiguration().getBounds();
          break;
        }
      }
      if (area == null) {
        throw new Exception("Screen not found");
      }
      var resolution = String.valueOf(comboResolution.getSelectedItem());
      var parts = resolution.split("x");
      if (parts.length != 2) {
        throw new Exception("Resolution mal formed");
      }
      var width = Integer.parseInt(parts[0]);
      var height = Integer.parseInt(parts[1]);
      var size = new Dimension(width, height);
      var sensitivity = Float.parseFloat(textSensitivity.getText());
      var resilience = Integer.parseInt(textResilience.getText());
      var destiny = new File(textDestiny.getText());
      recMotion = new RecMotion(area, size, destiny, sensitivity, resilience);
      recMotion.start();
      labelStatus.setText("Starting to record...");
      buttonAction.setText("Stop");
      save();
    } else {
      recMotion.stop();
      buttonAction.setEnabled(false);
      buttonAction.setText("Closing");
      labelStatus.setIcon(iconYellow);
      labelStatus.setText("Closing the record...");
      new Thread("Waiting to stop") {
        @Override
        public void run() {
          try {
            var elapsed = recMotion.join();
            var framesSave = recMotion.getSavedFrames();
            var saveTime = framesSave * 100;
            var framesDrop = recMotion.getDroppedFrames();
            var framesTotal = framesSave + framesDrop;
            var framesSavePer = String.format("%.2f", framesSave * 100.0 / framesTotal);
            var framesDropPer = String.format("%.2f", framesDrop * 100.0 / framesTotal);
            recMotion = null;
            SwingUtilities.invokeLater(() -> {
              labelStatus.setIcon(iconBlue);
              labelStatus.setText("Waiting to start...");
              buttonAction.setText("Start");
              buttonAction.setEnabled(true);
              var message = new StringBuilder("Recorded: ");
              message.append(formatTime(elapsed));
              message.append("\n");
              message.append("Produced: ");
              message.append(formatTime(saveTime));
              message.append("\n\n");
              message.append("Save ");
              message.append(framesSave);
              message.append(" frames ( ");
              message.append(framesSavePer);
              message.append("% )\n");
              message.append("Drop ");
              message.append(framesDrop);
              message.append(" frames ( ");
              message.append(framesDropPer);
              message.append("% )");
              JOptionPane.showMessageDialog(frame, message.toString(), "Finished", JOptionPane.INFORMATION_MESSAGE);
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
      minutes = minutes % 60;
      sb.append(hours).append("h ");
    }
    if (minutes > 0) {
      seconds = seconds % 60;
      sb.append(minutes).append("m ");
    }
    sb.append(seconds).append("s");
    return sb.toString();
  }
}
