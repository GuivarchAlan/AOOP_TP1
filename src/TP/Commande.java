package TP;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;


public class Commande implements Serializable {

    public enum typeMessage {
        compilation,
        chargement,
        creation,
        lecture,
        ecriture,
        fonction
    };
    public typeMessage type;

    // Message de Compilation : type et
    public ArrayList<String> pathSource = new ArrayList<String>();

    public String pathClasses;
    // Message de chargement : type et
    public String className = "";

    //Message de creation : type, ClassName et
    public String Id = "";

    //Message de lecture : type, Id et
    public String attributeName = "";

    // Message d'ecriture : type, Id, attributeName et
    public String value = "";

    // Message fonction : type, Id et
    public String functionName = "";

    public ArrayList<String> parameterType = new ArrayList<String>();

    public ArrayList<String> parameterValue = new ArrayList<String>();

    public Boolean result = false;

    public Object resultObject = null;

    public Commande(String command) {
        String[] parameters = command.split("#");
        // suivant le type de message la décomposition du reste du message se fera différement
        switch (parameters[0]) {
            case("compilation") :
                type = typeMessage.compilation;
                String[] Sources = parameters[1].split(",");
                for (int i = 0; i < Sources.length; i ++) {
                    pathSource.add(Sources[i]);
                }
                pathClasses = parameters[2];
                break;
            case("chargement") :
                type = typeMessage.chargement;
                className = parameters[1];
                break;
            case("creation") :
                type = typeMessage.creation;
                className = parameters[1];
                Id = parameters[2];
                break;
            case("lecture") :
                type = typeMessage.lecture;
                Id = parameters[1];
                attributeName = parameters[2];
                break;
            case("ecriture") :
                type = typeMessage.ecriture;
                Id = parameters[1];
                attributeName = parameters[2];
                value = parameters[3];
                break;
            case("fonction") :
                type = typeMessage.fonction;
                Id = parameters[1];
                functionName = parameters[2];
                // si la fonction à des parametres
                if (parameters.length > 3) {
                    String[] functionParameters = parameters[3].split(",");
                    for (int i = 0; i < functionParameters.length; i++) {
                        String[] param = functionParameters[i].split(":");
                        this.parameterType.add(param[0]);
                        this.parameterValue.add(param[1]);
                    }
                }
                break;
            default:
                break;
        }
    }
}
