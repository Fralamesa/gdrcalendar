package model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modello che rappresenta un evento nel sistema, comprensivo di informazioni statiche
 * (registrate nel database) e dinamiche (calcolate in fase di esecuzione).
 */
public class Evento {

    // === Campi persistenti ===
    private int id;
    private String titolo;
    private String descrizione; // Descrizione estesa dell'evento
    private String tipoGioco;
    private String master; // Nome del master che conduce l'evento
    private int maxGiocatori;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private String luogo;
    private String note; // Note aggiuntive

    // === Campi dinamici (non salvati nel DB, ma gestiti a runtime) ===
    private int numPrenotati;      // Numero di giocatori attualmente prenotati
    private String status;         // Stato dell'evento: "aperto", "chiuso", "terminato"
    private boolean isBooked;      // True se l'utente corrente ha già prenotato
    private boolean isMaster;      // True se l'utente corrente è un master
    private List<Prenotazione> prenotati; // Lista degli utenti prenotati

    /**
     * Costruttore principale. Inizializza i campi base a partire dai dati letti dal database.
     */
    public Evento(int id, String titolo, String descrizione, String tipoGioco, String master,
                  int maxGiocatori, LocalDateTime dataInizio, LocalDateTime dataFine,
                  String luogo, String note) {
        this.id = id;
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.tipoGioco = tipoGioco;
        this.master = master;
        this.maxGiocatori = maxGiocatori;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.luogo = luogo;
        this.note = note;
    }

    // === Getters per i campi persistenti ===
    public int getId() { return id; }
    public String getTitolo() { return titolo; }
    public String getDescrizione() { return descrizione; }
    public String getTipoGioco() { return tipoGioco; }
    public String getMaster() { return master; }
    public int getMaxGiocatori() { return maxGiocatori; }
    public LocalDateTime getDataInizio() { return dataInizio; }
    public LocalDateTime getDataFine() { return dataFine; }
    public String getLuogo() { return luogo; }
    public String getNote() { return note; }

    // === Getters e setters per i campi dinamici ===
    public int getNumPrenotati() { return numPrenotati; }
    public void setNumPrenotati(int numPrenotati) { this.numPrenotati = numPrenotati; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isBooked() { return isBooked; }
    public void setIsBooked(boolean isBooked) { this.isBooked = isBooked; }

    public boolean isMaster() { return isMaster; }
    public void setIsMaster(boolean isMaster) { this.isMaster = isMaster; }

    public List<Prenotazione> getPrenotati() { return prenotati; }
    public void setPrenotati(List<Prenotazione> prenotati) { this.prenotati = prenotati; }
}

