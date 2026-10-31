package esgi;

import static esgi.Util.*;

public class TP1 {
    public static void main(String[] args) {
        exercice1();
    }

    private static void exercice1() {

        afficher("Bonjour");
        nouvelleLigne();

        afficher("Mon int : ");
        int monInt = lireInt();

        afficher("mon int : " + monInt);

    }
}
