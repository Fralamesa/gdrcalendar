package servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.EventDAO;
import model.Evento;
import model.LocalDateTimeAdapter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Servlet che restituisce gli eventi del mese in formato JSON,
 * includendo informazioni sullo stato e prenotabilità di ciascun evento da parte dell'utente
 */

@WebServlet("/CalendarServlet")
public class CalendarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private EventDAO eventDAO;
    private Gson gson;

    // Inizializza il EventDAO e il parser JSON (Gson), aggiungendo il supporto per le date LocalDateTime

    @Override
    public void init() {
        eventDAO = new EventDAO();
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * Gestisce la richiesta GET, attesa con parametro `month=YYYY-MM`,
     * e restituisce gli eventi del mese organizzati per giorno.
     */
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String monthParam = request.getParameter("month");

        // Controlla che il parametro "month" sia presente e nel formato corretto "YYYY-MM" (es: 2025-07)

        
        if (monthParam == null || !monthParam.matches("\\d{4}-\\d{2}")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Param month=YYYY-MM richiesto");
            return;
        }

        // Verifica che l'utente sia autenticato (sessione valida con email presente)
        
        HttpSession session = request.getSession(false);
        String userEmail = session != null ? (String) session.getAttribute("userEmail") : null;

        if (userEmail == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utente non autenticato");
            return;
        }

        try {
            YearMonth ym = YearMonth.parse(monthParam);

            // Recupera eventi del mese richiesto, raggruppati per giorno
            Map<String, List<Evento>> rawEvents = eventDAO.getEventsForMonth(ym);

            // Recupera i tipi di gioco a cui l'utente ha già prenotato in eventi non ancora terminati
            Set<String> tipiPrenotati = eventDAO.getActiveGameTypesBooked(userEmail);

            // Inizializza la mappa di risposta: data -> lista di eventi con flag personalizzati
            // Scorre gli eventi di ogni giorno per preparare una struttura più semplice per il client
            
            Map<String, List<Map<String, Object>>> responseMap = new HashMap<>();

            for (Map.Entry<String, List<Evento>> entry : rawEvents.entrySet()) {
                String date = entry.getKey();
                List<Evento> eventiGiorno = entry.getValue();

                List<Map<String, Object>> eventiConFlag = new ArrayList<>();

                for (Evento ev : eventiGiorno) {
                	
                    // Determinazione flag dinamici per frontend
                	
                	// Verifica se l'utente ha già prenotato questo evento
                    boolean isBooked = ev.getPrenotati() != null &&
                            ev.getPrenotati().stream().anyMatch(p -> userEmail.equals(p.getEmail()));
                    // Controlla se l'evento ha raggiunto il limite massimo di prenotazioni
                    boolean isFull = ev.getNumPrenotati() >= ev.getMaxGiocatori();
                    // Verifica se l'evento è già terminato (data di fine passata)
                    boolean isExpired = ev.getDataFine().isBefore(LocalDateTime.now());
                    // Verifica se l'utente ha già prenotato altri eventi dello stesso tipo non ancora terminati
                    boolean isSameTypeBooked = tipiPrenotati.contains(ev.getTipoGioco());
                    // L'evento è prenotabile solo se tutte le condizioni precedenti non sono soddisfatte
                    boolean isBookable = ev.getStatus().equalsIgnoreCase("aperto")
                            && !isFull
                            && !isBooked
                            && !isSameTypeBooked
                            && !isExpired;

                    // Costruzione risposta JSON con tutti i dati richiesti
                    
                    Map<String, Object> evMap = new HashMap<>();
                    evMap.put("id", ev.getId());
                    evMap.put("titolo", ev.getTitolo());
                    evMap.put("tipoGioco", ev.getTipoGioco());
                    evMap.put("status", ev.getStatus());
                    evMap.put("numPrenotati", ev.getNumPrenotati());
                    evMap.put("maxGiocatori", ev.getMaxGiocatori());
                    evMap.put("luogo", ev.getLuogo());

                    evMap.put("isBooked", isBooked);
                    evMap.put("isFull", isFull);
                    evMap.put("isExpired", isExpired);
                    evMap.put("isSameTypeBooked", isSameTypeBooked);
                    evMap.put("isBookable", isBookable); // usato dal frontend per evidenziare

                    eventiConFlag.add(evMap);
                }

                responseMap.put(date, eventiConFlag);
            }

            // Risposta finale
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(responseMap));

        } catch (SQLException e) {
            e.printStackTrace(); // Per debug server
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore DB");
        }
    }
}
