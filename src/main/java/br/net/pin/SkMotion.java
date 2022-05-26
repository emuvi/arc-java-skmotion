package br.net.pin;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class SkMotion {
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

  private static Integer count = 0;

  private static Deque<BufferedImage> deque = new ConcurrentLinkedDeque<>();

  public static final Long startTime = System.currentTimeMillis();

  private static final AtomicBoolean isRunning = new AtomicBoolean(true);

  public static void main(String[] args) throws Exception {
    var saving = new Thread() {
      public void run() {
        while (true) {
          var image = deque.pollLast();
          if (image != null) {
            count++;
            // try {
            // ImageIO.write(image, "jpg", new File("screen" + count + ".jpg"));
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
          } else if (!isRunning.get()) {
            break;
          }
        }
      };
    };
    saving.start();
    Robot robot = new Robot();
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    BufferedImage previous = robot.createScreenCapture(screenRect);
    var index = 0;
    while (index < 500) {
      BufferedImage current = robot.createScreenCapture(screenRect);
      if (changed(previous, current)) {
        deque.addFirst(current);
      }
      previous = current;
      index++;
    }
    isRunning.set(false);
    System.out.println("Time: " + (System.currentTimeMillis() - startTime));
    saving.join();
  }
}
