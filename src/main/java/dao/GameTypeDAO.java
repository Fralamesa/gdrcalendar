package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;



/**
 * Classe DAO (Data Access Object) che gestisce tutte le operazioni
 * di lettura e scrittura relative ai tipi di gioco nel database.
 */

public class GameTypeDAO {
	
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

    
    // Recupera tutti i tipi di gioco presenti nel database.
  
    public List<String> getAllGameTypes() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT nome FROM tipi_gioco ORDER BY nome";       
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getString("nome"));
            }
        }

        return list;
    }

    
    // OPERAZIONI CRUD SUI TIPI DI GIOCO //
    
    // Inserisce un nuovo tipo di gioco nel database.
   
    public boolean addGameType(String nome) throws SQLException {
        String sql = "INSERT INTO tipi_gioco (nome) VALUES (?)";
     
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nome);
            ps.executeUpdate();
            return true;
        }
    }

 
    // Elimina tipo di gioco solo se NON usato in eventi aperti
   
    public boolean deleteGameType(String nome) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM eventi WHERE tipo_gioco = ? AND data_fine >= NOW()";
        String deleteSql = "DELETE FROM tipi_gioco WHERE nome = ?";

        try (Connection conn = getConnection()) {
            // Controlla se esistono eventi ancora attivi con quel tipo
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, nome);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; 
                }
            }

            // Se OK, elimina il tipo gioco
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, nome);
                ps.executeUpdate();
                return true;
            }
        }
    }
}

