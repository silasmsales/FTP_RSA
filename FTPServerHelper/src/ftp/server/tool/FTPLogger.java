package ftp.server.tool;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class FTPLogger {

    public static final int ERR = -1;
    public static final int OUT = 0;

    public FTPLogger() {
    }
    
    private String getTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("'['HH:mm:ss']' ");
        Date date = new Date();
        return dateFormat.format(date);
    }
   /**
   * Exibe uma mensagem no log da aplicação quando existe um usuário logado
   * @param fTPUser Usuário FTP em conexão
   * @param message Mensagem a ser exibida
   * @param OUTPUT  Arquivo de saída da mansagem(ERR ou OUT)
   */
    public void writeLog(FTPUser fTPUser, String message, int OUTPUT){
        switch (OUTPUT){
            case OUT:
                System.out.println(getTimestamp()+"["+fTPUser.getUsername()+"@"+fTPUser.getIPAddress().getHostAddress()+"] " + message);
                break;
            case ERR:
                System.err.println(getTimestamp()+"["+fTPUser.getUsername()+"@"+fTPUser.getIPAddress().getHostAddress()+"] " + message);
                break;
        }
    }
   /**
   * Exibe uma mensagem no log da aplicação
   * @param fTPUser Usuário FTP em conexão
   * @param message Mensagem a ser exibida
   * @param OUTPUT  Arquivo de saída da mansagem(ERR ou OUT)
   */
    public void writeLog(String message, int OUTPUT){
        switch (OUTPUT){
            case OUT:
                System.out.println(getTimestamp() + message);
                break;
            case ERR:
                System.err.println(getTimestamp() + message);
                break;
        }
    }
}