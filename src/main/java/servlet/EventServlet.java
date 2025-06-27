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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Servlet che gestisce le operazioni su un evento specifico:
 * - recupero dettagli evento
 * - annullamento prenotazione
 * - eliminazione evento (solo per utenti con ruolo Master)
 *
 * Il risultato viene restituito in formato JSON.
 */
@WebServlet("/EventServlet")
public class EventServlet extends HttpServlet {
    private EventDAO eventDAO;
    private Gson gson;

    /**
     * Inizializzazione della servlet: setup DAO e configurazione Gson per LocalDateTime.
     */
    @Override
    public void init() {
        eventDAO = new EventDAO();
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    /**
     * Gestisce richieste GET su un evento specifico, identificato dal parametro "id".
     * Supporta le azioni:
     * - cancel: annulla prenotazione
     * - delete: elimina evento (solo Master)
     * - nessuna azione: restituisce i dettagli dell'evento
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String idParam = request.getParameter("id");
        String action = request.getParameter("action");

        // Verifica che l'ID sia presente
        if (idParam == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID mancante");
            return;
        }

        int id = Integer.parseInt(idParam);

        try {
            HttpSession session = request.getSession();
            String userEmail = (String) session.getAttribute("userEmail");
            String userRuolo = (String) session.getAttribute("userRuolo");

            // Prenotazione non ammessa via GET
            if ("book".equalsIgnoreCase(action)) {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Usare POST per prenotare!");
                return;
            }

            // Annullamento prenotazione
            if ("cancel".equalsIgnoreCase(action)) {
                boolean cancelled = eventDAO.cancelBooking(id, userEmail);
                response.getWriter().write(cancelled
                        ? "Prenotazione annullata!"
                        : "Impossibile annullare la prenotazione!");
                return;
            }

            // Eliminazione evento (solo Master)
            if ("delete".equalsIgnoreCase(action)) {
                if (!"Master".equals(userRuolo)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Permesso negato");
                    return;
                }
                boolean deleted = eventDAO.deleteEvent(id);
                response.getWriter().write(deleted
                        ? "Evento eliminato!"
                        : "Impossibile eliminare l'evento.");
                return;
            }

            // Caricamento dettagli evento
            Evento evento = eventDAO.getEventDetails(id, userEmail, userRuolo);

            // Calcolo dei flag per la logica di prenotazione
            boolean isBooked = evento.getPrenotati().stream()
                    .anyMatch(p -> userEmail.equals(p.getEmail()));

            boolean isFull = evento.getNumPrenotati() >= evento.getMaxGiocatori();
            boolean isExpired = evento.getDataFine().isBefore(LocalDateTime.now());

            // Verifica se l'utente ha già prenotato altri eventi dello stesso tipo nel mese
            Set<String> tipiPrenotati = eventDAO.getGameTypesBookedByUserInMonth(
                    userEmail, YearMonth.from(evento.getDataInizio()));

            boolean isSameTypeBooked = tipiPrenotati.contains(evento.getTipoGioco());

            // L'utente può prenotare se:
            // - evento aperto
            // - non pieno
            // - non scaduto
            // - non ha già un evento dello stesso tipo (a meno che stia modificando se stesso)
            boolean isBookable = !isFull && !isExpired &&
                    evento.getStatus().equalsIgnoreCase("aperto") &&
                    (!isSameTypeBooked || isBooked);

            // Costruzione risposta JSON con tutti i dati richiesti
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("id", evento.getId());
            eventMap.put("titolo", evento.getTitolo());
            eventMap.put("descrizione", evento.getDescrizione());
            eventMap.put("tipoGioco", evento.getTipoGioco());
            eventMap.put("master", evento.getMaster());
            eventMap.put("maxGiocatori", evento.getMaxGiocatori());
            eventMap.put("numPrenotati", evento.getNumPrenotati());
            eventMap.put("dataInizio", evento.getDataInizio());
            eventMap.put("dataFine", evento.getDataFine());
            eventMap.put("note", evento.getNote());
            eventMap.put("luogo", evento.getLuogo());
            eventMap.put("status", evento.getStatus());
            eventMap.put("prenotati", evento.getPrenotati());
            eventMap.put("isMaster", "Master".equalsIgnoreCase(userRuolo));

            // Flag di logica lato client
            eventMap.put("isBooked", isBooked);
            eventMap.put("isFull", isFull);
            eventMap.put("isExpired", isExpired);
            eventMap.put("isSameTypeBooked", isSameTypeBooked);
            eventMap.put("isBookable", isBookable);

            // Risposta finale
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(eventMap));

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore server");
        }
    }



    /**
     * Gestisce operazioni di creazione, aggiornamento o prenotazione su un evento.
     * Richiede il parametro "action" con uno dei seguenti valori:
     * - create  → crea un nuovo evento (solo per Master)
     * - update  → aggiorna un evento esistente (solo per Master)
     * - book    → effettua una prenotazione a un evento
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        String userRuolo = (String) request.getSession().getAttribute("userRuolo");

        try {
            // === Creazione evento ===
            if ("create".equalsIgnoreCase(action)) {
                if (!"Master".equals(userRuolo)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Permesso negato");
                    return;
                }

                // Estrazione parametri evento
                String titolo = request.getParameter("titolo");
                String descrizione = request.getParameter("descrizione");
                String tipoGioco = request.getParameter("tipo_gioco");
                String masterName = request.getParameter("master");
                int maxGiocatori = Integer.parseInt(request.getParameter("max_giocatori"));
                LocalDateTime dataInizio = LocalDateTime.parse(request.getParameter("data_inizio"));
                LocalDateTime dataFine = LocalDateTime.parse(request.getParameter("data_fine"));
                String luogo = request.getParameter("luogo");
                String note = request.getParameter("note");

                // Controllo validità date
                if (dataFine.isBefore(dataInizio)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "La data di fine non può essere prima della data di inizio.");
                    return;
                }

                // Creazione evento
                boolean ok = eventDAO.createEvent(
                        titolo, descrizione, tipoGioco, masterName, maxGiocatori,
                        dataInizio, dataFine, luogo, note
                );

                response.getWriter().write(ok
                        ? "Evento creato!"
                        : "Errore durante la creazione.");

            }
            
         // === Aggiornamento evento ===
            else if ("update".equalsIgnoreCase(action)) {
                if (!"Master".equals(userRuolo)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Permesso negato");
                    return;
                }

                // Estrazione parametri aggiornamento
                int id = Integer.parseInt(request.getParameter("id"));
                String titolo = request.getParameter("titolo");
                String descrizione = request.getParameter("descrizione");
                String tipoGioco = request.getParameter("tipo_gioco");
                String masterName = request.getParameter("master");
                int maxGiocatori = Integer.parseInt(request.getParameter("max_giocatori"));
                LocalDateTime dataInizio = LocalDateTime.parse(request.getParameter("data_inizio"));
                LocalDateTime dataFine = LocalDateTime.parse(request.getParameter("data_fine"));
                String luogo = request.getParameter("luogo");
                String note = request.getParameter("note");

                if (dataFine.isBefore(dataInizio)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "La data di fine non può essere prima della data di inizio.");
                    return;
                }

                // Recupera lo stato precedente prima dell'aggiornamento
                String userEmail = (String) request.getSession().getAttribute("userEmail");
                Evento eventoCorrente = eventDAO.getEventDetails(id, userEmail, userRuolo);
                String statoPrecedente = eventoCorrente.getStatus();

                // Esegui aggiornamento
                boolean ok = eventDAO.updateEvent(
                        id, titolo, descrizione, tipoGioco, masterName, maxGiocatori,
                        dataInizio, dataFine, luogo, note
                );

                if (ok) {
                    // Ricarica l'evento per verificare il nuovo stato
                    Evento eventoAggiornato = eventDAO.getEventDetails(id, userEmail, userRuolo);
                    String nuovoStato = eventoAggiornato.getStatus();

                    // Se è passato da "aperto"/"chiuso" a "terminato", cancella le prenotazioni
                    if (!"terminato".equalsIgnoreCase(statoPrecedente) &&
                        "terminato".equalsIgnoreCase(nuovoStato)) {
                        eventDAO.deleteBookingsByEventId(id);
                    }
                }

                response.getWriter().write(ok
                        ? "Evento aggiornato!"
                        : "Errore durante l'aggiornamento.");
            }

            
            // === Prenotazione evento ===
            else if ("book".equalsIgnoreCase(action)) {
                String userEmail = (String) request.getSession().getAttribute("userEmail");
                int id = Integer.parseInt(request.getParameter("id"));
                String notePrenotazione = request.getParameter("notePrenotazione");

                boolean booked = eventDAO.bookEvent(id, userEmail, notePrenotazione);

                response.getWriter().write(booked
                        ? "Prenotazione effettuata!"
                        : "Impossibile prenotare: hai già una prenotazione attiva!");

            }
            // === Azione non riconosciuta ===
            else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Action non valida");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Errore: parametri non validi");
        }
    }
}
