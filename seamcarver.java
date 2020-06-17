import java.io.IOException;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class seamcarver{
	static String inputimage;
	static String nomimage;
	static BufferedImage image;
	static final int Infini = Integer.MAX_VALUE;
	static int[][] M;
	static int[][] matriceVerticale;
	static int[][] matriceHorizontale;
	static int[][] imageSeamVertical;
	static int[][] imageSeamHorizontal;
	static int L = 0;
	static int C = 0;
	static int horizontal = 0;
	static int vertical = 0;

	public static void main(String[] args) throws IOException {
		if (inputimage == null) inputimage = args[0];			// affectation image premier parametre
		if (vertical == 0) vertical = Integer.parseInt(args[2]);	// affectation pourcentage horizontal premier parametre
		if (horizontal == 0) horizontal = Integer.parseInt(args[1]);		// affectation pourcentage vertical deuxieme parametre
		if(inputimage == null || horizontal >= 100 || vertical >= 100) {	// test integrite des parametres
			System.out.println("debug : erreur syntax");
			return;
		}

		int positionPointFormat = inputimage.lastIndexOf('.');
		nomimage = inputimage.substring(0, positionPointFormat);

		ouvrirImage();				// ouverture de l'image passee en parametre
		BufferedImage newImage = image;		// creation d'une nouvelle image identique a l'originale

		int horizontalCarve = (horizontal * image.getHeight()) / 100;	// conversion pourcentage pixels horizontal
		int verticalCarve = (vertical * image.getWidth()) / 100;		// conversion pourcentage pixels vertical
		System.out.println("debug : running");
		
		while(verticalCarve > 0) {					// boucle decoupage vertical
	        newImage = verticalCarving(newImage);
	        afficherSeam(imageSeamVertical);
	        ecrireImage(newImage);
			verticalCarve--;
		}
		System.out.println("debug : vertical carving done");

		while(horizontalCarve > 0) {					// boucle decoupage horizontal
	        newImage = horizontalCarving(newImage);
	        afficherSeam(imageSeamHorizontal);
	        ecrireImage(newImage);
	        horizontalCarve--;
		}
		System.out.println("debug : horizontal carving done");
		System.out.println("debug : done");
	}

	/*
	 * Fonction de decoupage vertical de l'image passee en parametre
	 */
	private static BufferedImage verticalCarving(BufferedImage newImage) throws IOException {
		calculerM(newImage);				// conversion de l'image en matrice de pixels
		M = calculerEnergie();				// reafectation de la matrice de pixels en matrice d'energie
		calculerMatriceCoutVerticale();			// calcul de la matrice des couts minimums verticaux
	    calculerCheminVertical();			// calcul du chemin vertical de cout minimal
    	newImage = enleverSeamVertical(newImage);	// suppresion du chemin vertical de cout minimal
		return newImage;
	}

	/*
	 * Fonction de decoupage horizontal de l'image passee en parametre
	 */
	private static BufferedImage horizontalCarving(BufferedImage newImage) throws IOException {
		calculerM(newImage);				// conversion de l'image en matrice de pixels
		M = calculerEnergie();				// reafectation de la matrice de pixels en matrice d'energie
		calculerMatriceCoutHorizontale();		// calcul de la matrice des couts minimums horizontaux
    	calculerCheminHorizontal();			// calcul du chemin horizontal de cout minimal
    	newImage = enleverSeamHorizontal(newImage); 	// suppresion du chemin vertical de cout minimal
		return newImage;
	}

	/*
	 * Procedure d'ouverture de l'image source
	 */
	private static void ouvrirImage() throws IOException {
        try {
        	image = ImageIO.read(new File(inputimage));
        }
        catch(IOException e) {
           		System.err.println("debug : impossible d'ouvrir l'image : " + e);
           		return;
        }
	}

	/*
	 * Procedure d'ecriture de la nouvelle image reduite
	 */
	private static void ecrireImage(final BufferedImage newImage) throws IOException {
		try {
			ImageIO.write(newImage, "png", new File(nomimage + "_resized_" +  horizontal + "_"  + vertical + ".png"));
		}
		catch (IOException e) {
			System.err.println("debug : impossible d'ecrire l'image : " + e);
			return;
		}
	}

	/*
	 * Procedure d'affichage du seam de l'image traitee
	 */ 
	private static void afficherSeam(int[][] array) throws IOException {
        BufferedImage image = new BufferedImage(array[0].length, array.length, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < array[0].length; x++) {
            for (int y = 0; y < array.length; y++) {
                image.setRGB(x, y, array[y][x]);
            }
        }
        
        try {
        ImageIO.write(image, "png", new File(nomimage + "_seam_" +  horizontal + "_"  + vertical + ".png"));
        }
		catch (IOException e) {
			System.err.println("debug : impossible d'ecrire le seam : " + e);
			return;
		}
    }
	
	/*
	 * Procedure de conversion de l'image en matrice de pixels
	 * On calcule la valeur d'un pixel en calculant la différence des pixels qui l'entoure
	 */
	private static void calculerM(final BufferedImage image) {
		M = new int[image.getHeight()][image.getWidth()];	// initialisation de la matrice
		L = M.length;						// nombre de lignes max
		C = M[0].length;					// nombre de colonnes max

		for (int l = 0; l < L; l++) {
			for (int c = 0; c < C; c++) {
				int x1, x2, y1, y2;
				if(l == 0){							// Cas ou on est sur la bordure gauche de l'image
					y1 = image.getRGB(c, l);
					y2 = image.getRGB(c, l + 1);
				}
				else if(l == L - 1){				// Cas ou on est sur la bordure droite de l'image
					y1 = image.getRGB(c, l - 1);
					y2 = image.getRGB(c, l);
				}
				else{								// Cas ou on est sur l'image en l
					y1 = image.getRGB(c, l - 1);
					y2 = image.getRGB(c, l + 1);
				}
				if(c == 0){							// Cas ou on est sur la bordure inferieure de l'image
					x1 = image.getRGB(c, l);
					x2 = image.getRGB(c + 1, l);
				}
				else if(c == C - 1){				// Cas ou on est sur la bordure superieure de l'image
					x1 = image.getRGB(c - 1, l);
					x2 = image.getRGB(c, l);
				}
				else{								// Cas ou on est sur l'image en c
					x1 = image.getRGB(c - 1, l);
					x2 = image.getRGB(c + 1, l);
				}

				M[l][c] = Math.abs(x1 - x2) + Math.abs(y1 - y2);
			}
		}
	}

	/*
	 * Fonction de calcul de l'energie a partir de la matrice de pixels
	 * On calcule l'energie d'un pixel en faisant la différence d'energie avec le pixel qui le pixel
	 */
	private static int[][] calculerEnergie() {
        int[][] energie = new int[L][C];
        int deltal = 0, deltac = 0;
        for (int l = 0; l < L; l++) {
            for (int c = 0; c < C; c++) {
                if (l != 0) {
                    deltal = deltaRGB(M[l][c], M[l - 1][c]);
                }
                if (c != 0) {
                    deltac = deltaRGB(M[l][c], M[l][c - 1]);
                }
                energie[l][c] = deltal + deltac;
            }
        }
		return energie;
	}

	/*
	 * Fonction de calcul de la difference entre deux pixels
	 * On calcul deja la difference entre ces deux pixels, puis entre leurs composantes Rouge, Verte et Bleue.
	 */
	private static int deltaRGB(int pixel, int pixelBefore) {
        int delta = Math.abs(pixel - pixelBefore);
        int Red = Math.abs((pixel & 0xFF0000 >> 16) - (pixelBefore & 0xFF0000 >> 16));
		int Green = Math.abs((pixel & 0xFF00 >> 8) - (pixelBefore & 0xFF00 >> 8));
		int Blue = Math.abs((pixel & 0xFF) - (pixelBefore & 0xFF));
        return delta + Red + Green + Blue;
	}

	/*
	 * Procedure de calcul de la matrice des couts verticaux
	 * calcule M[0:L+1][0:C+1] de terme general M[l][c] = m(l,c),
	 * ou m(l,c) est le cout minimum d'un chemin allant de la case (0,0) a la case (l,c).
	 */
	private static void calculerMatriceCoutVerticale() {
		matriceVerticale = new int[L][C];

		// base de la recurrence :
		for(int c = 0; c < C; c++) {
			matriceVerticale[0][c] = M[0][c];
		}
		// cas generale, 1 <= l < L+1, 0 <= c < C+1 :
		for (int l = 1; l < L; l++) {
			for (int c = 0; c < C; c++) {
				int n = Infini, ne = Infini, no = Infini; 						// Deplacements Nord, Nord-Est, Nord-Ouest initialises a un cout infini
				n = matriceVerticale[l - 1][c] + M[l][c]; 						// Le dernier deplacement Nord existe toujours
				if(c > 0) ne = matriceVerticale[l - 1][c - 1] + M[l][c]; 		// Le dernier deplacement Nord-Est existe si la colonne c-1 existe (bordure gauche)
				if(c < C - 1) no = matriceVerticale[l - 1][c + 1] + M[l][c];	// Le dernier deplacement Nord-Ouest existe si la colonne c+1 existe (bordure droite)
				matriceVerticale[l][c] = Math.min(n, Math.min(no, ne));			// La case (l, c) prend la valeur issue du deplacement de cout minimum
			}
		}
	}

    /*
	 * Procedure de calcul de la matrice des couts horizontaux
	 * calcule M[0:L+1][0:C+1] de terme general M[l][c] = m(l,c),
	 * ou m(l,c) est le cout minimum d'un chemin allant de la case (0,0) a la case (l,c).
	 */
	private static void calculerMatriceCoutHorizontale() {
		matriceHorizontale = new int[L][C];

		// premiere colonne inchangee
		for(int l = 0; l < L; l++) {
			matriceHorizontale[l][0] = M[l][0];
		}
		// cas generale, 0 <= l < L+1, 1 <= c < C+1 :
		for (int c = 1; c < C; c++) {
			for (int l = 0; l < L; l++) {
				int e = Infini, se = Infini, ne = Infini; 						// Deplacements Est, Sud-Est, Nord-Est initialises a un cout infini
				e = matriceHorizontale[l][c-1] + M[l][c]; 						// Le dernier deplacement Est existe toujours
				if(l > 0) ne = matriceHorizontale[l - 1][c - 1] + M[l][c];		// Le dernier deplacement Nord-Est existe si la ligne l-1 existe (bordure inferieure)
				if(l < L - 1) se = matriceHorizontale[l + 1][c - 1] + M[l][c];	// Le dernier deplacement Sud-Est existe si la ligne l+1 existe (bordure superieure)
				matriceHorizontale[l][c] = Math.min(e, Math.min(ne, se));		// La case (l, c) prend la valeur issue du deplacement de cout minimum
			}
		}
	}

	/*
	 * Procedure de calcul du chemin vertical de cout minimum
	 */
	private static void calculerCheminVertical() {
																	// recherche du chemin de cout minimum en partant de la derniere ligne L de la matrice.
		imageSeamVertical = M;										// Nouvelle matrice egale a la matrice M d'energie.
		int cMin = 0, valeurMin = matriceVerticale[L-1][cMin];		// On part du principe que la case (l,0) a le cout le plus faible.
		for(int c = 0; c < C; c++) {
			if(matriceVerticale[L-1][c] < valeurMin) {				// Si la case (l, c+1) < (l, c), alors (l, c) est la nouvelle case de cout le plus faible
				valeurMin = matriceVerticale[L - 1][c];
				cMin = c;
			}
		}
		imageSeamVertical[L-1][cMin] = Infini;						// On etablit la case (l, c) minimale comme case du Seam vertical en changeant sa valeur par l'Infini.

		// cas generale, L - 2 > l > 0:
		for (int l = L - 2; l >= 0; l--) {
			int n = Infini, ne = Infini, no = Infini;				// Deplacements Nord, Nord-Est, Nord-Ouest initialises a un cout infini
			n = matriceVerticale[l][cMin];							// Le dernier deplacement Nord existe toujours
			if(cMin > 0) ne = matriceVerticale[l][cMin - 1];		// Le dernier deplacement Nord-Est existe si la colonne c-1 existe (bordure gauche)
			if(cMin < C - 1) no = matriceVerticale[l][cMin + 1];	// Le dernier deplacement Nord-Ouest existe si la colonne c+1 existe (bordure droite)

			valeurMin = Math.min(n, Math.min(no, ne));				// La case (l, c) prend la valeur issue du deplacement de cout minimum

			if(valeurMin == ne) cMin--;								// si la valeur minimale correspond a un deplacement Nord-Est, alors on affecte la future position de cout minimum (l-1, c-1)
			else if(ne == no) cMin++;								// si la valeur minimale correspond a un deplacement Nord-Ouest, alors on affecte la future position de cout minimum (l-1, c+1)
																	// sinon (Nord), on affecte la future position de cout minimum (l-1, c)
			imageSeamVertical[l][cMin] = Infini;					// On etablit la case (l, c) minimale comme case du Seam vertical en changeant sa valeur par l'Infini.
		}
	}

	/*
	 * Procedure de calcul du chemin horizontal de cout minimum
	 */
	private static void calculerCheminHorizontal() {
																	// recherche du chemin de cout minimum en partant de la derniere colonne L de la matrice.
		imageSeamHorizontal = M;									// Nouvelle matrice egale a la matrice M d'energie.
		int lMin = L-1, valeurMin = matriceHorizontale[lMin][C-1];	// On part du principe que la case (L,C) a le cout le plus faible.
		for(int l = L-1; l >= 0; l--) {
			if(matriceHorizontale[l][C-1] < valeurMin) {			// Si la case (L, c-1) < (l, c), alors (l, c) est la nouvelle case de cout le plus faible
				valeurMin = matriceHorizontale[l][C-1];
				lMin = l;
			}
		}
		imageSeamHorizontal[lMin][C-1] = Infini;					// On etablit la case (l, c) minimale comme case du Seam vertical en changeant sa valeur par l'Infini.

		// cas generale, C - 2 > c > 0:
		for (int c = C - 2; c >= 0; c--) {
			int e = Infini, ne = Infini, se = Infini;				// Deplacements Est, Sud-Est, Nord-Est initialises a un cout infini
			e = matriceHorizontale[lMin][c];						// Le dernier deplacement Est existe toujours
			if(lMin > 0) ne = matriceHorizontale[lMin-1][c];		// Le dernier deplacement Nord-Est existe si la ligne l-1 existe (bordure inferieure)
			if(lMin < L - 1) se = matriceHorizontale[lMin+1][c];	// Le dernier deplacement Sud-Est existe si la ligne l+1 existe (bordure superieure)

			valeurMin = Math.min(e, Math.min(ne, se));				// La case (l, c) prend la valeur issue du deplacement de cout minimum

			if(valeurMin == ne) lMin--;								// si la valeur minimale correspond a un deplacement Nord-Est, alors on affecte la future position de cout minimum (l-1, c-1)
			else if(valeurMin == se) lMin++;						// si la valeur minimale correspond a un deplacement Sud-Est, alors on affecte la future position de cout minimum (l+1, c-1)
																	// sinon (Est), on affecte la future position de cout minimum (l, c-1)
			imageSeamHorizontal[lMin][c] = Infini;					// On etablit la case (l, c) minimale comme case du Seam vertical en changeant sa valeur par l'Infini.
		}
	}

	/*
	 * Fonction de supppression du Seam vertical
	 */
	private static BufferedImage enleverSeamVertical(final BufferedImage imageSeam) {
		// Creation d'une nouvelle image aux dimensions de l'image actuelle avec une colonne en moins
		BufferedImage newImage = new BufferedImage(imageSeam.getWidth() - 1, imageSeam.getHeight(), BufferedImage.TYPE_INT_RGB);

		for (int l = 0; l < imageSeam.getHeight(); l++) {					// On parcours l'image a la recherche des pixels de valeur Infini (les Seam definis precedemment).
			boolean decalage = false;										// Boolean utilise pour proceder au decalage des pixels apres avoir recontre le Pixel Seam.
    		for (int c = 0; c < imageSeam.getWidth(); c++) {
    			boolean inSeam = false;										// Boolean utilise pour proceder a la suppresion du Pixel faisant partie du chemin de cout minimal (Seam)
				if(imageSeamVertical[l][c] == Infini) {						// On affecte ces deux dernieres variables a True des qu'on rencontre le Seam
					inSeam = true;
					decalage = true;
				}
				if(!inSeam) {												// Si le pixel est dans le Seam, il est ignore, sinon on procede au decalage ou a l'ecriture normale des pixels de la nouvelle image
					if(decalage) {											// ecriture des pixels en prenant en compte le decalage apres avoir ignore le pixel du Seam
						newImage.setRGB(c - 1, l, imageSeam.getRGB(c, l));	// Le pixel (l, c) de la nouvelle image correspond au pixel (l, c+1) de l'image calculee precedemment
					}
					else {													// si on a pas encore rencontre le Pixel du Seam, alors on ecrit la nouvelle image normalement.
						newImage.setRGB(c, l, imageSeam.getRGB(c, l));
					}
				}
			}
    	}
		return newImage;	// On retourne la nouvelle image fraichement creee a partir de l'image de base a laquelle on a supprime le chemin de pixels de cout minimum
	}

	/*
	 * Fonction de supppression du Seam horizontal
	 */
	private static BufferedImage enleverSeamHorizontal(final BufferedImage imageSeam) {
		// Creation d'une nouvelle image aux dimensions de l'image actuelle avec une ligne en moins
		BufferedImage newImage = new BufferedImage(imageSeam.getWidth(), imageSeam.getHeight() - 1, BufferedImage.TYPE_INT_RGB);
		for (int c = 0; c < imageSeam.getWidth(); c++) {					// On parcours l'image a la recherche des pixels de valeur Infini (les Seam definis precedemment).
			boolean decalage = false;										// Boolean utilise pour proceder au decalage des pixels apres avoir recontre le Pixel Seam.
			for (int l = 0; l < imageSeam.getHeight(); l++) {
    			boolean inSeam = false;										// Boolean utilise pour proceder a la suppresion du Pixel faisant partie du chemin de cout minimal (Seam)
				if(imageSeamHorizontal[l][c] == Infini) {					// On affecte ces deux dernieres variables a True des qu'on rencontre le Seam
					inSeam = true;
					decalage = true;
				}
				if(!inSeam) {												// Si le pixel est dans le Seam, il est ignore, sinon on procede au decalage ou a l'ecriture normale des pixels de la nouvelle image
					if(decalage) {											// ecriture des pixels en prenant en compte le decalage apres avoir ignore le pixel du Seam
						newImage.setRGB(c, l-1, imageSeam.getRGB(c, l));	// Le pixel (l, c) de la nouvelle image correspond au pixel (l+1, c) de l'image calculee precedemment
					}
					else {													// si on a pas encore rencontre le Pixel du Seam, alors on ecrit la nouvelle image normalement.
						newImage.setRGB(c, l, imageSeam.getRGB(c, l));
					}
				}
			}
    	}
		return newImage;	// On retourne la nouvelle image fraichement creee a partir de l'image de base a laquelle on a supprime le chemin de pixels de cout minimum
	}
}
