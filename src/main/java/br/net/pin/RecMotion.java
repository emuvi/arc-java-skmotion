package br.net.pin;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class RecMotion {

  private final Rectangle area;
  private final Long startTime = System.currentTimeMillis();
  private final AtomicBoolean isCapturing = new AtomicBoolean(true);
  private final Deque<BufferedImage> captured = new ConcurrentLinkedDeque<>();
  private final long captureInterval = 15;
  private volatile long lastCaptured = 0;

  public RecMotion(Rectangle area) {
    this.area = area;
  }

  private synchronized boolean isTimeToCapture() {
    var result = System.currentTimeMillis() - lastCaptured > captureInterval;
    if (result) {
      lastCaptured = System.currentTimeMillis();
    }
    return result;
  }

  private Thread spawnCapturer(int index) {
    var result = new Thread("Capturer " + index) {
      public void run() {
        try {
          var robot = new Robot();
          while (isCapturing.get()) {
            if (isTimeToCapture()) {
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

  private final AtomicInteger savedIndex = new AtomicInteger(0);

  private Thread spawnSaver(int index) {
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

  public void start() throws Exception {
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

  private boolean hasChanged(BufferedImage img1, BufferedImage img2) {
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
