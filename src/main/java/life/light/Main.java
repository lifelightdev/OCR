package life.light;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static life.light.Constant.*;
import static life.light.FileTool.deleteAllFiles;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger();


    public static void main(String[] args) {
        createWorkDirectory();
        emptyWorkDirectory();
        String pathPDF = "";
        String pathInstallTesseractOCRDirectoryTessdata = "";
        if (args.length == 2) {
            pathPDF = args[0];
            pathInstallTesseractOCRDirectoryTessdata = args[1];
        }
        extractPDFToTIFF(new File(pathPDF));
        extractTIFFToTXT(pathInstallTesseractOCRDirectoryTessdata);

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

    public static void extractTIFFToTXT(String pathInstallTesseractOCRDirectoryTessdata) {
        File dossier = new File(Constant.TEMP + File.separator + Constant.TIFF);
        File[] fichiers = dossier.listFiles();
        if (fichiers != null) {
            for (File tiffImage : fichiers) {
                if (tiffImage.getName().endsWith("." + Constant.TIFF)) {
                    try {
                        Tesseract tesseract = new Tesseract();
                        tesseract.setDatapath(pathInstallTesseractOCRDirectoryTessdata);
                        tesseract.setLanguage(FRANCE_CODE_ISO_639_3);
                        tesseract.setVariable("tessedit_pageseg_mode", SPARSE_TEXT);
                        tesseract.setOcrEngineMode(TESSERACT_LSTM);
                        String contenu = tesseract.doOCR(tiffImage);
                        System.out.println(contenu);
                        String nomFichier = tiffImage.getName().replace("." + Constant.TIFF, "") + "." + Constant.TXT;
                        try (FileWriter writer = new FileWriter(Constant.TEMP + File.separator + Constant.TXT + File.separator + nomFichier)) {
                            writer.write(contenu + "\n");
                            LOGGER.info("La chaîne de caractères a été écrite dans le fichier : {}", nomFichier);
                        } catch (IOException e) {
                            LOGGER.error("Erreur lors de l'écriture dans le fichier : {}", e.getMessage());
                        }
                    } catch (TesseractException e) {
                        LOGGER.error("Erreur lors de l'OCR : {}", e.getMessage());
                    } catch (Exception e) {
                        LOGGER.error("Erreur : {}", e.getMessage());
                    }
                }
            }
        }
    }

    public static void concatenationTextFiles() {
        File dossier = new File(Constant.TEMP + File.separator + Constant.TXT);
        File[] fichiers = dossier.listFiles();

        String fichierSortie = Constant.TEMP + File.separator + FICHIER_FUSIONNER;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierSortie))) {
            assert fichiers != null;
            for (File fichierEntree : fichiers) {
                try (BufferedReader reader = new BufferedReader(new FileReader(fichierEntree))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line += "\n";
                        writer.write(line);
                    }
                    LOGGER.info("Contenu de '{}' a été ajouté à '{}'.", fichierEntree, fichierSortie);
                } catch (IOException e) {
                    LOGGER.error("Erreur lors de la lecture du fichier '{}': {}", fichierEntree, e.getMessage());
                }
            }
            LOGGER.info("La concaténation des fichiers est terminée dans '{}'.", fichierSortie);
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'écriture dans le fichier de sortie '{}': {}", fichierSortie, e.getMessage());
        }

    }
}