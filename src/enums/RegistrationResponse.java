package enums;

/**
 * @author Marcin Bala
 */
public enum RegistrationResponse {
    AKCEPTUJE("1"),
    LOGIN_UZYWANY("2"),
    HASLO_ZA_PROSTE("3");

    private final String value;

    RegistrationResponse(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static RegistrationResponse forValue(String type) {
        for (RegistrationResponse response : RegistrationResponse.values()) {
            if (response.value.equals(type)) {
                return response;
            }
        }
        return null;
    }
}
