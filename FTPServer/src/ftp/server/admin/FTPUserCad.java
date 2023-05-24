package ftp.server.admin;

import ftp.server.tool.FTPLogger;
import ftp.server.tool.RSASingletonHelper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author Rafa
 */
public class FTPUserCad {

    private final String DIRECTORIES = "./directories";
    private final String USERS_FILE = "./logins.txt";
    private final String PUBLICKEY = "./public.rsa";
    private final String PRIVATEKEY = "./private.rsa";
    private final RSASingletonHelper rsaHelper = RSASingletonHelper.getInstance();
    private final FTPLogger logger;

    public FTPUserCad() {
        logger = new FTPLogger();
    }

    public void removeUser(String username, String password) {
        String newFile = "";
        
        try {
            File file = new File(USERS_FILE);
            Scanner scanner = new Scanner(file);
            while(scanner.hasNext()){
                String tmp = scanner.nextLine();
                if (!tmp.equals(username +" "+password)) {
                    newFile += tmp+"\n";
                }
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(newFile);
            fileWriter.close();
            
            deleteDirectory(new File(DIRECTORIES+"/"+username));
            
            logger.writeLog("O usuário \""+username+"\" foi deletado com sucesso.", FTPLogger.OUT);
            
        } catch (Exception iOException) {
            logger.writeLog("Erro ao deletar o usuário \""+username+"\".", 0);
            System.err.print(iOException.getMessage());
        }
    } 
    
    public void addUser(String username, String password) {

        try {

            File file = new File(USERS_FILE);
            FileWriter fw = new FileWriter(file, true);
            try ( //
                    BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(username + " " + password + "\n");

                boolean success = (new File(DIRECTORIES + "/" + username)).mkdir();
                if (success) {
                    logger.writeLog("O usuário \"" + username + "\" foi cadastrado com sucesso.", FTPLogger.OUT);
                }

            }

        } catch (IOException ioe) {
            logger.writeLog("Erro ao cadastrar novo usuário", FTPLogger.ERR);
        }
    }
    
    public void genKey() {

        try {

            File public_key_file = new File(PUBLICKEY);
            File private_key_file = new File(PRIVATEKEY);
            FileWriter fw_publickey = new FileWriter(public_key_file, false);
            FileWriter fw_privatekey = new FileWriter(private_key_file, false);
            try (
                BufferedWriter bw = new BufferedWriter(fw_publickey)) {
                bw.write(rsaHelper.getPublicKeys());
            }
            try (
                BufferedWriter bw = new BufferedWriter(fw_privatekey)) {
                bw.write(rsaHelper.getPrivateKeys());
            }
            logger.writeLog("Chaves pública e privada geradas com sucesso!", FTPLogger.OUT);

        } catch (IOException ioe) {
            logger.writeLog("Erro ao gerar chaves!", FTPLogger.ERR);
        }
    }
    
    public void help(){
        logger.writeLog("add {USUARIO} {SENHA} - adiciona um usuário", FTPLogger.OUT);
        logger.writeLog("remove {USUARIO} {SENHA} - remove um usuário", FTPLogger.OUT);
        logger.writeLog("keygen - gera as chaves pública e privada", FTPLogger.OUT);
        logger.writeLog("help - exibe essa mensagem", FTPLogger.OUT);
        logger.writeLog("exit - encerra a aplicação", FTPLogger.OUT);
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

}
