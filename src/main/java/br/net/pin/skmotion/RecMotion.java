package br.net.pin.skmotion;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

public class RecMotion {
  private final long captureWait = 100;
  private final long antiEagerWait = 10;

  private final SimpleDateFormat formatForNames = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

  private final Rectangle area;
  private final Dimension size;
  private final File destiny;
  private final Float sensitivity;
  private final Integer resilience;
  private final Image cursorImage;
  private final List<Thread> working = new ArrayList<>();
  private final ThreadGroup grouped = new ThreadGroup("RecMotion");
  private final AtomicBoolean isCapturing = new AtomicBoolean(true);
  private final Deque<ScreenAndCursor> toCheck = new ConcurrentLinkedDeque<>();
  private final Deque<ScreenAndCursor> toSave = new ConcurrentLinkedDeque<>();
  private final AtomicInteger framesSaved = new AtomicInteger(0);
  private final AtomicInteger framesDropped = new AtomicInteger(0);
  private volatile float lastSavedDiffers = 0.0f;
  private volatile float lastDroppedDiffers = 0.0f;
  private volatile long lastCaptured = 0;
  private volatile long startTime = System.currentTimeMillis();
  private volatile long stopTime = 0;

  public RecMotion(Rectangle area, Dimension size, File destiny, Float sensitivity, Integer resilience)
      throws Exception {
    this.area = area;
    this.size = size;
    this.destiny = destiny;
    this.sensitivity = sensitivity;
    this.resilience = resilience;
    var cursorDiff = Math.max(size.height - 480, 0);
    var cursorProp = 0.012f;
    var cursorWith = 12 + (int) (cursorDiff * cursorProp);
    var cursorSize = Math.min(cursorWith, 20);
    this.cursorImage = ImageIO.read(getClass().getResource("cursor.png")).getScaledInstance(cursorSize, cursorSize,
        Image.SCALE_SMOOTH);
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

  private void spawnLoadThread() {
    var thread = new Thread(grouped, "Loading") {
      public void run() {
        try {
          var robot = new Robot();
          while (isCapturing.get()) {
            if (isTimeToCapture()) {
              var screen = robot.createScreenCapture(area);
              var cursor = MouseInfo.getPointerInfo().getLocation();
              toCheck.addLast(new ScreenAndCursor(screen, cursor));
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
            var screenAndCursor = toCheck.pollFirst();
            if (screenAndCursor != null) {
              var screen = screenAndCursor.getScreen();
              var cursor = screenAndCursor.getCursor();
              if (last == null || hasMotion(last, screen)) {
                send(screen, cursor);
                last = screen;
                resilienceGuard = resilience;
              } else if (resilienceGuard > 0) {
                send(screen, cursor);
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

      private void send(BufferedImage screen, Point cursor) {
        var scaled = screen.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
        var result = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        result.createGraphics().drawImage(scaled, 0, 0, null);
        toSave.addLast(new ScreenAndCursor(result, cursor));
      }
    };
    working.add(thread);
    thread.start();
  }

  private void spawnSaveThread() {
    var thread = new Thread(grouped, "Saving") {
      private final float timesWidth = (float) size.width / area.width;
      private final float timesHeight = (float) size.height / area.height;

      public void run() {
        SeekableByteChannel out = null;
        try {
          out = NIOUtils.writableFileChannel(getPath());
          var encoder = new AWTSequenceEncoder(out, Rational.R(10, 1));
          while (true) {
            var screenAndCursor = toSave.pollFirst();
            if (screenAndCursor != null) {
              var screen = screenAndCursor.getScreen();
              var cursor = screenAndCursor.getCursor();
              var cursorPosition = getCursorPosition(cursor);
              if (cursorPosition != null) {
                screen.getGraphics().drawImage(cursorImage, cursorPosition.x, cursorPosition.y, null);
              }
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

      private String getPath() throws Exception {
        Files.createDirectories(destiny.toPath());
        var now = System.currentTimeMillis();
        var fileDestiny = new File(destiny, formatForNames.format(now) + ".mp4");
        return fileDestiny.getAbsolutePath();
      }

      public Point getCursorPosition(Point cursor) {
        if (!area.contains(cursor)) {
          return null;
        }
        cursor.x = (int) (cursor.x * timesWidth);
        cursor.y = (int) (cursor.y * timesHeight);
        return cursor;
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
    spawnLoadThread();
    Thread.sleep(antiEagerWait);
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

  private static class ScreenAndCursor {
    private final BufferedImage screen;
    private final Point cursor;

    public ScreenAndCursor(BufferedImage screen, Point cursor) {
      this.screen = screen;
      this.cursor = cursor;
    }

    public BufferedImage getScreen() {
      return screen;
    }

    public Point getCursor() {
      return cursor;
    }
  }
}
