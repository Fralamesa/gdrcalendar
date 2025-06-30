package model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Adapter per Gson che consente la serializzazione e deserializzazione
 * di oggetti {@link LocalDateTime}, non supportati da Gson.
 */

public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

	
	//Scrive un LocalDateTime come stringa ISO nel JSON.
	
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString());
        }
    }

    // Legge una stringa dal JSON e la converte in un oggetto LocalDateTime.
   
    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        String s = in.nextString();
        return LocalDateTime.parse(s);
    }
}
