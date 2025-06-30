package dao;

import model.Evento;
import model.Prenotazione;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;


/**
 * Classe DAO (Data Access Object) che gestisce tutte le operazioni
 * di lettura e scrittura relative agli eventi nel database.
 */

public class EventDAO {
	
	// Parametri di connessione al database PostgreSQL
    private String jdbcURL = "jdbc:postgresql://dpg-d1ea5oili9vc739r5ekg-a.oregon-postgres.render.com/gdrcalendar";
    private String jdbcUsername = "gdrcalendar_user";
    private String jdbcPassword = "ihczieayR85gPZDqKDgDYmArgikrAk6q";

    // Crea una connessione al database. Carica il driver JDBC se necessario.  
    protected Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // Stampa errore se il driver non viene trovato
        }
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
    }

    
    // METODI DI LETTURA (GET) //
    
    
    /**
     * Il metodo getEventsForMonth recupera tutti gli eventi programmati per il mese specificato,
     * includendo per ciascun evento:
     *  - i dettagli statici (titolo, descrizione, date, luogo...),
     *  - lo stato attuale ("aperto", "chiuso", "terminato"),
     *  - l’elenco degli utenti prenotati.
     * Gli eventi sono raggruppati per giorno (formato: "yyyy-MM-dd").
     */
      
    public Map<String, List<Evento>> getEventsForMonth(YearMonth month) throws SQLException {
        Map<String, List<Evento>> result = new HashMap<>();

        // Definisce il range temporale che copre il mese richiesto
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);
        
        
        // Query per caricare gli eventi del mese + numero prenotati via subquery        
        String sql = "SELECT *, (SELECT COUNT(*) FROM prenotazioni WHERE evento_id = e.id) AS num_prenotati " +
                     "FROM eventi e WHERE data_inizio BETWEEN ? AND ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                // Costruisce l'oggetto Evento a partire dai dati ottenuti dal database
                Evento e = new Evento(
                        rs.getInt("id"),
                        rs.getString("titolo"),
                        rs.getString("descrizione"),
                        rs.getString("tipo_gioco"),
                        rs.getString("master"),
                        rs.getInt("max_giocatori"),
                        rs.getTimestamp("data_inizio").toLocalDateTime(),
                        rs.getTimestamp("data_fine").toLocalDateTime(),
                        rs.getString("luogo"),
                        rs.getString("note")
                );
                
                // Imposta il numero di prenotazionI
                e.setNumPrenotati(rs.getInt("num_prenotati"));

                // Calcola lo stato in base alla data e al numero prenotati
                LocalDateTime now = LocalDateTime.now();
                String status;
                if (!now.isBefore(e.getDataFine())) {
                    status = "terminato"; // Evento concluso
                } else if (e.getNumPrenotati() >= e.getMaxGiocatori()) {
                    status = "chiuso"; // Raggiunto numero massimo di prenotazioni
                } else {
                    status = "aperto";
                }
                e.setStatus(status);

                // Recupera le prenotazioni associate all'evento           
                List<Prenotazione> prenotati = new ArrayList<>();
                String sqlPrenotati = "SELECT email, note_prenotazione FROM prenotazioni WHERE evento_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlPrenotati)) {
                    ps.setInt(1, e.getId());
                    ResultSet prs = ps.executeQuery();
                    while (prs.next()) {
                        Prenotazione p = new Prenotazione(
                                prs.getString("email"),
                                prs.getString("note_prenotazione")
                        );
                        prenotati.add(p);
                    }
                }
                
                e.setPrenotati(prenotati);

                // Inserisce evento nella mappa in base alla data (formato "yyyy-MM-dd")
                String key = e.getDataInizio().toLocalDate().toString();
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            }
        }
        return result;
    }



    /**
     * Recupera tutti i dettagli di un singolo evento specifico, comprensivi di:
     * - numero prenotati (via subquery),
     * - stato attuale (aperto/chiuso/terminato),
     * - verifica se l'utente è prenotato,
     * - verifica se l'utente è un master,
     * - elenco delle prenotazioni associate.
     */
    
    
    public Evento getEventDetails(int id, String userEmail, String userRuolo) throws SQLException {
        String sql = "SELECT *, (SELECT COUNT(*) FROM prenotazioni WHERE evento_id = e.id) AS num_prenotati " +
                     "FROM eventi e WHERE id = ?";
        
        Evento e = null;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
            	// Costruzione dell’oggetto Evento dal risultato della query
                e = new Evento(
                        rs.getInt("id"),
                        rs.getString("titolo"),
                        rs.getString("descrizione"),
                        rs.getString("tipo_gioco"),
                        rs.getString("master"),
                        rs.getInt("max_giocatori"),
                        rs.getTimestamp("data_inizio").toLocalDateTime(),
                        rs.getTimestamp("data_fine").toLocalDateTime(),
                        rs.getString("luogo"),
                        rs.getString("note")
                );

                // Imposta il numero attuale di prenotati (via subquery)
                int numPrenotati = rs.getInt("num_prenotati");
                e.setNumPrenotati(numPrenotati);

                // Calcolo dello stato corrente dell'evento
                LocalDateTime now = LocalDateTime.now();
                String status;
                if (!now.isBefore(e.getDataFine())) {
                    status = "terminato";
                } else if (numPrenotati >= e.getMaxGiocatori()) {
                    status = "chiuso";
                } else {
                    status = "aperto";
                }
                e.setStatus(status);

                // Verifica se l'utente ha già prenotato questo evento
                boolean isBooked = false;
                String checkSql = "SELECT 1 FROM prenotazioni WHERE evento_id = ? AND email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, id);
                    checkStmt.setString(2, userEmail);
                    ResultSet checkRs = checkStmt.executeQuery();
                    if (checkRs.next()) {
                        isBooked = true;
                    }
                }
                e.setIsBooked(isBooked);

                // Verifica se l'utente ha ruolo Master
                boolean isMaster = "Master".equals(userRuolo);
                e.setIsMaster(isMaster);

                // Recupera le prenotazioni associate all'evento  
                List<Prenotazione> prenotati = new ArrayList<>();
                String sqlPrenotati = "SELECT email, note_prenotazione FROM prenotazioni WHERE evento_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlPrenotati)) {
                    ps.setInt(1, id);
                    ResultSet prs = ps.executeQuery();
                    while (prs.next()) {
                        Prenotazione p = new Prenotazione(
                                prs.getString("email"),
                                prs.getString("note_prenotazione")
                        );
                        prenotati.add(p);
                    }
                }
                e.setPrenotati(prenotati);
            }
        }
        return e;
    }

    
    /**
     * Restituisce l'elenco dei tipi di gioco per cui un utente ha prenotazioni attive.
     * Considera solo eventi non ancora terminati (data_fine > data corrente).
     */
    
    public Set<String> getActiveGameTypesBooked(String email) throws SQLException {
        Set<String> tipiPrenotati = new HashSet<>();

        String sql = "SELECT DISTINCT e.tipo_gioco " +
                     "FROM prenotazioni p " +
                     "JOIN eventi e ON p.evento_id = e.id " +
                     "WHERE p.email = ? " +
                     "AND e.data_fine > ?"; 

        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setTimestamp(2, Timestamp.valueOf(now));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tipiPrenotati.add(rs.getString("tipo_gioco"));
            }
        }

        return tipiPrenotati;
    }
    
    
    //METODI PER CREARE/CANCELLARE UNA PRENOTAZIONE //
    
    
    // Registra la prenotazione ad un evento
    
    public boolean bookEvent(int id, String email, String notePrenotazione) throws SQLException {
    	
    	// Recupera il tipo di gioco dell'evento
        String getTipoSql = "SELECT tipo_gioco FROM eventi WHERE id = ?";

        // Verifica se l'utente ha già una prenotazione attiva per lo stesso tipo di gioco in un altro evento
        String checkSql = "SELECT 1 FROM prenotazioni p " +
                          "JOIN eventi e ON p.evento_id = e.id " +
                          "WHERE p.email = ? AND e.tipo_gioco = ? AND e.id != ? " +
                          "AND e.data_fine > NOW()";

        // Verifica se l'utente ha già prenotato questo evento
        String alreadyBookedSql = "SELECT 1 FROM prenotazioni WHERE evento_id = ? AND email = ?";

        try (Connection conn = getConnection()) {
            String tipoGioco;
            try (PreparedStatement ps = conn.prepareStatement(getTipoSql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tipoGioco = rs.getString("tipo_gioco");
                } else {
                    throw new SQLException("Evento non trovato");
                }
            }

            // Controlla se l'utente ha già prenotato questo evento
            try (PreparedStatement ps = conn.prepareStatement(alreadyBookedSql)) {
                ps.setInt(1, id);
                ps.setString(2, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return true; // Prenotazione già esistente
                }
            }

            // Verifica se l'utente ha prenotazioni attive per lo stesso tipo di gioco
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, email);
                ps.setString(2, tipoGioco);
                ps.setInt(3, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return false; // Non può prenotare un altro evento dello stesso tipo
                }
            }

            // Inserisce una nuova prenotazione
            String sqlInsert = "INSERT INTO prenotazioni (evento_id, email, note_prenotazione) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setInt(1, id);
                ps.setString(2, email);
                ps.setString(3, notePrenotazione);
                ps.executeUpdate();
                return true;
            }
        }
    }
    

  
     // Annulla la prenotazione di un utente per un evento.
   
    public boolean cancelBooking(int id, String email) throws SQLException {
        String sqlDelete = "DELETE FROM prenotazioni WHERE evento_id = ? AND email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDelete)) {
            ps.setInt(1, id);
            ps.setString(2, email);
            ps.executeUpdate();
            return true;
        }
    }

    
    // OPERAZIONI CRUD SUGLI EVENTI //
    
    
    
    // Crea un nuovo evento nel database.
  
    public boolean createEvent(String titolo, String descrizione, String tipoGioco, String masterName,
                               int maxGiocatori, LocalDateTime dataInizio, LocalDateTime dataFine,
                               String luogo, String note) throws SQLException {
    	
        String sql = "INSERT INTO eventi (titolo, descrizione, tipo_gioco, master, max_giocatori, " +
                     "data_inizio, data_fine, luogo, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
        	
            ps.setString(1, titolo);
            ps.setString(2, descrizione);
            ps.setString(3, tipoGioco);
            ps.setString(4, masterName); //usa il nome master, non la mail
            ps.setInt(5, maxGiocatori);
            ps.setTimestamp(6, Timestamp.valueOf(dataInizio));
            ps.setTimestamp(7, Timestamp.valueOf(dataFine));
            ps.setString(8, luogo);
            ps.setString(9, note);
            
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }
    
    
    
    // Elimina un evento e tutte le relative prenotazioni
    
    public boolean deleteEvent(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Inizia una transazione
            
            try (PreparedStatement delP = conn.prepareStatement("DELETE FROM prenotazioni WHERE evento_id = ?");
                 PreparedStatement delE = conn.prepareStatement("DELETE FROM eventi WHERE id = ?")) {

                delP.setInt(1, id);
                delP.executeUpdate();  // Cancella prima le prenotazioni

                delE.setInt(1, id);
                int rows = delE.executeUpdate(); // Poi cancella l'evento

                conn.commit(); // Conferma la transazione
                return rows > 0; 
            } catch (SQLException ex) {
                conn.rollback(); // In caso di errore annulla tutte le operazioni
                throw ex;
            }
        }
    }

   
    
    //  Modifica i dati di un evento esistente.
       
    public boolean updateEvent(int id, String titolo, String descrizione, String tipoGioco,
                               String masterName, int maxGiocatori, LocalDateTime dataInizio,
                               LocalDateTime dataFine, String luogo, String note) throws SQLException {
    	
        String sql = "UPDATE eventi SET titolo = ?, descrizione = ?, tipo_gioco = ?, master = ?, " +
                     "max_giocatori = ?, data_inizio = ?, data_fine = ?, luogo = ?, note = ? " +
                     "WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
        	
            ps.setString(1, titolo);
            ps.setString(2, descrizione);
            ps.setString(3, tipoGioco);
            ps.setString(4, masterName);
            ps.setInt(5, maxGiocatori);
            ps.setTimestamp(6, Timestamp.valueOf(dataInizio));
            ps.setTimestamp(7, Timestamp.valueOf(dataFine));
            ps.setString(8, luogo);
            ps.setString(9, note);
            ps.setInt(10, id);
            
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }
    
 }
