package Midterm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionDB {
    
    //Database Credentials
    private static final String URL = "jdbc:mysql://localhost:3306/student_app";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    public static Connection getConnection(){
        Connection conn = null;
        
        try {
            conn = DriverManager.getConnection(URL,USER,PASSWORD);
            System.out.println("Database Connected Successfully!");
            
        } catch (SQLException e) {
            System.out.println("Connection Failed");
            e.printStackTrace();
        }
        
        return conn;
    }
}