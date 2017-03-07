package main;

import enums.StatusType;

import java.io.PrintWriter;

/**
 * @author Marcin Bala
 */
public class User {

    private String login;
    private String password;
    private StatusType status;
    private PrintWriter streamOut;
    private boolean isAuthorized;

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
    public boolean isAuthorized() {
        return isAuthorized;
    }
    public void setAuthorized(boolean authorized) {
        isAuthorized = authorized;
    }

    public User(String login, String password, PrintWriter stream) {
        this.login = login;
        this.password = password;
        this.status = StatusType.NIEDOSTEPNY;
        this.streamOut = stream;
        this.isAuthorized = false;
    }
}
