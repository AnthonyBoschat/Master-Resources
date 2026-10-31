package esgi;

public class TP2 {
    public static void main(String[] args) {

        // exerciceSomme();
        exerciceFibonacci();
    }

    private static void exerciceSomme() {
        System.out.print("Saisir un n : ");
        int n = Util.lireInt();
        System.out.println("Pour n = " + n);

        for (int i = 0; i < n; i++) {
            System.out.print(i + " + ");
        }
        System.out.print(n + " = ");

        int sommeFinale = somme(n);

        System.out.println(sommeFinale);
    }

    private static int somme(int n) {

        // Le cas d'arret si n = 1 alors on ne relance pas récursivement et on retourne 1
        if (n == 1) return 1;

        // Sinon, on relance récursivement en faisant le calcul de n plus la somme des n-1
        return n + somme(n - 1);
    }

    private static void exerciceFibonacci() {
        System.out.print("Saisir un n : ");
        int n = Util.lireInt();
        System.out.println("Pour n = " + n + ", on a fibo(" + n + ") = " + fiboIterative(n));
    }

    private static int fibo(int n) {

        // A vous de jouer
        if (n == 0) return 0;
        if (n == 1) return 1;

        return fibo(n - 1) + fibo(n - 2);
    }

    private static int fiboIterative(int n) {
        int fibo0 = 0;
        int fibo1 = 1;

        if (n == 0) return fibo0;
        if (n == 1) return fibo1;

        int fiboAuRangNmoins1 = fibo1;
        int fiboAuRangNmoins2 = fibo0;

        // Initialisation pour le compilateur
        int fiboAuRangN =-1;

        for (int i = 2; i <= n; i++) {
            fiboAuRangN = fiboAuRangNmoins1 + fiboAuRangNmoins2;

            fiboAuRangNmoins2 = fiboAuRangNmoins1;
            fiboAuRangNmoins1 = fiboAuRangN;
        }

        return fiboAuRangN;
    }

}







