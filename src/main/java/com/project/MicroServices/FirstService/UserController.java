package com.project.MicroServices.FirstService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api")
class UserController {
    private Connection connection;

    public UserController() {
        initDB();
    }

    private void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:authorization.db");
    }

    private void disconnect() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    private void initDB() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:authorization.db"); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS user (id INTEGER PRIMARY KEY AUTOINCREMENT, " + "username VARCHAR(50) UNIQUE NOT NULL, " + "email VARCHAR(100) UNIQUE NOT NULL, " + "password_hash VARCHAR(255) NOT NULL, " + "role VARCHAR(10) NOT NULL CHECK (role IN ('customer', 'chef', 'manager')), " + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS session (id INTEGER PRIMARY KEY AUTOINCREMENT, " + "user_id INTEGER NOT NULL, " + "session_token VARCHAR(255) NOT NULL, " + "expires_at TIMESTAMP NOT NULL, " + "FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> regUser(@RequestBody User user) {
        if (!validateEmail(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email format");
        }

        try {
            connect();

            String query = "INSERT INTO user (username, email, password_hash, role) VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole());
            statement.executeUpdate();

            disconnect();

            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to register user");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> userLogin(@RequestBody User user) {
        try {
            connect();

            String query = "SELECT * FROM user WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, user.getEmail());
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String passwordHash = rs.getString("password_hash");
                if (passwordHash.equals(user.getPassword())) {
                    // Генерация и сохранение токена сессии
                    String sessionToken = generateSessionToken();
                    java.util.Date expiresAt = new java.util.Date(System.currentTimeMillis() + 3600000); // Срок действия токена: 1 час
                    saveUserSession(rs.getInt("id"), sessionToken, expiresAt);
                    return ResponseEntity.ok(sessionToken);
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to login");
        } finally {
            try {
                disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/user")
    public ResponseEntity<String> getUser(@RequestHeader("Authorization") String sessionToken) {
        try {
            connect();

            String query = "SELECT * FROM session WHERE session_token = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, sessionToken);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                java.util.Date expiresAt = rs.getTimestamp("expires_at");
                if (expiresAt.after(new java.util.Date())) {
                    int userId = rs.getInt("user_id");
                    PreparedStatement userStmt = connection.prepareStatement("SELECT * FROM user WHERE id = ?");
                    userStmt.setInt(1, userId);
                    ResultSet userRs = userStmt.executeQuery();
                    if (userRs.next()) {
                        String username = userRs.getString("username");
                        String email = userRs.getString("email");
                        String role = userRs.getString("role");
                        return ResponseEntity.ok("Username: " + username + "\nEmail: " + email + "\nRole: " + role);
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired");
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve user info");
        } finally {
            try {
                disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean validateEmail(String email) {
        return email.contains("@");
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }

    private void saveUserSession(int userId, String sessionToken, Date expiresAt) {
        try {
            String query = "INSERT INTO session (user_id, session_token, expires_at) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setString(2, sessionToken);
            statement.setTimestamp(3, new Timestamp(expiresAt.getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}