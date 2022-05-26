package br.net.pin;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.pushingpixels.radiance.theming.api.skin.RadianceGraphiteSunsetLookAndFeel;

public class SkMotion {

  public static final Long startTime = System.currentTimeMillis();

  private static final AtomicBoolean isCapturing = new AtomicBoolean(true);

  private static Deque<BufferedImage> captured = new ConcurrentLinkedDeque<>();

  private static final long captureInterval = 25;

  private static volatile long lastCapture = 0;

  private static synchronized boolean isInTimeToCapture() {
    var result = System.currentTimeMillis() - lastCapture > captureInterval;
    if (result) {
      lastCapture = System.currentTimeMillis();
    }
    return result;
  }

  private static Thread spawnCapturer(int index) {
    var result = new Thread("Capturer " + index) {
      public void run() {
        try {
          var robot = new Robot();
          var area = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
          while (isCapturing.get()) {
            if (isInTimeToCapture()) {
              var screen = robot.createScreenCapture(area);
              captured.addLast(screen);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    result.start();
    return result;
  }

  private static final AtomicInteger savedIndex = new AtomicInteger(0);

  private static Thread spawnSaver(int index) {
    var result = new Thread("Saver " + index) {
      public void run() {
        try {
          while (true) {
            var screen = captured.pollFirst();
            if (screen != null) {
              var file = new File("captured-" + savedIndex.incrementAndGet() + ".png");
              ImageIO.write(screen, "jpg", file);
            } else if (!isCapturing.get()) {
              break;
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    result.start();
    return result;
  }

  private static void startTests() throws Exception {
    var threadGroup = new ArrayList<Thread>();
    for (int i = 0; i < 4; i++) {
      threadGroup.add(spawnCapturer(i));
      Thread.sleep(captureInterval);
    }
    for (int i = 0; i < 2; i++) {
      threadGroup.add(spawnSaver(i));
      Thread.sleep(captureInterval);
    }
    var captureFor = 1000 * 10;
    while (true) {
      Thread.sleep(captureInterval);
      if (System.currentTimeMillis() - startTime > captureFor) {
        isCapturing.set(false);
        break;
      }
    }
    isCapturing.set(false);
    for (var thread : threadGroup) {
      thread.join();
    }
  }

  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(() -> {
      try {
        JFrame.setDefaultLookAndFeelDecorated(true);
        UIManager.setLookAndFeel(new RadianceGraphiteSunsetLookAndFeel());
        new Interface().show();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private static boolean changed(BufferedImage img1, BufferedImage img2) {
    if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
      for (int x = 0; x < img1.getWidth(); x++) {
        for (int y = 0; y < img1.getHeight(); y++) {
          if (img1.getRGB(x, y) != img2.getRGB(x, y))
            return true;
        }
      }
    } else {
      return true;
    }
    return false;
  }
}
