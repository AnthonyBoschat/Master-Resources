package esgi;

public class TP3 {

    public static void main(String[] args) {
        int cpt = 0;

        System.out.print("Saisir un nombre à deviner :");
        int monNombre = Util.lireInt();

        int nombreOrdi;
        int borneMin = 0;
        int borneMax = 1000;


        do {
            // Version avec de l'aléatoire O(n)
            //nombreOrdi = Util.nombreAleatoire(borneMin, borneMax);

            // Version avec recherche par dichotomie -> 0(log n)
            nombreOrdi = (borneMin + borneMax) / 2;

            System.out.println("L'ordi tente : " + nombreOrdi + " entre " + borneMin + " et " + borneMax);
            cpt++;

            if (nombreOrdi > monNombre) {
                System.out.println("C'est trop grand");
                borneMax = nombreOrdi - 1;
            } else if (nombreOrdi < monNombre) {
                System.out.println("C'est trop petit");
                borneMin = nombreOrdi + 1;
            }

        } while (monNombre != nombreOrdi);

        System.out.println("C'est trouvé en " + cpt + " tentatives");
    }
}
