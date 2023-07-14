package dev.ioliver.enums;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * The Suits enum represents the suits of playing cards.
 * Each suit has an associated image reference and label.
 */
public enum Suits {
  HEARTS("Hearts"), DIAMONDS("Diamonds"), CLUBS("Clubs"), SPADES("Spades"), JOKER("Joker");
  private final File PATH = Paths.get("assets/suits").toFile();
  private final Mat imgReference;
  private String label;

  {
    nu.pattern.OpenCV.loadLocally();
    if (!PATH.exists()) if (!PATH.mkdirs()) throw new RuntimeException("Couldn't create directory");
  }

  /**
   * Default constructor for Suits enum.
   * Loads the image reference and sets the label based on the suit name.
   * Converts the image to grayscale if it has multiple channels.
   *
   * @throws RuntimeException If the image file is not found.
   */
  Suits() {
    this.label = this.toString();
    File file = PATH.toPath().resolve(this + ".jpg").toFile();
    if (!file.exists()) throw new RuntimeException("Couldn't find file: " + this + ".jpg");
    this.imgReference = Imgcodecs.imread(file.getAbsolutePath());
    this.imgReference.convertTo(this.imgReference, CvType.CV_8U);
    if (this.imgReference.channels() > 1)
      Imgproc.cvtColor(this.imgReference, this.imgReference, Imgproc.COLOR_BGR2GRAY);
  }

  /**
   * Constructor for Suits enum with a custom label.
   * Delegates to the default constructor to load the image reference.
   *
   * @param label The custom label for the suit.
   */
  Suits(String label) {
    this();
    this.label = label;
  }

  /**
   * Returns a list of all suits.
   *
   * @return A list of Suits enum values.
   */
  static public List<Suits> getList() {
    return Arrays.asList(Suits.values());
  }

  /**
   * Returns the label associated with the suit.
   *
   * @return The label of the suit.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Returns the image reference for the suit.
   *
   * @return The image reference as a Mat object.
   */
  public Mat getImgReference() {
    return imgReference;
  }
}
