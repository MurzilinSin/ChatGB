package server.chat.auth;

import server.chat.MyServer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BaseAuthService implements AuthService {
    public final List<String> usernames = new ArrayList<>();
    public static Connection connection;
    public static Statement stmt;
    public static ResultSet rs;

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) throws SQLException, ClassNotFoundException {
        connectionDB();
        rs = stmt.executeQuery(String.format("SELECT username FROM clients WHERE login = '%s' AND password = '%s'", login, password));
        String username = rs.getString("username");
        disconnectionDB();
        return username;
    }

    @Override
    public boolean isUsernameBusy(String newUsername) throws SQLException, ClassNotFoundException {
        connectionDB();
        List<String> nameClients = new ArrayList<>();
        ResultSet resultSet = stmt.executeQuery("SELECT username FROM clients");
        while (resultSet.next()){
            nameClients.add(resultSet.getString("username"));
        }
        for (int i = 0; i < nameClients.size(); i++) {
            if(newUsername.equals(nameClients.get(i))){
                return true;
            }
        }
        disconnectionDB();
        return false;
    }

    @Override
    public String changeNickname(String username, String newUsername) throws SQLException, ClassNotFoundException {
        connectionDB();
        stmt.executeUpdate(String.format("UPDATE clients SET username = '%s' WHERE username = '%s'", newUsername, username));
        usernames.add(usernames.indexOf(username)+1,newUsername);
        usernames.remove(username);
        disconnectionDB();
        return null;
    }

    private void connectionDB() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/db/clients.db");
        stmt = connection.createStatement();
    }

    private void disconnectionDB() throws SQLException {
        rs.close();
        stmt.close();
        connection.close();
    }
}