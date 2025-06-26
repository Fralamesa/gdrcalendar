<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%> 

<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <title>Registrazione - GDRCalendar</title>

    <!-- Importa font personalizzato da Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;700&display=swap" rel="stylesheet">

    <style>
        /* Applica box-sizing a tutti gli elementi per semplificare il layout */
        * {
            box-sizing: border-box;
        }

        /* Stili base del body: centratura, sfondo sfumato e font leggibile */
        body {
            margin: 0;
            padding: 0;
            height: 100vh;
            background: linear-gradient(135deg, #74ebd5 0%, #ACB6E5 100%);
            font-family: 'Montserrat', sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #333;
            overflow: hidden;
        }

        /* Contenitore centrale con effetto vetro e animazione di entrata */
        .container {
            background: rgba(255, 255, 255, 0.15);
            padding: 50px 60px;
            border-radius: 20px;
            backdrop-filter: blur(10px);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
            text-align: center;
            width: 100%;
            max-width: 450px;
            animation: fadeIn 1s ease-out forwards;
            transform: translateY(20px);
            opacity: 0; 
        }

        /* Titolo principale con ombra per contrasto su sfondo chiaro */
        h2 {
            margin-bottom: 30px;
            color: #fff;
            font-size: 2em;
            text-shadow: 1px 1px 4px rgba(0,0,0,0.3);
        }

        /* Layout verticale del form con spaziatura uniforme tra campi */
        form {
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        /* Campi di input e select con aspetto coerente e arrotondato */
        input, select {
            padding: 12px 18px;
            border: none;
            border-radius: 30px;
            font-size: 1em;
            outline: none;
            background: rgba(255, 255, 255, 0.9);
            box-shadow: inset 0 1px 3px rgba(0,0,0,0.1);
            transition: box-shadow 0.3s ease;
        }

        /* Evidenziazione visiva del campo attivo */
        input:focus, select:focus {
            box-shadow: 0 0 5px rgba(255, 255, 255, 0.7), inset 0 1px 3px rgba(0,0,0,0.1);
        }

        /* Stile del bottone di invio: contrasto e reattività */
        button {
            padding: 12px 25px;
            border: none;
            border-radius: 30px;
            background: #ffffff;
            color: #333;
            font-weight: bold;
            font-size: 1em;
            cursor: pointer;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
            transition: all 0.3s ease;
        }

        /* Effetto hover sul bottone per indicare l’interattività */
        button:hover {
            background: #f0f0f0;
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(0,0,0,0.15);
        }

        /* Spaziatura per il paragrafo inferiore */
        p {
            margin-top: 20px;
        }

        /* Link alla home visibile su sfondo chiaro */
        a {
            color: #fff;
            text-decoration: none;
            font-weight: bold;
            transition: color 0.3s ease;
        }

        /* Leggera variazione colore al passaggio del mouse */
        a:hover {
            color: #e0e0e0;
        }

        /* Definizione dell’animazione per l’apparizione fluida del form */
        @keyframes fadeIn {
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <%-- Titolo della sezione di registrazione --%>
        <h2>Registrati</h2>

        <%-- Form che invia i dati alla servlet RegisterServlet con metodo POST --%>
        <form action="RegisterServlet" method="post">
            <input type="text" name="nome" placeholder="Nome" required> <!-- Campo obbligatorio: nome utente -->
            <input type="text" name="cognome" placeholder="Cognome" required> <!-- Campo obbligatorio: cognome utente -->
            <input type="email" name="email" placeholder="Email" required> <!-- Campo email con validazione lato client -->
            <input type="password" name="password" placeholder="Password" required> <!-- Password (mascherata) -->
            <input type="password" name="confirmPassword" placeholder="Conferma Password" required> <!-- Conferma password per coerenza -->

            <%-- Select con elenco di community disponibili (attualmente solo una opzione) --%>
            <select name="community" required>
                <option value="">Seleziona Community</option> <!-- Opzione placeholder -->
                <option value="Arx Draconis">Arx Draconis</option> <!-- Unica community disponibile al momento -->
            </select>

            <button type="submit">Registrati</button> <!-- Bottone per invio dati -->
        </form>

        <%-- Link per tornare alla home page principale (index.jsp) --%>
        <p><a href="index.jsp">Torna alla Home</a></p>
    </div>
</body>
</html>
