package model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Adapter personalizzato per Gson che consente la serializzazione e deserializzazione
 * di oggetti {@link LocalDateTime}, i quali non sono gestiti nativamente da Gson.
 *
 * Il formato usato è quello ISO-8601 standard: "yyyy-MM-dd'T'HH:mm:ss".
 * Esempio:
 *   - Serializzazione: LocalDateTime.of(2025, 7, 5, 18, 30) → "2025-07-05T18:30"
 *   - Deserializzazione: "2025-07-05T18:30" → LocalDateTime.of(2025, 7, 5, 18, 30)
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    /**
     * Serializza un oggetto LocalDateTime come stringa nel formato ISO-8601.
     * Se il valore è null, scrive esplicitamente un valore nullo nel JSON.
     *
     * @param out JsonWriter di Gson
     * @param value valore LocalDateTime da scrivere
     */
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue(); // Scrive "null" se il valore è assente
        } else {
            out.value(value.toString()); // "yyyy-MM-dd'T'HH:mm[:ss]" (ISO-8601)
        }
    }

    /**
     * Legge una stringa in formato ISO-8601 dal JSON e la converte in LocalDateTime.
     *
     * @param in JsonReader di Gson
     * @return oggetto LocalDateTime ottenuto dal parsing della stringa
     */
    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        String s = in.nextString();
        return LocalDateTime.parse(s); // Parsing diretto secondo il formato ISO standard
    }
}
