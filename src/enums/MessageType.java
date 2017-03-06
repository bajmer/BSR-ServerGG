package enums;

/**
 * @author Marcin Bala
 */
public enum MessageType {
    POTRZASANIE("POTRZASANIE"),
    POTWIERDZENIE("POTWIERDZENIE"),
    ODRZUCENIE("ODRZUCENIE"),
    REJESTRACJA("REJESTRACJA"),
    ODP_REJESTRACJA("ODP_REJESTRACJA"),
    AUTORYZCJA("AUTORYZACJA"),
    LISTA_KONWERSACJI("LISTA_KONWERSACJI"),
    LISTA_UZYTKOWNIKOW("LISTA_UZYTKOWNIKÓW"),
    WIADOMOSC("WIADOMOSC"),
    USTAW_STATUS("USTAW_STATUS"),
    ZMIANA_STATUSU("ZMIANA_STATUSU"),
    KTO_W_KONWERSACJI("KTO_W_KONWERSACJI"),
    LISTA_W_KONWERSACJI("LISTA_W_KONWERSACJI"),
    DOLACZ("DOLACZ"),
    ZREZYGNUJ("ZREZYGNUJ"),
    BYWAJ("BYWAJ"),
    ERROR("ERROR"),
    NIEZNANY("NIEZNANY");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static MessageType forValue(String type) {
        for (MessageType operationType : MessageType.values()) {
            if (operationType.value.equals(type)) {
                return operationType;
            }
        }
        return MessageType.NIEZNANY;
    }
}
