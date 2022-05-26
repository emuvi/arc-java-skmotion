package br.net.pin;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class RecMotion {

  private final Rectangle area;
  private final File destiny;
  private final Long startTime = System.currentTimeMillis();
  private final AtomicBoolean isCapturing = new AtomicBoolean(true);
  private final Deque<BufferedImage> captured = new ConcurrentLinkedDeque<>();
  private final long captureInterval = 15;
  private volatile long lastCaptured = 0;

  private ThreadGroup processing = null;

  public RecMotion(Rectangle area, File destiny) {
    this.area = area;
    this.destiny = destiny;
  }

  private synchronized boolean isTimeToCapture() {
    var result = System.currentTimeMillis() - lastCaptured > captureInterval;
    if (result) {
      lastCaptured = System.currentTimeMillis();
    }
    return result;
  }

  private void spawnCapturer(int index) {
    new Thread(processing, "Capturer " + index) {
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
    }.start();
  }

  private final AtomicInteger savedIndex = new AtomicInteger(0);

  private void spawnSaver(int index) {
    new Thread("Saver " + index) {
      public void run() {
        try {
          while (true) {
            var screen = captured.pollFirst();
            if (screen != null) {
              var file = new File(destiny, "captured-" + savedIndex.incrementAndGet() + ".png");
              ImageIO.write(screen, "jpg", file);
            } else if (!isCapturing.get()) {
              break;
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }.start();
  }

  public void start() throws Exception {
    if (processing != null) {
      throw new Exception("Already started");
    }
    processing = new ThreadGroup("RecMotion");
    isCapturing.set(true);
    for (int i = 0; i < 4; i++) {
      spawnCapturer(i);
      Thread.sleep(captureInterval);
    }
    for (int i = 0; i < 2; i++) {
      Thread.sleep(captureInterval);
      spawnSaver(i);
    }
  }

  public void stop() {
    isCapturing.set(false);
  }

  public void join() {
    while (true) {
      if (processing.activeCount() == 0) {
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
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
