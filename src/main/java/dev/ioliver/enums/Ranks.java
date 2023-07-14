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
 * The Ranks enum represents the ranks of playing cards.
 * Each rank has an associated image reference and label.
 */
public enum Ranks {
  N2("2"), N3("3"), N4("4"), N5("5"), N6("6"), N7("7"), N8("8"), N9("9"),
  N10("10"), J("J"), Q("Q"), K("K"), A("A"), JOKER("Joker");

  private final File PATH = Paths.get("assets/ranks").toFile();  // Directory path for rank images
  private final Mat imgReference;  // Image reference for the rank
  private String label;            // Label associated with the rank

  {
    nu.pattern.OpenCV.loadLocally();
    if (!PATH.exists()) if (!PATH.mkdirs()) throw new RuntimeException("Couldn't create directory");
  }

  /**
   * Default constructor for Ranks enum.
   * Loads the image reference and sets the label based on the rank name.
   * Converts the image to grayscale if it has multiple channels.
   *
   * @throws RuntimeException If the image file is not found.
   */
  Ranks() {
    this.label = this.toString();
    File file = PATH.toPath().resolve(this + ".jpg").toFile();
    if (!file.exists()) throw new RuntimeException("Couldn't find file: " + this + ".jpg");
    this.imgReference = Imgcodecs.imread(file.getAbsolutePath());
    this.imgReference.convertTo(this.imgReference, CvType.CV_8U);
    if (this.imgReference.channels() > 1)
      Imgproc.cvtColor(this.imgReference, this.imgReference, Imgproc.COLOR_BGR2GRAY);
  }

  /**
   * Constructor for Ranks enum with a custom label.
   * Delegates to the default constructor to load the image reference.
   *
   * @param label The custom label for the rank.
   */
  Ranks(String label) {
    this();
    this.label = label;
  }

  /**
   * Returns a list of all ranks.
   *
   * @return A list of Ranks enum values.
   */
  static public List<Ranks> getList() {
    return Arrays.asList(Ranks.values());
  }

  /**
   * Returns the label associated with the rank.
   *
   * @return The label of the rank.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Returns the image reference for the rank.
   *
   * @return The image reference as a Mat object.
   */
  public Mat getImgReference() {
    return imgReference;
  }
}
