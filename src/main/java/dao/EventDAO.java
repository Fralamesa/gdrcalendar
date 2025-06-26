package dao;

import model.Evento;
import model.Prenotazione;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

public class EventDAO {
	// Parametri di connessione al database PostgreSQL su Render
    private String jdbcURL = "jdbc:postgresql://dpg-d1ea5oili9vc739r5ekg-a.oregon-postgres.render.com/gdrcalendar";
    private String jdbcUsername = "gdrcalendar_user";
    private String jdbcPassword = "ihczieayR85gPZDqKDgDYmArgikrAk6q";

    // Ottiene una connessione al database PostgreSQL
    protected Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
    }

    /** 
     * Carica eventi per un mese specifico, inclusi dettagli e stato dell'evento,
     * e le prenotazioni associate a ciascun evento.
     * @param month Mese di cui si vogliono ottenere gli eventi
     * @return Mappa che associa date (in formato yyyy-MM-dd) a una lista di eventi presenti in quella giornata
     */
    public Map<String, List<Evento>> getEventsForMonth(YearMonth month) throws SQLException {
        Map<String, List<Evento>> result = new HashMap<>();

        // Query per ottenere gli eventi all'interno dell'intervallo temporale specificato,
        // includendo il numero di prenotazioni già effettuate per ciascun evento
        String sql = "SELECT *, (SELECT COUNT(*) FROM prenotazioni WHERE evento_id = e.id) AS num_prenotati " +
                     "FROM eventi e WHERE data_inizio BETWEEN ? AND ?";

        // Definizione dell'intervallo di date: primo giorno del mese fino all'ultimo, intera giornata
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

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
                
                // Imposta il numero di prenotazioni calcolato tramite subquery
                e.setNumPrenotati(rs.getInt("num_prenotati"));

                // Determinazione dello stato dell'evento basato su data e disponibilità posti
                LocalDateTime now = LocalDateTime.now();
                String status;
                if (!now.isBefore(e.getDataFine())) {
                    status = "terminato"; // Evento già concluso
                } else if (e.getNumPrenotati() >= e.getMaxGiocatori()) {
                    status = "chiuso"; // Raggiunto il numero massimo di prenotazioni
                } else {
                    status = "aperto"; // Ancora prenotabile
                }
                e.setStatus(status);

                // Carica la lista delle prenotazioni associate all'evento corrente
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

                // Inserisce l'evento nella mappa, utilizzando la data di inizio come chiave (formato yyyy-MM-dd)
                String key = e.getDataInizio().toLocalDate().toString();
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            }
        }
        return result;
    }



    /** Dettaglio evento con descrizione **/
    public Evento getEventDetails(int id, String userEmail, String userRuolo) throws SQLException {
        String sql = "SELECT *, (SELECT COUNT(*) FROM prenotazioni WHERE evento_id = e.id) AS num_prenotati " +
                     "FROM eventi e WHERE id = ?";
        Evento e = null;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Costruisce oggetto Evento dal risultato della query
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

                // Numero prenotati calcolato tramite subquery
                int numPrenotati = rs.getInt("num_prenotati");
                e.setNumPrenotati(numPrenotati);

                // Determina lo stato dell'evento: terminato / chiuso / aperto
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

                // Verifica se l'utente ha ruolo "Master"
                boolean isMaster = "Master".equals(userRuolo);
                e.setIsMaster(isMaster);

                // Carica elenco prenotati per l'evento
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

    /** Effettua la prenotazione a un evento **/
    public boolean bookEvent(int id, String email, String notePrenotazione) throws SQLException {
        // Recupera il tipo di gioco dell'evento
        String getTipoSql = "SELECT tipo_gioco FROM eventi WHERE id = ?";

        // Verifica se l'utente ha già una prenotazione attiva per lo stesso tipo di gioco in un altro evento
        String checkSql = "SELECT 1 FROM prenotazioni p " +
                          "JOIN eventi e ON p.evento_id = e.id " +
                          "WHERE p.email = ? AND e.tipo_gioco = ? AND e.id != ? " +
                          "AND e.data_fine >= NOW()";

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

    /** Annulla prenotazione esistente di un utente per uno specifico evento **/
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

    /** Elimina un evento e tutte le sue prenotazioni, usando transazioni per consistenza **/
    public boolean deleteEvent(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Inizio transazione
            try (PreparedStatement delP = conn.prepareStatement("DELETE FROM prenotazioni WHERE evento_id = ?");
                 PreparedStatement delE = conn.prepareStatement("DELETE FROM eventi WHERE id = ?")) {

                delP.setInt(1, id);
                delP.executeUpdate(); // Elimina prima le prenotazioni

                delE.setInt(1, id);
                int rows = delE.executeUpdate(); // Poi elimina l'evento

                conn.commit(); // Commit transazione
                return rows > 0; // Ritorna true se almeno una riga evento è stata eliminata
            } catch (SQLException ex) {
                conn.rollback(); // Rollback in caso di errore
                throw ex;
            }
        }
    }

    /** Recupera i tipi di gioco prenotati da un utente in un mese specifico **/
    public Set<String> getGameTypesBookedByUserInMonth(String email, YearMonth month) throws SQLException {
        Set<String> tipiPrenotati = new HashSet<>();

        String sql = "SELECT DISTINCT e.tipo_gioco " +
                     "FROM prenotazioni p " +
                     "JOIN eventi e ON p.evento_id = e.id " +
                     "WHERE p.email = ? " +
                     "AND e.data_inizio BETWEEN ? AND ?";

        // Calcolo dell'intervallo temporale del mese
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tipiPrenotati.add(rs.getString("tipo_gioco")); // Aggiunge solo tipi unici
            }
        }

        return tipiPrenotati;
    }

    
    /** Crea evento con descrizione*/
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

    /** Modifica evento**/
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
            ps.setString(4, masterName); // usa il nome master aggiornato
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
