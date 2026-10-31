package agw;

import java.util.Scanner;

public class Util {

    private static Scanner scanner = new Scanner(System.in);

    public static void afficher(String message) {
        System.out.print(message);
    }

    public static void nouvelleLigne() {
        System.out.println();
    }

    public static String lireString() {
        return scanner.next();
    }

    public static int lireInt() {
        return scanner.nextInt();
    }

    public static int nombreAleatoire(int valeurMin, int valeurMax) {
        return (int) (Math.random() * (valeurMax + 1 - valeurMin)) + valeurMin;
    }

    public static void afficherTableau(int[] tab) {
        afficher("[");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < tab.length; i++) {
            content.append(tab[i]).append(", ");
        }

        // On supprime la dernière virgule
        if (content.length() > 0) {
            content.delete(content.length() - 2, content.length());
        }

        afficher(content.toString() + "]");
        nouvelleLigne();
    }
}
