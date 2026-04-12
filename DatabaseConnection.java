    package vehiclemaintenancelog;

    import java.sql.Connection;
    import java.sql.DriverManager;
    import java.sql.SQLException;
    import javax.swing.JOptionPane;

    /**
     * DatabaseConnection.java
     * Handles MySQL database connection
     */
    public class DatabaseConnection {

        private static final String HOST = "localhost";
        private static final String PORT = "3306";
        private static final String DATABASE = "vehicle_db";
        private static final String USERNAME = "root";
        private static final String PASSWORD = ""; // Change to your MySQL password

        private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        private static Connection connection = null;

        public static Connection getConnection() {
            try {
                if (connection == null || connection.isClosed()) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                }
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                        "MySQL Driver not found!\nMake sure mysql-connector-java.jar is in your Libraries.",
                        "Driver Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Database connection failed!\n" + e.getMessage()
                        + "\n\nMake sure:\n1. MySQL server is running\n2. Database 'vehicle_maintenance_db' exists\n3. Username/Password is correct",
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
            return connection;
        }

        public static void closeConnection() {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }

        public static boolean testConnection() {
            return getConnection() != null;
        }
    }
