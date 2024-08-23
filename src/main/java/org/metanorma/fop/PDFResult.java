package org.metanorma.fop;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

public class PDFResult {

    private static PDFResult PDFResultSingleInstance = null;

    private String outFolder;

    private Path outTmpImagesPath;

    private PDFResult() {
    }

    private PDFResult(File pdfFile) {
        String parentFolder = pdfFile.getParent();
        if (parentFolder == null) {
            parentFolder = pdfFile.getAbsoluteFile().getParent();
        } else {
            parentFolder = new File(parentFolder).getAbsolutePath();
        }
        outTmpImagesPath = Paths.get(parentFolder, "_tmp_images_" + UUID.randomUUID().toString());
        outFolder = parentFolder;
    }

    public static PDFResult PDFResult(File pdfFile)
    {
        if (PDFResultSingleInstance == null) {
            PDFResultSingleInstance = new PDFResult(pdfFile);
        }
        return PDFResultSingleInstance;
    }

    public String getOutFolder() {
        return outFolder;
    }

    public Path getOutTmpImagesPath() {
        return outTmpImagesPath;
    }


    public void flushOutTmpImagesFolder () {
        if (Files.exists(outTmpImagesPath)) {
            try {
                Files.walk(outTmpImagesPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                Files.deleteIfExists(outTmpImagesPath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

}
