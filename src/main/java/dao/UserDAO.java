package dao;

import model.User;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {
    // Parametri di connessione al database
    private String jdbcURL = "jdbc:mysql://localhost:3306/gdrcalendar";
    private String jdbcUsername = "root";
    private String jdbcPassword = "root";

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
            Class.forName("com.mysql.cj.jdbc.Driver");
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

            // Imposta i parametri per la query di inserimento
            statement.setString(1, user.getNome());
            statement.setString(2, user.getCognome());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPasswordHash()); // Password già hashata con BCrypt
            statement.setString(5, user.getCommunity());    // Nome della community

            // Esegue l'inserimento e verifica se almeno una riga è stata modificata
            rowInserted = statement.executeUpdate() > 0;

        } catch (SQLException e) {
            printSQLException(e); // Gestione dell'eccezione SQL
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

            return rs.next(); // Ritorna true se è stata trovata almeno una corrispondenza

        } catch (SQLException e) {
            printSQLException(e); // Gestione dell'errore SQL
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

            statement.executeUpdate(); // Inserisce il nuovo utente pending
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
        String sql = "DELETE FROM pending_users WHERE created_at < (NOW() - INTERVAL 5 MINUTE)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.executeUpdate(); // Rimuove tutti i record scaduti
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
                // L'utente esiste in pending_users, procedi con la migrazione

                // Inserisce l'utente confermato nella tabella `users`
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

                // Rimuove l'utente dalla tabella `pending_users` ora che è stato confermato
                String delete = "DELETE FROM pending_users WHERE token = ?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(delete)) {
                    deleteStmt.setString(1, token);
                    deleteStmt.executeUpdate();
                }

                return true;
            }

        } catch (SQLException e) {
            printSQLException(e); // Gestione dell'errore SQL
        }

        return false; // Nessun utente trovato con quel token
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
                // Recupera l'hash memorizzato nel database
                String storedHash = rs.getString("password_hash");
                // Verifica che la password inserita corrisponda all'hash usando BCrypt
                return BCrypt.checkpw(password, storedHash);
            }

        } catch (SQLException e) {
            printSQLException(e); // Gestione dell'errore SQL
        }

        return false; // Email non trovata o errore durante il controllo
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
                return rs.getString("ruolo"); // Restituisce il ruolo associato all'utente
            }

        } catch (SQLException e) {
            printSQLException(e); // Gestione dell'errore SQL
        }

        return null; // Nessun utente trovato con l'email fornita
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
            statement.executeUpdate(); // Aggiorna il campo reset_token per l'utente specificato

        } catch (SQLException e) {
            printSQLException(e); // Gestione dell'errore SQL
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

            // Restituisce true se almeno una riga è stata modificata (token valido)
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            printSQLException(e);
        }

        return false; // Token non trovato o errore
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
                e.printStackTrace(System.err); // Stampa l'intera traccia dello stack
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
            stmt.executeUpdate(); // Esegue aggiornamento se oldEmail esiste
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

            // Hash della nuova password con BCrypt prima dell'aggiornamento
            stmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            stmt.setString(2, email);
            stmt.executeUpdate(); // Esegue aggiornamento password
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
            stmt.executeUpdate(); // Aggiorna il ruolo associato all'utente
        }
    }
}

