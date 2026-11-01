package lifelink;

import java.security.MessageDigest;

public class PasswordUtil {
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes("UTF-8"));
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                result.append(String.format("%02x", bytes[i] & 0xff));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
