package esgi;

import static esgi.Util.*;

public class TP1 {
    private static int compteur = 0;

    public static void main(String[] args) {
        trouverLesNnombrePremiers();
    }

    private static void exercice1() {

        // sout
        System.out.print("Combien de nombre aléatoire : ");
        int n = lireInt();

        int max = 0;
        int min = 100;

        for (int i = 1; i <= n; i++) {

            int random = nombreAleatoire(0, 100);
            //System.out.println(" nombre n°" + i + " ---> " + random);

            if (random > max) {
                max = random;
            }

            if (random < min) {
                min = random;
            }
        }

        System.out.println("La max est : " + max);
        System.out.println("La min est : " + min);

    }


    private static void exercice2() {

        int nombre = 74286;

        int nombreALenvers = 0;

        while (nombre > 0) {

            int unite = nombre % 10;
            System.out.println("---> " + unite);

            nombreALenvers = nombreALenvers * 10 + unite;

            nombre = nombre / 10;
            //System.out.println(nombre);

        }

        System.out.println("------> mon nombre à l'envers " + nombreALenvers);

    }

    private static void trouverLesNnombrePremiers() {

        System.out.print("Saisir la valeur de n : ");
        int n = lireInt();

        if (n < 2 || n >= 1000) {
            System.out.println("oulala...");
        } else {
            for (int i = 2; i <= n; i++) {

                if (estPremier(i)) {
                    System.out.println("---> " + i + " est premier");
                } else {
                    System.out.println("---> " + i + " n'est pas premier");
                }

            }
        }

        System.out.println("On a fait " + compteur + " de fois l'opération de %");
    }

    private static boolean estPremier(int valeur) {
        // On dit que 1 n'est pas premier
        if (valeur == 1) return false;

        // On dit que 2 est premier
        if (valeur == 2) return true;

        // Si il est paire -> ce n'est pas un nombre premier
        if (valeur % 2 == 0) return false;

        // Il est impaire et plus grande que 2.
        // On test à partir de trois jusqu'à la valeur de racine carré
        // avec un saut de 2 en 2 pour ne tester que les i impaires
        int racineDeValeur = (int) Math.sqrt(valeur);

        for (int i = 3; i <= racineDeValeur; i += 2) {

            // Est-ce que i est un diviseur de la valeur
            if (valeur % i == 0) {
                // Oui -> valeur pas premier, on sort
                return false;
            }
        }

        // On n'a pas trouvé de diviseurs -> il est premier
        return true;
    }

}
