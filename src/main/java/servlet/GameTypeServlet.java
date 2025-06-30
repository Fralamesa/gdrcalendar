package servlet;

import com.google.gson.Gson;
import dao.GameTypeDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Servlet per la gestione dei tipi di gioco.
 * Supporta:
 * - elencare i tipi disponibili (GET)
 * - aggiungere o eliminare un tipo di gioco (POST)
 */

@WebServlet("/GameTypeServlet")
public class GameTypeServlet extends HttpServlet {
    private GameTypeDAO gameTypeDAO;
    private Gson gson;

    // Inizializza GameTypeDAO e il parser JSON (Gson)
    
    @Override
    public void init() {
        gameTypeDAO = new GameTypeDAO();
        gson = new Gson();
    }

    /**
     * Gestisce richieste GET per ottenere tutti i tipi di gioco.
     */
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("list".equalsIgnoreCase(action)) {
            try {
                List<String> list = gameTypeDAO.getAllGameTypes();
                response.setContentType("application/json");
                response.getWriter().write(gson.toJson(list));
            } catch (SQLException e) {
                e.printStackTrace(); // Log server
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore DB");
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Action non valida");
        }
    }

    /**
     * Gestisce richieste POST per aggiunta o eliminazione di tipi di gioco.
     */
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        String nome = request.getParameter("nome");

        if (nome == null || nome.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nome mancante");
            return;
        }

        try {
            boolean success = false;

            // Aggiunta nuovo tipo di gioco
            if ("add".equalsIgnoreCase(action)) {
                success = gameTypeDAO.addGameType(nome);
            }

            // Eliminazione tipo di gioco
            else if ("delete".equalsIgnoreCase(action)) {
                success = gameTypeDAO.deleteGameType(nome);

                // Fallimento 
                if (!success) {
                    response.getWriter().write(
                        "Impossibile eliminare: ci sono eventi ancora attivi con questo tipo di gioco!"
                    );
                    return;
                }
            }
            
            // Azione non riconosciuta
            else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Action non valida");
                return;
            }

            response.getWriter().write(success ? "OK" : "Errore");

        } catch (SQLException e) {
            // PostgreSQL: 23505 = unique_violation
            if ("23505".equals(e.getSQLState())) {
                response.getWriter().write("Questo tipo di gioco esiste già!");
            } else {
                e.printStackTrace(); // Per debug server
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore DB");
            }
        }
    }
}



