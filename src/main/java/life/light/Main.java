package life.light;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static life.light.FileTool.deleteAllFiles;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PYTHON_SCRIPT_PATH = "easyocr_script.py";
    private static final String TEMP = "temp";
    private static final String PNG = "PNG";
    private static final String TXT = "TXT";

    public static void main(String[] args) {

        LocalDateTime debut = LocalDateTime.now();
        LOGGER.info("Début à {}:{}:{}", debut.getHour(), debut.getMinute(), debut.getSecond());

        String pathPDF = "grand_livre.pdf";
        createWorkDirectory();
        emptyWorkDirectory();

        List<String> imageFiles = new ArrayList<>();
        Main main = new Main();
        try {
            imageFiles = main.convertPDFToImages(pathPDF);
            LOGGER.info("{} imges ont été créées.", imageFiles.size());
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la génération des images {}", e.getMessage());
        }

        try {
            main.createEasyOCRScript();
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la création du scripte Python {}", e.getMessage());
        }

        for (int i = 0; i < imageFiles.size(); i++) {
            System.out.println("Traitement de la page " + (i + 1) + "/" + imageFiles.size());
            try {
                main.processImageWithEasyOCR(imageFiles.get(i));
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Erreur lors l'utilisation de l'OCR {}", e.getMessage());
            }
        }

        concatenationTextFiles(pathPDF);

        LocalDateTime fin = LocalDateTime.now();
        LOGGER.info("Fin à {}:{}:{}", fin.getHour(), fin.getMinute(), fin.getSecond());
        LOGGER.info("La durée du traitement est de {} minutes", ChronoUnit.MINUTES.between(debut, fin));
    }

    public static void createWorkDirectory() {
        FileTool.createDirectory(TEMP);
        FileTool.createDirectory(TEMP + File.separator + PNG);
        FileTool.createDirectory(TEMP + File.separator + TXT);
    }

    public static void emptyWorkDirectory() {
        deleteAllFiles(new File(TEMP));
        deleteAllFiles(new File(TEMP + File.separator + PNG));
        deleteAllFiles(new File(TEMP + File.separator + TXT));
    }

    private List<String> convertPDFToImages(String pdfPath) throws IOException {
        List<String> imageFiles = new ArrayList<>();

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                // Rendre la page en image avec une résolution élevée (300 DPI)
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                // Sauvegarder l'image
                String imageFileName = TEMP + File.separator + PNG + File.separator + "page_"
                        + addZeros(page + 1) + "." + PNG;
                ImageIO.write(bim, PNG, new File(imageFileName));
                imageFiles.add(imageFileName);
            }
        }
        return imageFiles;
    }

    private static String addZeros(int number) {
        int length = 3;
        StringBuilder numberString = new StringBuilder(String.valueOf(number));
        while (numberString.length() < length) {
            numberString.insert(0, "0");
        }
        return numberString.toString();
    }

    public static void concatenationTextFiles(String pathPDF) {
        File dossier = new File(TEMP + File.separator + TXT);
        File[] fichiers = dossier.listFiles();

        String fichierSortie = TEMP + File.separator + pathPDF.replace(".pdf", ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierSortie))) {
            assert fichiers != null;
            for (File fichierEntree : fichiers) {
                try (BufferedReader reader = new BufferedReader(new FileReader(fichierEntree))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("[")){
                            line = line.substring(1, line.length() - 1);
                        }
                        String[] splitLine = line.split(" ");
                        for (String s : splitLine) {
                            if (s.trim().isEmpty()) {
                                line = line.replace(s, "");
                            }
                        }
                        line += "\n";
                        writer.write(line);
                    }
                } catch (IOException e) {
                    LOGGER.error("Erreur lors de la lecture du fichier '{}': {}", fichierEntree, e.getMessage());
                }
            }
            LOGGER.info("La concaténation des fichiers est terminée dans '{}'.", fichierSortie);
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'écriture dans le fichier de sortie '{}': {}", fichierSortie, e.getMessage());
        }
    }

    private void processImageWithEasyOCR(String imagePath) throws IOException, InterruptedException {
        LocalDateTime debut = LocalDateTime.now();
        LOGGER.info("Début du traitement de la page {} à {}:{}:{}", imagePath, debut.getHour(), debut.getMinute(), debut.getSecond());
        ProcessBuilder pb = new ProcessBuilder("python", PYTHON_SCRIPT_PATH, imagePath, TEMP + File.separator + TXT);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        // Lire la sortie du processus Python
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                LOGGER.info("LOG Python = {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Erreur lors de l'exécution du script Python. Code de sortie: " + exitCode + "\n" + output);
        }
        LocalDateTime fin = LocalDateTime.now();
        LOGGER.info("Fin du traitement de la page {} à {}:{}:{}", imagePath, fin.getHour(), fin.getMinute(), fin.getSecond());
        LOGGER.info("La durée du traitement de la page {} est de {} minutes", imagePath, ChronoUnit.MINUTES.between(debut, fin));
    }

    private void createEasyOCRScript() throws IOException {
        String pythonScript = """
                import easyocr
                import cv2
                import sys
                import os
                from pathlib import Path
                
                
                class PDFTableExtractorOCR:
                    def __init__(self, langues=['fr']):
                        # print("Initialisation d'EasyOCR...")
                        self.reader = easyocr.Reader(langues, gpu=False)  # gpu=True si vous avez CUDA
                        # print("EasyOCR initialise")
                
                    def detecter_tableaux_dans_image(self, image):
                        # Convertir en niveaux de gris
                        gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
                        # Détecter les lignes horizontales et verticales
                        horizontal_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (40, 1))
                        vertical_kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (1, 40))
                        # Détecter les lignes horizontales
                        horizontal_lines = cv2.morphologyEx(gray, cv2.MORPH_OPEN, horizontal_kernel)
                        # Détecter les lignes verticales
                        vertical_lines = cv2.morphologyEx(gray, cv2.MORPH_OPEN, vertical_kernel)
                        # Combiner les lignes pour former la structure du tableau
                        table_structure = cv2.addWeighted(horizontal_lines, 0.5, vertical_lines, 0.5, 0.0)
                        # Trouver les contours des tableaux
                        contours, _ = cv2.findContours(table_structure, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                
                        # Filtrer les contours pour ne garder que les grands rectangles
                        table_areas = []
                        for contour in contours:
                            x, y, w, h = cv2.boundingRect(contour)
                            # Filtrer par taille minimale
                            if w > 200 and h > 100:
                                table_areas.append((x, y, x + w, y + h))
                
                        return table_areas
                
                    def extraire_texte_ocr(self, image, zone=None):
                        if zone:
                            x1, y1, x2, y2 = zone
                            image_crop = image[y1:y2, x1:x2]
                        else:
                            image_crop = image
                
                        # Prétraitement de l'image pour améliorer l'OCR
                        image_crop = self.pretraiter_image(image_crop)
                
                        # Extraction du texte avec EasyOCR
                        resultats = self.reader.readtext(image_crop)
                
                        return resultats
                
                    def pretraiter_image(self, image):
                        # Convertir en niveaux de gris si nécessaire
                        if len(image.shape) == 3:
                            gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
                        else:
                            gray = image
                
                        # Améliorer le contraste
                        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
                        enhanced = clahe.apply(gray)
                
                        # Débruitage
                        denoised = cv2.fastNlMeansDenoising(enhanced)
                
                        # Binarisation adaptative
                        binary = cv2.adaptiveThreshold(denoised, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                                       cv2.THRESH_BINARY, 11, 2)
                
                        return binary
                
                    def organiser_texte_en_tableau(self, resultats_ocr, largeur_image):
                        if not resultats_ocr:
                            return []
                
                        # Extraire les coordonnées et le texte
                        elements = []
                        for resultat in resultats_ocr:
                            bbox, texte, confiance = resultat
                            if confiance > 0.5:  # Filtrer par confiance
                                # Calculer le centre de la boîte
                                x_centre = sum([point[0] for point in bbox]) / 4
                                y_centre = sum([point[1] for point in bbox]) / 4
                                elements.append({
                                    'texte': texte.strip(),
                                    'x': x_centre,
                                    'y': y_centre,
                                    'confiance': confiance
                                })
                
                        if not elements:
                            return []
                
                        # Trier par position Y (lignes)
                        elements.sort(key=lambda x: x['y'])
                
                        # Grouper en lignes (éléments avec Y similaire)
                        lignes = []
                        ligne_actuelle = []
                        y_precedent = elements[0]['y']
                        tolerance_y = 20  # Tolérance pour considérer les éléments sur la même ligne
                
                        for element in elements:
                            if abs(element['y'] - y_precedent) <= tolerance_y:
                                ligne_actuelle.append(element)
                            else:
                                if ligne_actuelle:
                                    # Trier la ligne par position X (colonnes)
                                    ligne_actuelle.sort(key=lambda x: x['x'])
                                    lignes.append(ligne_actuelle)
                                ligne_actuelle = [element]
                                y_precedent = element['y']
                
                        # Ajouter la dernière ligne
                        if ligne_actuelle:
                            ligne_actuelle.sort(key=lambda x: x['x'])
                            lignes.append(ligne_actuelle)
                
                        # Convertir en tableau de texte
                        tableau = []
                        for ligne in lignes:
                            ligne_texte = [elem['texte'] for elem in ligne]
                            tableau.append(ligne_texte)
                
                        return tableau
                
                    def extraire_tableaux_pdf(self, chemin_pdf, chemin_sortie=None, format_sortie='txt'):
                        if not os.path.exists(chemin_pdf):
                            print(f"Erreur : Le fichier {chemin_pdf} n'existe pas.")
                            return []
                
                        if chemin_sortie is None:
                            chemin_sortie = os.path.dirname(chemin_pdf)
                
                        os.makedirs(chemin_sortie, exist_ok=True)
                        nom_fichier = Path(chemin_pdf).stem
                        fichiers_crees = []
                
                        try:
                            # Convertir PDF en images
                            image = cv2.imread(chemin_pdf)
                
                            # Détecter les zones de tableaux
                            zones_tableaux = self.detecter_tableaux_dans_image(image)
                
                            if not zones_tableaux:
                                print(f"Aucun tableau détecté sur la page")
                                # Extraire tout le texte de la page
                                resultats_ocr = self.extraire_texte_ocr(image)
                                if resultats_ocr:
                                        tableau = self.organiser_texte_en_tableau(resultats_ocr, image.shape[1])
                                        if tableau:
                                            fichier = self.sauvegarder_tableau(
                                                tableau, nom_fichier, 1, 1,
                                                chemin_sortie, format_sortie
                                            )
                                            fichiers_crees.append(fichier)
                            else:
                                print(f"{len(zones_tableaux)} zone(s) de tableau detectee(s)")
                
                                # Extraire chaque tableau détecté
                                for num_tableau, zone in enumerate(zones_tableaux, 1):
                                        # print(f"Extraction du tableau {num_tableau}...")
                
                                        # Extraire le texte de la zone du tableau
                                        resultats_ocr = self.extraire_texte_ocr(image, zone)
                
                                        if resultats_ocr:
                                            # Organiser en tableau
                                            tableau = self.organiser_texte_en_tableau(
                                                resultats_ocr, zone[2] - zone[0]
                                            )
                
                                            if tableau:
                                                fichier = self.sauvegarder_tableau(tableau, nom_fichier, chemin_sortie)
                                                fichiers_crees.append(fichier)
                
                            return fichiers_crees
                
                        except Exception as e:
                            print(f"Erreur lors de l'extraction : {str(e)}")
                            return []
                
                    def sauvegarder_tableau(self, tableau, nom_fichier, chemin_sortie):
                        # Égaliser le nombre de colonnes
                        max_colonnes = max(len(ligne) for ligne in tableau) if tableau else 0
                        tableau_normalise = []
                
                        for ligne in tableau:
                            ligne_complete = ligne + [''] * (max_colonnes - len(ligne))
                            tableau_normalise.append(ligne_complete)
                
                        # Nom du fichier
                        nom_sortie = f"{nom_fichier}"
                
                        chemin_fichier = os.path.join(chemin_sortie, f"{nom_sortie}.TXT")
                        with open(chemin_fichier, 'w', encoding='utf-8') as f:
                            for ligne in tableau_normalise:
                                f.write(' | '.join(ligne) + '\\n')
                        # print(f"Tableau sauvegarde : {os.path.basename(chemin_fichier)}")
                        return chemin_fichier
                
                
                # Fonction d'utilisation simple
                def extraire_tableaux_avec_ocr(chemin_pdf, langues=['fr'], chemin_sortie=None,
                                               format_sortie='txt'):
                    extracteur = PDFTableExtractorOCR(langues)
                    return extracteur.extraire_tableaux_pdf(chemin_pdf, chemin_sortie, format_sortie)
                
                
                def main():
                    if len(sys.argv) < 2:
                        print("Usage: python script.py <chemin_pdf> [dossier_sortie] [format] [langues]")
                        print("Formats: txt")
                        print("Langues: fr,en (séparées par des virgules)")
                        print("Exemple: python script.py document.pdf ./sortie txt fr")
                        return
                
                    chemin_pdf = sys.argv[1]
                    chemin_sortie = sys.argv[2] if len(sys.argv) > 2 else None
                    format_sortie = sys.argv[3] if len(sys.argv) > 3 else 'txt'
                    langues_str = sys.argv[4] if len(sys.argv) > 4 else 'fr'
                
                    # Parser les langues
                    langues = [lang.strip() for lang in langues_str.split(',')]
                
                    # print(f"Extraction des tableaux avec OCR...")
                    # print(f"Langues: {langues}")
                    # print(f"Format de sortie: {format_sortie}")
                
                    fichiers_crees = extraire_tableaux_avec_ocr(chemin_pdf, langues, chemin_sortie, format_sortie)
                
                    if fichiers_crees:
                        # print(f"\\n Extraction terminee ! {len(fichiers_crees)} fichier(s) cree(s):")
                        for fichier in fichiers_crees:
                            print(f"  - {os.path.basename(fichier)}")
                    else:
                        print("Aucun tableau extrait. Verifiez que le PDF contient des tableaux lisibles.")
                
                
                if __name__ == "__main__":
                    # Vérification rapide des dépendances
                    try:
                        import io
                
                        # print("Dependances verifiees")
                    except ImportError as e:
                        print(f"Dépendance manquante : {e}")
                        print("Installez avec : pip install easyocr opencv-python PyMuPDF pillow pandas openpyxl")
                
                    if len(sys.argv) > 1:
                        main()
                    else:
                        print("\\nInstallation requise:")
                        print("pip install easyocr opencv-python PyMuPDF pillow pandas openpyxl")
                        print("\\nExemple d'utilisation:")
                        print("python script.py mon_document.pdf")
                        print("python script.py mon_document.pdf ./sortie txt fr")
                """;

        try (FileOutputStream fos = new FileOutputStream(PYTHON_SCRIPT_PATH);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)
        ) {
            writer.write(pythonScript);
        }
    }
}