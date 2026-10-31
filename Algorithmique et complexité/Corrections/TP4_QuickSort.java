package esgi;

public class TP4_QuickSort {

    public static void main(String[] args) {
        int[] tab = {4, 6, 4, 2, 10, 2, 4, 8, 10, 1, 4};


        int[] tab1 = {0, 0, 0, 0, 5, 0, 0, 0, 0, 0};

        int[] tab2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        int[] tab3 = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};

        int[] tab4 = {2023, 2016, 2014, 2032, 2011, 2022, 2014, 1992, 2014, 1998, 2020, 2028, 2011, 2009, 2017, 2001,
                2028, 2014, 2025};

        int[] tab5 = {14, 15};

        printTab(tab, -1);
        System.out.println();
        quickSort(tab, 0, tab.length - 1);
        printTab(tab, -1);
    }

    private static void quickSort(int[] tab, int indiceDebut, int indiceFin) {

        // Le pivot est de dernier élément de la partie partie du tableau à trier
        int indicePivot = indiceFin;
        // Le premier indice à comparer est de premier élément de la partie partie du
        // tableau à trier
        int indiceAComparer = indiceDebut;

        System.out.println();
        System.out.println("On tri le tableau entre : " + indiceDebut + " et " + indiceFin + ", le pivot est : "
                + tab[indicePivot]);

        // Tant que les deux indices ne se sont pas re-joint
        while (indiceAComparer < indicePivot) {

            printTab(tab, indicePivot);
            System.out.print(" -> On compare : " + tab[indiceAComparer] + " à " + tab[indicePivot]);

            // On teste si l'élément est plus grand que le pivot
            if (tab[indiceAComparer] > tab[indicePivot]) {
                // Si oui on fait la permutation selon notre règle
                System.out.println(" et on permute !");
                int temp = tab[indiceAComparer];
                tab[indiceAComparer] = tab[indicePivot - 1];
                tab[indicePivot - 1] = tab[indicePivot];
                tab[indicePivot] = temp;

                // Et on décale l'indice du pivot
                indicePivot--;
            } else {
                // Sinon on décalé l'indice de l'élément à comparer
                System.out.println(" et on ne permute pas");
                indiceAComparer++;
            }
        }

        // Affichage
        printTab(tab, indicePivot);
        System.out.println(" -> " + tab[indicePivot] + " est à la place " + indicePivot);

        if (indicePivot - indiceDebut >= 2) {
            System.out.println("On va relancer à gauche entre " + indiceDebut + " et " + (indicePivot - 1));
        } else {
            System.out.println("On ne relance pas à gauche");
        }
        if (indiceFin - indicePivot >= 2) {
            System.out.println("On va relancer à droite entre " + (indicePivot + 1) + " et " + indiceFin);
        } else {
            System.out.println("On ne relance pas à droite");
        }

        // On relance sur la partie de gauche si j'ai au moins 2 éléments à gauche
        if (indicePivot - indiceDebut >= 2) {
            quickSort(tab, indiceDebut, indicePivot - 1);
        }

        // On relance sur la partie de droite si j'ai au moins 2 éléments à droite
        if (indiceFin - indicePivot >= 2) {
            quickSort(tab, indicePivot + 1, indiceFin);
        }
    }

    private static void printTab(int[] tab, int indexPivot) {
        for (int i = 0; i < tab.length; i++) {
            if (indexPivot == i)
                System.out.print("[" + tab[i] + "] ");
            else
                System.out.print(tab[i] + " ");

        }
    }
}
