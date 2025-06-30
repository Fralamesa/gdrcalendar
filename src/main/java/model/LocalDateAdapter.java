package model; 

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Adapter per Gson per la serializzazione e deserializzazione
 * di oggetti {@link LocalDate}, non supportati da Gson.
 */

public class LocalDateAdapter extends TypeAdapter<LocalDate> {

    
    //Scrive un LocalDate come stringa ISO nel JSON.
  
    @Override
    public void write(JsonWriter out, LocalDate value) throws IOException {
        out.value(value.toString());
    }

    
    // Legge una stringa dal JSON e la converte in un oggetto LocalDate.
    
    @Override
    public LocalDate read(JsonReader in) throws IOException {
        return LocalDate.parse(in.nextString());
    }
}
