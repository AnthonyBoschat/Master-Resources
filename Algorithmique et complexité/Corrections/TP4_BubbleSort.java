package esgi;

public class TP4_BubbleSort {

    public static void main(String[] args) {
        int[] tab = {4, 6, 4, 2, 10, 2, 4, 8, 10, 1, 4};


        int[] tab1 = {0, 0, 0, 0, 5, 0, 0, 0, 0, 0};

        int[] tab2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        int[] tab3 = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};

        int[] tab4 = {2023, 2016, 2014, 2032, 2011, 2022, 2014, 1992, 2014, 1998, 2020, 2028, 2011, 2009, 2017, 2001,
                2028, 2014, 2025};

        int[] tab5 = {14, 15};

        bubbleSort(tab);

    }

    private static void bubbleSort(int[] tab) {

        printTab(tab);

        for (int i = 0; i < tab.length - 1; i++) {

            System.out.println("Etape n°" + i);

            for (int j = 0; j < tab.length - 1 - i; j++) {
                printTab(tab);

                System.out.print("On compare : " + tab[j] + " avec " + tab[j + 1]);
                if (tab[j] > tab[j + 1]) {
                    System.out.println(" ---> On permute !!");
                    int temp = tab[j];
                    tab[j] = tab[j + 1];
                    tab[j + 1] = temp;
                } else {
                    System.out.println(" ---> On ne permute pas !!");
                }
            }
        }
    }

    private static void printTab(int[] tab) {
        for (int i = 0; i < tab.length; i++) {
            System.out.print(tab[i] + " ");
        }
        System.out.println();
    }

}
