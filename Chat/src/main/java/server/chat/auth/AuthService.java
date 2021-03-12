package server.chat.auth;

import java.sql.SQLException;

public interface AuthService {
    String getUsernameByLoginAndPassword(String login, String password) throws SQLException, ClassNotFoundException;
    boolean isUsernameBusy(String newUsername) throws SQLException, ClassNotFoundException;
    String changeNickname(String username, String newUsername) throws SQLException, ClassNotFoundException;
}
