package TP.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import TP.Commande;

public interface ServeurInterface extends Remote {

    /**
     * prend uneCommande dument formattée, et la traite. Dépendant du type de commande,
     * elle appelle la méthode spécialisée
     */
    public Commande traiteCommande(Commande uneCommande) throws RemoteException;

}
