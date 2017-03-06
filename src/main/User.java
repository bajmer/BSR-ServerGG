package main;

import enums.StatusType;

import java.io.PrintWriter;

/**
 * Created by mbala on 27.02.17.
 */
public class User {

    private String login;
    private String password;
    private StatusType status;
    private PrintWriter streamOut;

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public StatusType getStatus() {
        return status;
    }
    public void setStatus(StatusType status) {
        this.status = status;
    }
    public PrintWriter getStreamOut() { return streamOut; }
    public void setStreamOut(PrintWriter streamOut) { this.streamOut = streamOut; }

    public User(String login, String password, PrintWriter stream) {
        this.login = login;
        this.password = password;
        this.status = StatusType.NIEDOSTEPNY;
        this.streamOut = stream;
    }
}
