package admin;

import DAO.ArticleDAO;
import DAO.H2Connection;
import DAO.SessionDAO;
import DAO.UserDAO;
import model.User;
import webserver.HTTPExceptions;

import java.sql.Connection;
import java.sql.Statement;

public class AdminDAO {
    public static void main(String[] args) {
    }

    private void createTable() {
        String sql = """
                DROP TABLE IF EXISTS users;
                CREATE TABLE IF NOT EXISTS users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                userId VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255)
                );
                
                DROP TABLE IF EXISTS sessions;
                CREATE TABLE IF NOT EXISTS sessions (
                id INT PRIMARY KEY AUTO_INCREMENT,
                sessionId VARCHAR(255) NOT NULL,
                userId VARCHAR(255) NOT NULL,
                lastAccessTime TIME NOT NULL,
                maxInactiveInterval INT NOT NULL
                );
                
                DROP TABLE IF EXISTS articles;
                CREATE TABLE IF NOT EXISTS articles (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    content VARCHAR(255) NOT NULL,
                    authorId VARCHAR(255) NOT NULL,
                    authorName VARCHAR(255) NOT NULL,
                    createTime TIME NOT NULL
                );
                """;
        try (Connection connection = H2Connection.getConnection()) {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (Exception e) {
            throw new HTTPExceptions.Error500("refresh table failed");
        }
    }

    private void deleteAllUsers() {
        UserDAO userDAO = new UserDAO();
        userDAO.deleteAllUsers();
    }

    private void deleteAllSessions() {
        SessionDAO sessionDAO = new SessionDAO();
        sessionDAO.deleteAllSessions();
    }

    private void deleteAllArticles() {
        ArticleDAO articleDAO = new ArticleDAO();
        articleDAO.deleteAllArticles();
    }

    private void addAdminUser() {
        UserDAO userDAO = new UserDAO();
        userDAO.addUser(new User("adminId", "adminPassword", "admin"));
    }
}
