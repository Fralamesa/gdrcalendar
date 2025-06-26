package model; // oppure un altro package appropriato come 'util' o 'json'

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Adapter personalizzato per Gson che consente la serializzazione e deserializzazione
 * di oggetti {@link LocalDate}, non supportati nativamente da Gson.
 * 
 * Serializza LocalDate nel formato ISO standard: "yyyy-MM-dd".
 * 
 * Esempio:
 *  - Serializzazione: LocalDate.of(2025, 6, 25) → "2025-06-25"
 *  - Deserializzazione: "2025-06-25" → LocalDate.of(2025, 6, 25)
 */
public class LocalDateAdapter extends TypeAdapter<LocalDate> {

    /**
     * Scrive un LocalDate in formato stringa ISO (yyyy-MM-dd) nel JSON.
     *
     * @param out  writer fornito da Gson
     * @param value valore LocalDate da scrivere
     */
    @Override
    public void write(JsonWriter out, LocalDate value) throws IOException {
        out.value(value.toString()); // formato ISO 8601: "yyyy-MM-dd"
    }

    /**
     * Legge una stringa dal JSON e la converte in un oggetto LocalDate.
     *
     * @param in reader fornito da Gson
     * @return oggetto LocalDate ricostruito dalla stringa
     */
    @Override
    public LocalDate read(JsonReader in) throws IOException {
        return LocalDate.parse(in.nextString());
    }
}
