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
 * includendo informazioni sullo stato e prenotabilità di ciascun evento
 * per l'utente autenticato.
 */
@WebServlet("/CalendarServlet")
public class CalendarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private EventDAO eventDAO;
    private Gson gson;

    /**
     * Inizializza il DAO per l'accesso agli eventi e il parser JSON (Gson)
     * con supporto alla serializzazione di LocalDateTime.
     */
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

        // Verifica formato corretto del parametro (es. 2025-07)
        if (monthParam == null || !monthParam.matches("\\d{4}-\\d{2}")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Param month=YYYY-MM richiesto");
            return;
        }

        // Verifica sessione attiva e utente autenticato
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

            // Recupera i tipi di gioco già prenotati dall'utente nel mese
            Set<String> tipiPrenotati = eventDAO.getActiveGameTypesBooked(userEmail);

            // Mappa di risposta: data (yyyy-MM-dd) -> lista eventi con flag prenotabilità
            Map<String, List<Map<String, Object>>> responseMap = new HashMap<>();

            for (Map.Entry<String, List<Evento>> entry : rawEvents.entrySet()) {
                String date = entry.getKey();
                List<Evento> eventiGiorno = entry.getValue();

                List<Map<String, Object>> eventiConFlag = new ArrayList<>();

                for (Evento ev : eventiGiorno) {
                    // Determinazione flag dinamici per frontend
                    boolean isBooked = ev.getPrenotati() != null &&
                            ev.getPrenotati().stream().anyMatch(p -> userEmail.equals(p.getEmail()));

                    boolean isFull = ev.getNumPrenotati() >= ev.getMaxGiocatori();
                    boolean isExpired = ev.getDataFine().isBefore(LocalDateTime.now());
                    boolean isSameTypeBooked = tipiPrenotati.contains(ev.getTipoGioco());

                    boolean isBookable = ev.getStatus().equalsIgnoreCase("aperto")
                            && !isFull
                            && !isBooked
                            && !isSameTypeBooked
                            && !isExpired;

                    // Costruzione mappa evento da serializzare
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

            // Scrittura risposta JSON
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(responseMap));

        } catch (SQLException e) {
            e.printStackTrace(); // Per debug server
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore DB");
        }
    }
}
