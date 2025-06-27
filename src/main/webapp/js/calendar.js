document.addEventListener('DOMContentLoaded', function () {
  // Imposta il mese corrente in formato 'YYYY-MM' e inizializza l'elenco dei tipi di gioco validi
  let currentMonth = new Date().toISOString().slice(0, 7);
  let validGameTypes = [];

  // Crea un contenitore modale per i dettagli dell'evento, inizialmente nascosto
  const detailContainer = document.createElement('div');
  detailContainer.id = 'event-details';
  detailContainer.style.display = 'none';
  detailContainer.style.position = 'fixed';
  detailContainer.style.top = '50%';
  detailContainer.style.left = '50%';
  detailContainer.style.transform = 'translate(-50%, -50%)';
  detailContainer.style.background = '#fff';
  detailContainer.style.padding = '30px';
  detailContainer.style.borderRadius = '16px';
  detailContainer.style.boxShadow = '0 8px 24px rgba(0,0,0,0.2)';
  detailContainer.style.zIndex = '1000';
  detailContainer.style.maxWidth = '450px';
  detailContainer.style.width = '90%';
  detailContainer.style.maxHeight = '80vh';
  detailContainer.style.overflowY = 'auto';
  detailContainer.innerHTML = `<div id="event-content"></div>`;
  document.body.appendChild(detailContainer);

  // Inserisce uno stile CSS per i pulsanti della modale e per l'intestazione del mese
  const style = document.createElement('style');
  style.textContent = `
    .modal-button {
      padding: 10px 18px;
      background-color: #3498db;
      color: white;
      border: none;
      border-radius: 8px;
      font-weight: bold;
      cursor: pointer;
      margin: 5px 0;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      transition: background 0.3s ease;
    }
    .modal-button:hover {
      background-color: #2980b9;
    }

    /* Stile per la visualizzazione mese + anno */
    #current-month {
      font-size: 1.8rem;
      font-weight: bold;
      text-align: center;
      margin: 16px 0;
      text-transform: capitalize;
      display: inline-block;
    }
  `;
  document.head.appendChild(style);

  // Crea un overlay per lo sfondo scuro dietro la modale
  const overlay = document.createElement('div');
  overlay.id = 'event-overlay';
  overlay.style.display = 'none';
  overlay.style.position = 'fixed';
  overlay.style.top = '0';
  overlay.style.left = '0';
  overlay.style.width = '100vw';
  overlay.style.height = '100vh';
  overlay.style.backgroundColor = 'rgba(0, 0, 0, 0.5)';
  overlay.style.zIndex = '999';
  document.body.appendChild(overlay);

  // Richiede i tipi di gioco disponibili e poi carica il calendario per il mese corrente
  fetch("GameTypeServlet?action=list")
    .then(res => res.json())
    .then(data => {
      validGameTypes = data;
      loadCalendar(currentMonth);
    });

  // Funzione per caricare e visualizzare il calendario del mese specificato
  function loadCalendar(month) {
    fetch("CalendarServlet?month=" + month)
      .then(response => response.json())
      .then(data => {
        const container = document.getElementById('calendar');
        container.innerHTML = '';

        const table = document.createElement('table');

        // Intestazione con i giorni della settimana (Lunedì -> Domenica)
        const daysOfWeek = ['Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab', 'Dom'];
        const thead = document.createElement('thead');
        const headRow = document.createElement('tr');

        // Crea un'intestazione di tabella per ciascun giorno della settimana
        daysOfWeek.forEach(day => {
          const th = document.createElement('th');
          th.textContent = day;
          headRow.appendChild(th);
        });
        thead.appendChild(headRow);
        table.appendChild(thead);

        // Inizializza il corpo della tabella
        const tbody = document.createElement('tbody');
        const d = new Date(month + "-01");

        // Calcola il primo giorno visibile nel calendario (settimana che inizia con lunedì)
        const startDay = (d.getDay() + 6) % 7;
        d.setDate(1 - startDay);

        // Ottiene la data odierna per confronto
        const today = new Date();

        // Crea un massimo di 6 righe (settimane)
        for (let week = 0; week < 6; week++) {
          const tr = document.createElement('tr');

          // Crea 7 celle (giorni) per ogni riga
          for (let day = 0; day < 7; day++) {
            const td = document.createElement('td');
            const cellDate = new Date(d); // Copia la data corrente
            const dayNum = cellDate.getDate();
            const cellMonth = cellDate.getMonth() + 1;
            const monthNum = parseInt(month.split('-')[1], 10);

            td.textContent = dayNum;

            if (cellMonth === monthNum) {
              // Stile per i giorni appartenenti al mese corrente
              td.style.background = 'white';
              td.style.color = '#333';

              // Evidenzia con bordo il giorno corrente
              if (
                cellDate.getDate() === today.getDate() &&
                cellDate.getMonth() === today.getMonth() &&
                cellDate.getFullYear() === today.getFullYear()
              ) {
                td.style.border = '3px solid #3498db';
                td.style.borderRadius = '8px';
              }

              const isoDate = cellDate.toISOString().slice(0, 10); // YYYY-MM-DD

              if (data[isoDate]) {
                // Se ci sono eventi in questa data, aggiungili alla cella
                td.classList.add('has-event');

				data[isoDate].forEach(ev => {
				  const div = document.createElement('div');

				  // Calcola durata evento in giorni
				  const inizio = new Date(ev.dataInizio);
				  const fine = new Date(ev.dataFine);
				  const durataGiorni = Math.ceil((fine - inizio) / (1000 * 60 * 60 * 24));

				  // Costruzione testo evento: tipo gioco + titolo + luogo + durata (per eventi con durata maggiore di 1)
				  let testo = `${ev.tipoGioco} - ${ev.titolo} - ${ev.luogo}`;
				  if (durataGiorni >= 1) { 
				    const startDate = inizio.toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' });
				    const endDate = fine.toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' });
				    testo += `\nDal ${startDate} al ${endDate}`;
				  }

				  div.textContent = testo;
				  div.style.whiteSpace = "pre-line"; // per supportare l'andata a capo
				  div.style.cursor = 'pointer';
				  div.style.padding = '2px';
				  div.style.borderRadius = '4px';
				  div.style.marginTop = '4px';

                  // Applica uno stile in base allo stato dell'evento
                  if (ev.isExpired) {
                    div.style.backgroundColor = "#ffcccc";
                    div.style.opacity = "0.6";
                  } else if (ev.isBooked) {
                    div.style.backgroundColor = "#228B22";
                    div.style.color = "white";
                  } else if (ev.isBookable) {
                    div.style.backgroundColor = "#90ee90";
                  } else {
                    div.style.backgroundColor = "#e74c3c";
                  }

                  // Cliccando sull'evento si aprono i dettagli
                  div.addEventListener('click', () => openEventDetails(ev.id));
                  td.appendChild(div);
                });
              }
            } else {
              // Giorni fuori dal mese corrente: sfondo grigio
              td.style.background = '#f0f0f0';
              td.style.color = '#aaa';
            }

            tr.appendChild(td);
            d.setDate(d.getDate() + 1); // Passa al giorno successivo
          }

          tbody.appendChild(tr);
        }

        // Aggiunge il corpo della tabella alla tabella e la tabella al contenitore
        table.appendChild(tbody);
        container.appendChild(table);

        // Calcola e mostra il nome del mese e l’anno una sola volta, in alto
        const dateObj = new Date(month + "-01");
        const monthName = dateObj.toLocaleString('it-IT', { month: 'long' });
        const year = dateObj.getFullYear();
        document.getElementById('current-month').textContent =
          `${monthName.charAt(0).toUpperCase() + monthName.slice(1)} ${year}`;
      });
  }



// Funzione globale per aprire i dettagli di un evento
  window.openEventDetails = function (id) {
    // Recupera i dettagli dell’evento dal server
    fetch("EventServlet?id=" + id)
      .then(res => res.json())
      .then(ev => {
        // Formatter per le date in stile italiano
        const formatter = new Intl.DateTimeFormat('it-IT', {
          dateStyle: 'short',
          timeStyle: 'short'
        });

        // Popola la modale con le informazioni dettagliate dell'evento
        const container = document.getElementById('event-content');
        container.innerHTML = `
          <p><strong>Titolo:</strong> ${ev.titolo}</p>
          <p><strong>Descrizione:</strong> ${ev.descrizione || '-'}</p>
          <p><strong>Tipo Gioco:</strong> ${ev.tipoGioco}</p>
          <p><strong>Master:</strong> ${ev.master}</p>
          <p><strong>Luogo:</strong> ${ev.luogo || '-'}</p>
          <p><strong>Max Giocatori:</strong> ${ev.maxGiocatori}</p>
          <p><strong>Numero Prenotati:</strong> ${ev.numPrenotati}</p>
          <p><strong>Data Inizio:</strong> ${formatter.format(new Date(ev.dataInizio))}</p>
          <p><strong>Data Fine:</strong> ${formatter.format(new Date(ev.dataFine))}</p>
          <p><strong>Note:</strong> ${ev.note || '-'}</p>
          <p><strong>Status:</strong> ${ev.status}</p>
          <div><strong>Lista Prenotati:</strong><ul id="prenotati-list"></ul></div>
          <div id='event-buttons'></div>
        `;

        // Mostra la lista dei prenotati o un messaggio se vuota
        const ul = document.getElementById('prenotati-list');
        if (ev.prenotati && ev.prenotati.length > 0) {
          ev.prenotati.forEach(p => {
            const li = document.createElement('li');
            li.textContent = `${p.email} — ${p.note || ''}`;
            ul.appendChild(li);
          });
        } else {
          const li = document.createElement('li');
          li.textContent = "Nessun prenotato";
          ul.appendChild(li);
        }

        const buttonsContainer = document.getElementById('event-buttons');

        // Se l’utente è un master, mostra i pulsanti di modifica e cancellazione
        if (ev.isMaster) {
          const editBtn = document.createElement('button');
          editBtn.textContent = 'Modifica Evento';
          editBtn.className = 'modal-button';
          editBtn.onclick = () => showEditableForm(ev);
          buttonsContainer.appendChild(editBtn);

          const deleteBtn = document.createElement('button');
          deleteBtn.textContent = 'Elimina Evento';
          deleteBtn.className = 'modal-button';
          deleteBtn.onclick = () => deleteEvent(ev.id);
          buttonsContainer.appendChild(deleteBtn);
        }

        // Se l'evento è prenotato dall'utente, mostra il pulsante per annullare
        if (ev.isBooked) {
          const cancelBtn = document.createElement('button');
          cancelBtn.textContent = 'Annulla Prenotazione';
          cancelBtn.className = 'modal-button';
          cancelBtn.onclick = () => cancelBooking(ev.id);
          buttonsContainer.appendChild(cancelBtn);
        } 
        // Altrimenti, se prenotabile, mostra il pulsante per prenotarsi
        else if (ev.isBookable) {
          const bookBtn = document.createElement('button');
          bookBtn.textContent = 'Prenota';
          bookBtn.className = 'modal-button';
          bookBtn.onclick = () => bookEvent(ev.id);
          buttonsContainer.appendChild(bookBtn);
        }

        // Pulsante per chiudere la modale
        const closeBtn = document.createElement('button');
        closeBtn.textContent = 'Chiudi';
        closeBtn.className = 'modal-button';
        closeBtn.onclick = closeEventDetails;
        buttonsContainer.appendChild(closeBtn);

        // Mostra la modale e l’overlay
        document.getElementById('event-details').style.display = 'block';
        document.getElementById('event-overlay').style.display = 'block';
      });
  };

  // Chiude la modale dei dettagli evento
  window.closeEventDetails = function () {
    document.getElementById('event-details').style.display = 'none';
    document.getElementById('event-overlay').style.display = 'none';
  };

  // Invia richiesta di prenotazione per un evento
  window.bookEvent = function (id) {
    const note = prompt("Inserisci il tuo nome + nome personaggio + livello (se previsto):");
    if (!note || note.trim() === "") {
      alert("Prenotazione annullata: campo obbligatorio!");
      return;
    }
    const data = new URLSearchParams();
    data.append("action", "book");
    data.append("id", id);
    data.append("notePrenotazione", note);
    fetch("EventServlet", {
      method: "POST",
      body: data
    })
      .then(res => res.text())
      .then(msg => {
        alert(msg);
        closeEventDetails();
        location.reload(); // Aggiorna il calendario dopo la prenotazione
      });
  };

  // Annulla una prenotazione dopo conferma
  window.cancelBooking = function (id) {
    if (confirm("Vuoi annullare la prenotazione?")) {
      fetch("EventServlet?action=cancel&id=" + id)
        .then(res => res.text())
        .then(msg => {
          alert(msg);
          closeEventDetails();
          location.reload();
        });
    }
  };

  // Elimina un evento dopo conferma
  window.deleteEvent = function (id) {
    if (confirm("Eliminare evento?")) {
      fetch("EventServlet?action=delete&id=" + id)
        .then(res => res.text())
        .then(msg => {
          alert(msg);
          closeEventDetails();
          location.reload();
        });
    }
  };

  // Mostra un form di modifica precompilato per l’evento
  window.showEditableForm = function (ev) {
    const tipoOptions = validGameTypes.map(type =>
      `<option value="${type}" ${type === ev.tipoGioco ? 'selected' : ''}>${type}</option>`
    ).join('');

    const container = document.getElementById('event-content');
    container.innerHTML = `
      <label>Titolo: <input id="edit-titolo" value="${ev.titolo}"></label><br>
      <label>Descrizione: <input id="edit-descrizione" value="${ev.descrizione || ''}"></label><br>
      <label>Tipo Gioco:
        <select id="edit-tipo">${tipoOptions}</select>
      </label><br>
      <label>Master: <input id="edit-master" value="${ev.master}"></label><br>
      <label>Max Giocatori: <input id="edit-max" type="number" min="1" value="${ev.maxGiocatori}"></label><br>
      <label>Data Inizio: <input id="edit-inizio" type="datetime-local" value="${ev.dataInizio.substring(0, 16)}"></label><br>
      <label>Data Fine: <input id="edit-fine" type="datetime-local" value="${ev.dataFine.substring(0, 16)}"></label><br>
      <label>Luogo: <input id="edit-luogo" value="${ev.luogo || ''}"></label><br>
      <label>Note: <input id="edit-note" value="${ev.note || ''}"></label><br>
      <button onclick="saveEventChanges(${ev.id})">Salva Modifiche</button>
      <button onclick="closeEventDetails()">Chiudi</button>
    `;
  };

  // Salva le modifiche fatte al form dell’evento
  window.saveEventChanges = function (id) {
    const maxInput = document.getElementById('edit-max');
    if (parseInt(maxInput.value) < 1) {
      maxInput.value = 1; // Impedisce valori non validi
    }

    const dataInizio = new Date(document.getElementById('edit-inizio').value);
    const dataFine = new Date(document.getElementById('edit-fine').value);
    if (dataFine < dataInizio) {
      alert("Errore: la data di fine non può essere precedente alla data di inizio.");
      return;
    }

    // Prepara i dati aggiornati e li invia al server
    const data = new URLSearchParams();
    data.append("action", "update");
    data.append("id", id);
    data.append("titolo", document.getElementById('edit-titolo').value);
    data.append("descrizione", document.getElementById('edit-descrizione').value);
    data.append("tipo_gioco", document.getElementById('edit-tipo').value);
    data.append("master", document.getElementById('edit-master').value);
    data.append("max_giocatori", maxInput.value);
    data.append("data_inizio", document.getElementById('edit-inizio').value);
    data.append("data_fine", document.getElementById('edit-fine').value);
    data.append("luogo", document.getElementById('edit-luogo').value);
    data.append("note", document.getElementById('edit-note').value);

    fetch("EventServlet", {
      method: "POST",
      body: data
    })
      .then(res => res.text())
      .then(msg => {
        alert(msg);
        closeEventDetails();
        location.reload();
      });
  };

  // Pulsanti per navigare tra i mesi del calendario
  document.getElementById('prev-month').onclick = () => {
    const d = new Date(currentMonth + "-01");
    d.setMonth(d.getMonth() - 1);
    currentMonth = d.toISOString().slice(0, 7);
    loadCalendar(currentMonth);
  };

  document.getElementById('next-month').onclick = () => {
    const d = new Date(currentMonth + "-01");
    d.setMonth(d.getMonth() + 1);
    currentMonth = d.toISOString().slice(0, 7);
    loadCalendar(currentMonth);
  };
});