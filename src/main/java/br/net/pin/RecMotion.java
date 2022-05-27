package br.net.pin;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

public class RecMotion {

  private final Rectangle area;
  private final Dimension size;
  private final File destiny;
  private final AtomicBoolean isCapturing = new AtomicBoolean(true);
  private final Deque<BufferedImage> captured = new ConcurrentLinkedDeque<>();
  private final long captureInterval = 100;
  private volatile long lastCaptured = 0;
  private volatile long startTime = System.currentTimeMillis();

  private final ThreadGroup grouped = new ThreadGroup("RecMotion");
  private final List<Thread> working = new ArrayList<>();

  public RecMotion(Rectangle area, Dimension size, File destiny) {
    this.area = area;
    this.size = size;
    this.destiny = destiny;
  }

  private synchronized boolean isTimeToCapture() {
    var now = System.currentTimeMillis();
    var elapsed = now - lastCaptured;
    var result = elapsed >= captureInterval;
    if (result) {
      lastCaptured = now;
    }
    return result;
  }

  private void spawnLoadThread(int index) {
    var thread = new Thread(grouped, "Loading " + index) {
      public void run() {
        try {
          var robot = new Robot();
          while (isCapturing.get()) {
            if (isTimeToCapture()) {
              var all = robot.createScreenCapture(area);
              var tmp = all.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
              var screen = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
              screen.createGraphics().drawImage(tmp, 0, 0, null);
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

  private void spawnSaveThread() {
    var thread = new Thread(grouped, "Saving") {
      public void run() {
        SeekableByteChannel out = null;
        try {
          out = NIOUtils.writableFileChannel(destiny.getAbsolutePath());
          var encoder = new AWTSequenceEncoder(out, Rational.R(10, 1));
          while (true) {
            var screen = captured.pollFirst();
            if (screen != null) {
              encoder.encodeImage(screen);
            } else if (!isCapturing.get()) {
              break;
            }
          }
          encoder.finish();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          NIOUtils.closeQuietly(out);
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
    for (int i = 0; i < 2; i++) {
      spawnLoadThread(i);
      Thread.sleep(captureInterval);
    }
    spawnSaveThread();
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
