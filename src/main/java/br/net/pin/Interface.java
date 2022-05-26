package br.net.pin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class Interface {

  private final JFrame frame = new JFrame("SkMotion");

  private final JLabel labelMonitor = new JLabel("Monitor:");
  private final JComboBox<String> comboMonitor = new JComboBox<String>();
  private final JLabel labelResolution = new JLabel("Resolution:");
  private final JComboBox<String> comboResolution = new JComboBox<String>();
  private final JLabel labelDestiny = new JLabel("Destiny:");
  private final JTextField textDestiny = new JTextField(60);
  private final JButton buttonStart = new JButton("Start");

  public Interface() {
    setupElements();
    setupLayout();
  }

  private void setupElements() {
    labelMonitor.setHorizontalAlignment(SwingConstants.RIGHT);
    labelResolution.setHorizontalAlignment(SwingConstants.RIGHT);
    labelDestiny.setHorizontalAlignment(SwingConstants.RIGHT);
  }

  private void setupLayout() {
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
    pane.add(buttonStart, like);
    frame.pack();
  }

  public void show() {
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(360, frame.getMinimumSize().height);
    frame.setVisible(true);
  }

}
