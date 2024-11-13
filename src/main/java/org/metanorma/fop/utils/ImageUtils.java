package org.metanorma.fop.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.metanorma.utils.LoggerHelper;

/**
 *
 * @author Alexander Dyuzhev
 */
public class ImageUtils {
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    public static String getImageScale(String img, String width_effective, String height_effective) {
        try {
            ImageData imageData = new ImageData(img, width_effective, height_effective);
            return String.valueOf(imageData.getImageScale());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Can''t read DPI from image: {0}", ex.toString());
        }
        return "100";
    }

    public static String getImageWidth(String img, String width_effective, String height_effective) {
        try {
            ImageData imageData = new ImageData(img, width_effective, height_effective);
            return String.format("%.1f", imageData.getImageWidth());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Can''t read DPI from image: {0}", ex.toString());
        }
        return "100";
    }



    public static String convertWebPtoPNG(String img) {
        try {
            BufferedImage bufferedImage;
            if (!img.startsWith("data:")) {
                File file = new File(img);
                String filenameOut = img.substring(0, img.lastIndexOf(".")) + ".png";
                File fileOut = new File(filenameOut);
                bufferedImage = ImageIO.read(file);
                ImageIO.write(bufferedImage, "png", fileOut);
                return filenameOut;
            } else {
                String base64String = img.substring(img.indexOf("base64,") + 7);
                Base64.Decoder base64Decoder = Base64.getDecoder();
                byte[] fileContent = base64Decoder.decode(base64String);
                ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
                bufferedImage = ImageIO.read(bais);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imgPNGbytes = baos.toByteArray();
                String imgPNGbase64 =  Base64.getEncoder().encodeToString(imgPNGbytes);
                return "data:image/png;base64," + imgPNGbase64;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Can''t read the image: {0}", ex.toString());
        }
        return img;
    }
}
