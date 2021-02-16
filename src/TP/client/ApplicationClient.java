package TP.client;

import TP.Commande;
import TP.server.ServeurInterface;

import java.io.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ApplicationClient {

    BufferedReader commandesReader;
    BufferedWriter sortieWriter;

    /**
    * prend le fichier contenant la liste des commandes, et le charge dans une
    * variable du type Commande qui est retournée
    */
    public Commande saisisCommande(BufferedReader fichier) throws IOException {
        String line = fichier.readLine();
        Commande cmd = null;

        if(line != null) {
            cmd = new Commande(line);
        }

        return cmd;
    }
    /**
     * initialise : ouvre les différents fichiers de lecture et écriture
     */
    public void initialise(String fichCommandes, String fichSortie) {
        try {
            commandesReader = new BufferedReader(new FileReader(fichCommandes));
            sortieWriter = new BufferedWriter(new FileWriter(fichSortie));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * prend une Commande dûment formatée, et la fait exécuter par le serveur. Le résultat de
     * l’exécution est retournée. Si la commande ne retourne pas de résultat, on retourne null.
     * Chaque appel doit ouvrir une connexion, exécuter, et fermer la connexion. Si vous le
     * souhaitez, vous pourriez écrire six fonctions spécialisées, une par type de commande
     * décrit plus haut, qui seront appelées par traiteCommande(Commande uneCommande)
     */
    public Commande traiteCommande(Commande uneCommande, ServeurInterface stub) throws RemoteException {
        return stub.traiteCommande(uneCommande);
    }
    /**
     * cette méthode vous sera fournie plus tard. Elle indiquera la séquence d’étapes à exécuter
     * pour le test. Elle fera des appels successifs à saisisCommande(BufferedReader fichier) et
     * traiteCommande(Commande uneCommande).
     */
    public void scenario(ServeurInterface stub) throws IOException {
        sortieWriter.write("Debut des traitements: \n\r");
        sortieWriter.flush();
        Commande prochaine = saisisCommande(commandesReader);
        while (prochaine != null) {
            sortieWriter.write("\tTraitement de la commande " + prochaine.type.toString() + " ..." +"\n\r");
            sortieWriter.flush();
            Commande resultat = traiteCommande(prochaine,stub);
            sortieWriter.write("\t\tResultat: "+ resultat.result.toString() + " : " + resultat.resultObject + "\n\r");
            sortieWriter.flush();
            prochaine = saisisCommande(commandesReader);
        }
        sortieWriter.write("Fin des traitements" + "\n\r");
        sortieWriter.flush();
    }
    /**
     * programme principal. Prend 4 arguments: 1) “hostname” du serveur, 2) numéro de port,
     * 3) nom fichier commandes, et 4) nom fichier sortie. Cette méthode doit créer une
     * instance de la classe ApplicationClient, l’initialiser, puis exécuter le scénario
     */
    public static void main(String[] args) {

        try {
            if (args.length != 4) {
                System.out.println("il manque des arguments parmit: 1) “hostname” du serveur, 2) numéro de port, 3) nom fichier commandes, et 4) nom fichier sortie");
                System.exit(-1);
            }

            ApplicationClient client = new ApplicationClient();
            client.initialise(args[2],args[3]);
            // Initialisation de la connexion RMI
            Registry registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
            ServeurInterface stub = (ServeurInterface) registry.lookup("ServeurInterface");
            // lance le senario
            client.scenario(stub);

            //String response = stub.helloTo("moi");
            //System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

    }
}
