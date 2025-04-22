package org.metanorma.fop.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Descriptor;
import com.drew.metadata.exif.ExifIFD0Directory;
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
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageData {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private BufferedImage bufferedImage;
    private ImageInputStream imageInputStream;

    private double scale = 1;
    private double width_mm;
    private double height_mm;

     ImageData(String img, String width_effective, String height_effective) throws IOException {
        byte[] fileContent = null;
        if (!img.startsWith("data:")) {
            File file = new File(img);
            bufferedImage = ImageIO.read(file);
            imageInputStream = ImageIO.createImageInputStream(file);
        } else {
            String base64String = img.substring(img.indexOf("base64,") + 7);
            Base64.Decoder base64Decoder = Base64.getDecoder();
            fileContent = base64Decoder.decode(base64String);
            ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
            bufferedImage = ImageIO.read(bais);

            ByteArrayInputStream baisDPI = new ByteArrayInputStream(fileContent);
            imageInputStream = ImageIO.createImageInputStream(baisDPI);
        }

         if (bufferedImage != null) {
             int width_px = bufferedImage.getWidth();
             int height_px = bufferedImage.getHeight();

             int image_dpi = getDPI(imageInputStream);
             if (image_dpi == -1) {
                 if (!img.startsWith("data:")) {
                     image_dpi = getDPIFromMetadata(new File(img));
                 } else {
                     image_dpi = getDPIFromMetadata(new ByteArrayInputStream(fileContent));
                 }
             }

             this.width_mm = Double.valueOf(width_px) / image_dpi * 25.4;
             this.height_mm = Double.valueOf(height_px) / image_dpi * 25.4;

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

             this.scale = scale_x;
             if (scale_y != 1.0) {
                 this.scale = scale_x * scale_y;
             }

             //this.scale =  Math.round(scale * 100);

         }

    }

    private static int getDPI(ImageInputStream imageInputStream) {
        int default_DPI = -1;
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
                    if (pixelSizeMM == -1.0f) { // try get vertical pixel size
                        pixelSizeMM = getPixelSizeMM(dimension, "VerticalPixelSize");
                    }
                    if (pixelSizeMM == -1.0f) return default_DPI;
                    float dpi = (float) (25.4f / pixelSizeMM);
                    return Math.round(dpi);
                }
            } catch (Exception ex) {
            }
        }
        logger.log(Level.SEVERE, "Could not read image DPI");
        return default_DPI; //default DPI
    }

    private static int getDPIFromMetadata(Object img) {
        int default_DPI = 96;
        try {
            Metadata metadata = null;
            if (img instanceof File) {
                metadata = ImageMetadataReader.readMetadata((File)img);
            } else if (img instanceof ByteArrayInputStream) {
                metadata = ImageMetadataReader.readMetadata((ByteArrayInputStream)img);
            }
            if (metadata != null) {
                /*for (Directory directory : metadata.getDirectories()) {
                    for (Tag tag : directory.getTags()) {
                        System.out.println(tag);
                    }
                }*/
                ExifIFD0Descriptor descriptor = new ExifIFD0Descriptor(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class));
                String strXRes = descriptor.getXResolutionDescription();
                strXRes = strXRes.replaceAll("(\\d+).*", "$1");

                try {
                    int xRes = Integer.parseInt(strXRes);
                    return xRes;
                }
                catch (NumberFormatException e) {
                    return default_DPI; //default DPI
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not read image DPI, use default value {0} DPI", default_DPI);
        }
        return default_DPI; //default DPI
    }

    private static float getPixelSizeMM(final IIOMetadataNode dimension, final String elementName) {
        NodeList pixelSizes = dimension.getElementsByTagName(elementName);
        IIOMetadataNode pixelSize = pixelSizes.getLength() > 0 ? (IIOMetadataNode) pixelSizes.item(0) : null;
        return pixelSize != null ? Float.parseFloat(pixelSize.getAttribute("value")) : -1;
    }


    public long getImageScale() {
         return Math.round(scale * 100);
    }

    public double getImageWidth() {
         return width_mm * scale;
    }
}
