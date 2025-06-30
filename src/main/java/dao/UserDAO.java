package dao;

import model.User;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;


/**
 * Classe DAO (Data Access Object) che gestisce tutte le operazioni
 * di lettura e scrittura relative agli utenti nel database.
 */


public class UserDAO {
    
	// Parametri di connessione al database PostgreSQL
    private String jdbcURL = "jdbc:postgresql://dpg-d1ea5oili9vc739r5ekg-a.oregon-postgres.render.com/gdrcalendar";
    private String jdbcUsername = "gdrcalendar_user";
    private String jdbcPassword = "ihczieayR85gPZDqKDgDYmArgikrAk6q";

   
    // Query SQL per inserimento utente, il ruolo è impostato fisso su 'Giocatore'
    // La community è selezionata tramite subquery
    
    private static final String INSERT_USER_SQL =
            "INSERT INTO users (nome, cognome, email, password_hash, ruolo, community_id) " +
            "VALUES (?, ?, ?, ?, 'Giocatore', (SELECT id FROM community WHERE nome = ?))";

    public UserDAO() {}

    // Crea una connessione al database. Carica il driver JDBC se necessario.
    protected Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // Stampa errore se il driver non viene trovato
        }
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
    }

    // Inserisce l'utente nel database solo se ha confermato la registrazione.
    // Imposta il ruolo di default a 'Giocatore' e associa l'utente alla community indicata.
    
    public boolean registerUser(User user) throws SQLException {
        boolean rowInserted = false;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_USER_SQL)) {

            statement.setString(1, user.getNome());
            statement.setString(2, user.getCognome());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash());
            statement.setString(5, user.getCommunity());

            rowInserted = statement.executeUpdate() > 0;
        } catch (SQLException e) {
            printSQLException(e);
        }
        return rowInserted;
    }

    // Controlla se l'email è già stata usata, sia da un utente registrato che da uno in attesa di conferma.
    
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT email FROM users WHERE email = ? " +
                     "UNION SELECT email FROM pending_users WHERE email = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setString(2, email);
            ResultSet rs = statement.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            printSQLException(e);
        }
        return false;
    }

    // Salva temporaneamente l'utente in attesa di conferma email.
    // Usa il token per collegare la mail di conferma alla registrazione.
    
    public void savePendingUser(String nome, String cognome, String email, String passwordHash, String community, String token) throws SQLException {
        String sql = "INSERT INTO pending_users (nome, cognome, email, password_hash, ruolo, community_id, token, created_at) " +
                     "VALUES (?, ?, ?, ?, 'Giocatore', (SELECT id FROM community WHERE nome = ?), ?, NOW())";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nome);
            statement.setString(2, cognome);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.setString(5, community);
            statement.setString(6, token);
            statement.executeUpdate();
        } catch (SQLException e) {
            printSQLException(e);
        }
    }
    
    // Rimuove le registrazioni non confermate dopo 5 minuti.
 
    public void deleteExpiredPendingUsers() throws SQLException {
        String sql = "DELETE FROM pending_users WHERE created_at < NOW() - INTERVAL '5 minutes'";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            printSQLException(e);
        }
    }
    

    // Conferma l'utente usando il token ricevuto via email.
    // Se valido, l'utente viene spostato da pending_users a users.
    
    public boolean confirmUser(String token) throws SQLException {
        String sql = "SELECT * FROM pending_users WHERE token = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, token);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String insert = "INSERT INTO users (nome, cognome, email, password_hash, ruolo, community_id) " +
                                "VALUES (?, ?, ?, ?, 'Giocatore', ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
                    insertStmt.setString(1, rs.getString("nome"));
                    insertStmt.setString(2, rs.getString("cognome"));
                    insertStmt.setString(3, rs.getString("email"));
                    insertStmt.setString(4, rs.getString("password_hash"));
                    insertStmt.setInt(5, rs.getInt("community_id"));
                    insertStmt.executeUpdate();
                }

                String delete = "DELETE FROM pending_users WHERE token = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(delete)) {
                    deleteStmt.setString(1, token);
                    deleteStmt.executeUpdate();
                }

                return true;
            }

        } catch (SQLException e) {
            printSQLException(e);
        }

        return false;
    }

    // Verifica le credenziali email + password controllando l’hash salvato.
    // Usa BCrypt per proteggere la password.
    
    public boolean validateUser(String email, String password) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE email = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                return BCrypt.checkpw(password, storedHash);
            }

        } catch (SQLException e) {
            printSQLException(e);
        }

        return false;
    }

    // Ritorna il ruolo dell’utente in base all’email.
    
    public String getRuoloByEmail(String email) throws SQLException {
        String sql = "SELECT ruolo FROM users WHERE email = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return rs.getString("ruolo");
            }

        } catch (SQLException e) {
            printSQLException(e);
        }

        return null;
    }
    
    // Salva il token per il reset password richiesto dall’utente
    
    public void saveResetToken(String email, String token) throws SQLException {
        String sql = "UPDATE users SET reset_token = ? WHERE email = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, token);
            statement.setString(2, email);
            statement.executeUpdate();
        } catch (SQLException e) {
            printSQLException(e);
        }
    }

    // Sostituisce la vecchia password con quella nuova, se il token è valido
    
    public boolean updatePasswordByToken(String token, String newHashed) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, reset_token = NULL WHERE reset_token = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newHashed);
            statement.setString(2, token);
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            printSQLException(e);
        }

        return false;
    }

    
    // Stampa a console i dettagli dell'eccezione SQL.
    private void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Aggiorna l'indirizzo email di un utente.
     * Questo metodo aggiorna sia la tabella `users` che la tabella `prenotazioni per mantenere la coerenza tra i dati.
     *
     */
    
    public void updateEmail(String oldEmail, String newEmail) throws SQLException {
        String updateUserSql = "UPDATE users SET email = ? WHERE email = ?";
        String updatePrenotazioniSql = "UPDATE prenotazioni SET email = ? WHERE email = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Avvia una transazione

            try (
                PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql);
                PreparedStatement updatePrenotazioniStmt = conn.prepareStatement(updatePrenotazioniSql)
            ) {
                // Aggiorna l'email nella tabella utenti
                updateUserStmt.setString(1, newEmail);
                updateUserStmt.setString(2, oldEmail);
                updateUserStmt.executeUpdate();

                // Aggiorna l'email anche nelle prenotazioni legate a quell'utente
                updatePrenotazioniStmt.setString(1, newEmail);
                updatePrenotazioniStmt.setString(2, oldEmail);
                updatePrenotazioniStmt.executeUpdate();

                // Conferma la transazione
                conn.commit();
            } catch (SQLException e) {
                conn.rollback(); // In caso di errore, annulla tutto
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    //  Aggiorna la password dell'utente con una nuova, CON hash BCrypt.
    public void updatePassword(String email, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    // Aggiorna il ruolo assegnato ad un tuente
    public void updateUserRole(String email, String newRole) throws SQLException {
        String sql = "UPDATE users SET ruolo = ? WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }
    
   
    // Elimina tutte le prenotazioni fatte da un utente, identificate dalla sua email
    public void deletePrenotazioniByEmail(String email) throws SQLException {
        String sql = "DELETE FROM prenotazioni WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        }
    }

   
    // Elimina l'utente
    public void deleteUserByEmail(String email) throws SQLException {
        String sql = "DELETE FROM users WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        }
    }

    
}
