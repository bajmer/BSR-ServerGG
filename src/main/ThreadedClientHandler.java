package main;

import enums.MessageType;
import enums.RegistrationResponse;
import enums.StatusType;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Marcin Bala
 */
public class ThreadedClientHandler implements Runnable {

    private Socket incoming;
    private String clientLogin;

    private final String separator = ";";
    private final String InvalidCommunicateReason = MessageType.ERROR.toString() + ";Nieznany_komunikat";
    private final String InvalidArgsNumberReason = MessageType.ERROR.toString() + ";Nieprawidlowa_liczba_argumentow";
    private final String InvalidLoginOrPasswordReason = MessageType.ERROR.toString() + ";Nieprawidlowy_login_lub_haslo";
    private final String NoUsersReason = MessageType.ERROR.toString() + ";Brak_uzytkownikow_w_bazie";
    private final String InvalidStatusReason = MessageType.ERROR.toString() + ";Nieprawidłowy_status";
    private final String NoConversationsReason = MessageType.ERROR.toString() + ";Brak_konwersacji";
    private final String InvalidConversationReason = MessageType.ERROR.toString() + ";Nie_ma_takiej_konwersacji";
    private final String AleradyInConversationReason = MessageType.ERROR.toString() + ";Uzytkownik_jest_juz_w_konwersacji";
    private final String NoUserInConversationReason = MessageType.ERROR.toString() + ";Brak_uzytkownika_w_konwersacji";

    private static ArrayList<User> usersData = new ArrayList<>();
    private static ArrayList<Conversation> conversations = new ArrayList<>();

    public ThreadedClientHandler(Socket i) { incoming = i; }

    public void run() {
        try {
            try {
                System.out.println("New thread started");

                InputStream inStream = incoming.getInputStream();
                OutputStream outStream = incoming.getOutputStream();
                Scanner in = new Scanner(inStream);
                PrintWriter out = new PrintWriter(outStream, true /* autoFlush */);

                boolean done = false;
                while (!done && in.hasNextLine())
                {
                    String line = in.nextLine();
                    System.out.println("Processing line: " + line);

                    //String answer = null;
                    String[] fields = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, separator);

                    String inMessage = fields[0];
                    MessageType typeOfInMessage = MessageType.forValue(inMessage);

                    switch (typeOfInMessage) {
                        case POTRZASANIE:
                            if(fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            if(fields[1].equals("1"))
                                out.println(MessageType.POTWIERDZENIE.toString());
                            else
                                out.println(MessageType.ODRZUCENIE.toString());
                            break;

                        case REJESTRACJA:
                            if(fields.length != 3) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            RegistrationResponse responseType;
                            String userLogin = fields[1];
                            String userPassword = fields[2];

                            if(!usersData.isEmpty()) {
                                responseType = RegistrationResponse.AKCEPTUJE;
                                for (User user: usersData) {
                                    if(userLogin.equals(user.getLogin())) {
                                        responseType = RegistrationResponse.LOGIN_UZYWANY;
                                        break;
                                    }
                                }
                                if(responseType == RegistrationResponse.AKCEPTUJE) {
                                    if(!userPassword.matches("\\w{5,}"))
                                        responseType = RegistrationResponse.HASLO_ZA_PROSTE;
                                    else {
                                        User newUser = new User(userLogin,userPassword,out);
                                        usersData.add(newUser);
                                    }
                                }
                            } else {
                                if(!userPassword.matches("\\w{5,}"))
                                    responseType = RegistrationResponse.HASLO_ZA_PROSTE;
                                else {
                                    responseType = RegistrationResponse.AKCEPTUJE;
                                    User newUser = new User(userLogin,userPassword,out);
                                    usersData.add(newUser);
                                }
                            }
                            out.println(MessageType.ODP_REJESTRACJA.toString() + ";" + responseType.toString());

                            break;

                        case AUTORYZCJA:
                            if(fields.length != 3) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String login = fields[1];
                            String password = fields[2]; // TODO: 28.02.17 Haslo MD5

                            if(usersData.isEmpty()) {
                                out.println(NoUsersReason);
                                break;
                            }
                            int i = 1;
                            for (User user: usersData) {
                                if(login.equals(user.getLogin()) && password.equals(user.getPassword())) {
                                    user.setStatus(StatusType.DOSTEPNY);
                                    user.setStreamOut(out);
                                    this.clientLogin = user.getLogin();

                                    out.println(MessageType.POTWIERDZENIE.toString());

                                    StringBuilder convList = new StringBuilder();
                                    convList.append(MessageType.LISTA_KONWERSACJI.toString())
                                            .append(";")
                                            .append(conversations.size());
                                    for (Conversation conversation : conversations) {
                                        convList.append(";").append(conversation.getName());
                                    }
                                    out.println(convList.toString());

                                    StringBuilder usersList = new StringBuilder();
                                    usersList.append(MessageType.LISTA_UZYTKOWNIKOW.toString())
                                            .append(";")
                                            .append(usersData.size());
                                    for (User user1 : usersData) {
                                        usersList.append(";").append(user1.getLogin()).append(";").append(user1.getStatus().toString());
                                    }
                                    out.println(usersList.toString());
                                    break;
                                } else {
                                    if (i == usersData.size()) {
                                        out.println(InvalidLoginOrPasswordReason);
                                    }
                                }
                                i++;
                            }
                            break;

                        case WIADOMOSC:
                            if(fields.length != 4) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            break;

                        case USTAW_STATUS:
                            if(fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            StatusType status = StatusType.forValue(fields[1]);
                            if(status == null) {
                                out.println(InvalidStatusReason);
                                break;
                            }
                            if(usersData.isEmpty()) {
                                out.println(NoUsersReason);
                                break;
                            }
                            for(User user : usersData) {
                                if (user.getLogin().equals(clientLogin)) {
                                    user.setStatus(status);
                                    out.println(MessageType.POTWIERDZENIE);
                                }
                            }
                            for(User user : usersData) {
                                if (!user.getLogin().equals(clientLogin)) {
                                    PrintWriter tmpOut = user.getStreamOut();
                                    StringBuilder statusChange = new StringBuilder();
                                    statusChange.append(MessageType.ZMIANA_STATUSU)
                                            .append(";")
                                            .append(clientLogin)
                                            .append(";")
                                            .append(status.toString());
                                    tmpOut.println(statusChange.toString());
                                }
                            }
                            break;

                        case KTO_W_KONWERSACJI:
                            if(fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String conversationName = fields[1];
                            if(conversations.isEmpty()) {
                                out.println(NoConversationsReason);
                                break;
                            }

                            int j = 1;
                            for(Conversation conversation : conversations) {
                                if (conversation.getName().equals(conversationName)) {
                                    StringBuilder conversationAnswer = new StringBuilder();
                                    conversationAnswer.append(MessageType.LISTA_W_KONWERSACJI)
                                            .append(";")
                                            .append(conversation.getUsersInConversation().size());
                                    for(String userInConversation : conversation.getUsersInConversation()) {
                                        conversationAnswer.append(";").append(userInConversation);
                                    }
                                    out.println(conversationAnswer.toString());
                                    break;
                                } else {
                                    if(j == conversations.size()) {
                                        out.println(InvalidConversationReason);
                                    }
                                }
                                j++;
                            }
                            break;

                        case DOLACZ:
                            if(fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String discussion = fields[1];
                            if(conversations.isEmpty()) {
                                Conversation newConversation = new Conversation(discussion);
                                newConversation.getUsersInConversation().add(clientLogin);
                                conversations.add(newConversation);
                                out.println(MessageType.POTWIERDZENIE);
                                break;
                            }

                            boolean createConversation = true;
                            for(Conversation conversation : conversations) {
                                if (conversation.getName().equals(discussion)) {
                                    boolean alreadyInConversation = false;
                                    for(String loginOfClient : conversation.getUsersInConversation()) {
                                        if(loginOfClient.equals(clientLogin)) {
                                            alreadyInConversation = true;
                                        }
                                    }
                                    if(!alreadyInConversation) {
                                        conversation.getUsersInConversation().add(clientLogin);
                                        out.println(MessageType.POTWIERDZENIE);
                                        createConversation = false;
                                        break;
                                    } else {
                                        out.println(AleradyInConversationReason);
                                        createConversation = false;
                                        break;
                                    }
                                }
                            }
                            if(createConversation) {
                                Conversation newConversation = new Conversation(discussion);
                                newConversation.getUsersInConversation().add(clientLogin);
                                conversations.add(newConversation);
                                out.println(MessageType.POTWIERDZENIE);
                            }
                            break;

                        case ZREZYGNUJ:
                            if(fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String discussionName = fields[1];
                            if(conversations.isEmpty()) {
                                out.println(NoConversationsReason);
                            }
                            boolean noUserInConversation = true;
                            boolean noConversation = true;
                            boolean deleteConversation = false;
                            int id = 0;
                            for(Conversation conversation : conversations) {
                                if(conversation.getName().equals(discussionName)) {
                                    noConversation = false;
                                    for(String user : conversation.getUsersInConversation()) {
                                        if(user.equals(clientLogin)) {
                                            noUserInConversation = false;
                                        }
                                    }
                                    if(!noUserInConversation) {
                                        conversation.getUsersInConversation().remove(clientLogin);
                                        out.println(MessageType.POTWIERDZENIE);
                                    }
                                    if(conversation.getUsersInConversation().size() == 0) {
                                        deleteConversation = true;
                                    }
                                }
                            }
                            if(deleteConversation) {
                                conversations.remove(id);
                                //TODO
                            }

                            if(noConversation) {
                                out.println(InvalidConversationReason);
                                break;
                            }
                            if(noUserInConversation) {
                                out.println(NoUserInConversationReason);
                                break;
                            }

                            break;

                        case BYWAJ:
                            done = true;
                            if(!usersData.isEmpty()) {
                                for(User user : usersData) {
                                    if (user.getLogin().equals(clientLogin)) {
                                        user.setStatus(StatusType.NIEDOSTEPNY);
                                    }
                                }
                            }
                            break;

                        default:
                            out.println(InvalidCommunicateReason);
                            break;
                    }
                }
            }
            finally
            {
                System.out.println("Closing connection with the client");
                incoming.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
