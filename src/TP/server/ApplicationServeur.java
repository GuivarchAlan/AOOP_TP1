package TP.server;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import TP.Commande;

public class ApplicationServeur implements ServeurInterface{

    String repSource;

    String repClasse;

    String sortieServeur;

    HashMap<String,Object> repObjet;

    HashMap<String,Class<?>> classesCharge;

    ClassLoader clsLoader;

    Object resultObj = null;

    BufferedWriter sortieWriter;
    /**
     * prend le numéro de port, crée un SocketServer sur le port5
     */
    public ApplicationServeur (int port, String repSource,String repClasse, String sortieServeur) throws RemoteException, AlreadyBoundException {

        ServeurInterface stub = (ServeurInterface) UnicastRemoteObject.exportObject(this, port);

        Registry registry = LocateRegistry.createRegistry(port);
        registry.bind("ServeurInterface", stub);

        try {
            sortieWriter = new BufferedWriter(new FileWriter(sortieServeur));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.repSource = repSource;
        this.repClasse = repClasse;
        this.sortieServeur = sortieServeur;
        this.repObjet = new HashMap<String, Object>();
        this.clsLoader = null;
        this.classesCharge = new HashMap<String,Class<?>>();
    }

    /**
     * Se met en attente de connexions des clients. Suite aux connexions, elle lit
     * ce qui est envoyé à travers la Socket, recrée l’objet Commande envoyé par
     * le client, et appellera traiterCommande(Commande uneCommande)
     */
    public void aVosOrdres() throws RemoteException {}
    /**
     * prend uneCommande dument formattée, et la traite. Dépendant du type de commande,
     * elle appelle la méthode spécialisée
     */
    public Commande traiteCommande(Commande uneCommande) {
        Object elem;
        Class<?> cls = null;
        resultObj = null;
        if (!uneCommande.className.contentEquals("")) {
            try {
                cls = Class.forName(uneCommande.className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            sortieWriter.write("\tTraitement de la commande " + uneCommande.type.toString() + " ..." +"\n\r\r");
            sortieWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (uneCommande.type) {
            case compilation:
                try {
                    uneCommande.result = traiterCompilation(uneCommande.pathSource);
                    uneCommande.resultObject = resultObj;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;

            case chargement :
                uneCommande.result = traiterChargement(uneCommande.className);
                uneCommande.resultObject = resultObj;
                break;

            case creation :
                uneCommande.result = traiterCreation(cls,uneCommande.Id);
                uneCommande.resultObject = resultObj;
                break;

            case lecture:
                elem = repObjet.get(uneCommande.Id);
                if (elem == null) {
                    System.out.println("Objet non trouvé");
                    break;
                }
                uneCommande.result = traiterLecture(elem, uneCommande.attributeName);
                uneCommande.resultObject = resultObj;
                break;

            case ecriture:
                elem = repObjet.get(uneCommande.Id);
                if (elem == null) {
                    System.out.println("Objet non trouvé");
                    break;
                }
                uneCommande.result = traiterEcriture(elem,uneCommande.attributeName,uneCommande.value);
                uneCommande.resultObject = resultObj;
                break;
                
            case fonction :
                elem = repObjet.get(uneCommande.Id);
                uneCommande.result = traiterAppel(elem,uneCommande.functionName,uneCommande.parameterType,uneCommande.parameterValue);
                uneCommande.resultObject = resultObj;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + uneCommande.type);
        }

        try {
            sortieWriter.write("\t\tResultat: "+ uneCommande.result.toString() + " : " + uneCommande.resultObject + "\n\r\r");
            sortieWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return uneCommande;
    }
    /**
     * traiterLecture : traite la lecture d’un attribut. Renvoies le résultat par le
     * socket
     */
    public Boolean traiterLecture(Object pointeurObjet, String attribut) {
        try {
            resultObj = pointeurObjet.getClass().getField(attribut).get(pointeurObjet);
            return true;
        }catch (NoSuchFieldException | IllegalAccessException e) {
            try {
                resultObj = pointeurObjet.getClass().getMethod("get" + Character.toUpperCase(attribut.charAt(0)) + attribut.substring(1)).invoke(pointeurObjet);
                return true;
            } catch (NoSuchMethodException noSuchMethodException) {
                noSuchMethodException.printStackTrace();
                resultObj = noSuchMethodException;
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
                resultObj = illegalAccessException;
            } catch (InvocationTargetException invocationTargetException) {
                invocationTargetException.printStackTrace();
                resultObj = invocationTargetException;
            }
        }
        return false;
    }
    /**
     * traiterEcriture : traite l’écriture d’un attribut. Confirmes au client que l’écriture
     * s’est faite correctement.
     */
    public Boolean traiterEcriture(Object pointeurObjet, String attribut, String valeur) {
        Class<?> type = getType(pointeurObjet, attribut);
        if (type == null) {
            System.out.println("Pas de type trouvé pour set la valeur");
            return false;
        }

        Object valeurObjet = null;
        switch (type.getName()) {
            case "int":
                valeurObjet = Integer.parseInt(valeur);
                break;
            case "float":
                valeurObjet =  Float.parseFloat(valeur);
                break;
            case "char":
                valeurObjet = valeur.charAt(0);
                break;
            case "short":
                valeurObjet = Short.parseShort(valeur);
                break;
            case "double":
                valeurObjet = Double.parseDouble(valeur);
                break;
            case "boolean":
                valeurObjet = Boolean.parseBoolean(valeur);
                break;
            case "byte":
                valeurObjet = Byte.parseByte(valeur);
                break;
            case "long":
                valeurObjet = Long.parseLong(valeur);
                break;
            case "java.lang.String":
                valeurObjet = valeur;
                break;
            default:
                try {
                    valeurObjet =  type.getMethod("valueOf", String.class).invoke(null, valeur);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    resultObj = e;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    resultObj = e;
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    resultObj = e;
                }
        }
        try {
            pointeurObjet.getClass().getField(attribut).set(pointeurObjet,valeurObjet);
            resultObj = pointeurObjet.getClass().getField(attribut).get(pointeurObjet);
            return true;

        }catch (NoSuchFieldException | IllegalAccessException e) {
            try {

                pointeurObjet.getClass().getMethod("set" + Character.toUpperCase(attribut.charAt(0)) + attribut.substring(1), type).invoke(pointeurObjet, valeur);
                resultObj = pointeurObjet.getClass().getMethod("get" + Character.toUpperCase(attribut.charAt(0)) + attribut.substring(1)).invoke(pointeurObjet);
                return true;

            } catch (NoSuchMethodException noSuchMethodException) {
                noSuchMethodException.printStackTrace();
                resultObj = noSuchMethodException;
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
                resultObj = illegalAccessException;
            } catch (InvocationTargetException invocationTargetException) {
                invocationTargetException.printStackTrace();
                resultObj = invocationTargetException;
            }
        }
        return false;
    }

    // retourne le type de l'attribut "attribut" de l'objet pointeurObjet si il est possible de le retourner
    public Class<?> getType(Object pointeurObjet, String attribut) {
        try {
            return pointeurObjet.getClass().getField(attribut).getType();
        }catch (NoSuchFieldException e) {
            try {
               return pointeurObjet.getClass().getMethod("get" + Character.toUpperCase(attribut.charAt(0)) + attribut.substring(1)).getReturnType();

            } catch (NoSuchMethodException noSuchMethodException) {
                noSuchMethodException.printStackTrace();
            }
        }
        return null;
    }
    /**
     * traiterCreation : traite la création d’un objet. Confirme au client que la création
     * s’est faite correctement.
     */
    public Boolean traiterCreation(Class classeDeLobjet, String identificateur) {
        try {

            Constructor<?> constructeur = classeDeLobjet.getConstructor();
            Object newObj = constructeur.newInstance();
            repObjet.put(identificateur, newObj);
            resultObj = identificateur + " = " + newObj.toString();
            return true;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            resultObj = e;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            resultObj = e;
        } catch (InstantiationException e) {
            e.printStackTrace();
            resultObj = e;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            resultObj = e;
        }
        return false;
    }
    /**
     * traiterChargement : traite le chargement d’une classe. Confirmes au client que la création
     * s’est faite correctement.
     */
    public Boolean traiterChargement(String nomQualifie) {
        try {
            if (this.clsLoader == null) {
                URL[] urls = new URL[]{new File(repClasse).toURI().toURL()};
                this.clsLoader = new URLClassLoader(urls);
            }

            this.classesCharge.put(nomQualifie,clsLoader.loadClass(nomQualifie));
            resultObj = " classes chargées avec succés ";
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            resultObj = e;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            resultObj = e;
        }
        return false;
    }

    /**
     * traiterCompilation : traite la compilation d’un fichier source java. Confirme au client
     * que la compilation s’est faite correctement. Le fichier source est donné par son chemin
     * relatif par rapport au chemin des fichiers sources.
     * @param cheminRelatifFichierSource
     */
    public Boolean traiterCompilation(ArrayList<String> cheminRelatifFichierSource) throws RemoteException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String[] convert = new String[cheminRelatifFichierSource.size()];
        convert = cheminRelatifFichierSource.toArray(convert);
        resultObj = " classes compilées avec succés ";
       return compiler.run(null,null,null, convert) == 0;
    }
    /**
     * traiterAppel : traite l’appel d’une méthode, en prenant comme argument l’objet
     * sur lequel on effectue l’appel, le nom de la fonction à appeler, un tableau de nom de
     * types des arguments, et un tableau d’arguments pour la fonction. Le résultat de la
     * fonction est renvoyé par le serveur au client (ou le message que tout s’est bien
     * passé)
     **/
     public Boolean traiterAppel(Object pointeurObjet, String nomFonction, ArrayList<String> types, ArrayList<String> valeurs) {
         try {
             ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
             ArrayList<Object> attributs =new ArrayList<Object>();
             for (int i = 0; i < types.size(); i++) {
                 if(valeurs.get(i).startsWith("ID(")  && valeurs.get(i).endsWith(")")) {
                   String id = valeurs.get(i).substring(3,valeurs.get(i).length() - 1);
                   attributs.add(repObjet.get(id));
                   classes.add(classesCharge.get(types.get(i)));
                 }
                 else {
                     switch (types.get(i)) {
                         case "int":
                             classes.add(int.class);
                             attributs.add(Integer.parseInt(valeurs.get(i)));
                             break;
                         case "float":
                             classes.add(float.class);
                             attributs.add(Float.parseFloat(valeurs.get(i)));
                             break;
                         case "char":
                             classes.add(char.class);
                             attributs.add(valeurs.get(i).charAt(0));
                             break;
                         case "short":
                             classes.add(short.class);
                             attributs.add(Short.parseShort(valeurs.get(i)));
                             break;
                         case "double":
                             classes.add(double.class);
                             attributs.add(Double.parseDouble(valeurs.get(i)));
                             break;
                         case "boolean":
                             classes.add(boolean.class);
                             attributs.add(Boolean.parseBoolean(valeurs.get(i)));
                             break;
                         case "byte":
                             classes.add(byte.class);
                             attributs.add(Byte.parseByte(valeurs.get(i)));
                             break;
                         case "long":
                             classes.add(long.class);
                             attributs.add(Long.parseLong(valeurs.get(i)));
                             break;
                         default:
                             try {
                                 classes.add(Class.forName(types.get(i)));
                                 attributs.add(Class.forName(types.get(i)).getMethod("valueOf", String.class).invoke(null, valeurs.get(i)));
                             } catch (ClassNotFoundException e) {
                                 if (classesCharge.get(types.get(i)) != null) {
                                     classes.add(classesCharge.get(types.get(i)));
                                     attributs.add(classesCharge.get(types.get(i)).getMethod("valueOf", String.class).invoke(null, valeurs.get(i)));
                                 }

                             } catch (IllegalAccessException e) {
                                 e.printStackTrace();
                                 resultObj = e;
                             } catch (InvocationTargetException e) {
                                 e.printStackTrace();
                                 resultObj = e;
                             }

                     }
                 }
             }
             Class<?>[] convert1 = new Class<?>[classes.size()];
             convert1 = classes.toArray(convert1);
             Object[] convert2 = new Object[attributs.size()];
             convert2 = attributs.toArray(convert2);

             resultObj = pointeurObjet.getClass().getMethod(nomFonction,convert1).invoke(pointeurObjet,convert2);
             return true;

         } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
             resultObj = e;
         }

         return false;
     }

     /**
     * programme principal. Prend 4 arguments: 1) numéro de port, 2) répertoire source, 3)
     * répertoire classes, et 4) nom du fichier de traces (sortie)
     * Cette méthode doit créer une instance de la classe ApplicationServeur, l’initialiser
     * puis appeler aVosOrdres sur cet objet
     */
    public static void main(String[] args) throws RemoteException {

        try {
            if (args.length != 4) {
                System.out.println("il manque des arguments parmit: 1) numéro de port, 2) répertoire source, 3) répertoire classes, et 4) nom du fichier de traces (sortie)");
                System.exit(-1);
            }

            ApplicationServeur serveur = new ApplicationServeur(Integer.parseInt(args[0]), args[1], args[2], args[3]);


            System.err.println("Server ready");
        } catch (Exception e) {

            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();

        }

    }

}
