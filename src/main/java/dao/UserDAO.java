package dao;

import model.User;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {
    // Parametri di connessione al database PostgreSQL su Render
    private String jdbcURL = "jdbc:postgresql://dpg-d1ea5oili9vc739r5ekg-a.oregon-postgres.render.com/gdrcalendar";
    private String jdbcUsername = "gdrcalendar_user";
    private String jdbcPassword = "ihczieayR85gPZDqKDgDYmArgikrAk6q";

    // Query SQL per inserimento utente, il ruolo è impostato fisso su 'Giocatore'
    // La community è selezionata tramite subquery in base al nome fornito
    private static final String INSERT_USER_SQL =
            "INSERT INTO users (nome, cognome, email, password_hash, ruolo, community_id) " +
            "VALUES (?, ?, ?, ?, 'Giocatore', (SELECT id FROM community WHERE nome = ?))";

    public UserDAO() {}

    /**
     * Restituisce una connessione valida al database.
     * Carica il driver JDBC se non è già stato caricato.
     */
    protected Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // Stampa errore se il driver non viene trovato
        }
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
    }

    /**
     * Registra definitivamente un nuovo utente nel sistema (dopo conferma email).
     *
     * @param user Oggetto User contenente le informazioni dell'utente da salvare
     * @return true se l'inserimento nel database è avvenuto con successo
     */
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

    /**
     * Verifica se un'email è già presente nel sistema, sia tra gli utenti registrati
     * (tabella `users`) che tra quelli in attesa di conferma (tabella `pending_users`).
     *
     * @param email L'indirizzo email da verificare
     * @return true se l'email è già presente, false altrimenti
     */
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

    /**
     * Salva temporaneamente i dati dell'utente in `pending_users`, in attesa di conferma via email.
     * Include anche il token di verifica e timestamp di creazione.
     *
     * @param nome Nome dell'utente
     * @param cognome Cognome dell'utente
     * @param email Email dell'utente
     * @param passwordHash Password già hashata
     * @param community Nome della community di appartenenza
     * @param token Token univoco per la conferma email
     */
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
    
    /**
     * Elimina tutti gli utenti in attesa di conferma (`pending_users`) il cui timestamp
     * di creazione è più vecchio di 5 minuti.
     * Utile per mantenere pulita la tabella da registrazioni non confermate.
     */
    public void deleteExpiredPendingUsers() throws SQLException {
        String sql = "DELETE FROM pending_users WHERE created_at < NOW() - INTERVAL '5 minutes'";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            printSQLException(e);
        }
    }
    

    /**
     * Conferma l'utente utilizzando il token ricevuto via email.
     * Se il token è valido, sposta l'utente dalla tabella `pending_users` a `users`
     * e lo rimuove dalla lista dei registrati in attesa.
     *
     * @param token Token di conferma assegnato all'utente durante la registrazione
     * @return true se la conferma e la migrazione sono andate a buon fine, false altrimenti
     */
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

    /**
     * Verifica le credenziali di accesso di un utente.
     * Recupera l'hash della password associato all'email fornita e confronta
     * la password in chiaro con l'hash memorizzato usando BCrypt.
     *
     * @param email Email dell'utente
     * @param password Password in chiaro inserita dall'utente
     * @return true se le credenziali sono corrette, false altrimenti
     */
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

    /**
     * Restituisce il ruolo dell'utente associato a una determinata email.
     * Utile per determinare il tipo di accesso o reindirizzamento post-login.
     *
     * @param email Email dell'utente
     * @return Il ruolo dell'utente (es. "Giocatore", "Master", "Admin") oppure null se non trovato
     */
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
    
    /**
     * Salva un token di reset della password per l'utente identificato dall'email.
     * Questo token sarà utilizzato per verificare la richiesta di reset.
     *
     * @param email Email dell'utente che ha richiesto il reset
     * @param token Token generato per il reset della password
     */
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

    /**
     * Aggiorna la password dell'utente utilizzando un token di reset valido,
     * e invalida il token una volta usato.
     *
     * @param token Token di reset della password (precedentemente salvato)
     * @param newHashed Nuova password già hashata
     * @return true se l'aggiornamento ha avuto successo, false se il token non è valido o non esiste
     */
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

    /**
     * Stampa informazioni dettagliate sull'eccezione SQL ricevuta.
     * Utile per il debug durante lo sviluppo.
     *
     * @param ex Eccezione SQL da stampare
     */
    private void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Aggiorna l'indirizzo email di un utente.
     *
     * @param oldEmail L'attuale indirizzo email dell'utente
     * @param newEmail Il nuovo indirizzo email da assegnare
     */
    public void updateEmail(String oldEmail, String newEmail) throws SQLException {
        String sql = "UPDATE users SET email = ? WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newEmail);
            stmt.setString(2, oldEmail);
            stmt.executeUpdate();
        }
    }
    /**
     * Aggiorna la password di un utente, sostituendola con una nuova (hashata).
     * Utilizza BCrypt per la cifratura della password in chiaro prima del salvataggio.
     *
     * @param email Email dell'utente di cui aggiornare la password
     * @param newPassword Nuova password in chiaro da hashare e salvare
     */
    public void updatePassword(String email, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    /**
     * Aggiorna il ruolo assegnato a un utente.
     * Tipicamente usato da amministratori per promuovere/demotere un utente (es. Giocatore → Master).
     *
     * @param email Email dell'utente di cui modificare il ruolo
     * @param newRole Nuovo ruolo da assegnare (es. "Giocatore", "Master", "Admin")
     */
    public void updateUserRole(String email, String newRole) throws SQLException {
        String sql = "UPDATE users SET ruolo = ? WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }
}
