/*
 * Copyright 2017-2018   Eficode Oy
 * Copyright 2018-       Robot Framework Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javafxlibrary.keywords.Keywords;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafxlibrary.exceptions.JavaFXLibraryNonFatalException;
import javafxlibrary.utils.RobotLog;
import javafxlibrary.utils.TestFxAdapter;
import org.apache.commons.io.IOUtils;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywordOverload;
import org.robotframework.javalib.annotation.RobotKeywords;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static javafxlibrary.utils.HelperFunctions.*;

@RobotKeywords
public class ScreenCapturing extends TestFxAdapter {

    @RobotKeyword("Sets whether to embed log images directly into the log.html file or as a link to a file on local disk.\n\n"
            + "Argument ``value`` is a string. Accepted values are ``embedded`` (initial value) and ``diskonly``. They can be given in uppercase as well. \n\n"
            + "\nExample:\n"
            + "| Set Image Logging | DISKONLY |\n")
    @ArgumentNames({ "value" })
    public void setImageLogging(String value) {
        if (value.toLowerCase().equals("embedded"))
            TestFxAdapter.logImages = "embedded";
        else if (value.toLowerCase().equals("diskonly"))
            TestFxAdapter.logImages = "diskonly";
        else
            throw new JavaFXLibraryNonFatalException("Value \"" + value + "\" is not supported! Value must be either " +
                    "\"EMBEDDED\" or \"DISKONLY\"");
    }

    @RobotKeywordOverload
    public Object captureImage(Object locator){
            return captureImage(locator, true);
    }

    @RobotKeyword("Returns a screenshot of the given locator.\n\n"
            + "``locator`` is either a _query_ or _Object:Bounds, Node, Point2D, Rectangle, PointQuery, Scene, Window_ for identifying the element, see "
            + "`3. Locating or specifying UI elements`. \n\n"
            + "Argument ``logImage`` is a boolean value that specifies whether a captured image is also printed to test execution log. \n\n "
            + "\nExample:\n"
            + "| ${region}= | Create Rectangle | 11 | 22 | 33 | 44 | \n"
            + "| ${capture}= | Capture Image | ${region} | \n"
            + "| ${capture}= | Capture Image | ${node} | \n"
            + "| ${capture}= | Capture Image | ${window} | \n"
            + "| ${capture}= | Capture Image | \\#id | logImage=False |\n" )
    @ArgumentNames({"locator", "logImage=True"})
    public Object captureImage(Object locator, boolean logImage){
        if (locator == null)
            throw new JavaFXLibraryNonFatalException("Unable to capture image, given locator was null!");

        RobotLog.info("Capturing screenshot from locator: \"" + locator +  "\"");
        Image image;
        Bounds targetBounds = objectToBounds(locator);

        try {
            image = robot.capture(targetBounds).getImage();
            Path path = createNewImageFileNameWithPath();
            robotContext.getCaptureSupport().saveImage(image, path);

            if (logImage) {
                Double printSize = targetBounds.getWidth() > 800 ? 800 : targetBounds.getWidth();

                if(TestFxAdapter.logImages.toLowerCase().equals("embedded")) {
                    Image resizedImage = resizeImage(image, path);
                    Path tempPath = Paths.get(getCurrentSessionScreenshotDirectory(), "temp.png");
                    robotContext.getCaptureSupport().saveImage(resizedImage, tempPath);

                    File imageFile = convertToJpeg(tempPath);
                    byte[] imageBytes = IOUtils.toByteArray(new FileInputStream(imageFile));
                    String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
                    imageFile.delete();

                    RobotLog.html("<img src=\"data:image/png;base64," + encodedImage + "\" width=\"" + printSize + "px\">");

                } else {
                    // diskonly option
                    RobotLog.html("<img src=\"" + path + "\" width=\"" + printSize + "px\">");
                }
            }
            return mapObject(image);

        } catch (IOException e) {
            throw new JavaFXLibraryNonFatalException("Unable to take capture : \"" + locator + "\"", e);
        } catch (Exception e) {
            if (e instanceof JavaFXLibraryNonFatalException)
                throw e;
            throw new JavaFXLibraryNonFatalException("Unable to take capture : \"" + locator + "\"", e);
        }
    }

    @RobotKeyword("Loads an image from the given _path_ in hard drive \n\n"
            + "``path`` is the source path for image in local hard drive. \n\n"
            + "\nExample:\n"
            + "| ${image}= | Load Image | ${path to image}node.png |\n")
    @ArgumentNames({"path"})
    public Object loadImage(String path) {
        try {
            RobotLog.info("Loading image from: \"" + path + "\"");
            return mapObject(robot.capture(Paths.get(path)).getImage());
        } catch (Exception e) {
            if(e instanceof JavaFXLibraryNonFatalException)
                throw e;
            throw new JavaFXLibraryNonFatalException("Unable to load image from path: \"" + path + "\"", e);
        }
    }

    @RobotKeyword("Loads an image from the given _url_\n\n"
            + "``url`` is the url for the source image. \n\n"
            + "\nExample:\n"
            + "| ${path}= | Set Variable | http://i.imgur.com | \n"
            + "| ${image}= | Load Image From Url | ${path}/A99VNbK.png |\n")
    @ArgumentNames({"url"})
    public Object loadImageFromUrl(String url) {
        try {
            RobotLog.info("Loading image from URL: \"" + url + "\"");
            return mapObject(SwingFXUtils.toFXImage(ImageIO.read(new URL(url)), null));
        } catch(Exception e) {
            throw new JavaFXLibraryNonFatalException("Unable to load image from URL: \"" + url + "\"", e);
        }
    }

    @RobotKeyword("Saves given image to given location\n\n"
            + "``image`` is the target _Object:Image_ to be saved\n"
            + "``path`` is the target location where image will be saved")
    @ArgumentNames({ "image", "path" })
    public void saveImageAs(Image image, String path) {
        try {
            RobotLog.info("Saving image \"" + image + "\" to path \"" + path + "\"");
            robotContext.getCaptureSupport().saveImage(image, Paths.get(path));
        } catch (Exception e) {
            if(e instanceof JavaFXLibraryNonFatalException)
                throw e;
            throw new JavaFXLibraryNonFatalException("Unable to save image.", e);
        }
    }

    private Path createNewImageFileNameWithPath(){
        ZonedDateTime errorDateTime = ZonedDateTime.now();
        String errorTimestamp = formatErrorTimestamp(errorDateTime, "yyyyMMdd-HHmmss-SSS");
        String errorImageFilename = "JavaFXLib-" + errorTimestamp + ".png";
        String errorImageFilePath = getCurrentSessionScreenshotDirectory();
        File errDir = new File(errorImageFilePath);
        if(!errDir.exists())
            errDir.mkdirs();
        return Paths.get(errorImageFilePath, errorImageFilename);
    }

    private static String formatErrorTimestamp(ZonedDateTime dateTime, String dateTimePattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimePattern);
        return dateTime.format(formatter);
    }

    private static Image resizeImage(Image image, Path path) {
        double width = image.getWidth();
        double height = image.getHeight();

        if (width < 800)
            return image;

        RobotLog.html("Full resolution image can be found from <a href=" + path + " >" + path + "</a>.");
        double multiplier = width / 800;
        try {
            String url = path.toUri().toURL().toString();
            return new Image(url, width / multiplier, height / multiplier, true, true);
        } catch (MalformedURLException e) {
            throw new JavaFXLibraryNonFatalException("Unable to log the screenshot: image resizing failed!");
        }
    }

    private File convertToJpeg(Path path) throws IOException {
        BufferedImage bufferedImage;
        bufferedImage = ImageIO.read(path.toFile());
        BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
                bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, java.awt.Color.WHITE, null);
        path.toFile().delete();
        Path tempPathJpeg = Paths.get(getCurrentSessionScreenshotDirectory(), "temp.jpg");
        ImageIO.write(newBufferedImage, "jpg", tempPathJpeg.toFile());
        return tempPathJpeg.toFile();
    }
}