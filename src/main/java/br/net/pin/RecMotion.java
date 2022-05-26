package br.net.pin;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class RecMotion {

  private final Rectangle area;
  private final File destiny;
  private final AtomicBoolean isCapturing = new AtomicBoolean(true);
  private final Deque<BufferedImage> captured = new ConcurrentLinkedDeque<>();
  private final long captureInterval = 15;
  private volatile long lastCaptured = 0;
  private volatile long startTime = System.currentTimeMillis();

  private final ThreadGroup grouped = new ThreadGroup("RecMotion");
  private final List<Thread> working = new ArrayList<>();

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
    var thread = new Thread(grouped, "Capturer " + index) {
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
    working.add(thread);
    thread.start();
  }

  private final AtomicInteger savedIndex = new AtomicInteger(0);

  @SuppressWarnings("unused")
  private void spawnSaverPictures(int index) {
    var thread = new Thread("JPG Saver " + index) {
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
    };
    working.add(thread);
    thread.start();
  }

  private void spawnSaverMovie() {
    var thread = new Thread("MOV Saver") {
      public void run() {
        try {

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    working.add(thread);
    thread.start();
  }

  public void start() throws Exception {
    startTime = System.currentTimeMillis();
    if (!working.isEmpty()) {
      throw new Exception("Already started");
    }
    isCapturing.set(true);
    for (int i = 0; i < 4; i++) {
      spawnCapturer(i);
      Thread.sleep(captureInterval);
    }
    spawnSaverMovie();
  }

  public void stop() {
    isCapturing.set(false);
  }

  public long join() throws Exception {
    for (var thread : working) {
      thread.join();
    }
    working.clear();
    return System.currentTimeMillis() - startTime;
  }

  @SuppressWarnings("unused")
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
