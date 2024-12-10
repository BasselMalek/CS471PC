package com.hugsforbugs.cs471pc;


import java.sql.*;

public class DatabaseConnection {
    private final String dbHost = "jdbc:mysql://localhost:3306/";
    private final String dbUser = "root";
    private final String dbPassword = "root";

    public DatabaseConnection(){}

    public Connection getConnection() throws SQLException, ClassNotFoundException {
        DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        return DriverManager.getConnection(dbHost, dbUser, dbPassword);
    }
}
