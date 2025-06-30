package model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modello che rappresenta un evento GDR.
 * Include sia i dati persistenti nel database (titolo, data, luogo, ecc.)
 * sia informazioni dinamiche utili alla logica applicativa (stato, prenotazioni).
 */

public class Evento {

    // === Campi persistenti ===
    private int id;
    private String titolo;
    private String descrizione;
    private String tipoGioco;
    private String master; // Nome del master dell'evento
    private int maxGiocatori;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private String luogo;
    private String note;

    // === Campi gestiti dinamicamente in fase di esecuzione ===
    private int numPrenotati;      // conteggio prenotazioni attuali
    private String status;         // "aperto", "chiuso", "terminato"
    private boolean isBooked;      // True se l'utente ha prenotato
    private boolean isMaster;      // True se l'utente è ha ruolo master
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

    // Getters per i dati statici letti dal db
    
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

    // Getters e setters per i dati dinamici (calcolati a runtime nei DAO in base all'utente e allo stato del sistema)
    
    public int getNumPrenotati() { return numPrenotati; }
    public void setNumPrenotati(int numPrenotati) { this.numPrenotati = numPrenotati; }

    public String getStatus() { return status; } // aperto / chiuso / terminato
    public void setStatus(String status) { this.status = status; }

    public boolean isBooked() { return isBooked; } // true se l'utente ha prenotato
    public void setIsBooked(boolean isBooked) { this.isBooked = isBooked; }

    public boolean isMaster() { return isMaster; } // true se l'utente ha ruolo master
    public void setIsMaster(boolean isMaster) { this.isMaster = isMaster; }

    public List<Prenotazione> getPrenotati() { return prenotati; } // lista utenti iscritti
    public void setPrenotati(List<Prenotazione> prenotati) { this.prenotati = prenotati; }
}

