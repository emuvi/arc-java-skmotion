package br.net.pin;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

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

  private static void save(BufferedImage image) throws Exception {
    ImageIO.write(image, "jpg", new File("screen.jpg"));
  }

  public static void main(String[] args) throws Exception {
    Robot robot = new Robot();
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    BufferedImage img1 = robot.createScreenCapture(screenRect);
    Thread.sleep(3000);
    BufferedImage img2 = robot.createScreenCapture(screenRect);
    if (changed(img1, img2)) {
      save(img2);
    }
  }
}
