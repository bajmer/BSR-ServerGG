package enums;

/**
 * @author Marcin Bala
 */
public enum StatusType {
    DOSTEPNY("1"),
    ZARAZ_WRACAM("2"),
    NIE_PRZESZKADZAC("3"),
    NIEDOSTEPNY("4");

    private final String value;

    StatusType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static StatusType forValue(String type) {
        for (StatusType status : StatusType.values()) {
            if (status.value.equals(type)) {
                return status;
            }
        }
        return null;
    }
}
