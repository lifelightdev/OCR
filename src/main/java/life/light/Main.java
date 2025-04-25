package life.light;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static life.light.FileTool.deleteAllFiles;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger();


    public static void main(String[] args) {
        createWorkDirectory();
        emptyWorkDirectory();
        String pathPDF = "" ;
        if (args.length == 1) {
            pathPDF = args[0];
        }
        extractPDFToTIFF(new File(pathPDF));

    }

    public static void createWorkDirectory() {
        FileTool.createDirectory(Constant.TEMP);
        FileTool.createDirectory(Constant.TEMP + File.separator + Constant.TIFF);
        FileTool.createDirectory(Constant.TEMP + File.separator + Constant.TXT);
    }

    public static void emptyWorkDirectory() {
        deleteAllFiles(new File(Constant.TEMP));
        deleteAllFiles(new File(Constant.TEMP + File.separator + Constant.TIFF));
        deleteAllFiles(new File(Constant.TEMP + File.separator + Constant.TXT));
    }

    public static void extractPDFToTIFF(File pdf) {
        try {
            PDDocument document = PDDocument.load(pdf);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 2400, ImageType.BINARY);
                String outputFilePath = Constant.TEMP + File.separator + Constant.TIFF + File.separator
                        + "page-" + addZeros(page + 1) + "." + Constant.TIFF;
                ImageIO.write(bim, Constant.TIFF, new File(outputFilePath));
                LOGGER.info("Conversion de la page dans le fichier : {}", outputFilePath);
            }
            document.close();
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la conversion PDF en image : {}", e.getMessage());
        }
    }

    private static String addZeros(int number) {
        int length = 3;
        StringBuilder numberString = new StringBuilder(String.valueOf(number));
        while (numberString.length() < length) {
            numberString.insert(0, "0");
        }
        return numberString.toString();
    }

}