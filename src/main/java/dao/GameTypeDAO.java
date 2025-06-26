package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameTypeDAO {
    // Parametri di connessione al database
    private String jdbcURL = "jdbc:mysql://localhost:3306/gdrcalendar";
    private String jdbcUsername = "root";
    private String jdbcPassword = "root";

    /**
     * Ottiene una connessione al database.
     * Carica dinamicamente il driver MySQL se non già caricato.
     */
    protected Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // In caso di errore nel caricamento del driver JDBC
        }
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
    }

    /**
     * Recupera tutti i tipi di gioco presenti nel database.
     * 
     * @return Lista ordinata alfabeticamente dei nomi dei tipi di gioco
     */
    public List<String> getAllGameTypes() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT nome FROM tipi_gioco ORDER BY nome";

        // Esegue la query e costruisce una lista di stringhe dai risultati
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getString("nome"));
            }
        }

        return list;
    }

    /**
     * Inserisce un nuovo tipo di gioco nel database.
     *
     * @param nome Nome del tipo di gioco da aggiungere
     * @return true se l'inserimento ha avuto successo
     */
    public boolean addGameType(String nome) throws SQLException {
        String sql = "INSERT INTO tipi_gioco (nome) VALUES (?)";

        // Inserisce un nuovo record nella tabella tipi_gioco
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nome);
            ps.executeUpdate();
            return true;
        }
    }


    /**Elimina tipo di gioco solo se NON usato in eventi aperti **/
    public boolean deleteGameType(String nome) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM eventi WHERE tipo_gioco = ? AND data_fine >= NOW()";
        String deleteSql = "DELETE FROM tipi_gioco WHERE nome = ?";

        try (Connection conn = getConnection()) {
            // Controlla se esistono eventi ancora attivi con quel tipo
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, nome);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // NON eliminare se ci sono eventi aperti
                }
            }

            // 2Se OK, elimina il tipo
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, nome);
                ps.executeUpdate();
                return true;
            }
        }
    }
}
