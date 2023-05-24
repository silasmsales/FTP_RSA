package ftp.client.net;

import ftp.client.tool.FTPUser;
import ftp.client.tool.FTPLogger;
import ftp.client.tool.RSASingletonHelper;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author silasmsales
 */
public class FTPClientConnection {

    private static final String CONNECTION_CLOSE = "426";
    private static final String LOGGED_IN = "230";
    private static final String ACTION_ABORTED = "451";
    private static final String FILE_NOT_FOUND = "450";
    private static final String FILE_EXIST = "350";
    private static final String FILE_NOT_EXIST = "351";
    private static final String FILE_STATUS_OK = "150";
    private static final String SUCCESSFUL_ACTION = "226";

    private static final String USER = "USER";
    private static final String PASS = "PASS";
    private static final String STOR = "STOR";
    private static final String RETR = "RETR";
    private static final String LIST = "LIST";
    private static final String DELE = "DELE";
    private static final String DISCONNECT = "DISCONNECT";

    private static final String YES = "S";
    private static final String NO = "N";

    private DataInputStream connectionInputStream;
    private DataOutputStream connectionOutputStream;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Socket clientSocketConnection;
    private Socket clientSocketData;
    private FileStream fileStream;
    private FTPUser userClient;
    private FTPLogger log;
    private boolean isConnected = false;
    private String serverIPAddress;

    private RSASingletonHelper rsaHelper = RSASingletonHelper.getInstance();
    private String publicKeyStr = "1;1";
    private String privateKeyStr = "1;1";
    private byte[] data;

    public FTPClientConnection(Socket clientSocketConnection, Socket clientSocketData, String username, String password) {
        try {
            this.clientSocketConnection = clientSocketConnection;
            this.clientSocketData = clientSocketData;
            connectionInputStream = new DataInputStream(this.clientSocketConnection.getInputStream());
            connectionOutputStream = new DataOutputStream(this.clientSocketConnection.getOutputStream());
            dataInputStream = new DataInputStream(this.clientSocketData.getInputStream());
            dataOutputStream = new DataOutputStream(this.clientSocketData.getOutputStream());
            userClient = new FTPUser(username, password, (Inet4Address) clientSocketConnection.getInetAddress());
            fileStream = new FileStream(dataOutputStream, dataInputStream, userClient);
            log = new FTPLogger();

            serverIPAddress = this.clientSocketConnection.getInetAddress().getHostAddress();
                        
            File publicKey = new File("./public.rsa");
            try (FileInputStream fisPublicKey = new FileInputStream(publicKey)) {
                data = new byte[(int) publicKey.length()];
                fisPublicKey.read(data);
                publicKeyStr = new String(data, "UTF-8");
            }catch(IOException iOException){
                log.writeLog("Chave de segurança pública inválida", FTPLogger.ERR);
            }

            File privateKey = new File("./private.rsa");
            try (FileInputStream fisPrivateKey = new FileInputStream(privateKey)) {
                data = new byte[(int) privateKey.length()];
                fisPrivateKey.read(data);
                privateKeyStr = new String(data, "UTF-8");
            }catch(IOException iOException){
                log.writeLog("Chave de segurança privada inválida", FTPLogger.ERR);
            }

            authenticate(userClient.getUsername(), userClient.getPassword());
        } catch (IOException iOException) {
            log.writeLog("Não foi possível estabelecer uma conexão com " + serverIPAddress, FTPLogger.ERR);
        }

    }

    public boolean isConnected() {
        return this.isConnected;
    }

    private void authenticate(String username, String password) throws IOException {
        try {          
            connectionOutputStream.writeUTF(USER);
            connectionOutputStream.writeUTF(rsaHelper.encrypt(username, publicKeyStr));
            connectionOutputStream.writeUTF(PASS);
            connectionOutputStream.writeUTF(rsaHelper.encrypt(password, publicKeyStr));

            String reply = connectionInputStream.readUTF();
           
            if (reply.equals(LOGGED_IN)) {
                log.writeLog("Conexao estabelecida com " + serverIPAddress, FTPLogger.OUT);
                this.isConnected = true;
            } else if(reply.equals(CONNECTION_CLOSE)) {
                log.writeLog("Usuario e/ou senha incorreto(s)", FTPLogger.ERR);
                commandDISCONNECT();
                this.isConnected = false;
            }

        } catch (IOException iOException) {
            log.writeLog("Erro ao autenticar o usuário!", FTPLogger.ERR);
            connectionOutputStream.writeUTF(DISCONNECT);
        }
    }

    public void commandDELETE(String filename) {
        try {
            connectionOutputStream.writeUTF(DELE);
            connectionOutputStream.writeUTF(filename);
            String reply = connectionInputStream.readUTF();
            if (reply.equals(FILE_NOT_FOUND)) {
                log.writeLog("O arquivo \"" + filename + "\" não foi encontrado no servidor.", FTPLogger.OUT);
            } else if (reply.equals(SUCCESSFUL_ACTION)) {
                log.writeLog("O arquivo \"" + filename + "\" foi deletado com sucesso do servidor.", FTPLogger.OUT);
            }
        } catch (IOException ex) {
            log.writeLog("Não foi possível deletar o arquivo do servidor.", FTPLogger.ERR);
        }
    }

    public String[] commandLIST() {

        String[] files = null;
        try {
            List<String> fileList = new ArrayList<>();

            connectionOutputStream.writeUTF(LIST);
            String filename = connectionInputStream.readUTF();

            while (!filename.equals(SUCCESSFUL_ACTION)) {
                fileList.add(filename);
                filename = connectionInputStream.readUTF();
            }
            files = new String[fileList.size()];
            for (int i = 0; i < fileList.size(); i++) {
                files[i] = fileList.get(i);
            }

            log.writeLog("Arquivos listados com sucesso!", FTPLogger.OUT);
        } catch (IOException ex) {
            log.writeLog("Erro ao listar os arquivos!", FTPLogger.ERR);
        }
        return files;
    }

    public void commandDISCONNECT() {
        try {
            connectionOutputStream.writeUTF(DISCONNECT);
            String reply;
            reply = connectionInputStream.readUTF();
            if (reply.equals(DISCONNECT)) {
                log.writeLog("Desconectado do servidor " + serverIPAddress, FTPLogger.OUT);
                dataInputStream.close();
                dataOutputStream.close();
                connectionInputStream.close();
                connectionOutputStream.close();
            }
        } catch (IOException ex) {
            log.writeLog("Erro ao desconectar do servidor!", FTPLogger.ERR);
        }
    }

    public void commandRETR(String filename) {
        try {
            connectionOutputStream.writeUTF(RETR);
            connectionOutputStream.writeUTF(filename);

            String reply = connectionInputStream.readUTF();
            if (reply.equals(FILE_NOT_FOUND)) {
                log.writeLog("O arquivo \"" + filename + "\" não foi encontrado no servidor.", FTPLogger.OUT);
            } else if (reply.equals(FILE_STATUS_OK)) {
                log.writeLog("Recebendo o arquivo \"" + filename + "\" ...", FTPLogger.OUT);
                File file = new File(filename);
                if (file.exists()) {
                    int option = JOptionPane.showConfirmDialog(null, "O arquivo \"" + filename + "\" já existe, deseja sobrescrever?", "Aviso", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (option == JOptionPane.NO_OPTION) {
                        connectionOutputStream.flush();
                        return;
                    }
                }

                String checksum = rsaHelper.encrypt(connectionInputStream.readUTF(), privateKeyStr);
                
                fileStream.getFile(file);
                
                if(!fileIsSafe(file, checksum)){
                    file.delete();
                    commandDISCONNECT();
                    throw new IOException();
                }
                
                if (connectionInputStream.readUTF().equals(SUCCESSFUL_ACTION)) {
                    log.writeLog("O arquivo \"" + filename + "\" foi recebido com sucesso!", FTPLogger.OUT);
                }

            }
        } catch (IOException ex) {
            log.writeLog("Não foi possível receber o arquivo \"" + filename + "\".", FTPLogger.ERR);
        }
    }

    public void commandSTOR(String filename) {
        try {
            connectionOutputStream.writeUTF(STOR);

            File file = new File(filename);
            if (!file.exists()) {
                log.writeLog("O arquivo \"" + filename + "\" não existe!", FTPLogger.OUT);
                connectionOutputStream.writeUTF(FILE_NOT_FOUND);
                return;
            }

            connectionOutputStream.writeUTF(filename);

            String reply = connectionInputStream.readUTF();
            switch (reply) {
                case FILE_EXIST:
                    int option = JOptionPane.showConfirmDialog(null, "O arquivo \"" + filename + "\" já existe, deseja sobrescrever?", "Aviso", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        connectionOutputStream.writeUTF(FILE_STATUS_OK);
                    } else {
                        connectionOutputStream.writeUTF(ACTION_ABORTED);
                        return;
                    }
                    break;
                case FILE_NOT_EXIST:
                    connectionOutputStream.writeUTF(FILE_STATUS_OK);
                    break;
            }
            log.writeLog("Enviando o arquivo \"" + filename + "\" ...", FTPLogger.OUT);

            connectionOutputStream.writeUTF(rsaHelper.encrypt(getChecksum(file), publicKeyStr));
            fileStream.sendFile(file);

            if (connectionInputStream.readUTF().equals(SUCCESSFUL_ACTION)) {
                log.writeLog("O arquivo \"" + filename + "\" foi enviado com sucesso!", FTPLogger.OUT);
            } else {
                log.writeLog("Não foi possível completar a transação!", FTPLogger.OUT);
            }

        } catch (IOException iOException) {
            log.writeLog("Erro ao enviar arquivo!", FTPLogger.ERR);
        }
    }
    private String getChecksum(File file){
        try {
            byte[] data = Files.readAllBytes(Paths.get(file.toURI()));
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            String checksum = new BigInteger(1, hash).toString(16);
            return checksum;
        } catch (IOException | NoSuchAlgorithmException ex) {
            log.writeLog(userClient, "Impossivel gerar checksum para o arquivo" + file.getName(), FTPLogger.ERR);
            return null;
        }
    } 
    private boolean fileIsSafe(File file, String checksum){
        return getChecksum(file).equals(checksum);
    }

}
