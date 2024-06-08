package org.metanorma.fop.utils;

import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class ImageUtils {
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    public static String getImageScale(String img, String width_effective, String height_effective) {
        try {
            BufferedImage bufferedImage;
            ImageInputStream imageInputStream;
            if (!img.startsWith("data:")) {
                File file = new File(img);
                bufferedImage = ImageIO.read(file);
                imageInputStream = ImageIO.createImageInputStream(file);
            } else {
                String base64String = img.substring(img.indexOf("base64,") + 7);
                Base64.Decoder base64Decoder = Base64.getDecoder();
                byte[] fileContent = base64Decoder.decode(base64String);
                ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
                bufferedImage = ImageIO.read(bais);

                ByteArrayInputStream baisDPI = new ByteArrayInputStream(fileContent);
                imageInputStream = ImageIO.createImageInputStream(baisDPI);
            }
            if (bufferedImage != null) {
                int width_px = bufferedImage.getWidth();
                int height_px = bufferedImage.getHeight();

                int image_dpi = getDPI(imageInputStream);

                double width_mm = Double.valueOf(width_px) / image_dpi * 25.4;
                double height_mm = Double.valueOf(height_px) / image_dpi * 25.4;

                //double width_effective_px = Double.valueOf(width_effective) / 25.4 * image_dpi;
                //double height_effective_px = Double.valueOf(height_effective) / 25.4 * image_dpi;
                double width_effective_mm = Double.valueOf(width_effective);
                double height_effective_mm = Double.valueOf(height_effective);


                double scale_x = 1.0;
                if (width_mm > width_effective_mm) {
                    scale_x = width_effective_mm / width_mm;
                }

                double scale_y = 1.0;
                if (height_mm * scale_x > height_effective_mm) {
                    scale_y = height_effective_mm / (height_mm * scale_x);
                }

                double scale = scale_x;
                if (scale_y != 1.0) {
                    scale = scale_x * scale_y;
                }

                return String.valueOf(Math.round(scale * 100));

            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Can''t read DPI from image: {0}", ex.toString());
        }

        return "100";
    }

    private static int getDPI(ImageInputStream imageInputStream) {
        int default_DPI = 96;
        if (imageInputStream != null) {
            try {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(imageInputStream);

                    IIOMetadata metadata = reader.getImageMetadata(0);
                    IIOMetadataNode standardTree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
                    IIOMetadataNode dimension = (IIOMetadataNode) standardTree.getElementsByTagName("Dimension").item(0);
                    float pixelSizeMM = getPixelSizeMM(dimension, "HorizontalPixelSize");
                    if (pixelSizeMM == -1.0f) { // try get verrical pixel size
                        pixelSizeMM = getPixelSizeMM(dimension, "VerticalPixelSize");
                    }
                    if (pixelSizeMM == -1.0f) return default_DPI;
                    float dpi = (float) (25.4f / pixelSizeMM);
                    return Math.round(dpi);
                }
            } catch (Exception ex) {
            }
        }

        logger.log(Level.SEVERE, "Could not read image DPI, use default value {0} DPI", default_DPI);
        return default_DPI; //default DPI
    }

    private static float getPixelSizeMM(final IIOMetadataNode dimension, final String elementName) {
        NodeList pixelSizes = dimension.getElementsByTagName(elementName);
        IIOMetadataNode pixelSize = pixelSizes.getLength() > 0 ? (IIOMetadataNode) pixelSizes.item(0) : null;
        return pixelSize != null ? Float.parseFloat(pixelSize.getAttribute("value")) : -1;
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
