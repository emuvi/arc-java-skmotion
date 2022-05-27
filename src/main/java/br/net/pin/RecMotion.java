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
import java.util.concurrent.atomic.AtomicInteger;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

public class RecMotion {

  private static final long captureWait = 100;
  private static final long antiEagerWait = 30;

  private final Rectangle area;
  private final Dimension size;
  private final File destiny;
  private final Float sensitivity;
  private final Integer resilience;
  private final List<Thread> working = new ArrayList<>();
  private final ThreadGroup grouped = new ThreadGroup("RecMotion");
  private final AtomicBoolean isCapturing = new AtomicBoolean(true);
  private final Deque<BufferedImage> toCheck = new ConcurrentLinkedDeque<>();
  private final Deque<BufferedImage> toSave = new ConcurrentLinkedDeque<>();
  private final AtomicInteger framesSaved = new AtomicInteger(0);
  private final AtomicInteger framesDropped = new AtomicInteger(0);
  private volatile float lastSavedDiffers = 0.0f;
  private volatile float lastDroppedDiffers = 0.0f;
  private volatile long lastCaptured = 0;
  private volatile long startTime = System.currentTimeMillis();
  private volatile long stopTime = 0;

  public RecMotion(Rectangle area, Dimension size, File destiny, Float sensitivity, Integer resilience) {
    this.area = area;
    this.size = size;
    this.destiny = destiny;
    this.sensitivity = sensitivity;
    this.resilience = resilience;
  }

  private synchronized boolean isTimeToCapture() {
    var now = System.currentTimeMillis();
    var elapsed = now - lastCaptured;
    var result = elapsed >= captureWait;
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
              var screen = robot.createScreenCapture(area);
              toCheck.addLast(screen);
            } else {
              Thread.sleep(antiEagerWait);
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

  private void spawnCheckThread() {
    var thread = new Thread(grouped, "Checking") {
      public void run() {
        try {
          BufferedImage last = null;
          var resilienceGuard = resilience;
          while (true) {
            var screen = toCheck.pollFirst();
            if (screen != null) {
              if (last == null || hasMotion(last, screen)) {
                send(screen);
                last = screen;
                resilienceGuard = resilience;
              } else if (resilienceGuard > 0) {
                send(screen);
                resilienceGuard--;
              } else {
                framesDropped.incrementAndGet();
              }
            } else if (!isCapturing.get()) {
              break;
            } else {
              Thread.sleep(antiEagerWait);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      private void send(BufferedImage screen) {
        var scaled = screen.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
        var result = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        result.createGraphics().drawImage(scaled, 0, 0, null);
        toSave.addLast(result);
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
            var screen = toSave.pollFirst();
            if (screen != null) {
              encoder.encodeImage(screen);
              framesSaved.incrementAndGet();
            } else if (!isCapturing.get()) {
              break;
            } else {
              Thread.sleep(antiEagerWait);
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
      Thread.sleep(captureWait);
    }
    spawnCheckThread();
    Thread.sleep(antiEagerWait);
    spawnSaveThread();
  }

  public void stop() {
    isCapturing.set(false);
    stopTime = System.currentTimeMillis();
  }

  public boolean isStopped() {
    return !isCapturing.get();
  }

  public long join() throws Exception {
    for (var thread : working) {
      thread.join();
    }
    working.clear();
    return stopTime - startTime;
  }

  public int getSavedFrames() {
    return framesSaved.get();
  }

  public int getDroppedFrames() {
    return framesDropped.get();
  }

  public float getSavedDiffers() {
    return lastSavedDiffers;
  }

  public float getDroppedDiffers() {
    return lastDroppedDiffers;
  }

  private boolean hasMotion(BufferedImage img1, BufferedImage img2) {
    var equals = 0;
    var entire = img1.getWidth() * img1.getHeight();
    for (int x = 0; x < img1.getWidth(); x++) {
      for (int y = 0; y < img1.getHeight(); y++) {
        if (img1.getRGB(x, y) == img2.getRGB(x, y)) {
          equals++;
        }
      }
    }
    var differs = 1.0f - ((float) equals / entire);
    var result = differs > sensitivity;
    if (result) {
      lastSavedDiffers = differs;
    } else {
      lastDroppedDiffers = differs;
    }
    return result;
  }
}
