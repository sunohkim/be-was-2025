package DAO;

import model.Article;
import webserver.HTTPExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;

public class ArticleDAO {
    public void addArticle(Article article) {
        String sql;
        Connection connection = null;
        try {
            connection = H2Connection.getConnection();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            if (article.getImageUrl() == null) {
                sql = "INSERT INTO articles (content, authorId, authorName, createTime) VALUES (? ,?, ?, ?)";
            } else {
                sql = "INSERT INTO articles (content, authorId, authorName, createTime, imageUrl) VALUES (?, ?, ?, ?, ?)";
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, article.getContent());
                preparedStatement.setString(2, article.getAuthorId());
                preparedStatement.setString(3, article.getAuthorName());
                preparedStatement.setTime(4, Time.valueOf(article.getCreateTime()));
                if (article.getImageUrl() != null) {
                    preparedStatement.setString(5, article.getImageUrl());
                }
                preparedStatement.executeUpdate();
            }

            connection.commit();
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                } finally {
                    throw new HTTPExceptions.Error500("Failed to commit transaction.");
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    connection.close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                    throw new HTTPExceptions.Error500("Failed to close transaction.");
                }
            }
        }
    }

    public Article getLatestArticle() {
        String sql = "SELECT * FROM articles ORDER BY id DESC LIMIT 1";
        try {
            Connection connection = H2Connection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new Article(
                        resultSet.getInt("id"),
                        resultSet.getString("content"),
                        resultSet.getString("authorId"),
                        resultSet.getString("authorName"),
                        resultSet.getTime("createTime").toLocalTime()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new HTTPExceptions.Error500("Failed to get latest article");
        }
        return null;
    }

    public Article getNextArticle(int id) {
        String sql = "SELECT * FROM articles WHERE id < (?) ORDER BY id DESC LIMIT 1";
        try {
            Connection connection = H2Connection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new Article(
                        resultSet.getInt("id"),
                        resultSet.getString("content"),
                        resultSet.getString("authorId"),
                        resultSet.getString("authorName"),
                        resultSet.getTime("createTime").toLocalTime()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new HTTPExceptions.Error500("Failed to get next article");
        }
        return null;
    }

    public Article getPreviousArticle(int id) {
        String sql = "SELECT * FROM articles WHERE id > (?) ORDER BY id ASC LIMIT 1";
        try {
            Connection connection = H2Connection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new Article(
                        resultSet.getInt("id"),
                        resultSet.getString("content"),
                        resultSet.getString("authorId"),
                        resultSet.getString("authorName"),
                        resultSet.getTime("createTime").toLocalTime()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new HTTPExceptions.Error500("Failed to get previous article");
        }
        return null;
    }

    public void deleteAllArticles() {
        String sql = "DELETE FROM articles";
        Connection connection = null;
        try {
            connection = H2Connection.getConnection();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.executeUpdate();
            }

            connection.commit();
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                } finally {
                    throw new HTTPExceptions.Error500("Failed to commit transaction.");
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                    connection.close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                    throw new HTTPExceptions.Error500("Failed to close transaction.");
                }
            }
        }
    }
}
