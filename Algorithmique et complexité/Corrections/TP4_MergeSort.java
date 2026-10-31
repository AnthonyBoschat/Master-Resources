package esgi;

public class TP4_MergeSort {

    public static void main(String[] args) {
        int[] tab = {4, 6, 4, 2, 10, 2, 4, 8, 10, 1, 4};


        int[] tab1 = {0, 0, 0, 0, 5, 0, 0, 0, 0, 0};

        int[] tab2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        int[] tab3 = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};

        int[] tab4 = {2023, 2016, 2014, 2032, 2011, 2022, 2014, 1992, 2014, 1998, 2020, 2028, 2011, 2009, 2017, 2001,
                2028, 2014, 2025};

        int[] tab5 = {14, 15};

        Util.afficherTableau(tab);

        mergeSort(tab, 0, tab.length - 1);

        Util.afficherTableau(tab);
    }

    private static void mergeSort(int[] tab, int indiceDebut, int indiceFin) {
        if (indiceDebut < indiceFin) {

            int indiceMilieu = (indiceDebut + indiceFin) / 2;

            // On éclate en deux le tableau ou la sous-partie du tablea
            mergeSort(tab, indiceDebut, indiceMilieu);
            mergeSort(tab, indiceMilieu + 1, indiceFin);

            // On fusionnera ces deux sous parties en les triant
            fusionner(tab, indiceDebut, indiceMilieu, indiceFin);
        }
    }

    private static void fusionner(int[] tab, int indiceDebut, int indiceMilieu, int indiceFin) {

        int tailleTabGauche = indiceMilieu - indiceDebut + 1;
        int tailleTabDroite = indiceFin - indiceMilieu;

        int[] tabGauche = new int[tailleTabGauche];
        int[] tabDroite = new int[tailleTabDroite];

        for (int i = 0; i < tailleTabGauche; i++) {
            tabGauche[i] = tab[indiceDebut + i];
        }

        for (int i = 0; i < tailleTabDroite; i++) {
            tabDroite[i] = tab[indiceMilieu + 1 + i];
        }

        int k = indiceDebut;

        int indiceGauche = 0;
        int indiceDroite = 0;

        while (indiceDroite < tailleTabDroite && indiceGauche < tailleTabGauche) {

            if (tabGauche[indiceGauche] <= tabDroite[indiceDroite]) {
                tab[k] = tabGauche[indiceGauche];
                indiceGauche++;
            } else {
                tab[k] = tabDroite[indiceDroite];
                indiceDroite++;
            }

            k++;
        }

        while (indiceGauche < tailleTabGauche) {
            tab[k] = tabGauche[indiceGauche];
            indiceGauche++;
            k++;
        }

        while (indiceDroite < tailleTabDroite) {
            tab[k] = tabDroite[indiceDroite];
            indiceDroite++;
            k++;
        }
    }
}
