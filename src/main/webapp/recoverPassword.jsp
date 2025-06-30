<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%-- Impostazioni base per la pagina JSP --%>
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <title>Recupero Password - GDRCalendar</title> <%-- Titolo della pagina --%>

  <%-- Importazione del font Montserrat da Google Fonts--%>
  <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;700&display=swap" rel="stylesheet">

  <style>
    /* Applica box-sizing globale */
    * {
      box-sizing: border-box;
    }

    /* Stile generale*/
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

    /*  Contenitore centrale con effetto vetro */
    .container {
      background: rgba(255, 255, 255, 0.15); 
      padding: 50px 60px;
      border-radius: 20px;
      backdrop-filter: blur(10px); 
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
      text-align: center;
      width: 100%;
      max-width: 400px;
      animation: fadeIn 1s ease-out forwards;
      transform: translateY(20px); 
      opacity: 0; 
    }

    /* Titolo principale*/
    h2 {
      margin-bottom: 30px;
      color: #fff;
      font-size: 2em;
      text-shadow: 1px 1px 4px rgba(0,0,0,0.3);
    }

    /* Layout del form*/
    form {
      display: flex;
      flex-direction: column;
      gap: 15px;
    }

    /* Stile per i campi di input */
    input {
      padding: 12px 18px;
      border: none;
      border-radius: 30px;
      font-size: 1em;
      outline: none;
      background: rgba(255, 255, 255, 0.9);
      box-shadow: inset 0 1px 3px rgba(0,0,0,0.1);
      transition: box-shadow 0.3s ease;
    }

    /* Effetto evidenziato al focus sui campi */
    input:focus {
      box-shadow: 0 0 5px rgba(255, 255, 255, 0.7), inset 0 1px 3px rgba(0,0,0,0.1);
    }

    /* Stile del pulsante per inviare il form */
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

     /* Effetto sollevamento al passaggio */
    button:hover {
      background: #f0f0f0;
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(0,0,0,0.15);
    }

    /* Paragrafo per i link */
    p {
      margin-top: 20px;
    }

    /* Stile del link homepage*/
    a {
      color: #fff;
      text-decoration: none;
      font-weight: bold;
      transition: color 0.3s ease;
    }

    /* Cambia colore al passaggio */
    a:hover {
      color: #e0e0e0;
    }

    /* Animazione per far comparire il contenitore */
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
    <%-- Titolo della sezione --%>
    <h2>Recupera Password</h2>

    <%-- Form per il recupero della password, invia l'email a RecoverPasswordServlet--%>
    <form action="RecoverPasswordServlet" method="post">
      <input type="email" name="email" placeholder="Inserisci la tua email" required>
      <button type="submit">Invia link di reset</button>
    </form>

 	<%-- Link per homepage --%>
    <p><a href="index.jsp">Torna alla Home</a></p>
  </div>
</body>
</html>
