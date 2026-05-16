import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Small file-backed user store for the demo web app.
 */
public class UserStore {
    private static final String HEADER = "email\tname\tsalt\tpassword_hash\tcreated_at";
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;

    private final Path usersFile;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, String> sessions = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public UserStore(Path usersFile) {
        this.usersFile = usersFile;
        load();
    }

    public synchronized AuthResult register(String name, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String cleanName = clean(name);
        validateCredentials(cleanName, normalizedEmail, password);

        if (users.containsKey(normalizedEmail)) {
            throw new IllegalArgumentException("An account already exists for that email.");
        }

        String salt = randomSalt();
        User user = new User(
                normalizedEmail,
                cleanName,
                salt,
                hashPassword(password, salt),
                Instant.now().toString());
        users.put(normalizedEmail, user);
        save();
        return createSession(user);
    }

    public synchronized AuthResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        User user = users.get(normalizedEmail);
        if (user == null || !user.passwordHash.equals(hashPassword(password, user.salt))) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return createSession(user);
    }

    public synchronized User requireUser(String token) {
        String email = sessions.get(clean(token));
        if (email == null || !users.containsKey(email)) {
            throw new IllegalArgumentException("Please log in first.");
        }
        return users.get(email);
    }

    private AuthResult createSession(User user) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        sessions.put(token, user.email);
        return new AuthResult(token, user);
    }

    private void validateCredentials(String name, String email, String password) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    private void load() {
        if (!Files.exists(usersFile)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(usersFile, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.equals(HEADER)) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length == 5) {
                    User user = new User(parts[0], parts[1], parts[2], parts[3], parts[4]);
                    users.put(user.email, user);
                }
            }
        } catch (IOException ignored) {
            // Start with an empty user store if the file is unreadable.
        }
    }

    private void save() {
        try {
            Path parent = usersFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            for (User user : users.values()) {
                lines.add(String.join("\t",
                        user.email,
                        user.name,
                        user.salt,
                        user.passwordHash,
                        user.createdAt));
            }
            Files.write(usersFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Registration should still return; persistence is best effort.
        }
    }

    private String hashPassword(String password, String salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt),
                    ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Could not hash password.", e);
        }
    }

    private String randomSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String normalizeEmail(String email) {
        return clean(email).toLowerCase();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static class AuthResult {
        private final String token;
        private final User user;

        private AuthResult(String token, User user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() {
            return token;
        }

        public User getUser() {
            return user;
        }
    }

    public static class User {
        private final String email;
        private final String name;
        private final String salt;
        private final String passwordHash;
        private final String createdAt;

        private User(String email, String name, String salt, String passwordHash, String createdAt) {
            this.email = email;
            this.name = name;
            this.salt = salt;
            this.passwordHash = passwordHash;
            this.createdAt = createdAt;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }
    }
}
