package main;

import java.util.ArrayList;

/**
 * @author Marcin Bala
 */
public class Conversation {

    private String name;
    private ArrayList<String> usersInConversation;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ArrayList<String> getUsersInConversation() {
        return usersInConversation;
    }
    public void setUsersInConversation(ArrayList<String> usersInConversation) {
        this.usersInConversation = usersInConversation;
    }

    public Conversation(String name) {
        this.name = name;
        this.usersInConversation = new ArrayList<>();
    }
}
