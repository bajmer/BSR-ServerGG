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
    private boolean Handshake;

    private final String separator = ";";
    private final String InvalidCommunicateReason = MessageType.ERROR.toString() + ";Nieznany komunikat";
    private final String InvalidArgsNumberReason = MessageType.ERROR.toString() + ";Nieprawidlowa liczba argumentow";
    private final String LoginCannotBeBlankReason = MessageType.ERROR.toString() + ";Login nie moze byc pusty";
    private final String InvalidLoginOrPasswordReason = MessageType.ERROR.toString() + ";Nieprawidlowy login lub haslo";
    private final String NoUsersReason = MessageType.ERROR.toString() + ";Brak uzytkownikow w bazie";
    private final String UserIsNotLoggedInReason = MessageType.ERROR.toString() + ";Uzytkownik nie jest zalogowany";
    private final String UserAlreadyLoggedInReason = MessageType.ERROR.toString() + ";Uzytkownik jest juz zalogowany. Zaloguj sie na inne konto.";
    private final String InvalidReveiverReason = MessageType.ERROR.toString() + ";Nieznany adresat wiadomosci";
    private final String InvalidSenderReason = MessageType.ERROR.toString() + ";Nieznany nadawca wiadomosci";
    private final String InvalidStatusReason = MessageType.ERROR.toString() + ";Nieprawidłowy status";
    private final String NoConversationsReason = MessageType.ERROR.toString() + ";Brak konwersacji";
    private final String InvalidConversationReason = MessageType.ERROR.toString() + ";Nie ma takiej konwersacji";
    private final String AleradyInConversationReason = MessageType.ERROR.toString() + ";Uzytkownik jest juz w konwersacji";
    private final String NoUserInConversationReason = MessageType.ERROR.toString() + ";Brak uzytkownika w konwersacji";
    private final String Confirmation = MessageType.POTWIERDZENIE.toString();
    private final String Rejection = MessageType.ODRZUCENIE.toString();

    private static ArrayList<User> usersData = new ArrayList<>();
    private static ArrayList<Conversation> conversations = new ArrayList<>();

    public ThreadedClientHandler(Socket i) {
        this.incoming = i;
        this.clientLogin = null;
        this.Handshake = false;
    }

    public void run() {
        try {
            try {
                System.out.println("New thread started");

                InputStream inStream = incoming.getInputStream();
                OutputStream outStream = incoming.getOutputStream();
                Scanner in = new Scanner(inStream);
                PrintWriter out = new PrintWriter(outStream, true /* autoFlush */);

                boolean done = false;
                while (!done && in.hasNextLine()) {
                    String line = in.nextLine();
                    System.out.println("Processing line: " + line);

                    String[] fields = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, separator);

                    String inMessage = fields[0];
                    MessageType typeOfInMessage = MessageType.forValue(inMessage);

                    switch (typeOfInMessage) {
                        //**********************************************************************************************
                        case POTRZASANIE:
                            if (fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            if (fields[1].equals("1")) {
                                out.println(Confirmation);
                                Handshake = true;
                            } else {
                                out.println(Rejection);
                                done = true;
                            }
                            break;
                        //**********************************************************************************************
                        case REJESTRACJA:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            if (fields.length != 3) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            RegistrationResponse responseType;
                            String userLogin = fields[1];
                            String userPassword = fields[2];

                            if(StringUtils.isBlank(userLogin)) {
                                out.println(LoginCannotBeBlankReason);
                                break;
                            }

                            if (!usersData.isEmpty()) {
                                responseType = RegistrationResponse.AKCEPTUJE;
                                for (User user : usersData) {
                                    if (userLogin.equals(user.getLogin())) {
                                        responseType = RegistrationResponse.LOGIN_UZYWANY;
                                        break;
                                    }
                                }
                                if (responseType == RegistrationResponse.AKCEPTUJE) {
                                    if (!userPassword.matches("\\w{5,}"))
                                        responseType = RegistrationResponse.HASLO_ZA_PROSTE;
                                    else {
                                        User newUser = new User(userLogin, userPassword, out);
                                        usersData.add(newUser);
                                    }
                                }
                            } else {
                                if (!userPassword.matches("\\w{5,}"))
                                    responseType = RegistrationResponse.HASLO_ZA_PROSTE;
                                else {
                                    responseType = RegistrationResponse.AKCEPTUJE;
                                    User newUser = new User(userLogin, userPassword, out);
                                    usersData.add(newUser);
                                }
                            }
                            out.println(MessageType.ODP_REJESTRACJA.toString() + separator + responseType.toString());

                            break;
                        //**********************************************************************************************
                        case AUTORYZCJA:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            if (fields.length != 3) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String login = fields[1];
                            String password = fields[2]; // TODO: 28.02.17 Haslo MD5

                            if (usersData.isEmpty()) {
                                out.println(NoUsersReason);
                                break;
                            }
                            int i = 1;
                            for (User user : usersData) {
                                if (login.equals(user.getLogin()) && password.equals(user.getPassword())) {
                                    if(user.isAuthorized()) {
                                        out.println(UserAlreadyLoggedInReason);
                                        break;
                                    } else {
                                        user.setAuthorized(true);
                                        user.setStatus(StatusType.DOSTEPNY);
                                        user.setStreamOut(out);
                                        this.clientLogin = user.getLogin();
                                        out.println(Confirmation);

                                        updateUsers();
                                        //updateConversations();

                                        StringBuilder convList = new StringBuilder();
                                        convList.append(MessageType.LISTA_KONWERSACJI.toString())
                                                .append(separator)
                                                .append(conversations.size());
                                        for (Conversation conversation : conversations) {
                                            convList.append(separator).append(conversation.getName());
                                        }
                                        out.println(convList.toString());
                                        break;
                                    }
                                } else {
                                    if (i == usersData.size()) {
                                        out.println(InvalidLoginOrPasswordReason);
                                    }
                                }
                                i++;
                            }
                            break;
                        //**********************************************************************************************
                        case WIADOMOSC:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            if(StringUtils.isBlank(clientLogin)) {
                                out.println(UserIsNotLoggedInReason);
                                break;
                            }
                            if (fields.length != 4) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String chatName = fields[2];
                            String message = fields[3];
                            if(StringUtils.isBlank(chatName)) {
                                String receiver = fields[1];
                                boolean needConfirmation = false;
                                for (User user : usersData) {
                                    if(user.getLogin().equals(receiver)) {
                                        StringBuilder messageAnswer = new StringBuilder(MessageType.WIADOMOSC.toString());
                                        messageAnswer.append(separator)
                                                .append(clientLogin)
                                                .append(separator)
                                                .append(separator)
                                                .append(message);
                                        user.getStreamOut().println(messageAnswer);
                                        needConfirmation = true;
                                        break;
                                    }
                                }
                                if(needConfirmation) {
                                    out.println(Confirmation);
                                } else {
                                    out.println(InvalidReveiverReason);
                                }
                            } else {
                                String sender = fields[1];
                                if(!sender.equals(clientLogin)) {
                                    out.println(InvalidSenderReason);
                                    break;
                                }
                                StringBuilder messageAnswer = new StringBuilder(MessageType.WIADOMOSC.toString());
                                messageAnswer.append(separator)
                                        .append(sender)
                                        .append(separator)
                                        .append(chatName)
                                        .append(separator)
                                        .append(message);
                                boolean needConfirmation = false;
                                boolean invalidChatName = true;
                                for (Conversation conversation : conversations) {
                                    if(conversation.getName().equals(chatName)) {
                                        invalidChatName = false;
                                        for(String userInConversation : conversation.getUsersInConversation()) {
                                            for(User user : usersData) {
                                                if(userInConversation.equals(user.getLogin())) {
                                                    user.getStreamOut().println(messageAnswer);
                                                    needConfirmation = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                if(invalidChatName) {
                                    out.println(InvalidConversationReason);
                                }
                                if(needConfirmation) {
                                    out.println(Confirmation);
                                }
                            }
                            break;
                        //**********************************************************************************************
                        case USTAW_STATUS:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            if(StringUtils.isBlank(clientLogin)) {
                                out.println(UserIsNotLoggedInReason);
                                break;
                            }
                            if (fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            StatusType status = StatusType.forValue(fields[1]);
                            if (status == null) {
                                out.println(InvalidStatusReason);
                                break;
                            }
                            if (usersData.isEmpty()) {
                                out.println(NoUsersReason);
                                break;
                            }
                            for (User user : usersData) {
                                if (user.getLogin().equals(clientLogin)) {
                                    user.setStatus(status);
                                    out.println(Confirmation);
                                }
                            }
                            for (User user : usersData) {
                                PrintWriter tmpOut = user.getStreamOut();
                                StringBuilder statusChange = new StringBuilder();
                                statusChange.append(MessageType.ZMIANA_STATUSU)
                                        .append(separator)
                                        .append(clientLogin)
                                        .append(separator)
                                        .append(status.toString());
                                tmpOut.println(statusChange.toString());
                            }
                            break;
                        //**********************************************************************************************
                        case KTO_W_KONWERSACJI:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            if(StringUtils.isBlank(clientLogin)) {
                                out.println(UserIsNotLoggedInReason);
                                break;
                            }
                            if (fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String conversationName = fields[1];
                            if (conversations.isEmpty()) {
                                out.println(NoConversationsReason);
                                break;
                            }

                            int j = 1;
                            for (Conversation conversation : conversations) {
                                if (conversation.getName().equals(conversationName)) {
                                    StringBuilder conversationAnswer = new StringBuilder();
                                    conversationAnswer.append(MessageType.LISTA_W_KONWERSACJI)
                                            .append(separator)
                                            .append(conversation.getUsersInConversation().size());
                                    for (String userInConversation : conversation.getUsersInConversation()) {
                                        conversationAnswer.append(separator).append(userInConversation);
                                    }
                                    out.println(conversationAnswer.toString());
                                    break;
                                } else {
                                    if (j == conversations.size()) {
                                        out.println(InvalidConversationReason);
                                    }
                                }
                                j++;
                            }
                            break;
                        //**********************************************************************************************
                        case DOLACZ:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            if(StringUtils.isBlank(clientLogin)) {
                                out.println(UserIsNotLoggedInReason);
                                break;
                            }
                            if (fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String discussion = fields[1];
                            if (conversations.isEmpty()) {
                                Conversation newConversation = new Conversation(discussion);
                                newConversation.getUsersInConversation().add(clientLogin);
                                conversations.add(newConversation);
                                out.println(Confirmation);
                                updateConversations();
                                break;
                            }

                            boolean createConversation = true;
                            for (Conversation conversation : conversations) {
                                if (conversation.getName().equals(discussion)) {
                                    boolean alreadyInConversation = false;
                                    for (String loginOfClient : conversation.getUsersInConversation()) {
                                        if (loginOfClient.equals(clientLogin)) {
                                            alreadyInConversation = true;
                                        }
                                    }
                                    if (!alreadyInConversation) {
                                        conversation.getUsersInConversation().add(clientLogin);
                                        out.println(Confirmation);
                                        createConversation = false;
                                        updateConversations();
                                        break;
                                    } else {
                                        out.println(AleradyInConversationReason);
                                        createConversation = false;
                                        break;
                                    }
                                }
                            }
                            if (createConversation) {
                                Conversation newConversation = new Conversation(discussion);
                                newConversation.getUsersInConversation().add(clientLogin);
                                conversations.add(newConversation);
                                out.println(Confirmation);
                                updateConversations();
                            }
                            break;
                        //**********************************************************************************************
                        case ZREZYGNUJ:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            if(StringUtils.isBlank(clientLogin)) {
                                out.println(UserIsNotLoggedInReason);
                                break;
                            }
                            if (fields.length != 2) {
                                out.println(InvalidArgsNumberReason);
                                break;
                            }
                            String discussionName = fields[1];
                            if (conversations.isEmpty()) {
                                out.println(NoConversationsReason);
                            }
                            boolean noUserInConversation = true;
                            boolean noConversation = true;
                            Iterator<Conversation> it = conversations.iterator();
                            while (it.hasNext()) {
                                Conversation conversation = it.next();
                                if (conversation.getName().equals(discussionName)) {
                                    noConversation = false;
                                    for (String user : conversation.getUsersInConversation()) {
                                        if (user.equals(clientLogin)) {
                                            noUserInConversation = false;
                                        }
                                    }
                                    if (!noUserInConversation) {
                                        conversation.getUsersInConversation().remove(clientLogin);
                                        out.println(Confirmation);
                                    }
                                    if (conversation.getUsersInConversation().size() == 0) {
                                        it.remove();
                                        updateConversations();
                                    }
                                }
                            }
                            if (noConversation) {
                                out.println(InvalidConversationReason);
                                break;
                            }
                            if (noUserInConversation) {
                                out.println(NoUserInConversationReason);
                                break;
                            }
                            break;
                        //**********************************************************************************************
                        case BYWAJ:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            done = true;
                            if (!usersData.isEmpty()) {
                                for (User user : usersData) {
                                    if (user.getLogin().equals(clientLogin)) {
                                        user.setStatus(StatusType.NIEDOSTEPNY);
                                        user.setAuthorized(false);
                                    }
                                }
                                boolean noUserInConv = true;
                                Iterator<Conversation> itConv = conversations.iterator();
                                while (itConv.hasNext()) {
                                    Conversation conversation = itConv.next();
                                    for (String user : conversation.getUsersInConversation()) {
                                        if (user.equals(clientLogin)) {
                                            noUserInConv = false;
                                        }
                                    }
                                    if (!noUserInConv) {
                                        conversation.getUsersInConversation().remove(clientLogin);
                                    }
                                    if (conversation.getUsersInConversation().size() == 0) {
                                        itConv.remove();
                                    }
                                }
                                updateUsers();
                                updateConversations();
                            } else {
                                out.println(NoUsersReason);
                            }
                            break;
                        //**********************************************************************************************
                        default:
                            if(!Handshake) {
                                out.println(Rejection);
                                done = true;
                                break;
                            }
                            out.println(InvalidCommunicateReason);
                            break;
                        //**********************************************************************************************
                    }
                }
            } finally {
                System.out.println("Closing connection with the client");
                incoming.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateUsers() {
        StringBuilder usersList = new StringBuilder();
        usersList.append(MessageType.LISTA_UZYTKOWNIKOW.toString())
                .append(separator);
        int authorizedUsersNumber = 0;
        for (User user : usersData) {
            if (user.isAuthorized()) {
                authorizedUsersNumber++;
                usersList.append(separator).append(user.getLogin()).append(separator).append(user.getStatus().toString());
            }
        }
        usersList.insert(19, authorizedUsersNumber);
        for (User user : usersData) {
            user.getStreamOut().println(usersList.toString());
        }
    }

    public void updateConversations() {
        StringBuilder convList = new StringBuilder();
        convList.append(MessageType.LISTA_KONWERSACJI.toString())
                .append(separator)
                .append(conversations.size());
        for (Conversation conversation : conversations) {
            convList.append(separator).append(conversation.getName());
        }
        for (User user : usersData) {
            user.getStreamOut().println(convList.toString());
        }
    }
}
