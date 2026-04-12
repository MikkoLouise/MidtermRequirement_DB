package vehiclemaintenance;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!DatabaseConnection.testConnection()) {
                    JOptionPane.showMessageDialog(null,
                            "Cannot connect to the database!\n\n"
                            + "Please make sure:\n"
                            + "1. MySQL server is running\n"
                            + "2. Database 'vehicle_db' exists\n"
                            + "3. Username/password in DatabaseConnection.java is correct\n"
                            + "4. MySQL Connector JAR is added to Libraries",
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                new LoginFrame().setVisible(true);
            }
        });
    }
}
