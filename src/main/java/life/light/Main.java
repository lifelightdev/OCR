package life.light;

import net.sourceforge.tess4j.TessAPI1;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static life.light.Constant.*;
import static life.light.FileTool.deleteAllFiles;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {

        LocalDateTime debut = LocalDateTime.now();
        LOGGER.info("Début à {}:{}:{}", debut.getHour(), debut.getMinute(), debut.getSecond());

        String pathPDF = "";
        String pathInstallTesseractOCRDirectoryTessdata = "";
        String etape = Step.ALL.name();
        if (args.length == 3) {
            pathPDF = args[0];
            pathInstallTesseractOCRDirectoryTessdata = args[1];
            etape = args[2];
        }
        if (etape.equals(Step.ALL.name())) {
            createWorkDirectory();
            emptyWorkDirectory();
        }
        if (etape.equals(Step.ALL.name()) || (etape.equals(Step.ONE.name()))) {
            deleteAllFiles(new File(Constant.TEMP + File.separator + Constant.TIFF));
            extractPDFToTIFF(new File(pathPDF));
        }
        if (etape.equals(Step.ALL.name()) || (etape.equals(Step.TWO.name()))) {
            deleteAllFiles(new File(Constant.TEMP + File.separator + Constant.TXT));
            extractTIFFToTXT(pathInstallTesseractOCRDirectoryTessdata);
        }
        if (etape.equals(Step.ALL.name()) || (etape.equals(Step.THREE.name()))) {
            deleteAllFiles(new File(Constant.TEMP));
            concatenationTextFiles();
        }

        LocalDateTime fin = LocalDateTime.now();
        LOGGER.info("Fin à {}:{}:{}", fin.getHour(), fin.getMinute(), fin.getSecond());
        LOGGER.info("La durée du traitement est de {} minutes", ChronoUnit.MINUTES.between(debut, fin));
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
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, DPI, ImageType.BINARY);
                String outputFilePath = Constant.TEMP + File.separator + Constant.TIFF + File.separator
                        + "page-" + addZeros(page + 1) + "." + Constant.TIFF;
                File outputFile = new File(outputFilePath);
                ImageIO.write(bim, Constant.TIFF,outputFile);
                //LOGGER.info("Conversion de la page dans le fichier : {}", outputFilePath);
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
            List<File> items = Arrays.asList(fichiers);
            items.parallelStream().forEach(item -> {
                if (item.getName().endsWith("." + Constant.TIFF)) {
                    try {
                        Tesseract tesseract = new Tesseract();
                        tesseract.setDatapath(pathInstallTesseractOCRDirectoryTessdata);
                        tesseract.setLanguage(FRANCE_CODE_ISO_639_3);
                        tesseract.setVariable("preserve_interword_spaces", "TRUE");
                        /* Configuration du Page Segmentation Mode (PSM)
                         0 PSM_OSD_ONLY : Détecter uniquement l'orientation et le script.
                         1 PSM_AUTO_OSD: Orientation et script automatiques.
                         2 PSM_AUTO_ONLY: Segmentation automatique sans OSD ni orientation.
                         3 PSM_AUTO: Segmentation automatique de page avec OSD.
                         4 PSM_SINGLE_COLUMN: Traiter la page comme une seule colonne de texte de tailles variables.
                         5 PSM_SINGLE_BLOCK_VERT_TEXT: Traiter la page comme un seul bloc de texte vertical.
                         7 PSM_SINGLE_LINE: Traiter l'image comme une seule ligne de texte.
                         8 PSM_SINGLE_WORD: Traiter l'image comme un seul mot.
                         9 PSM_CIRCLE_WORD: Traiter l'image comme un seul mot dans un cercle.
                        10 PSM_SINGLE_CHAR: Traiter l'image comme un seul caractère.
                        11 PSM_SPARSE_TEXT: Rechercher autant de texte que possible dans un ordre variable.
                        12 PSM_SPARSE_TEXT_OSD: Avec OSD.
                        13 PSM_RAW_LINE: Traiter la page comme une seule ligne de texte, en ignorant tout le reste.
                        14 PSM_COUNT: Valeur interne, ne pas utiliser.
                        */
                        tesseract.setPageSegMode(TessAPI1.TessPageSegMode.PSM_SINGLE_COLUMN);
                        String contenu = tesseract.doOCR(item);
                        contenu = getString(contenu);
                        //LOGGER.info(contenu);
                        String nomFichier = item.getName().replace("." + Constant.TIFF, "") + "." + Constant.TXT;
                        try (FileWriter writer = new FileWriter(Constant.TEMP + File.separator + Constant.TXT + File.separator + nomFichier)) {
                            writer.write(contenu + "\n");
                            //LOGGER.info("La page a été écrite dans le fichier : {}", nomFichier);
                        } catch (IOException e) {
                            LOGGER.error("Erreur lors de l'écriture dans le fichier : {}", e.getMessage());
                        }
                    } catch (TesseractException e) {
                        LOGGER.error("Erreur lors de l'OCR : {}", e.getMessage());
                    } catch (Exception e) {
                        LOGGER.error("Erreur : {}", e.getMessage());
                    }
                }
                //LOGGER.info("Traitement de : {} par le thread : {}", item.getName(), Thread.currentThread().getName());
            });
            LOGGER.info("Traitement parallèle terminé.");
        }
    }

    private static String getString(String contenu) {
        contenu = contenu.replace("‘", "");
        contenu = contenu.replace("_", "");
        contenu = contenu.replace("—", "");
        contenu = contenu.replace("|", "");
        contenu = contenu.replace("=", "");
        contenu = contenu.replace("…", "");
        contenu = contenu.replace("=—", "");
        contenu = contenu.replace("\\", "");
        contenu = contenu.replace(" . ", "");
        return contenu;
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
                    //LOGGER.info("Contenu de '{}' a été ajouté à '{}'.", fichierEntree, fichierSortie);
                } catch (IOException e) {
                    LOGGER.error("Erreur lors de la lecture du fichier '{}': {}", fichierEntree, e.getMessage());
                }
            }
            //LOGGER.info("La concaténation des fichiers est terminée dans '{}'.", fichierSortie);
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'écriture dans le fichier de sortie '{}': {}", fichierSortie, e.getMessage());
        }
    }
}